package DSA1.src.daisyday.ui.dialogs;

import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.util.TimeFormat;

import javax.swing.JButton;
import javax.swing.JDialog;
import java.awt.Component;
import java.util.List;

/**
 * Soft warning shown when a task's time overlaps other tasks.
 * It never blocks anything: the user can save anyway or go back and adjust.
 */
public final class ConflictDialog {

    private static final int MAX_LISTED = 3;

    private ConflictDialog() {
    }

    /**
     * @return true if the user chose "Save Anyway"; false for "Adjust Schedule"
     *         (also when the window is closed with Esc)
     */
    public static boolean confirmSave(Component owner, List<Task> conflicts) {
        boolean[] saveAnyway = {false};

        JButton adjust = Modal.secondaryButton("Adjust Schedule");
        JButton save = Modal.primaryButton("Save Anyway");

        JDialog dialog = Modal.create(owner, "Schedule Conflict",
                Modal.messageBody(Modal.Kind.WARNING, message(conflicts)),
                Modal.footerOf(null, adjust, save),
                adjust); // Enter picks the safe choice

        adjust.addActionListener(event -> dialog.dispose());
        save.addActionListener(event -> {
            saveAnyway[0] = true;
            dialog.dispose();
        });

        dialog.setVisible(true); // blocks until closed
        return saveAnyway[0];
    }

    /** "This task overlaps with Study Mathematics from 9:00 AM to 10:00 AM." */
    static String message(List<Task> conflicts) {
        StringBuilder text = new StringBuilder();
        int shown = Math.min(conflicts.size(), MAX_LISTED);

        for (int i = 0; i < shown; i++) {
            Task other = conflicts.get(i);
            String line = "overlaps with \"" + other.getTitle() + "\" from "
                    + TimeFormat.time(other.getStart()) + " to " + TimeFormat.time(other.getEnd()) + ".";
            if (i == 0) {
                text.append("This task ").append(line);
            } else {
                text.append("\nIt also ").append(line);
            }
        }

        int hidden = conflicts.size() - shown;
        if (hidden > 0) {
            text.append("\nAnd ").append(hidden).append(hidden == 1 ? " more task." : " more tasks.");
        }

        text.append("\n\nYou can still save it, or go back and change the time.");
        return text.toString();
    }
}
