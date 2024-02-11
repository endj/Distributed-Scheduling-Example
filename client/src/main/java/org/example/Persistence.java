package org.example;

import org.example.model.UpdatingStatus;
import org.example.model.User;
import org.example.model.UserDetailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.example.db.ConnectionManager.usingConnection;

public class Persistence {

    private static final Logger log = LoggerFactory.getLogger(Persistence.class);

    public static final String UPDATE_USER_QUERY = """
            UPDATE user_details
            SET user_name=?, email=?, title=?, role=?, last_updated=?
            WHERE id=?
            """;
    public static final String ALL_USERS_QUERY = "SELECT * FROM user_details";
    public static final String USERS_TO_UPDATE_QUERY = "SELECT id FROM user_details WHERE last_updated < ? LIMIT ?";
    public static final String USER_BY_ID_QUERY = "SELECT * FROM user_details WHERE id = ?";
    public static final String INSERT_USERS_QUERY = """
            INSERT IGNORE INTO user_details (id, user_name, last_updated)
             VALUES (?, ?, ?)
            """;
    public static final String UPDATED_USERS_COUNT_QUERY = """
            SELECT
                COUNT(*) AS total_count,
                COUNT(title) AS updated_count
            FROM
                user_details;
            """;

    public Integer insertUsers(List<User> users) {
        return usingConnection(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(INSERT_USERS_QUERY)) {
                for (var user : users) {
                    preparedStatement.setLong(1, user.id());
                    preparedStatement.setString(2, user.userName());
                    preparedStatement.setLong(3, System.currentTimeMillis());
                    preparedStatement.addBatch();
                }
                int[] updateCounts = preparedStatement.executeBatch();
                return Arrays.stream(updateCounts).sum();
            } catch (SQLException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public void updateUser(UserDetailed userDetails) {
        usingConnection(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_USER_QUERY)) {
                preparedStatement.setString(1, userDetails.userName());
                preparedStatement.setString(2, userDetails.email());
                preparedStatement.setString(3, userDetails.title());
                preparedStatement.setString(4, userDetails.role());
                preparedStatement.setLong(5, System.currentTimeMillis() + 60_000 * 60);
                preparedStatement.setLong(6, userDetails.id());

                int rowsUpdated = preparedStatement.executeUpdate();
                if (rowsUpdated != 1)
                    throw new RuntimeException("Failed when updating %s, update count: %d ".formatted(userDetails, rowsUpdated));
            } catch (SQLException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }


    public List<UserDetailed> allUsers() {
        List<UserDetailed> users = new ArrayList<>();
        usingConnection(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(ALL_USERS_QUERY);
                 ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(toUserDetailed(resultSet));
                }
            } catch (SQLException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        });
        return users;
    }

    public List<Integer> usersToUpdate(Duration timeSinceUpdate, int usersToReturn) {
        List<Integer> userIds = new ArrayList<>();

        usingConnection(connection -> {
            try (PreparedStatement preparedStatement = connection.prepareStatement(USERS_TO_UPDATE_QUERY)) {
                long olderTHan = System.currentTimeMillis() - (timeSinceUpdate.toMillis());
                preparedStatement.setLong(1, olderTHan);
                preparedStatement.setInt(2, usersToReturn);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        int id = resultSet.getInt("id");
                        userIds.add(id);
                    }
                }
            } catch (SQLException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        });

        return userIds;
    }

    public UserDetailed userById(long userId) {
        return usingConnection(connection -> {
            UserDetailed result = null;
            try (PreparedStatement statement = connection.prepareStatement(USER_BY_ID_QUERY)) {
                statement.setLong(1, userId);
                try (var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        result = toUserDetailed(resultSet);
                    }
                }
            } catch (SQLException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }
            return result;
        });
    }


    private static UserDetailed toUserDetailed(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String userName = resultSet.getString("user_name");
        String email = resultSet.getString("email");
        String title = resultSet.getString("title");
        String role = resultSet.getString("role");
        return new UserDetailed(id, userName, email, title, role);
    }


    public UpdatingStatus getStats() {
        return usingConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(UPDATED_USERS_COUNT_QUERY)) {
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return new UpdatingStatus(resultSet.getInt("total_count"),
                            resultSet.getInt("updated_count"));
                }
                throw new RuntimeException("Empty result-set");
            } catch (SQLException e) {
                log.warn(e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
}
