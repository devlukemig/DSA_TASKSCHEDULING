package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Screen;
import DSA1.src.daisyday.ui.Session;
import DSA1.src.daisyday.ui.components.DonutChart;
import DSA1.src.daisyday.ui.components.Dot;
import DSA1.src.daisyday.ui.components.GoalBar;
import DSA1.src.daisyday.ui.components.HorizontalBarChart;
import DSA1.src.daisyday.ui.components.RowLayout;
import DSA1.src.daisyday.ui.components.WeekBarChart;
import DSA1.src.daisyday.ui.dialogs.WeeklyGoalDialog;
import DSA1.src.daisyday.util.TimeFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Map;

import static DSA1.src.daisyday.ui.Theme.GRID;
import static DSA1.src.daisyday.ui.Theme.GREEN;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;
import static DSA1.src.daisyday.ui.Theme.PAGE_BG;
import static DSA1.src.daisyday.ui.Theme.priorityColor;
import static DSA1.src.daisyday.ui.Theme.statusColor;
import static DSA1.src.daisyday.ui.UiFactory.label;
import static DSA1.src.daisyday.ui.UiFactory.link;
import static DSA1.src.daisyday.ui.UiFactory.section;

/**
 * Scrollable overview: key numbers, today's focus, upcoming tasks,
 * status / weekly / category charts and the weekly goal.
 * Everything is read from the database each time the page refreshes.
 */
public class DashboardPage extends Page {

    private static final int FOCUS_LIMIT = 4;
    private static final Color OVERDUE_RED = new Color(190, 30, 30);
    private static final Color DUE_BROWN = new Color(150, 100, 0);

    private JScrollPane scroll;

    public DashboardPage(Session session, Database database, AppController app) {
        super(session, database, app);
    }

