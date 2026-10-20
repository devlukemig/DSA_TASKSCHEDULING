package DSA1.src.daisyday.model;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class Task {

    public static final String PENDING = "Pending";
    public static final String IN_PROGRESS = "In Progress";
    public static final String COMPLETED = "Completed";

    public static final List<String> STATUSES = List.of(PENDING, IN_PROGRESS, COMPLETED);
    /** Archived tasks are deleted for good after this many days. */
    public static final int ARCHIVE_DAYS = 30;
    public static final String ARCHIVED_BY_USER = "Archived by you";
    public static final String ARCHIVED_BY_CATEGORY = "Category deleted";

    public static final List<String> PRIORITIES = List.of("Low", "Medium", "High");

    /** Statuses a task can still move to: its current one and the ones after it. */
    public static List<String> statusesFrom(String current) {
        int from = Math.max(0, STATUSES.indexOf(current));
        return STATUSES.subList(from, STATUSES.size());
    }

    /** A task may stay where it is or move forward (Pending, In Progress, Completed), never back. */
    public static boolean canChangeStatus(String from, String to) {
        return STATUSES.indexOf(to) >= STATUSES.indexOf(from);
    }

    private Long id;
    private String title;
    private String category;
    private String status;
    private String priority;
    private LocalDate date;
    private LocalTime start;
    private LocalTime end;
    private boolean requiresProof;
    private File proof;
    private int categoryColor;
    private LocalDate archivedAt;
    private String archiveReason;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isCompleted() {
        return COMPLETED.equals(status);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStart() {
        return start;
    }

    public void setStart(LocalTime start) {
        this.start = start;
    }

    public LocalTime getEnd() {
        return end;
    }

    public void setEnd(LocalTime end) {
        this.end = end;
    }

    public boolean isRequiresProof() {
        return requiresProof;
    }

    public void setRequiresProof(boolean requiresProof) {
        this.requiresProof = requiresProof;
    }

    /** Which category color to use (see Theme.categoryColor). */
    public int getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(int categoryColor) {
        this.categoryColor = categoryColor;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public LocalDate getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDate archivedAt) {
        this.archivedAt = archivedAt;
    }

    public String getArchiveReason() {
        return archiveReason;
    }

    public void setArchiveReason(String archiveReason) {
        this.archiveReason = archiveReason;
    }

    /** Days until an archived task is deleted for good (0 means it goes today). */
    public int daysLeftInArchive(LocalDate today) {
        if (archivedAt == null) {
            return ARCHIVE_DAYS;
        }
        return (int) Math.max(0, ChronoUnit.DAYS.between(today, archivedAt.plusDays(ARCHIVE_DAYS)));
    }

    public File getProof() {
        return proof;
    }

    public void setProof(File proof) {
        this.proof = proof;
    }
}
