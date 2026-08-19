package org.kulivatsky.autotests.demo;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.core.config.Env;
import org.kulivatsky.autotests.core.config.Service;
import org.kulivatsky.autotests.core.storage.DBConnectionPoolStorage;
import org.kulivatsky.autotests.core.storage.KafkaStorage;
import org.kulivatsky.autotests.core.util.AwaitilityUtils;
import org.kulivatsky.autotests.core.util.Database;
import org.kulivatsky.autotests.core.util.Kafka;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kulivatsky.autotests.core.config.TestLevel.Option.INTEGRATION;
import static org.kulivatsky.autotests.core.config.TestSubject.Option.BACKEND;
import static org.kulivatsky.autotests.core.config.TestType.Option.REGRESSION;
import static org.kulivatsky.autotests.core.step.DatabaseSteps.executeSelectOne;
import static org.kulivatsky.autotests.core.step.DatabaseSteps.executeUpdate;
import static org.kulivatsky.autotests.core.step.KafkaSteps.send;
import static org.kulivatsky.autotests.core.step.Steps.step;

/**
 * Backend integration example against real PostgreSQL and Kafka instances managed by Testcontainers.
 *
 * <p>The test uses the same configuration-driven storages a consuming project would use. Container
 * connection details are injected as JVM properties only for the duration of the test.</p>
 */
public final class ComplexBackendTest {
  /** Logical database service used to build framework configuration keys. */
  private static final Service DATABASE_SERVICE = Service.of("demo_database");
  /** Logical Kafka service used to build framework configuration keys. */
  private static final Service KAFKA_SERVICE = Service.of("demo_kafka");
  /** Topic isolated inside the per-test Kafka container. */
  private static final String TOPIC = "demo.orders";

  /** Disposable PostgreSQL instance. */
  private PostgreSQLContainer postgres;
  /** Disposable Apache Kafka instance. */
  private KafkaContainer kafkaContainer;
  /** Shared-pool facade created through the framework storage. */
  private Database database;
  /** Caller-owned producer created through the framework storage. */
  private Producer<String, String> producer;
  /** Test-owned scheduler used to simulate eventual consistency. */
  private ScheduledExecutorService scheduler;

  /** Starts real infrastructure and wires its dynamic addresses into framework configuration. */
  @BeforeMethod
  public void setUp() {
    this.postgres = new PostgreSQLContainer("postgres:16-alpine")
      .withDatabaseName("demo")
      .withUsername("demo")
      .withPassword("demo");
    this.postgres.start();

    this.kafkaContainer = new KafkaContainer("apache/kafka-native:3.8.0");
    this.kafkaContainer.start();

    setDatabaseProperty("driver", "org.postgresql.Driver");
    setDatabaseProperty("url", this.postgres.getJdbcUrl());
    setDatabaseProperty("username", this.postgres.getUsername());
    setDatabaseProperty("password", this.postgres.getPassword());
    setDatabaseProperty("maxTotal", "4");
    setKafkaProperty("bootstrapServers", this.kafkaContainer.getBootstrapServers());
    setKafkaProperty("securityProtocol", "PLAINTEXT");
    setKafkaProperty("groupId", "demo-autotests");
    setKafkaProperty("autoOffsetReset", "earliest");
    createKafkaTopic();

    this.database = DBConnectionPoolStorage.databaseFor(DATABASE_SERVICE, Env.Option.LOCAL);
    this.database.execute("create table orders (id bigint primary key, status varchar(32) not null)");
    this.producer = KafkaStorage.producerFor(KAFKA_SERVICE, Env.Option.LOCAL);
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
  }

