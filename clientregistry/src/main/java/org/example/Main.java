package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.example.Config.REGISTRY_TTL_MS;
import static org.example.Config.SERVER_PORT;

public class Main {

    record Client(int port, String clientId) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ConcurrentMap<Client, Long> REGISTRY = new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {
        @SuppressWarnings("resource")
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            REGISTRY.entrySet().removeIf(e -> {
                var durationSinceLastPingMs = now - e.getValue();
                return durationSinceLastPingMs > REGISTRY_TTL_MS;
            });
        }, 0, 5, SECONDS);

        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(SERVER_PORT), 0);
        httpServer.createContext("/register", handleRegister());
        httpServer.createContext("/discover", handleDiscover());
        httpServer.start();
    }

    private static HttpHandler handleDiscover() {
        return exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

                List<Client> clients = REGISTRY.keySet().stream().toList();
                respond(exchange, 200,
                        MAPPER.writeValueAsString(clients)
                );
            }
        };
    }

    private static HttpHandler handleRegister() {
        return exchange -> {
            try (exchange) {

                try {
                    Client client = parseClient(exchange.getRequestURI());
                    var timestamp = System.currentTimeMillis();
                    REGISTRY.put(client, timestamp);
                    System.out.printf("Client %s Healthy At %d%n", client, timestamp);
                    respond(exchange, 200);
                } catch (Exception e) {
                    respond(exchange, 400, e.getMessage());
                }
            }
        };
    }


    private static Client parseClient(URI requestURI) {
        String rawQuery = Objects.requireNonNull(requestURI.getRawQuery(), "Query param null");
        String[] split = rawQuery.split("&");
        if (split.length != 2) throw new IllegalArgumentException("Bad query param " + rawQuery);
        var clientParam = split[0];
        var portParam = split[1];
        if (!clientParam.startsWith("client")
            || !portParam.startsWith("port")) {
            throw new IllegalArgumentException("Expecting client=id&port=number, got " + rawQuery + "\n");
        }

        int port = Integer.parseInt(portParam.split("=")[1]);
        String clientId = clientParam.split("=")[1];
        return new Client(port, clientId);
    }

    private static void respond(HttpExchange exchange, int statusCode) throws IOException {
        respond(exchange, statusCode, null);
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        var length = body == null ? -1 : body.length();
        exchange.sendResponseHeaders(statusCode, length);
        if (length != -1) {
            exchange.getResponseBody().write(body.getBytes(UTF_8));
        }
    }
}