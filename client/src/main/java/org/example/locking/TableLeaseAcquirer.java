package org.example.locking;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.example.Config.LEASE_DURATION_MS;
import static org.example.db.ConnectionManager.usingConnection;


public class TableLeaseAcquirer {
    private static final Logger log = LoggerFactory.getLogger(TableLeaseAcquirer.class.getSimpleName());

    public static final String ROW_LOCK_QUERY = "SELECT * FROM update_leases WHERE lease_name = ? FOR UPDATE";
    public static final String UPDATE_QUERY = """
            UPDATE update_leases
            SET acquired_at = ?, held_by = ?, available_after = ?
            WHERE lease_name = ? AND available_after < ?
            """;


    public static LeaseTakeAttempt acquireUsersLock(Leases lease, String holderName) {
        var now = System.currentTimeMillis();
        var leaseName = lease.field;

        return usingConnection(connection -> {
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement selectStatement = connection.prepareStatement(ROW_LOCK_QUERY)) {
                    selectStatement.setString(1, leaseName);
                    try (ResultSet resultSet = selectStatement.executeQuery()) {
                        if (resultSet.next()) {

                            try (PreparedStatement updateStatement = connection.prepareStatement(UPDATE_QUERY)) {
                                updateStatement.setLong(1, now);
                                updateStatement.setString(2, holderName);
                                updateStatement.setLong(3, now + LEASE_DURATION_MS);
                                updateStatement.setString(4, leaseName);
                                updateStatement.setLong(5, now);

                                int rowsUpdated = updateStatement.executeUpdate();
                                if (rowsUpdated > 0) {
                                    connection.commit();
                                    return LeaseTakeAttempt.OK;
                                } else {
                                    connection.rollback();
                                    return LeaseTakeAttempt.TAKEN;
                                }
                            }
                        } else {
                            return LeaseTakeAttempt.ERROR;
                        }
                    }
                }
            } catch (SQLException e) {
                return handleException(connection, e);
            } finally {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException aut) {
                    log.warn(aut.getMessage());
                }
            }
        });
    }

    private static LeaseTakeAttempt handleException(Connection connection, SQLException e) {
        try {
            log.warn(e.getMessage());
            connection.rollback();
        } catch (SQLException rollbackException) {
            log.warn(e.getMessage());
        }
        return LeaseTakeAttempt.ERROR;
    }

}
