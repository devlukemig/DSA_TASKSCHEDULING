package DSA1.src.daisyday.model;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class Task {

    public static final String PENDING = "Pending";
    public static final String IN_PROGRESS = "In Progress";
    public static final String COMPLETED = "Completed";

    public static final List<String> STATUSES = List.of(PENDING, IN_PROGRESS, COMPLETED);
    public static final List<String> PRIORITIES = List.of("Low", "Medium", "High");

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

    public File getProof() {
        return proof;
    }

    public void setProof(File proof) {
        this.proof = proof;
    }
}
