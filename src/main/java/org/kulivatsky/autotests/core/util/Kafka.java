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
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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
  /** Defensive copy of Kafka client properties. */
  private final Properties properties;
  /** Maximum acknowledgement wait used by {@link #send(String, String, String)}. */
  private final Duration timeout;

  /**
   * Creates a facade from a defensive copy of client properties.
   *
   * @param properties Kafka client properties
   * @param timeout producer acknowledgement timeout
   */
  private Kafka(final Properties properties, final Duration timeout) {
    this.properties = new Properties();
    this.properties.putAll(properties);
    this.timeout = timeout;
  }

  /**
   * Creates a facade with a ten-second producer acknowledgement timeout.
   *
   * @param properties Kafka client properties; copied immediately
   * @return Kafka facade
   */
  public static Kafka of(final Properties properties) {
    return new Kafka(properties, Duration.ofSeconds(10));
  }

  /**
   * Creates a facade with an explicit producer acknowledgement timeout.
   *
   * @param properties Kafka client properties; copied immediately
   * @param timeout maximum wait for producer acknowledgement
   * @return Kafka facade
   */
  public static Kafka of(final Properties properties, final Duration timeout) {
    return new Kafka(properties, timeout);
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
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(this.properties)) {
      return producer.send(new ProducerRecord<>(topic, key, value))
        .get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
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
    if (pollDuration.isZero() || pollDuration.isNegative()) {
      throw new IllegalArgumentException("pollDuration must be positive");
    }
    final Properties consumerProperties = new Properties();
    consumerProperties.putAll(this.properties);
    consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    final long deadline = System.nanoTime() + pollDuration.toNanos();
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {
      consumer.subscribe(List.of(topic));
      while (true) {
        final long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
          return List.of();
        }
        final Duration slice = Duration.ofNanos(Math.min(remainingNanos, Duration.ofMillis(250).toNanos()));
        final ConsumerRecords<String, String> records = consumer.poll(slice);
        if (!records.isEmpty()) {
          final List<ConsumerRecord<String, String>> result = new ArrayList<>(records.count());
          records.forEach(result::add);
          return result;
        }
      }
    }
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
    copy.putAll(this.properties);
    return copy;
  }
}
