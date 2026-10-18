package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Session;
import DSA1.src.daisyday.ui.components.CategoryChips;
import DSA1.src.daisyday.util.TextFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
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

    private static final int MAX_CATEGORY_NAME = 60;
    private static final int CARD_CATEGORY_LIMIT = 12;

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

        CategoryChips chips = new CategoryChips(this::deleteCategory);
        chips.setCategories(database.accounts().findCategories(accountId()));

        categoryBody.add(chips, BorderLayout.CENTER);
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
            app.warn("Enter a category name.");
            return;
        }

        if (name.length() > MAX_CATEGORY_NAME) {
            app.warn("Category names can be up to " + MAX_CATEGORY_NAME + " characters long.");
            return;
        }

        try {
            if (database.accounts().categoryExists(accountId(), name)) {
                app.warn("This category already exists.");
                return;
            }
            database.accounts().addCategory(accountId(), name);

        } catch (DataAccessException e) {
            app.error(e.getMessage());
            return;
        }

        app.refreshAll();
        app.success("Category Added", "\"" + name + "\" was added to your categories.");
    }

    private void deleteCategory(String name) {
        if (!session.isLoggedIn()) {
            return;
        }

        try {
            if (database.accounts().findCategories(accountId()).size() <= 1) {
                app.warn("You need at least one category, so \"" + name + "\" can't be deleted.");
                return;
            }

            int taskCount = database.accounts().countTasksInCategory(accountId(), name);
            String tasks = taskCount + (taskCount == 1 ? " task" : " tasks");

            String question = taskCount == 0
                    ? "Delete the category \"" + name + "\"?"
                    : "Delete the category \"" + name + "\"?\nIt has " + tasks
                            + ", and they will be deleted too. This can't be undone.";
            if (!app.confirm("Delete Category", question, "Delete", true)) {
                return;
            }

            database.accounts().deleteCategory(accountId(), name);
            app.refreshAll();
            app.success("Category Deleted", taskCount == 0
                    ? "\"" + name + "\" was deleted."
                    : "\"" + name + "\" and its " + tasks + " were deleted.");

        } catch (DataAccessException e) {
            app.error(e.getMessage());
        }
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
                        + "</b><br>" + html(TextFormat.shorten(t.getCategory(), CARD_CATEGORY_LIMIT))
                        + "<br>" + t.getDate() + "</html>"
        );
        item.setToolTipText(t.getCategory());
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
