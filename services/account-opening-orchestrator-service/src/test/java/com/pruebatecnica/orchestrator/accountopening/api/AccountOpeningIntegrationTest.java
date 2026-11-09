package com.pruebatecnica.orchestrator.accountopening.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountOpeningIntegrationTest {

    private static final AtomicInteger accountCreates = new AtomicInteger();
    private static final AtomicInteger accountReversals = new AtomicInteger();
    private static volatile boolean slowAccount;
    private static volatile boolean failCard;
    private static HttpServer accountServer;
    private static HttpServer cardServer;

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @DynamicPropertySource
    static void downstreams(DynamicPropertyRegistry registry) throws IOException {
        startServers();
        registry.add("app.clients.account-service-url", () -> "http://localhost:" + accountServer.getAddress().getPort());
        registry.add("app.clients.card-service-url", () -> "http://localhost:" + cardServer.getAddress().getPort());
    }

    @BeforeEach
    void resetBreakers() {
        circuitBreakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        accountCreates.set(0);
        accountReversals.set(0);
        slowAccount = false;
        failCard = false;
    }

    @AfterAll
    static void stopServers() {
        if (accountServer != null) {
            accountServer.stop(0);
        }
        if (cardServer != null) {
            cardServer.stop(0);
        }
    }

    @Test
    @Order(1)
    @DisplayName("CA-01 integración: éxito de cuenta, tarjeta y comisión")
    void success() throws Exception {
        ApiCall response = open();

        assertEquals(201, response.status());
        assertEquals("COMPLETED", response.body().sagaStatus());
        assertNotNull(response.body().accountId());
        assertNotNull(response.body().cardId());
        assertNotNull(response.correlationId());
    }

    @Test
    @Order(2)
    @DisplayName("CA-03 integración: fallo de tarjeta compensa la cuenta")
    void cardFailureCompensates() throws Exception {
        failCard = true;

        ApiCall response = open("X-Force-Failure", "CARD_CREATE");

        assertEquals(422, response.status());
        assertEquals("FAILED", response.body().sagaStatus());
        assertEquals(1, accountReversals.get());
    }

    @Test
    @Order(3)
    @DisplayName("CA-04 la latencia sostenida abre el circuito y la siguiente llamada no sale")
    void slowCallsOpenTheCircuit() throws Exception {
        slowAccount = true;
        for (int attempt = 0; attempt < 3; attempt++) {
            ApiCall response = open();
            assertEquals(201, response.status());
        }
        int callsBeforeOpen = accountCreates.get();

        ApiCall blocked = open();

        assertEquals(503, blocked.status());
        assertEquals("PENDING_RETRY", blocked.body().sagaStatus());
        assertEquals(callsBeforeOpen, accountCreates.get());
        assertEquals(CircuitBreaker.State.OPEN, circuitBreakers.circuitBreaker("accountService").getState());
    }

    @Test
    @Order(4)
    @DisplayName("CA-04 el circuito semiabierto vuelve a cerrado tras llamadas rápidas")
    void halfOpenRecovers() throws Exception {
        slowAccount = true;
        for (int attempt = 0; attempt < 3; attempt++) {
            open();
        }
        assertEquals(CircuitBreaker.State.OPEN, circuitBreakers.circuitBreaker("accountService").getState());

        Thread.sleep(1_100);
        slowAccount = false;
        ApiCall first = open();
        ApiCall second = open();

        assertEquals(201, first.status());
        assertEquals(201, second.status());
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreakers.circuitBreaker("accountService").getState());
        assertTrue(accountCreates.get() >= 2);
    }

    private ApiCall open(String... extraHeaders) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/v1/account-openings"))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {"customerId":"customer-1","productType":"SAVINGS","accountAlias":"nomina","cardType":"DEBIT"}
                        """));
        for (int index = 0; index < extraHeaders.length; index += 2) {
            request.header(extraHeaders[index], extraHeaders[index + 1]);
        }
        HttpResponse<String> response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        OpeningResponse body = response.body() == null || response.body().isBlank()
                ? null
                : mapper.readValue(response.body(), OpeningResponse.class);
        return new ApiCall(response.statusCode(), body, response.headers().firstValue("X-Correlation-Id").orElse(null));
    }

    private record ApiCall(int status, OpeningResponse body, String correlationId) {
    }

    private static synchronized void startServers() throws IOException {
        if (accountServer != null) {
            return;
        }
        accountServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        accountServer.createContext("/api/v1/accounts", AccountOpeningIntegrationTest::account);
        accountServer.start();
        cardServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        cardServer.createContext("/api/v1/cards", AccountOpeningIntegrationTest::card);
        cardServer.start();
    }

    private static void account(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.endsWith("/reversals")) {
            accountReversals.incrementAndGet();
            write(exchange, 200, "{\"accountId\":\"account-1\",\"accountNumber\":\"00000000001\",\"status\":\"REVERSED\",\"createdAt\":\"2026-11-09T12:00:00Z\"}");
            return;
        }
        if (slowAccount) {
            sleep();
        }
        accountCreates.incrementAndGet();
        write(exchange, 201, "{\"accountId\":\"account-1\",\"accountNumber\":\"00000000001\",\"status\":\"CREATED\",\"createdAt\":\"2026-11-09T12:00:00Z\"}");
    }

    private static void card(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String failure = exchange.getRequestHeaders().getFirst("X-Force-Failure");
        if (path.endsWith("/commission-charges")) {
            write(exchange, 200, "{\"cardId\":\"card-1\",\"maskedPan\":\"************1234\",\"commissionStatus\":\"CHARGED\",\"status\":\"CREATED\"}");
            return;
        }
        if (path.endsWith("/reversals")) {
            write(exchange, 200, "{\"cardId\":\"card-1\",\"maskedPan\":\"************1234\",\"commissionStatus\":\"PENDING\",\"status\":\"REVERSED\"}");
            return;
        }
        if (failCard || "CARD_CREATE".equals(failure)) {
            write(exchange, 503, "{\"title\":\"Dependencia no disponible\"}");
            return;
        }
        write(exchange, 201, "{\"cardId\":\"card-1\",\"maskedPan\":\"************1234\",\"commissionStatus\":\"PENDING\",\"status\":\"CREATED\"}");
    }

    private static void sleep() {
        try {
            Thread.sleep(700);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void write(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
