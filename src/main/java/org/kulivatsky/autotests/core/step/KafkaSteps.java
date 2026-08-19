package org.kulivatsky.autotests.core.step;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.kulivatsky.autotests.core.step.Params.params;
import static org.kulivatsky.autotests.core.step.Params.secret;

/** Step descriptors for publishing records through an existing Kafka producer. */
public final class KafkaSteps {
  /** No instances. */
  private KafkaSteps() {
  }

  /**
   * Creates a synchronous publish step with a ten-second acknowledgement timeout.
   *
   * @param producer producer owned by the caller; this step never closes it
   * @param topic destination topic
   * @param key record key
   * @param value record value
   * @param <K> key type
   * @param <V> value type
   * @return lazy step that produces broker metadata after acknowledgement
   */
  public static <K, V> SupplierStep<RecordMetadata> send(final Producer<K, V> producer,
                                                          final String topic,
                                                          final K key,
                                                          final V value) {
    return send(producer, topic, key, value, Duration.ofSeconds(10));
  }

  /**
   * Creates a synchronous publish step with an explicit acknowledgement timeout.
   *
   * <p>{@link Producer#send(ProducerRecord)} is asynchronous, but the step waits on the returned
   * future so a successful Allure step means the broker acknowledged the record. Producer
   * ownership remains with the caller. The record key is masked in report parameters because Kafka
   * keys frequently contain business identifiers.</p>
   *
   * @param producer producer used to send the record; not closed by the step
   * @param topic destination topic
   * @param key record key
   * @param value record value
   * @param timeout maximum time to wait for broker acknowledgement
   * @param <K> key type
   * @param <V> value type
   * @return lazy step producing {@link RecordMetadata}
   */
  public static <K, V> SupplierStep<RecordMetadata> send(final Producer<K, V> producer,
                                                          final String topic,
                                                          final K key,
                                                          final V value,
                                                          final Duration timeout) {
    final Duration checkedTimeout = Objects.requireNonNull(timeout, "timeout");
    if (checkedTimeout.isZero() || checkedTimeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
    return new SupplierStep.Of<>(
      "Publish Kafka record",
      params("topic", topic, "key", secret(key)),
      () -> {
        try {
          return producer.send(new ProducerRecord<>(topic, key, value))
            .get(checkedTimeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (final InterruptedException exception) {
          Thread.currentThread().interrupt();
          throw exception;
        }
      }
    );
  }
}
