package org.example.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.example.Config.DB_ARGS;
import static org.example.Config.DB_HOST;
import static org.example.Config.DB_PORT;

public class ConnectionManager {
    private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);

    public static final String JDBC_URL = "jdbc:mysql://%s:%d/%s".formatted(DB_HOST, DB_PORT, DB_ARGS);

    public static <T> T usingConnection(Function<Connection, T> query) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_ARGS, DB_ARGS)) {
            return query.apply(connection);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void usingConnection(Consumer<Connection> query) {
        try (Connection connection = DriverManager.getConnection(JDBC_URL, DB_ARGS, DB_ARGS)) {
            query.accept(connection);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
