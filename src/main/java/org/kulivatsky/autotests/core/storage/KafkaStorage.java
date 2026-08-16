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

/** Creates Kafka clients and client properties from framework configuration. */
public final class KafkaStorage {
  /** No instances. */
  private KafkaStorage() {
  }

  /**
   * Creates a short-lived {@link Kafka} facade from the configured service/environment properties.
   *
   * @param apiService logical Kafka service
   * @param env target environment
   * @return facade that creates and closes producers/consumers per operation
   */
  public static Kafka kafkaFor(final APIService apiService, final Env env) {
    return Kafka.of(propertiesFor(apiService, env));
  }

  /**
   * Creates a {@link Kafka} facade with a unique consumer group id for this call.
   *
   * <p>The configured group id is used as a readable prefix and an eight-character UUID fragment is
   * appended. This avoids one test consuming offsets that another parallel test expected to see.</p>
   *
   * @param apiService logical Kafka service
   * @param env target environment
   * @return facade configured with an isolated consumer group id
   */
  public static Kafka kafkaForWithUniqueGroupId(final APIService apiService, final Env env) {
    final Properties properties = propertiesFor(apiService, env);
    final String baseGroup = properties.getProperty(ConsumerConfig.GROUP_ID_CONFIG, "autotests");
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, baseGroup + "-" + UUID.randomUUID().toString().substring(0, 8));
    return Kafka.of(properties);
  }

  /**
   * Creates a string-key/string-value producer.
   *
   * <p>The caller owns the returned client and must close it.</p>
   *
   * @param apiService logical Kafka service
   * @param env target environment
   * @return new producer instance
   */
  public static Producer<String, String> producerFor(final APIService apiService, final Env env) {
    return new KafkaProducer<>(propertiesFor(apiService, env));
  }

  /**
   * Creates a string-key/string-value consumer.
   *
   * <p>The caller owns the returned client and must close it.</p>
   *
   * @param apiService logical Kafka service
   * @param env target environment
   * @return new consumer instance
   */
  public static Consumer<String, String> consumerFor(final APIService apiService, final Env env) {
    return new KafkaConsumer<>(propertiesFor(apiService, env));
  }

  /**
   * Builds Kafka client properties for a service/environment pair.
   *
   * <p>The same property set contains producer serializers and consumer deserializers so it can be
   * used to construct either client type. Only string keys and values are configured by default.</p>
   *
   * @param apiService logical Kafka service
   * @param env target environment
   * @return fresh mutable properties object
   */
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

  /**
   * Converts a service/environment identifier to a stable key fragment.
   *
   * @param value identifier
   * @return lower-case text using {@link Locale#ROOT}
   */
  private static String normalize(final Object value) {
    return value.toString().toLowerCase(Locale.ROOT);
  }
}
