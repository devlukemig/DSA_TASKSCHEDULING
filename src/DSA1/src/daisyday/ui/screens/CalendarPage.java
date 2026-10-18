package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Session;
import DSA1.src.daisyday.ui.components.MonthPanel;
import DSA1.src.daisyday.util.TextFormat;
import DSA1.src.daisyday.util.TimeFormat;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static DSA1.src.daisyday.ui.Theme.BLUE;
import static DSA1.src.daisyday.ui.Theme.priorityColor;
import static DSA1.src.daisyday.ui.UiFactory.html;
import static DSA1.src.daisyday.ui.UiFactory.label;
import static DSA1.src.daisyday.ui.UiFactory.tag;

public class CalendarPage extends Page {

    public CalendarPage(Session session, Database database, AppController app) {
        super(session, database, app);
    }

    @Override
    public void refresh() {
        removeAll();
        add(label("CALENDAR VIEW", 20), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);
        body.add(buildMonth(), BorderLayout.CENTER);
        body.add(buildSchedule(), BorderLayout.EAST);

        add(body, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private MonthPanel buildMonth() {
        return new MonthPanel(
                session.getDisplayedMonth(), session.getSelectedDate(),
                database.tasks().countsByDay(accountId(), session.getDisplayedMonth()),
                date -> {
                    session.setSelectedDate(date);
                    refresh();
                },
                month -> {
                    session.setDisplayedMonth(month);
                    refresh();
                }
        );
    }

    private JPanel buildSchedule() {
        LocalDate selected = session.getSelectedDate();

        JPanel schedule = new JPanel(new BorderLayout(5, 10));
        schedule.setOpaque(false);
        schedule.setPreferredSize(new Dimension(205, 350));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setOpaque(false);
        heading.add(label(
                selected.equals(LocalDate.now())
                        ? "TODAY’S SCHEDULE" : "DAY’S SCHEDULE",
                19
        ));
        heading.add(label(TimeFormat.fullDate(selected), 14));
        schedule.add(heading, BorderLayout.NORTH);

        JPanel entries = new JPanel();
        entries.setLayout(new BoxLayout(entries, BoxLayout.Y_AXIS));
        entries.setOpaque(false);

        List<Task> dayTasks = database.tasks().findByDate(accountId(), selected);

        if (dayTasks.isEmpty()) {
            entries.add(label("No tasks for this date.", 14));
        }

        for (Task t : dayTasks) {
            entries.add(buildEntry(t));
            entries.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(entries);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        schedule.add(scroll, BorderLayout.CENTER);
        return schedule;
    }

    private JPanel buildEntry(Task t) {
        JPanel entry = new JPanel(new BorderLayout(4, 8));
        entry.setBackground(BLUE);
        entry.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        entry.setMaximumSize(new Dimension(205, 145));

        JButton title = new JButton("<html><b>" + html(t.getTitle()) + "</b></html>");
        title.setHorizontalAlignment(SwingConstants.LEFT);
        title.setForeground(new Color(105, 66, 245));
        title.setFont(new Font("Serif", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder());
        title.setContentAreaFilled(false);
        title.setFocusPainted(false);
        title.addActionListener(event -> app.editTask(t));
        entry.add(title, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(2, 1));
        details.setOpaque(false);
        JLabel category = label(TextFormat.shorten(t.getCategory(), 18), 13);
        category.setToolTipText(t.getCategory());
        details.add(category);
        details.add(label(
                TimeFormat.time(t.getStart()) + " - " + TimeFormat.time(t.getEnd()),
                11
        ));
        entry.add(details, BorderLayout.CENTER);

        JPanel tags = new JPanel(new BorderLayout(5, 0));
        tags.setOpaque(false);
        tags.add(tag(
                t.getPriority().toUpperCase(Locale.ENGLISH),
                priorityColor(t.getPriority())
        ), BorderLayout.WEST);
        tags.add(tag(t.getStatus(), new Color(0, 174, 239)), BorderLayout.EAST);
        entry.add(tags, BorderLayout.SOUTH);

        return entry;
    }
}
