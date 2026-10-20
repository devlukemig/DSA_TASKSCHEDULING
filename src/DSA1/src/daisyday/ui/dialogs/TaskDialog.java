package DSA1.src.daisyday.ui.dialogs;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.Session;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static DSA1.src.daisyday.ui.UiFactory.button;
import static DSA1.src.daisyday.ui.UiFactory.label;
import static DSA1.src.daisyday.ui.UiFactory.section;

/** Form for creating a task or editing an existing one. */
public class TaskDialog extends JDialog {

    private static final String STATUS_COMPLETED = Task.COMPLETED;

    private final Session session;
    private final Database database;
    private final Task existing;
    private final boolean editing;
    private final Runnable onSaved;

    private final JTextField title;
    private final JComboBox<String> category;
    private final JComboBox<String> status;
    private final JComboBox<String> priority;
    private final JButton dateButton;
    private final JSpinner start;
    private final JSpinner end;
    private final JCheckBox requiresProof = new JCheckBox("This task requires proof");
    private final ProofCard proofCard; // only for tasks that require proof

    private LocalDate date;

    /**
     * @param existing the task to edit, or null to create a new one
     * @param onSaved  runs after the task has been saved
     */
    public TaskDialog(
            Frame owner, Session session, Database database,
            Task existing, Runnable onSaved) {
        super(owner, existing != null ? "Task Details" : "Create New Task", true);
        this.session = session;
        this.database = database;
        this.existing = existing;
        this.editing = existing != null;
        this.onSaved = onSaved;

        this.date = editing ? existing.getDate() : LocalDate.now();

        title = new JTextField(editing ? existing.getTitle() : "", 23);
        category = new JComboBox<>(
                database.accounts().findCategories(session.getAccountId())
                        .toArray(new String[0])
        );
        // A new task always starts as Pending (no status choice is shown for it).
        // An existing task can stay where it is or move forward, never back.
        status = new JComboBox<>((editing
                ? Task.statusesFrom(existing.getStatus())
                : List.of(Task.PENDING)).toArray(new String[0]));
        priority = new JComboBox<>(Task.PRIORITIES.toArray(new String[0]));

        dateButton = new JButton(date + " ▾");
        dateButton.addActionListener(event -> chooseDate());

        start = timeSpinner(editing ? existing.getStart() : LocalTime.of(9, 0));
        end = timeSpinner(editing ? existing.getEnd() : LocalTime.of(10, 0));

        if (editing) {
            category.setSelectedItem(existing.getCategory());
            status.setSelectedItem(existing.getStatus());
            priority.setSelectedItem(existing.getPriority());
            lockDetails();
        }

        requiresProof.setSelected(editing && existing.isRequiresProof());
        requiresProof.setBackground(Color.WHITE);

        // A completed task keeps the proof it was completed with: it can be viewed, not changed.
        proofCard = editing && existing.isRequiresProof()
                ? new ProofCard(this, existing.isCompleted(), existing.getProof())
                : null;
        if (proofCard != null) {
            proofCard.setChangeListener(file -> {
                pack(); // the card is taller with a file attached
                setLocationRelativeTo(getOwner());
            });
        }

        JPanel root = section(getTitle(), buildForm());
        root.add(buildActions(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    // ---------- layout ----------

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        formRow(form, 0, "Task Title", title);
        formRow(form, 1, "Category", category);
        formRow(form, 2, "Due Date", dateButton);
        formRow(form, 3, "Start Time", start);
        formRow(form, 4, "End Time", end);
        formRow(form, 5, "Priority", priority);
        if (editing) {
            formRow(form, 6, "Status", status);
        }

        // Whether a task needs proof is chosen only when it is created.
        // Attaching the proof happens afterwards, in the task details.
        if (!editing) {
            formRow(form, 7, "Proof", requiresProof);
        } else if (proofCard != null) {
            proofRow(form, 7);
        }
        return form;
    }

    /** The proof card, with its label at the top left instead of centered. */
    private void proofRow(JPanel form, int row) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(10, 0, 6, 12);
        form.add(label("Proof", 13), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(proofCard, c);
    }

    private JPanel buildActions() {
        JButton save = button(editing ? "Save Changes" : "Save Task");
        save.addActionListener(event -> save());

        JButton cancel = new JButton(editing ? "Close" : "Cancel");
        cancel.addActionListener(event -> dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setBackground(Color.WHITE);
        actions.add(cancel);
        actions.add(save);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Color.WHITE);
        bar.add(actions, BorderLayout.EAST);

        if (editing) {
            JButton archive = Modal.secondaryButton("Archive");
            archive.setToolTipText("Move this task to the archive");
            archive.addActionListener(event -> archive());

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT));
            left.setBackground(Color.WHITE);
            left.add(archive);
            bar.add(left, BorderLayout.WEST);
        }
        return bar;
    }

