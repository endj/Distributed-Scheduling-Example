package org.example.users;

import org.example.Config;
import org.example.locking.Leases;
import org.example.locking.TableLeaseAcquirer;
import org.example.model.UserDetailed;
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

import static org.example.Config.UPDATE_POLL_RATE_SECONDS;
import static org.example.Config.USER_API_BASE_URL;
import static org.example.Config.USER_STALENESS_LIMIT_SECONDS;
import static org.example.Config.USER_UPDATE_BATCH_SIZE;
import static org.example.Main.DB;

public class UserUpdater {
    private static final Logger log = LoggerFactory.getLogger(UserUpdater.class);
    private static final String UPDATER_NAME = Config.CLIENT_ID + "-" + "Updater";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void start() {
        @SuppressWarnings("resource")
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleWithFixedDelay(() -> {
            var result = TableLeaseAcquirer.acquireUsersLock(Leases.USER_UPDATE, UPDATER_NAME);
            switch (result) {
                case OK -> doWork();
                case ERROR, TAKEN -> log.debug("Failed to acquire lock ->  [" + result + "]");
            }
        }, 0, UPDATE_POLL_RATE_SECONDS, TimeUnit.SECONDS);
    }

    private static void doWork() {
        List<Integer> integers = DB.usersToUpdate(Duration.ofSeconds(USER_STALENESS_LIMIT_SECONDS), USER_UPDATE_BATCH_SIZE);
        for (Integer userId : integers) {
            try {
                HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder()
                        .timeout(Duration.ofSeconds(1))
                        .header("CLIENT_ID", Config.CLIENT_ID)
                        .uri(URI.create(USER_API_BASE_URL + "/userprofile?user=" + userId))
                        .build(), HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    log.warn("Got statusCode %d on /users".formatted(response.statusCode()));
                }

                UserDetailed userDetailed = Config.MAPPER.readValue(response.body(), UserDetailed.class);
                DB.updateUser(userDetailed);
            } catch (Exception e) {
                log.warn(e.getMessage());
            }
        }
        log.info("Updated users " + integers);
    }
}
