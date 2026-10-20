package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Session;
import DSA1.src.daisyday.model.TaskFilter;
import DSA1.src.daisyday.ui.components.CategoryChips;
import DSA1.src.daisyday.ui.components.SearchField;
import DSA1.src.daisyday.ui.dialogs.Modal;
import DSA1.src.daisyday.util.TextFormat;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;

import static DSA1.src.daisyday.ui.Theme.GREEN;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;
import static DSA1.src.daisyday.ui.Theme.categoryColor;
import static DSA1.src.daisyday.ui.UiFactory.button;
import static DSA1.src.daisyday.ui.UiFactory.html;
import static DSA1.src.daisyday.ui.UiFactory.label;
import static DSA1.src.daisyday.ui.UiFactory.section;

/** Task list (by status) and category management. */
public class TasksPage extends Page {

    private static final int MAX_CATEGORY_NAME = 60;
    private static final int CARD_CATEGORY_LIMIT = 12;

    private static final String ALL_CATEGORIES = "All categories";
    private static final String ALL_PRIORITIES = "All priorities";

    // The filter lives in the page, so it survives the refreshes that follow every change.
    private final TaskFilter filter = new TaskFilter();
    private final SearchField search = new SearchField("Search tasks...");
    private final JComboBox<String> categoryBox = new JComboBox<>();
    private final JComboBox<String> priorityBox = new JComboBox<>();
    private final JComboBox<TaskFilter.Sort> sortBox = new JComboBox<>(TaskFilter.Sort.values());
    private final JButton clear = Modal.secondaryButton("Clear");
    private final JPanel columnsHolder = new JPanel(new BorderLayout());
    private boolean syncing; // true while the boxes are filled in by code, not by the user

