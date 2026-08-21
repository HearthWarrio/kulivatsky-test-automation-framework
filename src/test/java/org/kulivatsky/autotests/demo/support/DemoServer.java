package org.kulivatsky.autotests.demo.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Self-contained local HTTP server used only by showcase tests.
 *
 * <p>The server binds to loopback on an ephemeral port, so it neither depends on external test
 * infrastructure nor reserves a fixed port. It exposes a tiny order API, multipart-upload endpoint,
 * and catalog HTML page. A scheduled executor simulates asynchronous order completion.</p>
 *
 * <p>The class owns its HTTP server and both executors. Tests must close it; the demo test fixtures
 * do so from {@code @AfterMethod(alwaysRun = true)}.</p>
 */
public final class DemoServer implements AutoCloseable {
  /** JDK HTTP server bound to loopback. */
  private final HttpServer server;
  /** Executor used by incoming HTTP exchanges. */
  private final ExecutorService requestExecutor;
  /** Executor used to simulate delayed state transitions. */
  private final ScheduledExecutorService scheduler;
  /** In-memory order id to status state shared safely across request/scheduler threads. */
  private final Map<Long, String> orderStatuses = new ConcurrentHashMap<>();

  /**
   * Creates, configures, and immediately starts a local server on a free port.
   *
   * @throws IllegalStateException when the JDK HTTP server cannot bind/create
   */
  public DemoServer() {
    try {
      this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    } catch (final IOException ex) {
      throw new IllegalStateException("Unable to create local demo server", ex);
    }
    this.requestExecutor = Executors.newCachedThreadPool();
    this.scheduler = Executors.newSingleThreadScheduledExecutor();
    this.server.setExecutor(this.requestExecutor);
    this.server.createContext("/api/orders", this::handleOrders);
    this.server.createContext("/api/uploads", this::handleUpload);
    this.server.createContext("/catalog", this::handleCatalog);
    this.orderStatuses.put(42L, "READY");
    this.server.start();
  }

  /**
   * Returns the loopback origin of the running server.
   *
   * @return URL in {@code http://127.0.0.1:<ephemeralPort>} form
   */
  public String baseUrl() {
    return "http://127.0.0.1:" + this.server.getAddress().getPort();
  }

  /**
   * Appends a path to {@link #baseUrl()} without additional slash normalization.
   *
   * @param path path expected to begin with {@code /}
   * @return absolute local URL
   */
  public String url(final String path) {
    return baseUrl() + path;
  }

  /**
   * Stops accepting requests and interrupts both owned executors.
   *
   * <p>{@code stop(0)} requests immediate HTTP-server shutdown; this demo fixture does not wait for
   * graceful draining because no shared external work is performed.</p>
   */
  @Override
  public void close() {
    this.server.stop(0);
    this.scheduler.shutdownNow();
    this.requestExecutor.shutdownNow();
  }

  /**
   * Handles creation and lookup for the synthetic order resource.
   *
   * <p>Order 42 is a fixed READY fixture. POST creates order 1001 in PENDING state and schedules a
   * transition to COMPLETED after 250 ms. Other numeric ids are looked up from the in-memory status
   * map.</p>
   *
   * @param exchange incoming HTTP exchange
   * @throws IOException when request/response stream I/O fails
   */
  private void handleOrders(final HttpExchange exchange) throws IOException {
    final String method = exchange.getRequestMethod();
    final String path = exchange.getRequestURI().getPath();

    if ("GET".equals(method) && "/api/orders/42".equals(path)) {
      json(exchange, 200, orderJson(42, "Notebook", 2, this.orderStatuses.get(42L)));
      return;
    }
    if ("POST".equals(method) && "/api/orders".equals(path)) {
      exchange.getRequestBody().readAllBytes();
      final long id = 1001L;
      this.orderStatuses.put(id, "PENDING");
      this.scheduler.schedule(() -> this.orderStatuses.put(id, "COMPLETED"), 250, TimeUnit.MILLISECONDS);
      json(exchange, 201, orderJson(id, "Keyboard", 1, "PENDING"));
      return;
    }
    if ("GET".equals(method) && path.matches("/api/orders/\\d+")) {
      final String rawId = path.substring(path.lastIndexOf('/') + 1);
      final long id;
      try {
        id = Long.parseLong(rawId);
      } catch (final NumberFormatException ex) {
        json(exchange, 400, "{\"message\":\"Order id is out of range\"}");
        return;
      }
      final String status = this.orderStatuses.get(id);
      if (status == null) {
        json(exchange, 404, "{\"message\":\"Order not found\"}");
      } else {
        json(exchange, 200, orderJson(id, "Keyboard", 1, status));
      }
      return;
    }
    json(exchange, 405, "{\"message\":\"Method not allowed\"}");
  }

  /**
   * Handles the synthetic multipart upload endpoint.
   *
   * <p>The demo does not implement a multipart parser. It reads the raw request body and checks for
   * the known fixture text, which is sufficient to prove that the file content reached the server.</p>
   *
   * @param exchange incoming HTTP exchange
   * @throws IOException when request/response stream I/O fails
   */
  private void handleUpload(final HttpExchange exchange) throws IOException {
    if (!"POST".equals(exchange.getRequestMethod())) {
      json(exchange, 405, "{\"message\":\"Method not allowed\"}");
      return;
    }
    final String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    if (!body.contains("portable demo payload")) {
      json(exchange, 400, "{\"message\":\"Upload payload is missing\"}");
      return;
    }
    json(exchange, 201, "{\"fileName\":\"sample-upload.txt\",\"status\":\"STORED\"}");
  }

  /**
   * Serves a minimal interactive HTML page used by the Selenide example.
   *
   * @param exchange incoming HTTP exchange
   * @throws IOException when response stream I/O fails
   */
  private void handleCatalog(final HttpExchange exchange) throws IOException {
    final String html = """
      <!doctype html>
      <html lang="en">
      <head><meta charset="utf-8"><title>Demo Catalog</title></head>
      <body>
        <h1>Demo Catalog</h1>
        <div class="product">Mechanical Keyboard</div>
        <button id="add-item" onclick="document.getElementById('cart').textContent='1 item in cart'">Add to cart</button>
        <div id="cart">Cart is empty</div>
      </body>
      </html>
      """;
    final byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
    exchange.sendResponseHeaders(200, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }

  /**
   * Serializes the fixed demo order shape.
   *
   * <p>Inputs are controlled test fixtures; this helper is not a general JSON encoder and performs
   * no escaping.</p>
   *
   * @param id order id
   * @param product product name
   * @param quantity quantity
   * @param status order status
   * @return compact JSON document
   */
  private static String orderJson(final long id,
                                  final String product,
                                  final int quantity,
                                  final String status) {
    return "{\"id\":%d,\"product\":\"%s\",\"quantity\":%d,\"status\":\"%s\"}"
      .formatted(id, product, quantity, status);
  }

  /**
   * Writes a UTF-8 JSON response and closes the response stream.
   *
   * @param exchange exchange receiving the response
   * @param status HTTP status code
   * @param body serialized JSON body
   * @throws IOException when headers or body cannot be written
   */
  private static void json(final HttpExchange exchange, final int status, final String body) throws IOException {
    final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
