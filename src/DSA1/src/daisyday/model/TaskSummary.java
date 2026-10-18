package DSA1.src.daisyday.model;

/** How many tasks are completed, still pending, or overdue. */
public record TaskSummary(int completed, int pending, int overdue) {

    public int total() {
        return completed + pending + overdue;
    }
}
