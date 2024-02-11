package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNullElse;

public class Config {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static final int SERVER_PORT = parseInt(requireNonNullElse(System.getenv("PORT"), "8082"));
    public static final String CLIENT_ID = requireNonNullElse(System.getenv("CLIENT_ID"), "TEST_CLIENT");
    public static final int DB_PORT = parseInt(requireNonNullElse(System.getenv("DB_PORT"), "3306"));
    public static final String DB_ARGS = requireNonNullElse(System.getenv("DB_ARGS"), "test");
    public static final String DB_HOST = requireNonNullElse(System.getenv("DB_HOST"), "localhost");
    public static final int HEALTH_CHECK_REPORT_INTERVAL = parseInt(requireNonNullElse(System.getenv("HEALTH_CHECK_SECONDS"), "10"));

    public static final int UPDATE_POLL_RATE_SECONDS = parseInt(requireNonNullElse(System.getenv("UPDATE_POLL_RATE"), "10"));
    public static final int INSERT_POLL_RATE_SECONDS = parseInt(requireNonNullElse(System.getenv("INSERT_POLL_RATE"), "30"));

    public static final int USER_STALENESS_LIMIT_SECONDS = parseInt(requireNonNullElse(System.getenv("USER_STALE_LIMIT_TTL"), "3600"));
    public static final int USER_UPDATE_BATCH_SIZE = parseInt(requireNonNullElse(System.getenv("USER_UPDATE_BATCH_SIZE"), "10"));

    public static final String USER_API_BASE_URL = requireNonNullElse(System.getenv("USER_API_BASE_URL"),"http://localhost:8080");
    public static final String REGISTRY_BASE_URL = requireNonNullElse(System.getenv("REGISTRY_BASE_URL"),"http://localhost:8081");
    public static final int LEASE_DURATION_MS = parseInt(requireNonNullElse(System.getenv("LEASE_DURATION_MS"), "20000"));


}
