package org.example.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import static org.example.db.ConnectionManager.usingConnection;


public class SchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private static final String[] QUERIES = new String[]{"""
                CREATE TABLE IF NOT EXISTS user_details (
                    id INT,
                    user_name VARCHAR(128) PRIMARY KEY,
                    email VARCHAR(128),
                    title VARCHAR(128),
                    role VARCHAR(64),
                    last_updated BIGINT NOT NULL
                );""",
            """
                CREATE TABLE IF NOT EXISTS update_leases (
                    lease_name VARCHAR(64) PRIMARY KEY,
                    acquired_at BIGINT,
                    held_by VARCHAR(128),
                    available_after BIGINT
                );
                """,
            """
                INSERT IGNORE INTO update_leases (lease_name, acquired_at, held_by, available_after)
                VALUES
                    ('user_profiles_fetching', 0, NULL, 0),
                    ('user_updating', 0, NULL, 0);
                """
    };

    public static void createSchema() {
        usingConnection(connection -> {
            for (String query : QUERIES) {
                try {
                    var statement = connection.createStatement();
                    statement.execute(query);
                } catch (SQLException e) {
                    log.error(e.getMessage());
                    System.exit(1);
                    throw new RuntimeException(e);
                }
            }
        });
    }
}
