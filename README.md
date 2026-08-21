# Kulivatsky Test Automation Framework

A reusable Java 21 test automation framework project by **Ilya Kulivatsky**.

The demo domain is synthetic. The main JAR contains no client environments, credentials, certificates, private service names, demo endpoints, or demo fixtures.

## What is in the framework

- REST API requests and reusable REST Assured specifications
- Selenide Page Object support and reusable UI steps
- JDBC access through Apache DBCP2; the consuming project supplies its database driver
- Kafka producer and short-lived consumer support
- TestNG launch metadata and opt-in lifecycle integration
- Allure-backed step DSL with nested contexts and checked-exception support
- redacted and size-limited text diagnostics for Allure
- Awaitility helpers for eventually consistent flows
- JSON Schema checks
- classpath-safe resource materialization for file uploads
- extensible project and service identifiers instead of framework-owned business enums

## Project layout

```text
src/main/java/org/kulivatsky/autotests/core
├── annotation   Test and Page Object metadata
├── config       Runtime configuration and launch filters
├── listener     Opt-in TestNG lifecycle and diagnostics
├── spec         REST specification helpers
├── step         Step DSL and reusable API/UI/DB/Kafka steps
├── storage      URL, database, Kafka and REST configuration storage
├── util         JDBC, Kafka, Allure, JSON, resource and wait utilities
└── web          Base Page Object functionality

src/test/java/org/kulivatsky/autotests/demo
├── EasyApiTest
├── MediumUiTest
├── ComplexBackendTest
└── ComplexE2ETest
```

Demo-only configuration, schemas, and upload fixtures live under `src/test/resources`; they are not packaged into the reusable framework JAR.

## Demo scenarios

| Test | Purpose | Main framework pieces |
|---|---|---|
| `EasyApiTest` | Compact API smoke example | Requests → Steps, step context, response checks, JSON Schema |
| `MediumUiTest` | Page Object example | Selenide, reusable UI steps, Page Object, Allure |
| `ComplexBackendTest` | Real backend integration example | Testcontainers PostgreSQL and Kafka, JDBC/DBCP2, Kafka, Awaitility |
| `ComplexE2ETest` | Multi-stage E2E example | API steps, polling, schema validation, classpath file upload |

The default build excludes the `ui` and `container` groups. This keeps an ordinary build independent of a browser and Docker while dedicated profiles exercise both integrations in CI.

## Build

Requirement: JDK 21. Maven itself does not need to be installed globally: both repository-local launchers use the JDK-only bootstrap under `.mvn/wrapper` to download the pinned Maven distribution, verify its SHA-512 checksum, and extract it. The bootstrap has no `curl`, `wget`, checksum-utility, `unzip`, or PowerShell dependency.

Linux/macOS:

```bash
sh ./mvnw clean verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd clean verify
```

Run the real PostgreSQL + Kafka example. Docker must be available:

```bash
sh ./mvnw test -Pintegration -Dgroups=container
```

Run the headless UI example:

```bash
sh ./mvnw test -Pui -Dgroups=ui -Dselenide.headless=true
```

Run every demo scenario:

```bash
sh ./mvnw test -Pfull
```

Build the reusable binary, source, and Javadoc JARs:

```bash
sh ./mvnw clean package -DskipTests
```

Install the library into the local Maven repository:

```bash
sh ./mvnw clean install -DskipTests
```

## Configuration

The framework resolves a property in this order:

1. JVM system property
2. environment variable
3. optional classpath `autotests.properties`

For example, this property:

```text
kafka.orders.local.bootstrapServers
```

maps to this environment variable:

```text
KAFKA_ORDERS_LOCAL_BOOTSTRAP_SERVERS
```

A consuming project may provide its own `src/test/resources/autotests.properties`, configure everything at runtime, or mix both approaches. The framework JAR does not contain project-specific defaults.

Typical keys are built from a logical service or project id and an environment:

```properties
api.orders_api.test.url=https://example.invalid
web.storefront.test.url=https://example.invalid
db.orders_database.test.url=jdbc:postgresql://example.invalid/orders
kafka.events.test.bootstrapServers=example.invalid:9092
kafka.events.test.enableAutoCommit=false
```

