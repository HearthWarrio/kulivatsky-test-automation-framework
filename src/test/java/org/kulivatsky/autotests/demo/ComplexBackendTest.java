package org.kulivatsky.autotests.demo;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.kulivatsky.autotests.core.annotation.TestInfo;
import org.kulivatsky.autotests.core.util.AwaitilityUtils;
import org.kulivatsky.autotests.core.util.Database;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.kulivatsky.autotests.core.config.APIService.Option.DEMO_DATABASE;
import static org.kulivatsky.autotests.core.config.APIService.Option.DEMO_KAFKA;
import static org.kulivatsky.autotests.core.config.Project.Option.DEMO;
import static org.kulivatsky.autotests.core.config.TestLevel.Option.INTEGRATION;
import static org.kulivatsky.autotests.core.config.TestSubject.Option.BACKEND;
import static org.kulivatsky.autotests.core.config.TestType.Option.REGRESSION;
import static org.kulivatsky.autotests.core.step.CommonSteps.executeSelectOne;
import static org.kulivatsky.autotests.core.step.CommonSteps.executeUpdate;
import static org.kulivatsky.autotests.core.step.KafkaSteps.send;
import static org.kulivatsky.autotests.core.step.Steps.step;

/**
 * Backend integration example combining JDBC persistence, a Kafka producer, polling, and explicit
 * report steps without requiring external infrastructure.
 */
public final class ComplexBackendTest {
  /** Owning facade for the per-test in-memory H2 connection pool. */
  private Database database;
  /** In-memory Kafka producer used to demonstrate the producer step contract. */
  private MockProducer<String, String> producer;

  /** Creates isolated H2 state and a Kafka mock producer before each test method. */
  @BeforeMethod
  public void setUp() {
    final BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName("org.h2.Driver");
    dataSource.setUrl("jdbc:h2:mem:backend_demo;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    this.database = Database.of(dataSource);
    this.database.execute("drop table if exists orders");
    this.database.execute("create table orders (id bigint primary key, status varchar(32) not null)");
    this.producer = new MockProducer<>(true, new StringSerializer(), new StringSerializer());
  }

  /** Closes both test-owned resources even after a failed test body. */
  @AfterMethod(alwaysRun = true)
  public void tearDown() {
    if (this.producer != null) {
      this.producer.close();
    }
    if (this.database != null) {
      this.database.close();
    }
  }

  /**
   * Demonstrates a backend flow that persists an entity, checks stored state, publishes an event,
   * and verifies the observable producer history.
   */
  @Test
  @TestInfo(
    name = "Persist an order and publish its event",
    testType = REGRESSION,
    testSubject = BACKEND,
    testLevel = INTEGRATION,
    project = DEMO,
    apiService = {DEMO_DATABASE, DEMO_KAFKA}
  )
  public void persistOrderAndPublishEvent() {
    final int updated = step(executeUpdate(
      this.database,
      "insert into orders(id, status) values (?, ?)",
      501L,
      "CREATED"
    ));
    step("One database row is inserted", () -> assertThat(updated).isEqualTo(1));

    final Map<String, Object> row = step(executeSelectOne(
      this.database,
      "select id as \"id\", status as \"status\" from orders where id = ?",
      501L
    ));
    step("Stored order has expected state", () -> {
      assertThat(row.get("id")).isEqualTo(501L);
      assertThat(row.get("status")).isEqualTo("CREATED");
    });

    step(send(this.producer, "demo.orders", "501", "{\"id\":501,\"status\":\"CREATED\"}"));
    AwaitilityUtils.waitForAssertTrue(() -> this.producer.history().size() == 1, Duration.ofSeconds(2));
    step("Published event contains the order id", () ->
      assertThat(this.producer.history().getFirst().value()).contains("\"id\":501")
    );
  }
}
