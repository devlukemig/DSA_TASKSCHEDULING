package DSA1.src.daisyday.data;

import DSA1.src.daisyday.model.Task;

import java.io.File;
import java.sql.Connection;
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
                    + "t.task_date, t.start_time, t.end_time, t.requires_proof, t.proof_path, "
                    + "c.color_index AS category_color, t.archived_at, t.archive_reason "
                    + "FROM tasks t JOIN categories c ON c.id = t.category_id "
                    + "WHERE t.account_id = ? ";

    /** Same, but only tasks that are not in the archive. */
    private static final String SELECT_ACTIVE = SELECT_TASKS + "AND t.archived_at IS NULL ";

    private static final String CATEGORY_ID =
            "(SELECT id FROM categories WHERE account_id = ? AND name = ? AND deleted_at IS NULL)";

    /** The next number on the task board: the task that gets it is shown first in its column. */
    private static final String NEXT_BOARD_POS =
            "(SELECT COALESCE(MAX(board_pos), 0) + 1 FROM tasks)";

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
                + "task_date, start_time, end_time, requires_proof, proof_path, completed_at, board_pos) "
                + "VALUES (?, " + CATEGORY_ID + ", ?, ?, ?, ?, ?, ?, ?, ?, ?, " + NEXT_BOARD_POS + ")";

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
        String currentStatus = currentStatus(accountId, task.getId());
        if (currentStatus != null && !Task.canChangeStatus(currentStatus, task.getStatus())) {
            throw new DataAccessException("A task can't go back from \"" + currentStatus
                    + "\" to \"" + task.getStatus() + "\"");
        }

        if (task.isCompleted() && task.isRequiresProof() && task.getProof() == null
                && !Task.COMPLETED.equals(currentStatus)) {
            throw new DataAccessException("This task requires proof before it can be completed");
        }

        String sql = "UPDATE tasks SET category_id = " + CATEGORY_ID
                + ", title = ?, status = ?, priority = ?, "
                // a completed task keeps the proof it was completed with
                + "task_date = ?, start_time = ?, end_time = ?, requires_proof = ?, "
                + "proof_path = CASE WHEN status = 'Completed' THEN proof_path ELSE ? END, "
                // keep the original completion day; clear it when the task is reopened
                + "completed_at = CASE WHEN ? = 'Completed' THEN COALESCE(completed_at, ?) ELSE NULL END, "
                // a task that changes status goes to the top of its new column
                + "board_pos = CASE WHEN status <> ? THEN " + NEXT_BOARD_POS + " ELSE board_pos END "
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
            statement.setString(13, task.getStatus());
            statement.setLong(14, task.getId());
            statement.setLong(15, accountId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Could not update the task", e);
        }
    }

    // ---------- lists ----------

    /** Tasks on one day, earliest first. */
    public List<Task> findByDate(long accountId, LocalDate date) {
        String sql = SELECT_ACTIVE + "AND t.task_date = ? ORDER BY t.start_time, t.id";
        return query(sql, accountId, date.toString());
    }

    /**
     * Tasks on one day whose time range overlaps start-end, earliest first.
     * Tasks that merely touch (one ends exactly when the other starts) do not overlap.
     *
     * @param ignoreId a task to leave out (the one being edited), or null
     */
    public List<Task> findOverlapping(
            long accountId, LocalDate date, LocalTime start, LocalTime end, Long ignoreId) {

        // times are stored as ISO text (09:00), so comparing the text compares the times
        String sql = SELECT_ACTIVE
                + "AND t.task_date = ? AND t.start_time < ? AND t.end_time > ? "
                + "AND t.id <> ? ORDER BY t.start_time, t.id";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, date.toString());
            statement.setString(3, end.toString());
            statement.setString(4, start.toString());
            statement.setLong(5, ignoreId == null ? -1L : ignoreId);

            List<Task> tasks = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tasks.add(read(result));
                }
            }
            return tasks;

        } catch (SQLException e) {
            throw new DataAccessException("Could not check the schedule for conflicts", e);
        }
    }

    /** Tasks with one status, newest on top (new or just-moved tasks first). */
    public List<Task> findByStatus(long accountId, String status) {
        String sql = SELECT_ACTIVE + "AND t.status = ? ORDER BY t.board_pos DESC, t.id DESC";
        return query(sql, accountId, status);
    }

    // ---------- archive ----------

    /** Moves a task to the archive. It is deleted for good after {@link Task#ARCHIVE_DAYS} days. */
    public void archive(long accountId, long taskId, String reason) {
        String sql = "UPDATE tasks SET archived_at = ?, archive_reason = ? "
                + "WHERE id = ? AND account_id = ? AND archived_at IS NULL";

        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setString(1, LocalDate.now().toString());
            statement.setString(2, reason);
            statement.setLong(3, taskId);
            statement.setLong(4, accountId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not archive the task", e);
        }
    }

    /** Archived tasks, the ones that will be deleted first at the top. */
    public List<Task> findArchived(long accountId) {
        String sql = SELECT_TASKS + "AND t.archived_at IS NOT NULL ORDER BY t.archived_at, t.id";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, accountId);

            List<Task> tasks = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tasks.add(read(result));
                }
            }
            return tasks;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load the archive", e);
        }
    }

    public int countArchived(long accountId) {
        return count("SELECT COUNT(*) FROM tasks WHERE account_id = ? AND archived_at IS NOT NULL", accountId);
    }

    /**
     * Takes a task out of the archive and puts it back on top of its board column.
     * If its category was deleted in the meantime, the category comes back too.
     *
     * @return the name of the category that was brought back, or null if it was never deleted
     */
    public String restore(long accountId, long taskId) {
        Connection connection = database.connection();
        try {
            connection.setAutoCommit(false);
            try {
                String revived = null;
                try (PreparedStatement find = connection.prepareStatement(
                        "SELECT c.id, c.name FROM tasks t JOIN categories c ON c.id = t.category_id "
                                + "WHERE t.id = ? AND t.account_id = ? AND c.deleted_at IS NOT NULL")) {
                    find.setLong(1, taskId);
                    find.setLong(2, accountId);
                    try (ResultSet result = find.executeQuery()) {
                        if (result.next()) {
                            revived = result.getString(2);
                            try (PreparedStatement bring = connection.prepareStatement(
                                    "UPDATE categories SET deleted_at = NULL WHERE id = ?")) {
                                bring.setLong(1, result.getLong(1));
                                bring.executeUpdate();
                            }
                        }
                    }
                }

                try (PreparedStatement put = connection.prepareStatement(
                        "UPDATE tasks SET archived_at = NULL, archive_reason = NULL, board_pos = "
                                + NEXT_BOARD_POS + " WHERE id = ? AND account_id = ?")) {
                    put.setLong(1, taskId);
                    put.setLong(2, accountId);
                    put.executeUpdate();
                }

                connection.commit();
                return revived;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not restore the task", e);
        }
    }

    /** Deletes an archived task for good. Tasks that are not archived are never touched. */
    public void deleteArchived(long accountId, long taskId) {
        try (PreparedStatement statement = database.connection().prepareStatement(
                "DELETE FROM tasks WHERE id = ? AND account_id = ? AND archived_at IS NOT NULL")) {
            statement.setLong(1, taskId);
            statement.setLong(2, accountId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete the task", e);
        }
        removeUnusedDeletedCategories();
    }

    /**
     * Deletes every task (of every account) that has been in the archive for
     * {@link Task#ARCHIVE_DAYS} days or more. Returns how many were deleted.
     */
    public int purgeExpired(LocalDate today) {
        int deleted;
        try (PreparedStatement statement = database.connection().prepareStatement(
                "DELETE FROM tasks WHERE archived_at IS NOT NULL AND archived_at <= ?")) {
            statement.setString(1, today.minusDays(Task.ARCHIVE_DAYS).toString());
            deleted = statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not clean up the archive", e);
        }
        if (deleted > 0) {
            removeUnusedDeletedCategories();
        }
        return deleted;
    }

    /** A deleted category is only kept while archived tasks still belong to it. */
    private void removeUnusedDeletedCategories() {
        try (Statement statement = database.connection().createStatement()) {
            statement.executeUpdate("DELETE FROM categories WHERE deleted_at IS NOT NULL "
                    + "AND NOT EXISTS (SELECT 1 FROM tasks WHERE tasks.category_id = categories.id)");
        } catch (SQLException e) {
            throw new DataAccessException("Could not clean up the categories", e);
        }
    }

    // ---------- counts ----------

    public int countAll(long accountId) {
        return count("SELECT COUNT(*) FROM tasks WHERE account_id = ? AND archived_at IS NULL", accountId);
    }

    /** Tasks on that day that are not completed yet. */
    public int countDue(long accountId, LocalDate date) {
        return count(
                "SELECT COUNT(*) FROM tasks WHERE account_id = ? AND archived_at IS NULL "
                        + "AND task_date = ? AND status <> 'Completed'",
                accountId, date.toString()
        );
    }

    /** Tasks that are not completed and were due before today. */
    public int countOverdue(long accountId, LocalDate today) {
        return count(
                "SELECT COUNT(*) FROM tasks WHERE account_id = ? AND archived_at IS NULL "
                        + "AND status <> 'Completed' AND task_date < ?",
                accountId, today.toString()
        );
    }

    /** Tasks that are not completed and are due from one day to another (inclusive). */
    public int countIncompleteBetween(long accountId, LocalDate from, LocalDate to) {
        return count(
                "SELECT COUNT(*) FROM tasks WHERE account_id = ? AND archived_at IS NULL "
                        + "AND status <> 'Completed' AND task_date BETWEEN ? AND ?",
                accountId, from.toString(), to.toString()
        );
    }

    /** Number of tasks with each status (statuses with no tasks are left out). */
    public Map<String, Integer> countsByStatus(long accountId) {
        return groupedCounts(
                "SELECT status, COUNT(*) FROM tasks WHERE account_id = ? AND archived_at IS NULL GROUP BY status",
                accountId
        );
    }

    /** Number of tasks in each category, biggest first (empty categories are left out). */
    public Map<String, Integer> countsByCategory(long accountId) {
        return groupedCounts(
                "SELECT c.name, COUNT(*) AS total FROM tasks t "
                        + "JOIN categories c ON c.id = t.category_id "
                        + "WHERE t.account_id = ? AND t.archived_at IS NULL GROUP BY c.id "
                        + "ORDER BY total DESC, c.name",
                accountId
        );
    }

    /**
     * Tasks completed on each day from one day to another (days with none are left out).
     * Archived tasks still count: archiving a finished task must not undo the work.
     */
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
        String sql = "SELECT task_date, COUNT(*) FROM tasks WHERE account_id = ? AND archived_at IS NULL "
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

    /** The status saved in the database for this task, or null if it is not there. */
    private String currentStatus(long accountId, long taskId) {
        String sql = "SELECT status FROM tasks WHERE id = ? AND account_id = ?";
        try (PreparedStatement statement = database.connection().prepareStatement(sql)) {
            statement.setLong(1, taskId);
            statement.setLong(2, accountId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not update the task", e);
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
        task.setCategoryColor(result.getInt("category_color"));
        String archivedAt = result.getString("archived_at");
        task.setArchivedAt(archivedAt == null ? null : LocalDate.parse(archivedAt));
        task.setArchiveReason(result.getString("archive_reason"));

        String proof = result.getString("proof_path");
        task.setProof(proof == null ? null : new File(proof));
        return task;
    }
}
