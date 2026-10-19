package DSA1.src.daisyday.data;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens the SQLite file and creates the tables on first run.
 * One connection is shared by the whole app.
 */
public final class Database implements AutoCloseable {

    /** Relative to where the app is run (the project folder in IntelliJ). */
    public static final Path DEFAULT_FILE = Path.of("data", "daisyday.db");

    private static final int SCHEMA_VERSION = 5;

    private final Connection connection;
    private final AccountRepository accounts;
    private final TaskRepository tasks;

    private Database(Connection connection) {
        this.connection = connection;
        this.accounts = new AccountRepository(this);
        this.tasks = new TaskRepository(this);
    }

    public static Database open() {
        return open(DEFAULT_FILE);
    }

    public static Database open(Path file) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String url = "jdbc:sqlite:" + file.toString().replace('\\', '/');
            Connection connection = DriverManager.getConnection(url);

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }

            Database database = new Database(connection);
            database.migrate();
            return database;

        } catch (SQLException | IOException e) {
            throw new DataAccessException("Could not open the database", e);
        }
    }

    public AccountRepository accounts() {
        return accounts;
    }

    public TaskRepository tasks() {
        return tasks;
    }

    Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing useful to do while shutting down
        }
    }

    // ---------- schema ----------

    private void migrate() throws SQLException {
        int version = userVersion();
        if (version >= SCHEMA_VERSION) {
            return;
        }

        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS accounts (
                        id            INTEGER PRIMARY KEY AUTOINCREMENT,
                        username      TEXT NOT NULL UNIQUE,
                        password_hash TEXT NOT NULL,
                        salt          TEXT NOT NULL,
                        created_at    TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        weekly_goal   INTEGER CHECK (weekly_goal IS NULL OR weekly_goal > 0)
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id         INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_id INTEGER NOT NULL
                                   REFERENCES accounts(id) ON DELETE CASCADE,
                        name       TEXT NOT NULL COLLATE NOCASE,
                        UNIQUE (account_id, name)
                    )""");

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        account_id  INTEGER NOT NULL
                                    REFERENCES accounts(id) ON DELETE CASCADE,
                        category_id INTEGER NOT NULL
                                    REFERENCES categories(id),
                        title       TEXT NOT NULL,
                        status      TEXT NOT NULL
                                    CHECK (status IN ('Pending', 'In Progress', 'Completed')),
                        priority    TEXT NOT NULL
                                    CHECK (priority IN ('Low', 'Medium', 'High')),
                        task_date   TEXT NOT NULL,
                        start_time  TEXT NOT NULL,
                        end_time    TEXT NOT NULL,
                        requires_proof INTEGER NOT NULL DEFAULT 0
                                    CHECK (requires_proof IN (0, 1)),
                        proof_path  TEXT,
                        completed_at TEXT,
                        board_pos   INTEGER NOT NULL DEFAULT 0
                    )""");

            // Databases created before version 2 lack the requires_proof column.
            if (version == 1) {
                statement.execute(
                        "ALTER TABLE tasks ADD COLUMN requires_proof INTEGER NOT NULL DEFAULT 0");
                statement.execute(
                        "UPDATE tasks SET requires_proof = 1 WHERE proof_path IS NOT NULL");
            }

            // Version 3 records the day a task was completed (for the dashboard).
            // Tasks completed earlier get their due date, but never a future day.
            if (version == 1 || version == 2) {
                statement.execute("ALTER TABLE tasks ADD COLUMN completed_at TEXT");
                statement.execute(
                        "UPDATE tasks SET completed_at = MIN(task_date, date('now', 'localtime')) "
                                + "WHERE status = 'Completed'");
            }

            statement.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tasks_account_date
                    ON tasks (account_id, task_date)""");

            // Version 4: each account can set its own weekly goal (empty = automatic).
            if (version >= 1 && version < 4) {
                statement.execute("ALTER TABLE accounts ADD COLUMN weekly_goal INTEGER "
                        + "CHECK (weekly_goal IS NULL OR weekly_goal > 0)");
            }

            // Version 5: board_pos orders the task board, highest first. New tasks and tasks
            // that change status get the next number, so they stack on top of their column.
            // Existing tasks keep their current order (newer ones were already higher ids).
            if (version >= 1 && version < 5) {
                statement.execute("ALTER TABLE tasks ADD COLUMN board_pos INTEGER NOT NULL DEFAULT 0");
                statement.execute("UPDATE tasks SET board_pos = id");
            }

            statement.execute("PRAGMA user_version = " + SCHEMA_VERSION);
            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private int userVersion() throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA user_version")) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
