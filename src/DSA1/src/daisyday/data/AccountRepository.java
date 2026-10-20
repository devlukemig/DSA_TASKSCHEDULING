package DSA1.src.daisyday.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import DSA1.src.daisyday.model.Category;
import DSA1.src.daisyday.model.Task;

import java.time.LocalDate;
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

    /** Names of the categories the user has (deleted ones are not included). */
    public List<String> findCategories(long accountId) {
        List<String> names = new ArrayList<>();
        for (Category category : findCategoryList(accountId)) {
            names.add(category.name());
        }
        return names;
    }

    /** The categories with their colors, in the order they were created. */
    public List<Category> findCategoryList(long accountId) {
        String sql = "SELECT name, color_index FROM categories "
                + "WHERE account_id = ? AND deleted_at IS NULL ORDER BY id";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);

            List<Category> categories = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    categories.add(new Category(result.getString("name"), result.getInt("color_index")));
                }
            }
            return categories;

        } catch (SQLException e) {
            throw new DataAccessException("Could not load the categories", e);
        }
    }

    /** Case-insensitive. Deleted categories do not count. */
    public boolean categoryExists(long accountId, String name) {
        String sql = "SELECT 1 FROM categories WHERE account_id = ? AND name = ? AND deleted_at IS NULL";

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

    /**
     * Adds a category. If a category with this name was deleted earlier (and still has
     * archived tasks), that one comes back with its old color and its archived tasks.
     */
    public void addCategory(long accountId, String name) {
        try {
            try (PreparedStatement revive = database.connection().prepareStatement(
                    "UPDATE categories SET deleted_at = NULL, name = ? "
                            + "WHERE account_id = ? AND name = ? AND deleted_at IS NOT NULL")) {
                revive.setString(1, name);
                revive.setLong(2, accountId);
                revive.setString(3, name);
                if (revive.executeUpdate() > 0) {
                    return;
                }
            }
            insertCategory(database.connection(), accountId, name);
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the category", e);
        }
    }

    /** How many tasks (not counting archived ones) are filed under this category. */
    public int countTasksInCategory(long accountId, String name) {
        String sql = "SELECT COUNT(*) FROM tasks t JOIN categories c ON c.id = t.category_id "
                + "WHERE t.account_id = ? AND c.name = ? AND c.deleted_at IS NULL "
                + "AND t.archived_at IS NULL";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, name);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the tasks in the category", e);
        }
    }

    /**
     * Deletes a category. Its tasks are not lost: they move to the archive (and are deleted
     * for good after 30 days unless restored). A category that still has archived tasks is
     * only hidden, so those tasks keep their category; an empty one is removed completely.
     *
     * @return how many tasks were moved to the archive
     */
    public int deleteCategory(long accountId, String name) {
        Connection connection = database.connection();
        String today = LocalDate.now().toString();
        String categoryId = "(SELECT id FROM categories WHERE account_id = ? AND name = ? AND deleted_at IS NULL)";

        try {
            connection.setAutoCommit(false);
            try {
                int archived;
                try (PreparedStatement move = connection.prepareStatement(
                        "UPDATE tasks SET archived_at = ?, archive_reason = ? "
                                + "WHERE account_id = ? AND archived_at IS NULL AND category_id = " + categoryId)) {
                    move.setString(1, today);
                    move.setString(2, Task.ARCHIVED_BY_CATEGORY);
                    move.setLong(3, accountId);
                    move.setLong(4, accountId);
                    move.setString(5, name);
                    archived = move.executeUpdate();
                }

                // keep the category (hidden) while any archived task still belongs to it
                try (PreparedStatement hide = connection.prepareStatement(
                        "UPDATE categories SET deleted_at = ? WHERE account_id = ? AND name = ? "
                                + "AND deleted_at IS NULL AND EXISTS "
                                + "(SELECT 1 FROM tasks WHERE tasks.category_id = categories.id)");
                     PreparedStatement remove = connection.prepareStatement(
                             "DELETE FROM categories WHERE account_id = ? AND name = ? AND deleted_at IS NULL "
                                     + "AND NOT EXISTS (SELECT 1 FROM tasks WHERE tasks.category_id = categories.id)")) {
                    hide.setString(1, today);
                    hide.setLong(2, accountId);
                    hide.setString(3, name);
                    hide.executeUpdate();

                    remove.setLong(1, accountId);
                    remove.setString(2, name);
                    remove.executeUpdate();
                }

                connection.commit();
                return archived;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the category", e);
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

        String sql = "INSERT INTO categories (account_id, name, color_index) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, name);
            statement.setInt(3, leastUsedColor(connection, accountId));
            statement.executeUpdate();
        }
    }

    /** The category color that the fewest of the user's categories use (the first one on a tie). */
    private static int leastUsedColor(Connection connection, long accountId) throws SQLException {
        int[] used = new int[Category.PALETTE_SIZE];
        String sql = "SELECT color_index, COUNT(*) FROM categories "
                + "WHERE account_id = ? AND deleted_at IS NULL GROUP BY color_index";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    used[Math.floorMod(result.getInt(1), used.length)] += result.getInt(2);
                }
            }
        }

        int best = 0;
        for (int i = 1; i < used.length; i++) {
            if (used[i] < used[best]) {
                best = i;
            }
        }
        return best;
    }
}
