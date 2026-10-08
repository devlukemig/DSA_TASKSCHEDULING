package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.model.TaskSummary;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Session;
import DSA1.src.daisyday.ui.components.TaskChart;
import DSA1.src.daisyday.util.TimeFormat;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.List;

import static DSA1.src.daisyday.ui.UiFactory.label;
import static DSA1.src.daisyday.ui.UiFactory.section;

public class DashboardPage extends Page {

    private static final String[] COLUMNS = {
            "Task title", "Category", "Start", "End", "Priority", "Status"
    };

    public DashboardPage(Session session, Database database, AppController app) {
        super(session, database, app);
    }

    @Override
    public void refresh() {
        removeAll();
        add(label("DASHBOARD", 18), BorderLayout.NORTH);

        LocalDate today = LocalDate.now();

        JPanel body = new JPanel(new BorderLayout(10, 14));
        body.setOpaque(false);
        body.add(buildCounts(today), BorderLayout.NORTH);
        body.add(buildTodayTable(today), BorderLayout.CENTER);
        body.add(buildCharts(today), BorderLayout.SOUTH);

        add(body, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel buildCounts(LocalDate today) {
        int due = database.tasks().countDue(accountId(), today);
        int total = database.tasks().countAll(accountId());

        JPanel counts = new JPanel(new GridLayout(1, 2, 8, 0));
        counts.setOpaque(false);
        counts.setPreferredSize(new Dimension(600, 80));
        counts.add(section("DUE TODAY", label("   " + due, 27)));
        counts.add(section("TOTAL TASK", label("   " + total, 27)));
        return counts;
    }

    private JPanel buildTodayTable(LocalDate date) {
        List<Task> today = database.tasks().findByDate(accountId(), date);

        Object[][] rows = today.stream().map(t -> new Object[]{
                t.getTitle(), t.getCategory(),
                TimeFormat.time(t.getStart()), TimeFormat.time(t.getEnd()),
                t.getPriority(), t.getStatus()
        }).toArray(Object[][]::new);

        JTable table = new JTable(new DefaultTableModel(rows, COLUMNS) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });

        table.setRowHeight(28);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int row = table.rowAtPoint(event.getPoint());
                if (row >= 0) {
                    app.editTask(today.get(table.convertRowIndexToModel(row)));
                }
            }
        });

        return section("TODAY TASKS", new JScrollPane(table));
    }

    private JPanel buildCharts(LocalDate today) {
        TaskSummary summary = database.tasks().summary(accountId(), today);

        JPanel charts = new JPanel(new GridLayout(1, 2, 8, 0));
        charts.setOpaque(false);
        charts.setPreferredSize(new Dimension(600, 170));
        charts.add(section("BAR GRAPH", new TaskChart(false, summary)));
        charts.add(section("PIE CHART", new TaskChart(true, summary)));
        return charts;
    }
}
