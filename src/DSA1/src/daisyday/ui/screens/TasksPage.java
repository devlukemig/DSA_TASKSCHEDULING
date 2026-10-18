package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Session;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;

import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.UiFactory.button;
import static DSA1.src.daisyday.ui.UiFactory.html;
import static DSA1.src.daisyday.ui.UiFactory.label;
import static DSA1.src.daisyday.ui.UiFactory.section;

/** Task list (by status) and category management. */
public class TasksPage extends Page {

    private static final Color[] BADGE_COLORS = {
            new Color(244, 143, 204),
            new Color(229, 217, 132),
            new Color(147, 225, 157),
            new Color(0, 175, 233)
    };

    public TasksPage(Session session, Database database, AppController app) {
        super(session, database, app);
    }

    @Override
    public void refresh() {
        removeAll();
        add(buildTop(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(10, 14));
        body.setOpaque(false);

        JPanel categories = section("CATEGORIES", buildCategoryBody());
        categories.setPreferredSize(new Dimension(600, 115));
        body.add(categories, BorderLayout.NORTH);
        body.add(buildStatusColumns(), BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel buildTop() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label("TASK LIST & CATEGORIES", 17), BorderLayout.WEST);

        JButton create = button("Create New Task +");
        create.addActionListener(event -> app.editTask(null));
        top.add(create, BorderLayout.EAST);
        return top;
    }

    // ---------- categories ----------

    private JPanel buildCategoryBody() {
        JPanel categoryBody = new JPanel(new BorderLayout(6, 6));
        categoryBody.setBackground(Color.WHITE);
        categoryBody.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT));
        badges.setBackground(Color.WHITE);

        List<String> names = database.accounts().findCategories(accountId());
        for (int i = 0; i < names.size(); i++) {
            JLabel badge = label(names.get(i), 13);
            badge.setOpaque(true);
            badge.setBackground(BADGE_COLORS[i % BADGE_COLORS.length]);
            badge.setBorder(BorderFactory.createEmptyBorder(3, 15, 3, 15));
            badges.add(badge);
        }

        JScrollPane badgeScroll = new JScrollPane(badges);
        badgeScroll.setBorder(BorderFactory.createEmptyBorder());
        badgeScroll.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_NEVER
        );

        categoryBody.add(badgeScroll, BorderLayout.CENTER);
        categoryBody.add(buildAddCategoryRow(), BorderLayout.SOUTH);
        return categoryBody;
    }

    private JPanel buildAddCategoryRow() {
        JTextField categoryName = new JTextField();
        categoryName.setToolTipText("New Category Name");

        JButton add = button("Add");
        add.addActionListener(event -> addCategory(categoryName.getText().trim()));

        JPanel addRow = new JPanel(new BorderLayout(6, 0));
        addRow.setOpaque(false);
        addRow.add(categoryName, BorderLayout.CENTER);
        addRow.add(add, BorderLayout.EAST);
        return addRow;
    }

    private void addCategory(String name) {
        if (!session.isLoggedIn()) {
            return;
        }

        if (name.isEmpty()) {
            app.message("Enter a category name.");
            return;
        }

        try {
            if (database.accounts().categoryExists(accountId(), name)) {
                app.message("This category already exists.");
                return;
            }
            database.accounts().addCategory(accountId(), name);

        } catch (DataAccessException e) {
            app.message(e.getMessage());
            return;
        }

        app.refreshAll();
    }

    // ---------- status columns ----------

    private JPanel buildStatusColumns() {
        JPanel columns = new JPanel(new GridLayout(1, 3, 8, 0));
        columns.setOpaque(false);

        for (String status : Task.STATUSES) {
            JPanel list = new JPanel();
            list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
            list.setBackground(Color.WHITE);

            List<Task> tasks = database.tasks().findByStatus(accountId(), status);
            for (Task t : tasks) {
                list.add(buildTaskCard(t));
            }

            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setHorizontalScrollBarPolicy(
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );

            columns.add(section(
                    status.toUpperCase(Locale.ENGLISH) + "   " + tasks.size(),
                    scroll
            ));
        }

        return columns;
    }

    private JButton buildTaskCard(Task t) {
        String check = t.isCompleted() && t.getProof() != null ? " ✓" : "";

        JButton item = new JButton(
                "<html><b>" + html(t.getTitle()) + check
                        + "</b><br>" + html(t.getCategory())
                        + "<br>" + t.getDate() + "</html>"
        );
        item.setHorizontalAlignment(SwingConstants.LEFT);
        item.setBackground(Color.WHITE);
        item.setForeground(HEADER);
        item.setFocusPainted(false);
        item.setMaximumSize(new Dimension(500, 80));
        item.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 7, 8, 7)
        ));
        item.addActionListener(event -> app.editTask(t));
        return item;
    }
}
