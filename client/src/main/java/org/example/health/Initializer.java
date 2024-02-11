package org.example.health;

import org.example.Config;
import org.example.db.SchemaMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.example.db.ConnectionManager.JDBC_URL;
import static org.example.db.ConnectionManager.usingConnection;

public class Initializer {
    private static final Logger log = LoggerFactory.getLogger(Initializer.class);

    public static void initialize() {
        waitForDb();
        SchemaMigration.createSchema();
    }

    static void waitForDb() {
        int tries = 3;
        while (tries-- > 0) {

            try {
                var success = trySelect1();
                if (!success) {
                    log.error("Client %s Failed to connect to DB %n".formatted(Config.CLIENT_ID));
                    System.exit(1);
                } else {
                    return;
                }
            } catch (Exception e) {
                log.info(JDBC_URL);
                log.error(e.getMessage());
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.exit(1);
    }

    private static Boolean trySelect1() {
        var success = usingConnection(connection -> {
            int attempts = 3;
            while (attempts-- > 0) {
                String query = "SELECT 1";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        resultSet.getInt(1);
                        log.info("Database connected!");
                        return true;
                    }
                } catch (SQLException e) {
                    log.error("DB connection error %s, trying again in 5 seconds.. attempts left %d%n".formatted(e, attempts));
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
            return false;
        });
        return success;
    }

}
