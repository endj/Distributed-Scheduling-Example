package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.health.HealthReporter;
import org.example.health.Initializer;
import org.example.model.UserDetailed;
import org.example.users.UserInserter;
import org.example.users.UserUpdater;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Objects;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.example.Config.MAPPER;
import static org.example.Config.SERVER_PORT;

public class Main {
    public static final Persistence DB = new Persistence();

    public static void main(String[] args) throws IOException {
        Initializer.initialize();
        HealthReporter.start();
        UserInserter.start();
        UserUpdater.start();

        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(SERVER_PORT), 0);
        httpServer.createContext("/userprofile", handlerUserProfile());
        httpServer.createContext("/users", handleUsers());
        httpServer.createContext("/stats", stats());
        httpServer.start();
        System.out.printf("%s Server started on port %d %n", Config.CLIENT_ID, SERVER_PORT);
    }

    private static HttpHandler stats() {
        return exchange -> {
            try (exchange) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                respond(exchange, MAPPER.writeValueAsString(DB.getStats()));
            }
        };
    }

    private static HttpHandler handleUsers() {
        return exchange -> {
            try (exchange) {
                respond(exchange, MAPPER.writeValueAsString(DB.allUsers()));
            }
        };
    }

    private static HttpHandler handlerUserProfile() {
        return exchange -> {
            try (exchange) {
                long userProfileIdArg = getUserProfileIdArg(exchange.getRequestURI());
                UserDetailed user = DB.userById(userProfileIdArg);
                respond(exchange, MAPPER.writeValueAsString(user));
            }
        };
    }

    private static long getUserProfileIdArg(URI uri) {
        String queryParam = Objects.requireNonNull(uri.getRawQuery(), "Missing id query param");
        String[] split = queryParam.split("=");
        if (split.length != 2) throw new IllegalArgumentException("Invalid id query param " + queryParam);
        return Long.parseLong(split[1]);
    }


    private static void respond(HttpExchange exchange, String body) throws IOException {
        var length = body == null ? -1 : body.length();
        exchange.sendResponseHeaders(200, length);
        if (length != -1) {
            exchange.getResponseBody().write(body.getBytes(UTF_8));
        }
    }

}