  /** Releases framework pools, Kafka clients, executors, containers, and temporary properties. */
  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    if (this.scheduler != null) {
      this.scheduler.shutdownNow();
    }
    if (this.producer != null) {
      this.producer.close();
    }
    DBConnectionPoolStorage.closeAll();
    clearInfrastructureProperties();
    if (this.kafkaContainer != null) {
      this.kafkaContainer.stop();
    }
    if (this.postgres != null) {
      this.postgres.stop();
    }
  }

  /**
   * Persists an order, observes an asynchronous database transition, publishes a real Kafka record,
   * and consumes it through the short-lived framework Kafka facade.
   */
  @Test(groups = "container")
  @TestInfo(
    name = "Persist an order and publish its event",
    testType = REGRESSION,
    testSubject = BACKEND,
    testLevel = INTEGRATION,
    project = "demo",
    service = {"demo_database", "demo_kafka"}
  )
  public void persistOrderAndPublishEvent() {
    final int updated = step(executeUpdate(
      this.database,
      "insert into orders(id, status) values (?, ?)",
      501L,
      "CREATED"
    ));
    step("One database row is inserted", () -> assertThat(updated).isEqualTo(1));

    this.scheduler.schedule(
      () -> this.database.executeUpdate("update orders set status = ? where id = ?", "PUBLISHED", 501L),
      250,
      TimeUnit.MILLISECONDS
    );
    AwaitilityUtils.waitForAssertTrue(() -> {
      final Map<String, Object> row = this.database.selectOne(
        "select status as \"status\" from orders where id = ?",
        501L
      );
      return "PUBLISHED".equals(row.get("status"));
    }, Duration.ofSeconds(5));

    final Map<String, Object> row = step(executeSelectOne(
      this.database,
      "select id as \"id\", status as \"status\" from orders where id = ?",
      501L
    ));
    step("Stored order has expected state", () -> {
      assertThat(row.get("id")).isEqualTo(501L);
      assertThat(row.get("status")).isEqualTo("PUBLISHED");
    });

    step(send(this.producer, TOPIC, "501", "{\"id\":501,\"status\":\"PUBLISHED\"}"));
    final Kafka kafka = KafkaStorage.kafkaForWithUniqueGroupId(KAFKA_SERVICE, Env.Option.LOCAL);
    final List<ConsumerRecord<String, String>> records = step(
      "Consume published Kafka record",
      () -> kafka.poll(TOPIC, Duration.ofSeconds(10))
    );
    step("Published event contains the order state", () -> {
      assertThat(records).hasSize(1);
      assertThat(records.getFirst().key()).isEqualTo("501");
      assertThat(records.getFirst().value()).contains("\"status\":\"PUBLISHED\"");
    });
  }

  /** Creates the topic explicitly so the example does not depend on broker auto-creation settings. */
  private void createKafkaTopic() {
    final Properties properties = new Properties();
    properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, this.kafkaContainer.getBootstrapServers());
    try (AdminClient adminClient = AdminClient.create(properties)) {
      adminClient.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1)))
        .all()
        .get(10, TimeUnit.SECONDS);
    } catch (final InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while creating Kafka topic " + TOPIC, exception);
    } catch (final Exception exception) {
      throw new IllegalStateException("Cannot create Kafka topic " + TOPIC, exception);
    }
  }

  /** Sets one database property for the demo service and local environment. */
  private static void setDatabaseProperty(final String suffix, final String value) {
    System.setProperty("db.demo_database.local." + suffix, value);
  }

  /** Sets one Kafka property for the demo service and local environment. */
  private static void setKafkaProperty(final String suffix, final String value) {
    System.setProperty("kafka.demo_kafka.local." + suffix, value);
  }

  /** Removes every JVM property introduced by {@link #setUp()}. */
  private static void clearInfrastructureProperties() {
    for (final String suffix : List.of("driver", "url", "username", "password", "maxTotal")) {
      System.clearProperty("db.demo_database.local." + suffix);
    }
    for (final String suffix : List.of("bootstrapServers", "securityProtocol", "groupId", "autoOffsetReset")) {
      System.clearProperty("kafka.demo_kafka.local." + suffix);
    }
  }
}
