package DSA1.src.daisyday.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** What the Task List page shows: a text search, a category, a priority and a sort order. */
public final class TaskFilter {

    public enum Sort {
        NEWEST("Newest first"),
        DUE_SOON("Due date: soonest"),
        DUE_LATE("Due date: latest"),
        PRIORITY("Priority: high to low"),
        TITLE("Title: A to Z");

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private String query = "";
    private String category; // null = any
    private String priority; // null = any
    private Sort sort = Sort.NEWEST;

    public void setQuery(String query) {
        this.query = query == null ? "" : query.trim();
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setSort(Sort sort) {
        this.sort = sort == null ? Sort.NEWEST : sort;
    }

    public String getCategory() {
        return category;
    }

    public String getPriority() {
        return priority;
    }

    public Sort getSort() {
        return sort;
    }

    /** True if the filter hides some tasks (the sort order alone does not hide any). */
    public boolean isFiltering() {
        return !query.isEmpty() || category != null || priority != null;
    }

    /** True if everything is at its default, including the sort order. */
    public boolean isDefault() {
        return !isFiltering() && sort == Sort.NEWEST;
    }

    public void reset() {
        query = "";
        category = null;
        priority = null;
        sort = Sort.NEWEST;
    }

    /** Every word of the search must appear in the title or the category (any case). */
    public boolean matches(Task task) {
        if (category != null && !category.equalsIgnoreCase(task.getCategory())) {
            return false;
        }
        if (priority != null && !priority.equals(task.getPriority())) {
            return false;
        }

        if (!query.isEmpty()) {
            String haystack = (task.getTitle() + " " + task.getCategory()).toLowerCase(Locale.ROOT);
            for (String word : query.toLowerCase(Locale.ROOT).split("\\s+")) {
                if (!haystack.contains(word)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** The tasks that match, in the chosen order. The list you pass in is not changed. */
    public List<Task> apply(List<Task> tasks) {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks) {
            if (matches(task)) {
                result.add(task);
            }
        }

        Comparator<Task> byDue = Comparator.comparing(Task::getDate).thenComparing(Task::getStart);
        switch (sort) {
            case DUE_SOON -> result.sort(byDue.thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER));
            case DUE_LATE -> result.sort(byDue.reversed().thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER));
            case PRIORITY -> result.sort(Comparator
                    .comparingInt((Task t) -> Task.PRIORITIES.indexOf(t.getPriority())).reversed()
                    .thenComparing(byDue));
            case TITLE -> result.sort(Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER));
            default -> { /* board order: new and just-moved tasks first */ }
        }
        return result;
    }
}
