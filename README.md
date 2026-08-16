# Kulivatsky Test Automation Framework

A reusable Java 21 test automation framework project by **Ilya Kulivatsky**.

The repository is intentionally self-contained and uses a synthetic demo domain. 
## What is included

- REST API testing with REST Assured
- UI testing with Selenide and a lightweight Page Object base
- database access with Apache DBCP2 and JDBC
- Kafka producer/consumer support
- Allure reporting for API, UI and framework steps
- TestNG metadata and launch filtering
- Awaitility helpers for eventual consistency
- JSON Schema validation
- classpath-safe file upload fixtures
- an internal step DSL with nested contexts and checked-exception support

## Project layout

```text
src/main/java/org/kulivatsky/autotests/core
├── annotation   Test metadata and page annotations
├── config       Environment and launch configuration
├── listener     TestNG lifecycle and diagnostics
├── spec         REST specification helpers
├── step         Step DSL and reusable API/UI/DB/Kafka steps
├── storage      URL, database, Kafka and driver storage
├── util         JDBC, JSON, Allure, resource and wait utilities
└── web          Base Page Object functionality

src/test/java/org/kulivatsky/autotests/demo
├── EasyApiTest
├── MediumUiTest
├── ComplexBackendTest
└── ComplexE2ETest
```

## Demo test levels

| Test | Purpose | Main framework pieces |
|---|---|---|
| `EasyApiTest` | Compact API example | REST Assured, step context, response checks, JSON Schema |
| `MediumUiTest` | Page Object example | Selenide, UI steps, Page Object, Allure `@Step` |
| `ComplexBackendTest` | Integration-style backend example | H2, JDBC/DBCP2, Kafka `MockProducer`, Awaitility |
| `ComplexE2ETest` | Multi-stage scenario | API steps, polling, schema validation, classpath file upload |

The UI example is excluded from the default build so that `mvn test` does not require a browser.

## Build

Requirements:

- JDK 21
- Maven 3.6.3+

Run the self-contained API/backend/E2E examples:

```bash
mvn clean test
```

Run all examples, including the headless UI test:

```bash
mvn clean test -Pui
```

Build the reusable JAR:

```bash
mvn clean package -DskipTests
```

Install it into the local Maven repository:

```bash
mvn clean install -DskipTests
```

## Configuration

Defaults live in `src/main/resources/autotests.properties`. JVM system properties override them, for example:

```bash
mvn test -Dautotests.env=TEST -Drest.timeoutMs=20000
```

Connection settings are resolved by normalized service/environment keys instead of hardcoded maps:

```properties
api.demo_api.local.url=http://127.0.0.1:8080
db.demo_database.local.url=jdbc:h2:mem:demo
kafka.demo_kafka.local.bootstrapServers=127.0.0.1:9092
```

Do not commit credentials or private keys. Supply secrets through the runtime environment or CI secret storage.

## Step DSL

The framework keeps test code readable without hiding normal Java control flow:

```java
stepContext()
  .with(getOrder(baseUrl, 42))
  .exec(responseStatusCodeIs(200))
  .exec(responseJsonValueIs("status", "READY"));
```

Reusable steps can carry structured parameters into Allure:

```java
return new SupplierStep.Of<>(
  "Get order by id",
  params("orderId", orderId),
  () -> DemoApiRequests.getOrder(baseUrl, orderId)
);
```

## CI

The included GitHub Actions workflow runs the browser-free test set on JDK 21 for pushes and pull requests.