    @Override
    public void refresh() {
        // stay where the user was scrolled to after a refresh
        int scrollPosition = scroll == null ? 0 : scroll.getVerticalScrollBar().getValue();

        removeAll();
        add(label("DASHBOARD", 18), BorderLayout.NORTH);

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<LocalDate, Integer> completedPerDay =
                database.tasks().completedPerDay(accountId(), weekStart, weekEnd);
        int completedThisWeek = completedPerDay.values().stream().mapToInt(Integer::intValue).sum();

        ScrollContent content = new ScrollContent();
        addRow(content, 0, buildKpis(today, completedThisWeek));
        addRow(content, 1, twoColumns(2, 1, buildFocus(today), buildUpcoming(today)));
        addRow(content, 2, twoColumns(1, 1,
                buildStatusChart(), buildWeeklyProductivity(weekStart, today, completedPerDay, completedThisWeek)));
        addRow(content, 3, twoColumns(2, 1,
                buildCategoryChart(), buildWeeklyGoal(weekStart, weekEnd, completedThisWeek)));

        scroll = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PAGE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        add(scroll, BorderLayout.CENTER);
        revalidate();
        repaint();
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(scrollPosition));
    }

    // ---------- layout helpers ----------

    private static void addRow(JPanel content, int row, JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 12, 0);
        content.add(component, c);
    }

    private static JPanel twoColumns(double leftWeight, double rightWeight, JComponent left, JComponent right) {
        JPanel row = new JPanel(new RowLayout(8, leftWeight, rightWeight));
        row.setOpaque(false);
        row.add(left);
        row.add(right);
        return row;
    }

    private static JLabel small(String text, Color color) {
        JLabel result = new JLabel(text);
        result.setFont(new Font("SansSerif", Font.PLAIN, 11));
        result.setForeground(color);
        return result;
    }

    private static JPanel stack(int top, int sides) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(top, sides, 10, sides));
        return panel;
    }

    private static <T extends JComponent> T left(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }

    /** Header line under a card title, like "Distribution of all tasks". */
    private static JPanel withSubtitle(String subtitle, JComponent body) {
        JLabel text = small(subtitle, MUTED);
        text.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.add(text, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private void openCalendar(LocalDate day) {
        session.setSelectedDate(day);
        session.setDisplayedMonth(YearMonth.from(day));
        app.refreshAll();
        app.showScreen(Screen.CALENDAR);
    }

    // ---------- key numbers ----------

    private JPanel buildKpis(LocalDate today, int completedThisWeek) {
        int overdue = database.tasks().countOverdue(accountId(), today);
        int due = database.tasks().countDue(accountId(), today);
        int inProgress = database.tasks().countsByStatus(accountId()).getOrDefault(Task.IN_PROGRESS, 0);

        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        grid.add(kpi("OVERDUE", overdue, overdue > 0 ? OVERDUE_RED : MUTED,
                overdue > 0 ? "Needs attention" : "Nothing overdue", overdue > 0 ? OVERDUE_RED : GREEN));
        grid.add(kpi("DUE TODAY", due, DUE_BROWN, "Remaining tasks", MUTED));
        grid.add(kpi("IN PROGRESS", inProgress, HEADER, "Active tasks", MUTED));
        grid.add(kpi("COMPLETED THIS WEEK", completedThisWeek, HEADER, "Tasks finished", GREEN));
        return grid;
    }

    private static JPanel kpi(String title, int value, Color valueColor, String caption, Color captionColor) {
        JLabel number = left(label(String.valueOf(value), 30));
        number.setForeground(valueColor);

        JPanel body = stack(6, 12);
        body.add(number);
        body.add(left(small(caption, captionColor)));
        return section(title, body);
    }

    // ---------- today's focus + upcoming ----------

    private JPanel buildFocus(LocalDate today) {
        List<Task> remaining = new ArrayList<>();
        for (Task t : database.tasks().findByDate(accountId(), today)) {
            if (!t.isCompleted()) {
                remaining.add(t);
            }
        }

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);

        if (remaining.isEmpty()) {
            JLabel empty = small("You're all caught up for today.", MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(14, 12, 14, 12));
            list.add(left(empty));
        }
        for (int i = 0; i < Math.min(FOCUS_LIMIT, remaining.size()); i++) {
            list.add(focusRow(remaining.get(i)));
        }

        JLabel count = small(remaining.size() + " remaining", MUTED);
        count.setBorder(BorderFactory.createEmptyBorder(8, 12, 6, 12));

        int hidden = remaining.size() - FOCUS_LIMIT;
        JButton all = link(hidden > 0
                ? "View all today's tasks (+" + hidden + " more) →"
                : "View all today's tasks →");
        all.addActionListener(event -> openCalendar(today));

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        footer.add(all, BorderLayout.WEST);

        JPanel countRow = new JPanel(new BorderLayout());
        countRow.setBackground(Color.WHITE);
        countRow.add(count, BorderLayout.EAST);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(countRow, BorderLayout.NORTH);
        body.add(list, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        return section("TODAY'S FOCUS", body);
    }

    private JPanel focusRow(Task t) {
        JLabel title = new JLabel(t.getTitle());
        title.setFont(new Font("Serif", Font.BOLD, 14));
        title.setForeground(HEADER);

        JLabel detail = small(t.getPriority() + " priority  ·  " + TimeFormat.time(t.getStart()), MUTED);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(left(title));
        text.add(Box.createVerticalStrut(2));
        text.add(left(detail));

        JPanel dot = new JPanel(new BorderLayout());
        dot.setOpaque(false);
        dot.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 10));
        dot.add(new Dot(priorityColor(t.getPriority()), 12), BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GRID),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        row.setToolTipText("Open task details");
        row.add(dot, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                app.editTask(t);
            }
        });
        return row;
    }

    private JPanel buildUpcoming(LocalDate today) {
        int upcoming = database.tasks().countIncompleteBetween(
                accountId(), today.plusDays(1), today.plusDays(7));

        JLabel number = left(label(String.valueOf(upcoming), 30));
        JLabel hint = left(small("<html><body style='width:150px'>Plan ahead and prepare for upcoming deadlines.</body></html>", MUTED));
        JButton schedule = link("View schedule →");
        schedule.addActionListener(event -> openCalendar(today.plusDays(1)));
        schedule.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel divider = new JPanel();
        divider.setBackground(GRID);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        divider.setPreferredSize(new Dimension(10, 1));
        divider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel body = stack(6, 12);
        body.add(number);
        body.add(left(small("Tasks in the next 7 days", MUTED)));
        body.add(Box.createVerticalStrut(12));
        body.add(divider);
        body.add(Box.createVerticalStrut(12));
        body.add(hint);
        body.add(Box.createVerticalStrut(10));
        body.add(schedule);
        return section("UPCOMING", body);
    }

    // ---------- charts ----------

    private JPanel buildStatusChart() {
        Map<String, Integer> byStatus = database.tasks().countsByStatus(accountId());

        List<String> names = List.of(Task.COMPLETED, Task.IN_PROGRESS, Task.PENDING);
        int[] counts = new int[names.size()];
        Color[] colors = new Color[names.size()];
        for (int i = 0; i < names.size(); i++) {
            counts[i] = byStatus.getOrDefault(names.get(i), 0);
            colors[i] = statusColor(names.get(i));
        }
        return section("TASK STATUS",
                withSubtitle("Distribution of all tasks", new DonutChart(names, counts, colors)));
    }

    private JPanel buildWeeklyProductivity(
            LocalDate weekStart, LocalDate today, Map<LocalDate, Integer> completedPerDay, int total) {

        String[] days = new String[7];
        int[] values = new int[7];
        int todayIndex = -1;
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            days[i] = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            values[i] = completedPerDay.getOrDefault(day, 0);
            if (day.equals(today)) {
                todayIndex = i;
            }
        }

        JPanel footer = stack(10, 12);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 12, 0, 12, Color.WHITE),
                BorderFactory.createEmptyBorder(10, 0, 10, 0)));
        footer.add(left(label(total + (total == 1 ? " task" : " tasks"), 20)));
        footer.add(left(small("Completed this week", MUTED)));

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(new WeekBarChart(days, values, todayIndex, 215), BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);
        return section("WEEKLY PRODUCTIVITY", withSubtitle("Tasks completed each day", body));
    }

    private JPanel buildCategoryChart() {
        Map<String, Integer> byCategory = database.tasks().countsByCategory(accountId());
        return section("TASKS BY CATEGORY", withSubtitle("Where your workload comes from",
                new HorizontalBarChart(
                        new ArrayList<>(byCategory.keySet()),
                        new ArrayList<>(byCategory.values()))));
    }

    private JPanel buildWeeklyGoal(LocalDate weekStart, LocalDate weekEnd, int completed) {
        OptionalInt custom = database.accounts().weeklyGoal(accountId());

        // no goal set: use everything finished this week plus everything still due this week
        int stillDue = database.tasks().countIncompleteBetween(accountId(), weekStart, weekEnd);
        int goal = custom.isPresent() ? custom.getAsInt() : completed + stillDue;
        int remaining = Math.max(0, goal - completed);
        int percent = goal == 0 ? 0 : (int) Math.round(completed * 100.0 / goal);

        String message;
        if (goal == 0) {
            message = "No tasks planned this week. Set a goal to get started.";
        } else if (remaining == 0) {
            message = "Goal reached. Great work!";
        } else {
            message = remaining + (remaining == 1 ? " more task" : " more tasks") + " to reach your target.";
        }

        GoalBar bar = new GoalBar(goal == 0 ? 0 : completed / (double) goal);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));

        JButton edit = link("Edit goal");
        edit.setAlignmentX(Component.LEFT_ALIGNMENT);
        edit.addActionListener(event -> editWeeklyGoal(custom, completed + stillDue));

        JPanel body = stack(6, 12);
        body.add(left(label(completed + "/" + goal, 30)));
        body.add(left(small(custom.isPresent() ? "Tasks completed (your goal)" : "Tasks completed (automatic goal)", MUTED)));
        body.add(Box.createVerticalStrut(14));
        body.add(bar);
        body.add(Box.createVerticalStrut(12));
        body.add(left(label(percent + "%", 22)));
        body.add(Box.createVerticalStrut(6));
        body.add(left(small("<html><body style='width:150px'>" + message + "</body></html>", MUTED)));
        body.add(Box.createVerticalStrut(10));
        body.add(edit);
        return section("WEEKLY GOAL", body);
    }

    /** Asks how many tasks the user wants to finish each week and saves the answer. */
    private void editWeeklyGoal(OptionalInt current, int automaticGoal) {
        WeeklyGoalDialog.Result result = WeeklyGoalDialog.show(this, current, automaticGoal);
        if (!result.saved()) {
            return; // cancelled
        }

        try {
            database.accounts().setWeeklyGoal(accountId(), result.goal());
        } catch (DataAccessException e) {
            app.error(e.getMessage());
            return;
        }

        app.refreshAll();
        if (result.goal() == null) {
            app.success("Weekly Goal Reset",
                    "Your weekly goal is automatic again. It follows the tasks due this week.");
        } else {
            app.success("Weekly Goal Saved",
                    "You are aiming to complete " + result.goal()
                            + (result.goal() == 1 ? " task" : " tasks") + " each week.");
        }
    }

    /** Page content that is as wide as the window and as tall as it needs to be. */
    private static final class ScrollContent extends JPanel implements Scrollable {

        ScrollContent() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 18;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(40, visible.height - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
