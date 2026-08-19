package org.kulivatsky.autotests.core.storage;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.kulivatsky.autotests.core.config.Service;
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
   * @param service logical Kafka service
   * @param env target environment
   * @return facade that creates and closes producers/consumers per operation
   */
  public static Kafka kafkaFor(final Service service, final Env env) {
    return Kafka.of(producerPropertiesFor(service, env), consumerPropertiesFor(service, env));
  }

  /**
   * Creates a {@link Kafka} facade with a unique consumer group id for this call.
   *
   * <p>The configured group id is used as a readable prefix and an eight-character UUID fragment is
   * appended. This avoids one test consuming offsets that another parallel test expected to see.</p>
   *
   * @param service logical Kafka service
   * @param env target environment
   * @return facade configured with an isolated consumer group id
   */
  public static Kafka kafkaForWithUniqueGroupId(final Service service, final Env env) {
    final Properties producerProperties = producerPropertiesFor(service, env);
    final Properties consumerProperties = consumerPropertiesFor(service, env);
    final String baseGroup = consumerProperties.getProperty(ConsumerConfig.GROUP_ID_CONFIG, "autotests");
    consumerProperties.put(
      ConsumerConfig.GROUP_ID_CONFIG,
      baseGroup + "-" + UUID.randomUUID().toString().substring(0, 8)
    );
    return Kafka.of(producerProperties, consumerProperties);
  }

  /**
   * Creates a string-key/string-value producer.
   *
   * <p>The caller owns the returned client and must close it.</p>
   *
   * @param service logical Kafka service
   * @param env target environment
   * @return new producer instance
   */
  public static Producer<String, String> producerFor(final Service service, final Env env) {
    return new KafkaProducer<>(producerPropertiesFor(service, env));
  }

  /**
   * Creates a string-key/string-value consumer.
   *
   * <p>The caller owns the returned client and must close it.</p>
   *
   * @param service logical Kafka service
   * @param env target environment
   * @return new consumer instance
   */
  public static Consumer<String, String> consumerFor(final Service service, final Env env) {
    return new KafkaConsumer<>(consumerPropertiesFor(service, env));
  }

  /**
   * Builds a compatibility property set containing both producer and consumer settings.
   *
   * <p>Existing callers that construct the short-lived {@link Kafka} facade can keep using this
   * method. Direct producers and consumers use the narrower methods below so each client receives
   * only settings relevant to its role.</p>
   *
   * @param service logical Kafka service
   * @param env target environment
   * @return fresh mutable properties object
   */
  public static Properties propertiesFor(final Service service, final Env env) {
    final Properties properties = producerPropertiesFor(service, env);
    properties.putAll(consumerPropertiesFor(service, env));
    return properties;
  }

  /**
   * Builds properties required by a string-key/string-value Kafka producer.
   *
   * @param service logical Kafka service
   * @param env target environment
   * @return fresh producer properties
   */
  public static Properties producerPropertiesFor(final Service service, final Env env) {
    final Config config = Config.current();
    final String prefix = "kafka.%s.%s".formatted(service.id(), normalize(env));
    final Properties properties = commonProperties(config, prefix);
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    return properties;
  }

  /**
   * Builds properties required by a string-key/string-value Kafka consumer.
   *
   * @param service logical Kafka service
   * @param env target environment
   * @return fresh consumer properties
   */
  public static Properties consumerPropertiesFor(final Service service, final Env env) {
    final Config config = Config.current();
    final String prefix = "kafka.%s.%s".formatted(service.id(), normalize(env));
    final Properties properties = commonProperties(config, prefix);
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, config.property(prefix + ".groupId", "autotests"));
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, config.property(prefix + ".autoOffsetReset", "earliest"));
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    return properties;
  }

  /** Builds settings shared by producer and consumer clients. */
  private static Properties commonProperties(final Config config, final String prefix) {
    final Properties properties = new Properties();
    properties.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, config.property(prefix + ".bootstrapServers"));
    properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, config.property(prefix + ".securityProtocol", "PLAINTEXT"));
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
