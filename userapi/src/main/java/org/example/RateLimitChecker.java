package org.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.example.Config.RATE_LIMIT_RESET_DURATION_SECONDS;
import static org.example.Config.REQUEST_LIMIT_USERS;
import static org.example.Config.REQUEST_LIMIT_USER_PROFILE;

public record RateLimitChecker(Map<PathAccess, AtomicInteger> hitMap,
                               Map<String, Integer> pathLimits) {
    public static RateLimitChecker build() {
        var hitMap = new ConcurrentHashMap<PathAccess, AtomicInteger>();
        @SuppressWarnings("resource")
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> hitMap.values().forEach(counter -> counter.set(0)), 0, RATE_LIMIT_RESET_DURATION_SECONDS, SECONDS);

        return new RateLimitChecker(hitMap, Map.of("/users", REQUEST_LIMIT_USERS, "/userprofile", REQUEST_LIMIT_USER_PROFILE));
    }

    public RateLimitStatus rateLimited(PathAccess access) {
        AtomicInteger compute = hitMap.compute(access, (k, v) -> {
            if (v == null) return new AtomicInteger(1);
            v.incrementAndGet();
            return v;
        });
        int hits = compute.get();
        return pathLimits.get(access.path()) < hits ? RateLimitStatus.RATE_LIMITED : RateLimitStatus.OK;
    }

    public enum RateLimitStatus {
        OK, RATE_LIMITED
    }

    public record PathAccess(String clientId, String path) {
    }

}
