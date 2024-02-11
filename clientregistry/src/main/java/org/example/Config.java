package org.example;


import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNullElse;

public class Config {

    public static final int SERVER_PORT = parseInt(requireNonNullElse(System.getenv("PORT"), "8081"));
    public static final int REGISTRY_TTL_MS = parseInt(requireNonNullElse(System.getenv("REGISTRY_TTL_MS"), "15000"));
}

