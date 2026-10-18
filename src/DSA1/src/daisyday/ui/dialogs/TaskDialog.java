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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.filechooser.FileNameExtensionFilter;
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
    private final JLabel proofName = new JLabel();

    private LocalDate date;
    private File proof;

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
        this.proof = editing ? existing.getProof() : null;

        title = new JTextField(editing ? existing.getTitle() : "", 23);
        category = new JComboBox<>(
                database.accounts().findCategories(session.getAccountId())
                        .toArray(new String[0])
        );
        status = new JComboBox<>(Task.STATUSES.toArray(new String[0]));
        priority = new JComboBox<>(Task.PRIORITIES.toArray(new String[0]));

        dateButton = new JButton(date + " ▾");
        dateButton.addActionListener(event -> chooseDate());

        start = timeSpinner(editing ? existing.getStart() : LocalTime.of(9, 0));
        end = timeSpinner(editing ? existing.getEnd() : LocalTime.of(10, 0));

        if (editing) {
            category.setSelectedItem(existing.getCategory());
            status.setSelectedItem(existing.getStatus());
            priority.setSelectedItem(existing.getPriority());
        }

        requiresProof.setSelected(editing && existing.isRequiresProof());
        requiresProof.setBackground(Color.WHITE);

        proofName.setText(proof == null ? "No proof attached" : proof.getName());

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
        formRow(form, 6, "Status", status);

        // Whether a task needs proof is chosen only when it is created.
        // Attaching the proof happens afterwards, in the task details.
        if (!editing) {
            formRow(form, 7, "Proof", requiresProof);
        } else if (existing.isRequiresProof()) {
            formRow(form, 7, "Attachment", buildProofPanel());
        }
        return form;
    }

    private JPanel buildProofPanel() {
        JButton attach = button("Attach Proof Here");
        JPopupMenu options = new JPopupMenu();

        JMenuItem file = new JMenuItem("Attach File");
        JMenuItem photo = new JMenuItem("Photo Library");
        file.addActionListener(event -> chooseProof(false));
        photo.addActionListener(event -> chooseProof(true));

        options.add(file);
        options.add(photo);
        attach.addActionListener(event ->
                options.show(attach, 0, attach.getHeight())
        );

        JPanel proofPanel = new JPanel(new BorderLayout(5, 5));
        proofPanel.setBackground(Color.WHITE);
        proofPanel.add(attach, BorderLayout.NORTH);
        proofPanel.add(proofName, BorderLayout.CENTER);
        return proofPanel;
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
        return actions;
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

    private void chooseProof(boolean imagesOnly) {
        JFileChooser chooser = new JFileChooser();
        if (imagesOnly) {
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(new FileNameExtensionFilter(
                    "Images", "png", "jpg", "jpeg", "gif"
            ));
        }

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            if (!selected.isFile()) {
                JOptionPane.showMessageDialog(this, "Please select an existing file.");
                return;
            }

            proof = selected;
            proofName.setText(selected.getName());
        }
    }

    private void save() {
        if (title.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a task title.");
            return;
        }

        try {
            start.commitEdit();
            end.commitEdit();
        } catch (ParseException exception) {
            JOptionPane.showMessageDialog(this, "Please enter a valid time.");
            return;
        }

        LocalTime startValue = spinnerTime(start);
        LocalTime endValue = spinnerTime(end);

        if (!endValue.isAfter(startValue)) {
            JOptionPane.showMessageDialog(this, "End time must be later than start time.");
            return;
        }

        boolean needsProof = editing ? existing.isRequiresProof() : requiresProof.isSelected();
        if (needsProof && STATUS_COMPLETED.equals(status.getSelectedItem())
                && (proof == null || !proof.isFile())) {
            JOptionPane.showMessageDialog(this,
                    "This task requires proof. Attach a file before marking it as Completed.");
            return;
        }

        Task t = editing ? existing : new Task();
        t.setTitle(title.getText().trim());
        t.setCategory((String) category.getSelectedItem());
        t.setStatus((String) status.getSelectedItem());
        t.setPriority((String) priority.getSelectedItem());
        t.setDate(date);
        t.setStart(startValue);
        t.setEnd(endValue);
        t.setRequiresProof(needsProof);
        t.setProof(needsProof ? proof : null);

        try {
            database.tasks().save(session.getAccountId(), t);
        } catch (DataAccessException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage());
            return;
        }

        onSaved.run();
        dispose();
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
