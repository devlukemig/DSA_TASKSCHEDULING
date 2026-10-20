package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Session;
import DSA1.src.daisyday.ui.dialogs.Modal;
import DSA1.src.daisyday.util.TextFormat;
import DSA1.src.daisyday.util.TimeFormat;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.List;

import static DSA1.src.daisyday.ui.Theme.GREEN;
import static DSA1.src.daisyday.ui.Theme.GRID;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;
import static DSA1.src.daisyday.ui.Theme.categoryColor;
import static DSA1.src.daisyday.ui.Theme.statusColor;
import static DSA1.src.daisyday.ui.UiFactory.label;
import static DSA1.src.daisyday.ui.UiFactory.section;
import static DSA1.src.daisyday.ui.UiFactory.tag;

/**
 * Archived tasks. Each one can be restored to the task board or deleted right away,
 * and is deleted automatically after 30 days in the archive.
 */
public class ArchivePage extends Page {

    private static final Color URGENT = new Color(190, 30, 30);
    private static final int URGENT_DAYS = 7;
    private static final int CATEGORY_LIMIT = 18;

    public ArchivePage(Session session, Database database, AppController app) {
        super(session, database, app);
    }

    @Override
    public void refresh() {
        removeAll();

        JLabel note = small("Archived tasks are deleted permanently after " + Task.ARCHIVE_DAYS + " days.", MUTED);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("ARCHIVE", 18), BorderLayout.WEST);
        top.add(note, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        List<Task> archived = database.tasks().findArchived(accountId());
        add(section("ARCHIVED TASKS   " + archived.size(), buildList(archived)), BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JComponent buildList(List<Task> archived) {
        if (archived.isEmpty()) {
            return buildEmpty();
        }

        LocalDate today = LocalDate.now();
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Color.WHITE);
        for (Task t : archived) {
            list.add(buildRow(t, today));
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildEmpty() {
        JLabel title = label("The archive is empty.", 18);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel hint = small("<html><div style='text-align:center'>Tasks you archive, and tasks of a deleted category,<br>"
                + "wait here for " + Task.ARCHIVE_DAYS + " days before they are deleted.<br>"
                + "You can restore them any time before that.</div></html>", MUTED);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.add(title);
        box.add(Box.createVerticalStrut(10));
        box.add(hint);

        JPanel empty = new JPanel(new java.awt.GridBagLayout());
        empty.setBackground(Color.WHITE);
        empty.add(box);
        return empty;
    }

    // ---------- one archived task ----------

    private JPanel buildRow(Task t, LocalDate today) {
        Color color = categoryColor(t.getCategoryColor());

        JLabel title = new JLabel(t.getTitle());
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(HEADER);

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titleRow.setOpaque(false);
        titleRow.add(title);
        if (t.isCompleted()) {
            JLabel check = new JLabel("✓");
            check.setFont(new Font("SansSerif", Font.BOLD, 14));
            check.setForeground(GREEN);
            titleRow.add(check);
        }

        JLabel pill = new JLabel(TextFormat.shorten(t.getCategory(), CATEGORY_LIMIT));
        pill.setOpaque(true);
        pill.setBackground(color);
        pill.setForeground(Color.BLACK);
        pill.setFont(new Font("Serif", Font.PLAIN, 12));
        pill.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        pill.setToolTipText(t.getCategory());

        JLabel status = tag(t.getStatus().toUpperCase(), statusColor(t.getStatus()));
        JLabel due = small("Due " + t.getDate(), MUTED);

        JPanel details = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        details.setOpaque(false);
        details.add(pill);
        details.add(status);
        details.add(due);

        JLabel archivedOn = small("Archived " + TimeFormat.fullDate(t.getArchivedAt())
                + "  ·  " + t.getArchiveReason(), MUTED);
        JPanel archivedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        archivedRow.setOpaque(false);
        archivedRow.add(archivedOn);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        for (JComponent line : new JComponent[]{titleRow, details, archivedRow}) {
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(line);
            text.add(Box.createVerticalStrut(5));
        }

        JPanel stripe = new JPanel();
        stripe.setBackground(color);
        stripe.setPreferredSize(new Dimension(5, 10));

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, GRID));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textWrap = new JPanel(new BorderLayout());
        textWrap.setOpaque(false);
        textWrap.setBorder(BorderFactory.createEmptyBorder(9, 4, 4, 0));
        textWrap.add(text, BorderLayout.CENTER);

        row.add(stripe, BorderLayout.WEST);
        row.add(textWrap, BorderLayout.CENTER);
        row.add(buildActions(t, today), BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    private JPanel buildActions(Task t, LocalDate today) {
        int days = t.daysLeftInArchive(today);
        JLabel left = small(days == 0 ? "Deleted today"
                : "Deleted in " + days + (days == 1 ? " day" : " days"),
                days <= URGENT_DAYS ? URGENT : MUTED);
        left.setFont(left.getFont().deriveFont(Font.BOLD));
        left.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JButton restore = compact(Modal.primaryButton("Restore"));
        restore.addActionListener(event -> restore(t));
        JButton delete = compact(Modal.secondaryButton("Delete"));
        delete.addActionListener(event -> delete(t));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.setOpaque(false);
        buttons.add(delete);
        buttons.add(restore);
        buttons.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.add(left);
        box.add(Box.createVerticalStrut(8));
        box.add(buttons);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(9, 0, 6, 10));
        wrap.add(box, BorderLayout.NORTH);
        return wrap;
    }

    // ---------- actions ----------

    private void restore(Task t) {
        String revived;
        try {
            revived = database.tasks().restore(accountId(), t.getId());
        } catch (DataAccessException e) {
            app.error(e.getMessage());
            return;
        }

        app.refreshAll();
        app.success("Task Restored", "\"" + t.getTitle() + "\" is back on your task board."
                + (revived == null ? "" : "\nIts category \"" + revived + "\" was brought back too."));
    }

    private void delete(Task t) {
        if (!app.confirm("Delete Task",
                "Delete \"" + t.getTitle() + "\" permanently?\nThis can't be undone.", "Delete", true)) {
            return;
        }

        try {
            database.tasks().deleteArchived(accountId(), t.getId());
        } catch (DataAccessException e) {
            app.error(e.getMessage());
            return;
        }

        app.refreshAll();
        app.success("Task Deleted", "\"" + t.getTitle() + "\" was deleted permanently.");
    }

    // ---------- small helpers ----------

    private static JLabel small(String text, Color color) {
        JLabel result = new JLabel(text);
        result.setFont(new Font("SansSerif", Font.PLAIN, 11));
        result.setForeground(color);
        return result;
    }

    private static JButton compact(JButton button) {
        button.setFont(new Font("Serif", Font.BOLD, 12));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(button.getBackground().equals(Color.WHITE)
                        ? new Color(200, 190, 190) : HEADER, 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        return button;
    }
}
