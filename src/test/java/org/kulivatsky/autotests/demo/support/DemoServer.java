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

public final class DemoServer implements AutoCloseable {
  private final HttpServer server;
  private final ExecutorService requestExecutor;
  private final ScheduledExecutorService scheduler;
  private final Map<Long, String> orderStatuses = new ConcurrentHashMap<>();

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

  public String baseUrl() {
    return "http://127.0.0.1:" + this.server.getAddress().getPort();
  }

  public String url(final String path) {
    return baseUrl() + path;
  }

  @Override
  public void close() {
    this.server.stop(0);
    this.scheduler.shutdownNow();
    this.requestExecutor.shutdownNow();
  }

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
      final long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
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

  private static String orderJson(final long id,
                                  final String product,
                                  final int quantity,
                                  final String status) {
    return "{\"id\":%d,\"product\":\"%s\",\"quantity\":%d,\"status\":\"%s\"}"
      .formatted(id, product, quantity, status);
  }

  private static void json(final HttpExchange exchange, final int status, final String body) throws IOException {
    final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, bytes.length);
    try (OutputStream output = exchange.getResponseBody()) {
      output.write(bytes);
    }
  }
}
