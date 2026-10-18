package DSA1.src.daisyday.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** Accounts and their categories. */
public class AccountRepository {

    public static final List<String> DEFAULT_CATEGORIES =
            List.of("School", "Personal", "Work", "Home");

    private final Database database;

    AccountRepository(Database database) {
        this.database = database;
    }

    public boolean exists(String username) {
        String sql = "SELECT 1 FROM accounts WHERE username = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the username", e);
        }
    }

    /** Creates the account with the default categories and returns its id. */
    public long create(String username, char[] password) {
        Connection connection = database.connection();
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash(password, salt);

        try {
            connection.setAutoCommit(false);
            try {
                long id = insertAccount(connection, username, hash, salt);
                for (String name : DEFAULT_CATEGORIES) {
                    insertCategory(connection, id, name);
                }
                connection.commit();
                return id;

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not create the account", e);
        }
    }

    /** Returns the account id if the password is right, otherwise empty. */
    public OptionalLong authenticate(String username, char[] password) {
        String sql = "SELECT id, password_hash, salt FROM accounts WHERE username = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return OptionalLong.empty();
                }

                boolean ok = PasswordHasher.matches(
                        password, result.getString("salt"), result.getString("password_hash")
                );
                return ok ? OptionalLong.of(result.getLong("id")) : OptionalLong.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not log in", e);
        }
    }

    public List<String> findCategories(long accountId) {
        String sql = "SELECT name FROM categories WHERE account_id = ? ORDER BY id";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);

            List<String> names = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    names.add(result.getString("name"));
                }
            }
            return names;

        } catch (SQLException e) {
            throw new DataAccessException("Could not load the categories", e);
        }
    }

    /** Case-insensitive. */
    public boolean categoryExists(long accountId, String name) {
        String sql = "SELECT 1 FROM categories WHERE account_id = ? AND name = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, name);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check the category", e);
        }
    }

    public void addCategory(long accountId, String name) {
        try {
            insertCategory(database.connection(), accountId, name);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the category", e);
        }
    }

    // ---------- weekly goal ----------

    /** The number of tasks the user wants to complete each week, if they set one. */
    public OptionalInt weeklyGoal(long accountId) {
        String sql = "SELECT weekly_goal FROM accounts WHERE id = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    int goal = result.getInt(1);
                    if (!result.wasNull()) {
                        return OptionalInt.of(goal);
                    }
                }
                return OptionalInt.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the weekly goal", e);
        }
    }

    /** Saves the weekly goal. Pass null to go back to the automatic goal. */
    public void setWeeklyGoal(long accountId, Integer goal) {
        String sql = "UPDATE accounts SET weekly_goal = ? WHERE id = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            if (goal == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setInt(1, goal);
            }
            statement.setLong(2, accountId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the weekly goal", e);
        }
    }

    // ---------- helpers ----------

    private static long insertAccount(
            Connection connection, String username, String hash, String salt)
            throws SQLException {

        String sql = "INSERT INTO accounts (username, password_hash, salt) VALUES (?, ?, ?)";
        try (PreparedStatement statement =
                     connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, hash);
            statement.setString(3, salt);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    private static void insertCategory(Connection connection, long accountId, String name)
            throws SQLException {

        String sql = "INSERT INTO categories (account_id, name) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, name);
            statement.executeUpdate();
        }
    }
}
