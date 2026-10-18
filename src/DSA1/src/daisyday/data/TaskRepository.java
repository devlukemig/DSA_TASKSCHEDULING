package DSA1.src.daisyday.data;

import DSA1.src.daisyday.model.Task;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tasks. Dates and times are stored as ISO text (2026-10-07, 09:00). */
public class TaskRepository {

    private static final String SELECT_TASKS =
            "SELECT t.id, t.title, c.name AS category, t.status, t.priority, "
                    + "t.task_date, t.start_time, t.end_time, t.requires_proof, t.proof_path "
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
                + "task_date, start_time, end_time, requires_proof, proof_path, completed_at) "
                + "VALUES (?, " + CATEGORY_ID + ", ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            statement.setInt(10, task.isRequiresProof() ? 1 : 0);
            statement.setString(11, proofPath(task));
            statement.setString(12, task.isCompleted() ? LocalDate.now().toString() : null);
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
                + "task_date = ?, start_time = ?, end_time = ?, requires_proof = ?, proof_path = ?, "
                // keep the original completion day; clear it when the task is reopened
                + "completed_at = CASE WHEN ? = 'Completed' THEN COALESCE(completed_at, ?) ELSE NULL END "
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
            statement.setInt(9, task.isRequiresProof() ? 1 : 0);
            statement.setString(10, proofPath(task));
            statement.setString(11, task.getStatus());
            statement.setString(12, LocalDate.now().toString());
            statement.setLong(13, task.getId());
            statement.setLong(14, accountId);
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

    /** Tasks that are not completed and were due before today. */
    public int countOverdue(long accountId, LocalDate today) {
        return count(
                "SELECT COUNT(*) FROM tasks WHERE account_id = ? "
                        + "AND status <> 'Completed' AND task_date < ?",
                accountId, today.toString()
        );
    }

    /** Tasks that are not completed and are due from one day to another (inclusive). */
    public int countIncompleteBetween(long accountId, LocalDate from, LocalDate to) {
        return count(
                "SELECT COUNT(*) FROM tasks WHERE account_id = ? "
                        + "AND status <> 'Completed' AND task_date BETWEEN ? AND ?",
                accountId, from.toString(), to.toString()
        );
    }

    /** Number of tasks with each status (statuses with no tasks are left out). */
    public Map<String, Integer> countsByStatus(long accountId) {
        return groupedCounts(
                "SELECT status, COUNT(*) FROM tasks WHERE account_id = ? GROUP BY status",
                accountId
        );
    }

    /** Number of tasks in each category, biggest first (empty categories are left out). */
    public Map<String, Integer> countsByCategory(long accountId) {
        return groupedCounts(
                "SELECT c.name, COUNT(*) AS total FROM tasks t "
                        + "JOIN categories c ON c.id = t.category_id "
                        + "WHERE t.account_id = ? GROUP BY c.id "
                        + "ORDER BY total DESC, c.name",
                accountId
        );
    }

    /** Tasks completed on each day from one day to another (days with none are left out). */
    public Map<LocalDate, Integer> completedPerDay(long accountId, LocalDate from, LocalDate to) {
        String sql = "SELECT completed_at, COUNT(*) FROM tasks WHERE account_id = ? "
                + "AND status = 'Completed' AND completed_at BETWEEN ? AND ? "
                + "GROUP BY completed_at";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, from.toString());
            statement.setString(3, to.toString());

            Map<LocalDate, Integer> counts = new HashMap<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    counts.put(LocalDate.parse(result.getString(1)), result.getInt(2));
                }
            }
            return counts;

        } catch (SQLException e) {
            throw new DataAccessException("Could not count the completed tasks", e);
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

    private Map<String, Integer> groupedCounts(String sql, long accountId) {
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);

            Map<String, Integer> counts = new LinkedHashMap<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    counts.put(result.getString(1), result.getInt(2));
                }
            }
            return counts;

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

        task.setRequiresProof(result.getInt("requires_proof") == 1);

        String proof = result.getString("proof_path");
        task.setProof(proof == null ? null : new File(proof));
        return task;
    }
}
