package org.kulivatsky.autotests.core.storage;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.kulivatsky.autotests.core.config.APIService;
import org.kulivatsky.autotests.core.config.Config;
import org.kulivatsky.autotests.core.config.Env;
import org.kulivatsky.autotests.core.util.Kafka;

import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

public final class KafkaStorage {
  private KafkaStorage() {
  }

  public static Kafka kafkaFor(final APIService apiService, final Env env) {
    return Kafka.of(propertiesFor(apiService, env));
  }

  public static Kafka kafkaForWithUniqueGroupId(final APIService apiService, final Env env) {
    final Properties properties = propertiesFor(apiService, env);
    final String baseGroup = properties.getProperty(ConsumerConfig.GROUP_ID_CONFIG, "autotests");
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, baseGroup + "-" + UUID.randomUUID().toString().substring(0, 8));
    return Kafka.of(properties);
  }

  public static Producer<String, String> producerFor(final APIService apiService, final Env env) {
    return new KafkaProducer<>(propertiesFor(apiService, env));
  }

  public static Consumer<String, String> consumerFor(final APIService apiService, final Env env) {
    return new KafkaConsumer<>(propertiesFor(apiService, env));
  }

  public static Properties propertiesFor(final APIService apiService, final Env env) {
    final Config config = Config.current();
    final String prefix = "kafka.%s.%s".formatted(normalize(apiService), normalize(env));
    final Properties properties = new Properties();
    properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, config.property(prefix + ".bootstrapServers"));
    properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, config.property(prefix + ".securityProtocol", "PLAINTEXT"));
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, config.property(prefix + ".groupId", "autotests"));
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.property(prefix + ".autoOffsetReset", "earliest"));
    properties.put(org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    return properties;
  }

  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
