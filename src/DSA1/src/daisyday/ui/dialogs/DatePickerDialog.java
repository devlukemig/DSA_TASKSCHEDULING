package DSA1.src.daisyday.ui.dialogs;

import DSA1.src.daisyday.ui.components.MonthPanel;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.function.Function;

import static DSA1.src.daisyday.ui.UiFactory.button;

/** Modal calendar for picking a due date. */
public class DatePickerDialog extends JDialog {

    private final Function<YearMonth, Map<LocalDate, Integer>> taskCounts;
    private final JPanel container = new JPanel(new BorderLayout());

    private LocalDate selection;
    private YearMonth month;
    private LocalDate result;

    private DatePickerDialog(
            Window owner, Component parent, LocalDate initial,
            Function<YearMonth, Map<LocalDate, Integer>> taskCounts) {

        super(owner, "Select Due Date", Dialog.ModalityType.APPLICATION_MODAL);
        this.taskCounts = taskCounts;
        this.selection = initial;
        this.month = YearMonth.from(initial);

        rebuild();

        JButton set = button("Set Date");
        set.addActionListener(event -> {
            result = selection;
            dispose();
        });

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> dispose());

        JPanel actions = new JPanel();
        actions.add(cancel);
        actions.add(set);

        add(container, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        setSize(470, 390);
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    /** Shows the picker and returns the chosen date, or null if cancelled. */
    public static LocalDate pick(
            Component parent, LocalDate initial,
            Function<YearMonth, Map<LocalDate, Integer>> taskCounts) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        DatePickerDialog dialog = new DatePickerDialog(owner, parent, initial, taskCounts);
        dialog.setVisible(true);
        return dialog.result;
    }

    private void rebuild() {
        container.removeAll();
        container.add(new MonthPanel(
                month, selection, taskCounts.apply(month),
                date -> {
                    selection = date;
                    rebuild();
                },
                value -> {
                    month = value;
                    rebuild();
                }
        ), BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }
}
