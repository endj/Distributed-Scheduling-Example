package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Config {
    public static final int SERVER_PORT = intSystemEnv("PORT", 8080);
    public static final int REQUEST_LIMIT_USERS = intSystemEnv("USERS_LIMIT", 10);
    public static final int REQUEST_LIMIT_USER_PROFILE = intSystemEnv("USER_PROFILE_LIMIT", 100);
    public static final int RATE_LIMIT_RESET_DURATION_SECONDS = intSystemEnv("RATE_LIMIT_RESET_DURATION", 60);

    public static final ObjectMapper MAPPER = new ObjectMapper();


    static int intSystemEnv(String key, int defaultVal) {
        String env = System.getenv(key);
        if (env == null) return defaultVal;
        return Integer.parseInt(env);
    }
}
