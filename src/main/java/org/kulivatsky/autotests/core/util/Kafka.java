package org.kulivatsky.autotests.core.util;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Small string-key/string-value Kafka facade for tests that do not need long-lived clients.
 *
 * <p>Every {@link #send(String, String, String)} call creates and closes its own producer. Every
 * {@link #poll(String, Duration)} call creates, subscribes, polls, and closes its own consumer.
 * This keeps ownership simple at the cost of connection setup per operation. Tests that need to
 * reuse a client should obtain one from
 * {@link org.kulivatsky.autotests.core.storage.KafkaStorage} instead.</p>
 */
public final class Kafka {
  /** Defensive producer properties used by short-lived send operations. */
  private final Properties producerProperties;
  /** Defensive consumer properties used by short-lived poll operations. */
  private final Properties consumerProperties;
  /** Maximum acknowledgement wait used by {@link #send(String, String, String)}. */
  private final Duration timeout;

  /**
   * Creates a facade from defensive copies of producer and consumer properties.
   *
   * @param producerProperties properties used by short-lived producers
   * @param consumerProperties properties used by short-lived consumers
   * @param timeout producer acknowledgement timeout
   */
  private Kafka(final Properties producerProperties,
                final Properties consumerProperties,
                final Duration timeout) {
    this.producerProperties = copyOf(producerProperties, "producerProperties");
    this.consumerProperties = copyOf(consumerProperties, "consumerProperties");
    this.timeout = requirePositive(timeout, "timeout");
  }

  /**
   * Creates a facade with a ten-second producer acknowledgement timeout.
   *
   * @param properties Kafka client properties; copied immediately
   * @return Kafka facade
   */
  public static Kafka of(final Properties properties) {
    return new Kafka(properties, properties, Duration.ofSeconds(10));
  }

  /**
   * Creates a facade with separate producer and consumer settings.
   *
   * <p>This form prevents producer-only serializers and consumer-only deserializers/group settings
   * from being passed to the wrong Kafka client. Both inputs are copied immediately.</p>
   *
   * @param producerProperties properties used only to construct producers
   * @param consumerProperties properties used only to construct consumers
   * @return Kafka facade with a ten-second producer acknowledgement timeout
   */
  public static Kafka of(final Properties producerProperties, final Properties consumerProperties) {
    return new Kafka(producerProperties, consumerProperties, Duration.ofSeconds(10));
  }

  /**
   * Creates a facade with an explicit producer acknowledgement timeout.
   *
   * @param properties Kafka client properties; copied immediately
   * @param timeout maximum wait for producer acknowledgement
   * @return Kafka facade
   */
  public static Kafka of(final Properties properties, final Duration timeout) {
    return new Kafka(properties, properties, timeout);
  }

  /**
   * Sends one record and waits synchronously for broker acknowledgement.
   *
   * <p>The producer is always closed before this method returns. A successful return therefore
   * includes both acknowledged send and producer cleanup.</p>
   *
   * @param topic destination topic
   * @param key record key
   * @param value record value
   * @return broker metadata for the acknowledged record
   * @throws IllegalStateException when client creation, send, acknowledgement wait, or close fails
   */
  public RecordMetadata send(final String topic, final String key, final String value) {
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(this.producerProperties)) {
      return producer.send(new ProducerRecord<>(topic, key, value))
        .get(this.timeout.toNanos(), TimeUnit.NANOSECONDS);
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Kafka send interrupted for topic " + topic, exception);
    } catch (final Exception exception) {
      throw new IllegalStateException("Kafka send failed for topic " + topic, exception);
    }
  }

  /**
   * Subscribes a temporary consumer and waits up to the supplied duration for at least one record.
   *
   * <p>Kafka group assignment commonly needs an initial poll before records can be returned. This
   * method therefore polls repeatedly in short slices until a record arrives or the overall
   * deadline expires. The temporary consumer is closed before the method returns.</p>
   *
   * @param topic topic to subscribe to
   * @param pollDuration total time allowed for assignment and polling
   * @return first non-empty batch observed before the deadline, or an empty list on timeout
   * @throws IllegalArgumentException when {@code pollDuration} is zero or negative
   */
  public List<ConsumerRecord<String, String>> poll(final String topic, final Duration pollDuration) {
    return poll(topic, record -> true, pollDuration);
  }

  /**
   * Waits for records that satisfy a caller-supplied correlation predicate.
   *
   * <p>Non-matching records are consumed by the temporary consumer but are not returned and do not
   * end the wait. Auto-commit is forcibly disabled for this short-lived consumer, so merely looking
   * past unrelated records cannot advance a shared consumer group's committed offset. The facade
   * does not commit offsets at all; callers that need durable group progress should own a long-lived
   * consumer from {@code KafkaStorage.consumerFor(...)} instead.</p>
   *
   * @param topic topic to subscribe to
   * @param predicate correlation predicate applied to each consumed record
   * @param pollDuration total time allowed for assignment and polling
   * @return matching records from the first batch containing at least one match, or an empty list on timeout
   * @throws IllegalArgumentException when {@code pollDuration} is zero or negative
   */
  public List<ConsumerRecord<String, String>> poll(final String topic,
                                                    final Predicate<ConsumerRecord<String, String>> predicate,
                                                    final Duration pollDuration) {
    Objects.requireNonNull(topic, "topic");
    Objects.requireNonNull(predicate, "predicate");
    final Duration timeout = requirePositive(pollDuration, "pollDuration");
    final Properties consumerProperties = new Properties();
    consumerProperties.putAll(this.consumerProperties);
    consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    final long timeoutNanos = timeoutNanos(timeout);
    final long startedAt = System.nanoTime();
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {
      consumer.subscribe(List.of(topic));
      while (true) {
        final long elapsedNanos = System.nanoTime() - startedAt;
        final long remainingNanos = timeoutNanos - elapsedNanos;
        if (remainingNanos <= 0) {
          return List.of();
        }
        final Duration slice = Duration.ofNanos(Math.min(remainingNanos, Duration.ofMillis(250).toNanos()));
        final ConsumerRecords<String, String> records = consumer.poll(slice);
        final List<ConsumerRecord<String, String>> result = new ArrayList<>();
        records.forEach(record -> {
          if (predicate.test(record)) {
            result.add(record);
          }
        });
        if (!result.isEmpty()) {
          return result;
        }
      }
    }
  }

  /** Converts a positive duration to nanoseconds and rejects values that cannot fit in a {@code long}. */
  private static long timeoutNanos(final Duration duration) {
    try {
      return duration.toNanos();
    } catch (final ArithmeticException ex) {
      throw new IllegalArgumentException("pollDuration is too large", ex);
    }
  }

  /** Copies a caller-owned properties object before it can be mutated elsewhere. */
  private static Properties copyOf(final Properties source, final String name) {
    final Properties copy = new Properties();
    copy.putAll(Objects.requireNonNull(source, name));
    return copy;
  }

  /** Validates a timeout shared by producer and consumer operations. */
  private static Duration requirePositive(final Duration duration, final String name) {
    final Duration checked = Objects.requireNonNull(duration, name);
    if (checked.isZero() || checked.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
    return checked;
  }

  /**
   * Alias for {@link #properties()} retained for bean-style callers.
   *
   * @return defensive copy of client properties
   */
  public Properties getProperties() {
    return properties();
  }

  /**
   * Returns a defensive copy of the stored Kafka properties.
   *
   * @return independent mutable properties object
   */
  public Properties properties() {
    final Properties copy = new Properties();
    copy.putAll(this.producerProperties);
    copy.putAll(this.consumerProperties);
    return copy;
  }
}
