package org.example;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.example.Config.MAPPER;
import static org.example.Config.SERVER_PORT;
import static org.example.Data.USERS_RESPONSE;
import static org.example.Data.USER_PROFILE;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static final RateLimitChecker RATE_LIMITER = RateLimitChecker.build();

    public static void main(String[] args) throws Exception {
        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(SERVER_PORT), 0);
        httpServer.createContext("/users", handleUsers());
        httpServer.createContext("/userprofile", handlerUserProfile());
        httpServer.start();
    }

    private static ProxyHandler handlerUserProfile() {
        return new ProxyHandler(exchange -> {
            long userId = getUserProfileIdArg(exchange.getRequestURI());
            log.info("client: %s path: /userprofile arg: %d ".formatted(
                    exchange.getRequestHeaders().getFirst("CLIENT_ID"), userId));
            try (exchange) {
                UserDetailed userDetailed = USER_PROFILE.get(userId);
                if (userDetailed != null) {
                    respond(exchange, 200, MAPPER.writeValueAsString(userDetailed));
                } else {
                    respond(exchange, 404);
                }
            }
        });
    }

    private static ProxyHandler handleUsers() {
        return new ProxyHandler(exchange -> {
            log.info("client: %s path: /users ".formatted(
                    exchange.getRequestHeaders().getFirst("CLIENT_ID")));
            try (exchange) {
                respond(exchange, 200, USERS_RESPONSE);
            }
        });
    }


    private static long getUserProfileIdArg(URI uri) {
        String queryParam = Objects.requireNonNull(uri.getRawQuery(), "Missing id query param");
        String[] split = queryParam.split("=");
        if (split.length != 2) throw new IllegalArgumentException("Invalid id query param " + queryParam);
        return Long.parseLong(split[1]);
    }


    record ProxyHandler(HttpHandler handler) implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers requestHeaders = exchange.getRequestHeaders();
            String id = requestHeaders.getFirst("CLIENT_ID");
            if (id == null) {
                respond(exchange, 400, "Missing CLIENT_ID header\n");
                return;
            }
            switch (RATE_LIMITER.rateLimited(new RateLimitChecker.PathAccess(id, exchange.getRequestURI().getPath()))) {
                case RATE_LIMITED -> {
                    log.debug("Client: [%s] rate-limited".formatted(id));
                    respond(exchange, 429);
                }
                case OK -> handler.handle(exchange);
            }
        }
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

    public record User(long id, String userName) {
    }

    public record UserDetailed(long id, String userName, String email, String title, String role) {
    }
}