Short-lived `Kafka.poll(...)` operations always disable offset auto-commit, even if a direct consumer is configured differently, so filtering past unrelated records cannot advance a shared group.

Runtime overrides can be supplied without changing files:

```bash
sh ./mvnw test -Dautotests.env=TEST -Drest.timeoutMs=20000
```

## Extensible project and service identifiers

The framework does not own an enum containing consumer-specific services. A project can use a simple identifier:

```java
Service ordersApi = Service.of("orders_api");
Project storefront = Project.of("storefront");
```

or implement `Service` / `Project` with its own enum when compile-time constants are useful.

`@TestInfo` stores project and service ids as strings, so a consuming project does not have to modify the framework to add another service:

```java
@TestInfo(
  name = "Create an order",
  project = "storefront",
  service = "orders_api"
)
```

## TestNG lifecycle is opt-in

Adding the JAR to a classpath does not register listeners or mutate REST Assured/Selenide global state by itself. Tests that want framework lifecycle, launch filtering, and failure diagnostics explicitly register the listener in the TestNG runner configuration. With Maven Surefire:

```xml
<properties>
  <property>
    <name>listener</name>
    <value>org.kulivatsky.autotests.core.listener.TestNGListener</value>
  </property>
</properties>
```

The same class may be registered as a listener in `testng.xml`. It should not be wired with TestNG's `@Listeners` annotation because annotation transformers must be known before TestNG processes test annotations.

## Step DSL

The framework keeps ordinary Java control flow visible while giving reusable operations stable Allure step names. Step reporting is implemented by the framework DSL itself, so consuming projects do not need an AspectJ Java agent merely to use KTAF page objects or steps:

```java
stepContext()
  .with(getOrder(baseUrl, 42))
  .exec(responseStatusCodeIs(200))
  .exec(responseJsonValueIs("status", "READY"));
```

Reusable steps carry ordered report parameters:

```java
return new SupplierStep.Of<>(
  "Get order by id",
  params("orderId", orderId),
  () -> DemoApiRequests.getOrder(baseUrl, orderId)
);
```

Database operations have their own step facade rather than living in generic common steps:

```java
step(executeUpdate(database, "update orders set status = ? where id = ?", "READY", 42L));
```

The framework follows a `Requests → Steps → Test` split for API examples; there is no second competing request DSL.

## Report safety

Text diagnostics are treated as report data, not as an unrestricted log dump.

- common password/token/cookie/authorization/session forms are redacted before text reaches Allure
- sensitive step parameters are masked by name
- an unusual parameter name can be marked explicitly with `Params.secret(value)`
- text attachments are capped by `report.attachment.maxChars` (20,000 by default)
- automatic REST request/response body attachment is intentionally disabled
- automatic Selenide event steps are intentionally disabled; UI reporting goes through KTAF steps instead of an unmasked second reporting path
- browser screenshots and page source are opt-in because screenshots cannot be redacted and DOM content may contain business data
- binary attachments use a separate API because text redaction cannot safely inspect arbitrary bytes

Example:

```java
params(
  "user", username,
  "credential", Params.secret(password)
);
```

`Params.secret(...)` is a report marker only. The real value stays in the variable passed to the actual request or client operation.

Failure-time browser artifacts are disabled by default in the reusable library. Enable them explicitly when the tested data is safe to retain in reports:

```properties
report.browser.screenshot.enabled=true
report.browser.pageSource.enabled=true
report.browser.url.enabled=true
```

## Public API compatibility

The project is deliberately below `1.0.0` while its public API is still allowed to evolve. After the first baseline artifact has been published or installed, compatibility with that version can be checked with the opt-in `api-compat` profile:

```bash
sh ./mvnw verify -Dcompat.version=0.1.0
```

Normal builds do not attempt to resolve a historical baseline.

## CI and dependency controls

GitHub Actions runs three separate paths:

- core build and packaging
- real PostgreSQL/Kafka integration through Testcontainers
- headless UI execution

Separate workflows run CodeQL and dependency review. Dependabot checks Maven and GitHub Actions dependencies. Third-party GitHub Actions are pinned to exact commit SHAs rather than mutable version tags.

## License

Apache License 2.0. See `LICENSE`.
