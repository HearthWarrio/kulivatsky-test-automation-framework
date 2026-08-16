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

public final class Kafka {
  private final Properties properties;
  private final Duration timeout;

  private Kafka(final Properties properties, final Duration timeout) {
    this.properties = new Properties();
    this.properties.putAll(properties);
    this.timeout = timeout;
  }

  public static Kafka of(final Properties properties) {
    return new Kafka(properties, Duration.ofSeconds(10));
  }

  public static Kafka of(final Properties properties, final Duration timeout) {
    return new Kafka(properties, timeout);
  }

  public RecordMetadata send(final String topic, final String key, final String value) {
    try (KafkaProducer<String, String> producer = new KafkaProducer<>(this.properties)) {
      return producer.send(new ProducerRecord<>(topic, key, value))
        .get(this.timeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (final Exception ex) {
      throw new IllegalStateException("Kafka send failed for topic " + topic, ex);
    }
  }

  public List<ConsumerRecord<String, String>> poll(final String topic, final Duration pollDuration) {
    final Properties consumerProperties = new Properties();
    consumerProperties.putAll(this.properties);
    consumerProperties.putIfAbsent(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProperties)) {
      consumer.subscribe(List.of(topic));
      final ConsumerRecords<String, String> records = consumer.poll(pollDuration);
      final List<ConsumerRecord<String, String>> result = new ArrayList<>();
      records.forEach(result::add);
      return result;
    }
  }

  public Properties getProperties() {
    return properties();
  }

  public Properties properties() {
    final Properties copy = new Properties();
    copy.putAll(this.properties);
    return copy;
  }
}
