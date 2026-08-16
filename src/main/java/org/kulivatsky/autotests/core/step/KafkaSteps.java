package org.kulivatsky.autotests.core.step;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.kulivatsky.autotests.core.step.Params.params;

public final class KafkaSteps {
  private KafkaSteps() {
  }

  public static <K, V> SupplierStep<RecordMetadata> send(final Producer<K, V> producer,
                                                          final String topic,
                                                          final K key,
                                                          final V value) {
    return send(producer, topic, key, value, Duration.ofSeconds(10));
  }

  public static <K, V> SupplierStep<RecordMetadata> send(final Producer<K, V> producer,
                                                          final String topic,
                                                          final K key,
                                                          final V value,
                                                          final Duration timeout) {
    return new SupplierStep.Of<>(
      "Publish Kafka record",
      params("topic", topic, "key", key),
      () -> producer.send(new ProducerRecord<>(topic, key, value))
        .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
    );
  }
}
