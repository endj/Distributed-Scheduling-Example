package org.example.health;

import org.example.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.example.Config.CLIENT_ID;
import static org.example.Config.HEALTH_CHECK_REPORT_INTERVAL;
import static org.example.Config.REGISTRY_BASE_URL;
import static org.example.Config.SERVER_PORT;

public class HealthReporter {
    private static final Logger log = LoggerFactory.getLogger(HealthReporter.class);
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    public static void start() {
        @SuppressWarnings("resource")
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(HealthReporter::reportHealth, 0, HEALTH_CHECK_REPORT_INTERVAL, SECONDS);
    }

    private static void reportHealth() {
        try {
            HttpResponse<String> response = CLIENT.send(HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(REGISTRY_BASE_URL + "/register?client=%s&port=%d".formatted(CLIENT_ID, SERVER_PORT)))
                    .build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200)
                log.warn("Got response code " + response.statusCode());
        } catch (Exception e) {
            if (e instanceof ConnectException) {
                log.warn("Registry down? check %s/discovery ".formatted(REGISTRY_BASE_URL));
            } else {
                log.warn("Got error while reporting health check " + e);
            }
        }

    }
}