    /** Moves the task to the archive after asking. It can be restored from the Archive page. */
    private void archive() {
        String name = "\"" + existing.getTitle() + "\"";
        if (!Modal.confirm(this, "Archive Task",
                "Move " + name + " to the archive?\nIt stays there for " + Task.ARCHIVE_DAYS
                        + " days and is then deleted permanently. You can restore it before that.",
                "Archive", false)) {
            return;
        }

        try {
            database.tasks().archive(session.getAccountId(), existing.getId(), Task.ARCHIVED_BY_USER);
        } catch (DataAccessException exception) {
            Modal.error(this, exception.getMessage());
            return;
        }

        onSaved.run();
        dispose();
        Modal.success(getOwner(), "Task Archived",
                name + " was moved to the archive. You can restore it from the Archive page.");
    }

    private void formRow(JPanel form, int row, String text, JComponent input) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 12);
        form.add(label(text, 13), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(input, c);
    }

    // ---------- actions ----------

    private void chooseDate() {
        long accountId = session.getAccountId();
        LocalDate picked = DatePickerDialog.pick(
                this, date,
                month -> database.tasks().countsByDay(accountId, month)
        );
        if (picked != null) {
            date = picked;
            dateButton.setText(picked + " ▾");
        }
    }

    private void save() {
        if (editing) {
            updateStatus();
        } else {
            createTask();
        }
    }

    /** Task details: only the status (and the proof, if the task needs one) can change. */
    private void updateStatus() {
        String newStatus = (String) status.getSelectedItem();
        File newProof = existing.isRequiresProof() && proofCard != null ? proofCard.getFile() : null;

        boolean statusChanged = !newStatus.equals(existing.getStatus());
        boolean proofChanged = !Objects.equals(newProof, existing.getProof());

        if (!statusChanged && !proofChanged) {
            Modal.info(this, "No Changes",
                    "You haven't changed anything, so there is nothing to save.");
            return;
        }

        if (existing.isRequiresProof() && STATUS_COMPLETED.equals(newStatus)
                && (newProof == null || !newProof.isFile())) {
            Modal.warning(this,
                    "This task requires proof.\nAttach a file before marking it as Completed.");
            return;
        }

        String oldStatus = existing.getStatus();
        File oldProof = existing.getProof();

        existing.setStatus(newStatus);
        existing.setProof(newProof);

        try {
            database.tasks().save(session.getAccountId(), existing);
        } catch (DataAccessException exception) {
            existing.setStatus(oldStatus); // keep the task as it was
            existing.setProof(oldProof);
            Modal.error(this, exception.getMessage());
            return;
        }

        onSaved.run();
        dispose();

        String name = "\"" + existing.getTitle() + "\"";
        if (statusChanged && existing.isCompleted()) {
            Modal.success(getOwner(), "Task Completed", name + " is now marked as completed. Nice work!");
        } else if (statusChanged) {
            Modal.success(getOwner(), "Task Updated", name + " is now " + newStatus + ".");
        } else {
            Modal.success(getOwner(), "Proof Saved", "The proof for " + name + " was saved.");
        }
    }

    private void createTask() {
        if (title.getText().trim().isEmpty()) {
            Modal.warning(this, "Please enter a task title.");
            return;
        }

        try {
            start.commitEdit();
            end.commitEdit();
        } catch (ParseException exception) {
            Modal.warning(this, "Please enter a valid time.");
            return;
        }

        LocalTime startValue = spinnerTime(start);
        LocalTime endValue = spinnerTime(end);

        if (!endValue.isAfter(startValue)) {
            Modal.warning(this, "End time must be later than start time.");
            return;
        }

        if (!confirmNoConflict(startValue, endValue)) {
            return; // user chose "Adjust Schedule"; nothing has been saved
        }

        Task t = new Task();
        t.setTitle(title.getText().trim());
        t.setCategory((String) category.getSelectedItem());
        t.setStatus(Task.PENDING);
        t.setPriority((String) priority.getSelectedItem());
        t.setDate(date);
        t.setStart(startValue);
        t.setEnd(endValue);
        t.setRequiresProof(requiresProof.isSelected());
        t.setProof(null); // proof is attached afterwards, in the task details

        try {
            database.tasks().save(session.getAccountId(), t);
        } catch (DataAccessException exception) {
            Modal.error(this, exception.getMessage());
            return;
        }

        onSaved.run();
        dispose();
        notifySaved(t, false);
    }

    /** Task details are read-only except for the status. */
    private void lockDetails() {
        title.setEditable(false);
        category.setEnabled(false);
        priority.setEnabled(false);
        dateButton.setEnabled(false);
        for (JSpinner spinner : new JSpinner[]{start, end}) {
            spinner.setEnabled(false);
            if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor) {
                editor.getTextField().setEditable(false);
            }
        }
        // nothing left to choose when the task is already completed
        status.setEnabled(status.getItemCount() > 1);
    }

    /**
     * Soft conflict check: looks for other tasks overlapping this one and, if there are
     * any, lets the user choose. Returns true to go ahead and save, false to stay in the form.
     */
    private boolean confirmNoConflict(LocalTime startValue, LocalTime endValue) {
        List<Task> conflicts;
        try {
            conflicts = database.tasks().findOverlapping(
                    session.getAccountId(), date, startValue, endValue,
                    editing ? existing.getId() : null);
        } catch (DataAccessException exception) {
            Modal.error(this, exception.getMessage());
            return false;
        }

        if (conflicts.isEmpty() || ConflictDialog.confirmSave(this, conflicts)) {
            return true;
        }

        start.requestFocusInWindow();
        return false;
    }

    /** Tells the user what just happened, over the main window. */
    private void notifySaved(Task saved, boolean wasCompleted) {
        String name = "\"" + saved.getTitle() + "\"";
        if (saved.isCompleted() && !wasCompleted) {
            Modal.success(getOwner(), "Task Completed", name + " is now marked as completed. Nice work!");
        } else if (editing) {
            Modal.success(getOwner(), "Task Updated", "Your changes to " + name + " were saved.");
        } else {
            Modal.success(getOwner(), "Task Created", name + " was added to your tasks.");
        }
    }

    // ---------- time spinner helpers ----------

    private static JSpinner timeSpinner(LocalTime time) {
        Date value = Date.from(
                LocalDate.now().atTime(time)
                        .atZone(ZoneId.systemDefault()).toInstant()
        );
        JSpinner spinner = new JSpinner(new SpinnerDateModel(
                value, null, null, Calendar.MINUTE
        ));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "h:mm a"));
        return spinner;
    }

    private static LocalTime spinnerTime(JSpinner spinner) {
        return ((Date) spinner.getValue()).toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalTime().withSecond(0).withNano(0);
    }
}
