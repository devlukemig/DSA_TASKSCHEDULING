package DSA1.src.daisyday.data;

import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.model.TaskSummary;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Tasks. Dates and times are stored as ISO text (2026-10-07, 09:00). */
public class TaskRepository {

    private static final String SELECT_TASKS =
            "SELECT t.id, t.title, c.name AS category, t.status, t.priority, "
                    + "t.task_date, t.start_time, t.end_time, t.proof_path "
                    + "FROM tasks t JOIN categories c ON c.id = t.category_id "
                    + "WHERE t.account_id = ? ";

    private static final String CATEGORY_ID =
            "(SELECT id FROM categories WHERE account_id = ? AND name = ?)";

    private final Database database;

    TaskRepository(Database database) {
        this.database = database;
    }

    // ---------- save ----------

    /** Inserts the task if it has no id yet, otherwise updates it. */
    public void save(long accountId, Task task) {
        if (task.getId() == null) {
            insert(accountId, task);
        } else {
            update(accountId, task);
        }
    }

    private void insert(long accountId, Task task) {
        String sql = "INSERT INTO tasks (account_id, category_id, title, status, priority, "
                + "task_date, start_time, end_time, proof_path) "
                + "VALUES (?, " + CATEGORY_ID + ", ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = database.connection()
                .prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(1, accountId);
            statement.setLong(2, accountId);
            statement.setString(3, task.getCategory());
            statement.setString(4, task.getTitle());
            statement.setString(5, task.getStatus());
            statement.setString(6, task.getPriority());
            statement.setString(7, task.getDate().toString());
            statement.setString(8, task.getStart().toString());
            statement.setString(9, task.getEnd().toString());
            statement.setString(10, proofPath(task));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                task.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the task", e);
        }
    }

    private void update(long accountId, Task task) {
        String sql = "UPDATE tasks SET category_id = " + CATEGORY_ID
                + ", title = ?, status = ?, priority = ?, "
                + "task_date = ?, start_time = ?, end_time = ?, proof_path = ? "
                + "WHERE id = ? AND account_id = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, task.getCategory());
            statement.setString(3, task.getTitle());
            statement.setString(4, task.getStatus());
            statement.setString(5, task.getPriority());
            statement.setString(6, task.getDate().toString());
            statement.setString(7, task.getStart().toString());
            statement.setString(8, task.getEnd().toString());
            statement.setString(9, proofPath(task));
            statement.setLong(10, task.getId());
            statement.setLong(11, accountId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Could not update the task", e);
        }
    }

    // ---------- lists ----------

    /** Tasks on one day, earliest first. */
    public List<Task> findByDate(long accountId, LocalDate date) {
        String sql = SELECT_TASKS + "AND t.task_date = ? ORDER BY t.start_time, t.id";
        return query(sql, accountId, date.toString());
    }

    /** Tasks with one status, oldest first. */
    public List<Task> findByStatus(long accountId, String status) {
        String sql = SELECT_TASKS + "AND t.status = ? ORDER BY t.id";
        return query(sql, accountId, status);
    }

    // ---------- counts ----------

    public int countAll(long accountId) {
        return count("SELECT COUNT(*) FROM tasks WHERE account_id = ?", accountId);
    }

    /** Tasks on that day that are not completed yet. */
    public int countDue(long accountId, LocalDate date) {
        return count(
                "SELECT COUNT(*) FROM tasks WHERE account_id = ? "
                        + "AND task_date = ? AND status <> 'Completed'",
                accountId, date.toString()
        );
    }

    /** Completed, pending (not completed, not past) and overdue (not completed, past). */
    public TaskSummary summary(long accountId, LocalDate today) {
        String sql = "SELECT "
                + "COALESCE(SUM(status = 'Completed'), 0), "
                + "COALESCE(SUM(status <> 'Completed' AND task_date >= ?), 0), "
                + "COALESCE(SUM(status <> 'Completed' AND task_date < ?), 0) "
                + "FROM tasks WHERE account_id = ?";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, today.toString());
            statement.setString(2, today.toString());
            statement.setLong(3, accountId);

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return new TaskSummary(result.getInt(1), result.getInt(2), result.getInt(3));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the tasks", e);
        }
    }

    /** Number of tasks on each day of the month (days with none are left out). */
    public Map<LocalDate, Integer> countsByDay(long accountId, YearMonth month) {
        String sql = "SELECT task_date, COUNT(*) FROM tasks WHERE account_id = ? "
                + "AND task_date BETWEEN ? AND ? GROUP BY task_date";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, month.atDay(1).toString());
            statement.setString(3, month.atEndOfMonth().toString());

            Map<LocalDate, Integer> counts = new HashMap<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    counts.put(LocalDate.parse(result.getString(1)), result.getInt(2));
                }
            }
            return counts;

        } catch (SQLException e) {
            throw new DataAccessException("Could not count the tasks per day", e);
        }
    }

    // ---------- helpers ----------

    private List<Task> query(String sql, long accountId, String value) {
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, value);

            List<Task> tasks = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tasks.add(read(result));
                }
            }
            return tasks;

        } catch (SQLException e) {
            throw new DataAccessException("Could not load the tasks", e);
        }
    }

    private int count(String sql, long accountId, String... values) {
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            for (int i = 0; i < values.length; i++) {
                statement.setString(i + 2, values[i]);
            }

            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not count the tasks", e);
        }
    }

    private static String proofPath(Task task) {
        File proof = task.getProof();
        return proof == null ? null : proof.getPath();
    }

    private static Task read(ResultSet result) throws SQLException {
        Task task = new Task();
        task.setId(result.getLong("id"));
        task.setTitle(result.getString("title"));
        task.setCategory(result.getString("category"));
        task.setStatus(result.getString("status"));
        task.setPriority(result.getString("priority"));
        task.setDate(LocalDate.parse(result.getString("task_date")));
        task.setStart(LocalTime.parse(result.getString("start_time")));
        task.setEnd(LocalTime.parse(result.getString("end_time")));

        String proof = result.getString("proof_path");
        task.setProof(proof == null ? null : new File(proof));
        return task;
    }
}