    public TasksPage(Session session, Database database, AppController app) {
        super(session, database, app);

        priorityBox.addItem(ALL_PRIORITIES);
        Task.PRIORITIES.reversed().forEach(priorityBox::addItem);
        for (JComboBox<?> box : List.of(categoryBox, priorityBox, sortBox)) {
            box.setFont(new Font("SansSerif", Font.PLAIN, 12));
            box.setBackground(Color.WHITE);
            box.setFocusable(false);
        }
        categoryBox.setPrototypeDisplayValue(ALL_CATEGORIES + "  ");
        priorityBox.setPrototypeDisplayValue(ALL_PRIORITIES + "  ");
        sortBox.setPrototypeDisplayValue(TaskFilter.Sort.PRIORITY);

        clear.setFont(new Font("Serif", Font.BOLD, 12));
        clear.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 190, 190), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));

        columnsHolder.setOpaque(false);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filterChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filterChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filterChanged();
            }
        });
        categoryBox.addActionListener(event -> filterChanged());
        priorityBox.addActionListener(event -> filterChanged());
        sortBox.addActionListener(event -> filterChanged());
        clear.addActionListener(event -> {
            syncing = true;
            search.setText("");
            categoryBox.setSelectedIndex(0);
            priorityBox.setSelectedIndex(0);
            sortBox.setSelectedIndex(0);
            syncing = false;
            filterChanged();
        });
    }

    @Override
    public void refresh() {
        removeAll();
        add(buildTop(), BorderLayout.NORTH);

        syncCategoryBox();

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.setOpaque(false);
        top.add(section("CATEGORIES", buildCategoryBody()), BorderLayout.NORTH);
        top.add(buildFilterBar(), BorderLayout.CENTER);

        JPanel body = new JPanel(new BorderLayout(10, 10));
        body.setOpaque(false);
        body.add(top, BorderLayout.NORTH);
        body.add(columnsHolder, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
        showColumns();
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

    // ---------- search, filter and sort ----------

    /** Search box, category, priority and sort order in one row. */
    private JPanel buildFilterBar() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 224, 224), 3),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));

        // fixed widths, so the search box gets all the space that is left
        categoryBox.setPreferredSize(new Dimension(118, 28));
        priorityBox.setPreferredSize(new Dimension(104, 28));
        sortBox.setPreferredSize(new Dimension(134, 28));
        clear.setPreferredSize(new Dimension(72, 28));
        search.setPreferredSize(new Dimension(120, 28));

        JPanel boxes = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        boxes.setOpaque(false);
        boxes.add(categoryBox);
        boxes.add(priorityBox);
        boxes.add(sortBox);

        JPanel right = new JPanel(new BorderLayout(6, 0));
        right.setOpaque(false);
        right.add(boxes, BorderLayout.CENTER);
        right.add(clear, BorderLayout.EAST);

        row.add(search, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    /** Fills the category box from the database and keeps the chosen category if it still exists. */
    private void syncCategoryBox() {
        syncing = true;
        categoryBox.removeAllItems();
        categoryBox.addItem(ALL_CATEGORIES);
        List<String> names = database.accounts().findCategories(accountId());
        names.forEach(categoryBox::addItem);

        String wanted = filter.getCategory();
        if (wanted != null && names.stream().anyMatch(wanted::equalsIgnoreCase)) {
            categoryBox.setSelectedItem(names.stream().filter(wanted::equalsIgnoreCase).findFirst().get());
        } else {
            categoryBox.setSelectedIndex(0); // that category was deleted
            filter.setCategory(null);
        }
        syncing = false;
    }

    private void filterChanged() {
        if (syncing) {
            return;
        }

        filter.setQuery(search.getText());
        filter.setCategory(categoryBox.getSelectedIndex() <= 0 ? null : (String) categoryBox.getSelectedItem());
        filter.setPriority(priorityBox.getSelectedIndex() <= 0 ? null : (String) priorityBox.getSelectedItem());
        filter.setSort((TaskFilter.Sort) sortBox.getSelectedItem());
        showColumns();
    }

    /** Rebuilds only the three columns, so typing in the search box keeps its focus. */
    private void showColumns() {
        clear.setEnabled(!filter.isDefault());
        columnsHolder.removeAll();
        columnsHolder.add(buildStatusColumns(), BorderLayout.CENTER);
        columnsHolder.revalidate();
        columnsHolder.repaint();
    }

    // ---------- categories ----------

    private JPanel buildCategoryBody() {
        JPanel categoryBody = new JPanel(new BorderLayout(6, 6));
        categoryBody.setBackground(Color.WHITE);
        categoryBody.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        CategoryChips chips = new CategoryChips(this::deleteCategory);
        chips.setCategories(database.accounts().findCategoryList(accountId()));

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
                            + ". They will be moved to the Archive and deleted permanently after "
                            + Task.ARCHIVE_DAYS + " days, unless you restore them.";
            if (!app.confirm("Delete Category", question, "Delete", true)) {
                return;
            }

            int archived = database.accounts().deleteCategory(accountId(), name);
            app.refreshAll();
            app.success("Category Deleted", archived == 0
                    ? "\"" + name + "\" was deleted."
                    : "\"" + name + "\" was deleted. Its " + archived
                            + (archived == 1 ? " task was" : " tasks were") + " moved to the Archive.");

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

            List<Task> all = database.tasks().findByStatus(accountId(), status);
            List<Task> tasks = filter.apply(all);
            for (Task t : tasks) {
                list.add(buildTaskCard(t));
            }
            if (tasks.isEmpty() && !all.isEmpty()) {
                list.add(buildNoMatch());
            }

            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setHorizontalScrollBarPolicy(
                    JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            );

            columns.add(section(
                    status.toUpperCase(Locale.ENGLISH) + "   " + tasks.size()
                            + (filter.isFiltering() ? " / " + all.size() : ""),
                    scroll
            ));
        }

        return columns;
    }

    /** Shown in a column whose tasks are all hidden by the filter. */
    private JComponent buildNoMatch() {
        JLabel text = new JLabel("No matching tasks");
        text.setFont(new Font("SansSerif", Font.PLAIN, 12));
        text.setForeground(MUTED);
        text.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));
        text.setAlignmentX(Component.LEFT_ALIGNMENT);
        return text;
    }

    /** A task on the board: a stripe and pill in its category's color, then title and date. */
    private JPanel buildTaskCard(Task t) {
        Color color = categoryColor(t.getCategoryColor());

        JLabel title = new JLabel(t.getTitle());
        title.setFont(new Font("SansSerif", Font.BOLD, 12));
        title.setForeground(HEADER);

        JPanel titleRow = new JPanel(new BorderLayout(5, 0));
        titleRow.setOpaque(false);
        titleRow.add(title, BorderLayout.CENTER);
        if (t.isCompleted()) {
            JLabel check = new JLabel("✓");
            check.setFont(new Font("SansSerif", Font.BOLD, 14));
            check.setForeground(GREEN);
            titleRow.add(check, BorderLayout.EAST);
        }

        JLabel pill = new JLabel(TextFormat.shorten(t.getCategory(), CARD_CATEGORY_LIMIT));
        pill.setOpaque(true);
        pill.setBackground(color);
        pill.setForeground(Color.BLACK);
        pill.setFont(new Font("Serif", Font.PLAIN, 12));
        pill.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        pill.setToolTipText(t.getCategory());

        JPanel pillRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pillRow.setOpaque(false);
        pillRow.add(pill);

        JLabel date = new JLabel(t.getDate().toString());
        date.setFont(new Font("SansSerif", Font.PLAIN, 11));
        date.setForeground(MUTED);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(8, 9, 8, 7));
        for (JComponent line : new JComponent[]{titleRow, pillRow, date}) {
            line.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(line);
            text.add(Box.createVerticalStrut(4));
        }

        JPanel stripe = new JPanel();
        stripe.setBackground(color);
        stripe.setPreferredSize(new Dimension(5, 10));

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.add(stripe, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        // natural height, so cards in a short column are not stretched
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));

        MouseAdapter open = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                app.editTask(t);
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                card.setBackground(new Color(250, 246, 246));
            }

            @Override
            public void mouseExited(MouseEvent event) {
                card.setBackground(Color.WHITE);
            }
        };
        for (JComponent part : new JComponent[]{card, text, titleRow, title, pillRow, pill, date, stripe}) {
            part.addMouseListener(open);
        }
        return card;
    }
}
