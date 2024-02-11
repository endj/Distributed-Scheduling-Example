package org.example.users;

import com.fasterxml.jackson.core.type.TypeReference;
import org.example.Config;
import org.example.locking.Leases;
import org.example.locking.TableLeaseAcquirer;
import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.example.Config.CLIENT_ID;
import static org.example.Config.INSERT_POLL_RATE_SECONDS;
import static org.example.Config.MAPPER;
import static org.example.Config.USER_API_BASE_URL;
import static org.example.Main.DB;
import static org.example.locking.Leases.USER_INSERTING;

public class UserInserter {
    private static final Logger log = LoggerFactory.getLogger(UserInserter.class);

    private static final String INSERTER_NAME = CLIENT_ID + "-" + "Inserter";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void start() {
        @SuppressWarnings("resource")
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(() -> {
            var result = TableLeaseAcquirer.acquireUsersLock(USER_INSERTING, INSERTER_NAME);
            switch (result) {
                case OK -> doWork();
                case ERROR, TAKEN -> log.debug("Failed to acquire lock ->  ["+ result+"]");
            }
        }, 0, INSERT_POLL_RATE_SECONDS, TimeUnit.SECONDS);
    }

    private static void doWork() {
        try {
            HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder()
                    .timeout(Duration.ofSeconds(1))
                    .header("CLIENT_ID", CLIENT_ID)
                    .uri(URI.create(USER_API_BASE_URL + "/users"))
                    .build(), HttpResponse.BodyHandlers.ofString());

            if(response.statusCode() != 200) {
                log.warn("Got statusCode %d on /users".formatted(response.statusCode()));
            }

            List<User> users = MAPPER.readValue(response.body(), new TypeReference<>() {
            });
            Integer i = DB.insertUsers(users);
            log.info("Fetched %d users updated %d".formatted(users.size(), i));
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
