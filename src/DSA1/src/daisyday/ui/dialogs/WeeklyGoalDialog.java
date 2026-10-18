package DSA1.src.daisyday.ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.OptionalInt;

import static DSA1.src.daisyday.ui.Theme.BLUE;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;
import static DSA1.src.daisyday.ui.UiFactory.link;

/** Asks how many tasks the user wants to complete each week. */
public final class WeeklyGoalDialog {

    public static final int MAX_GOAL = 999;
    private static final int[] QUICK_PICKS = {5, 10, 15, 20};

    /** @param saved false when cancelled; @param goal null means "use the automatic goal" */
    public record Result(boolean saved, Integer goal) {
    }

    private WeeklyGoalDialog() {
    }

    /**
     * @param current   the goal the user set earlier, if any
     * @param suggested the number to start from when there is no goal yet
     */
    public static Result show(Component owner, OptionalInt current, int suggested) {
        Result[] result = {new Result(false, null)};
        int start = current.isPresent() ? current.getAsInt()
                : suggested > 0 ? Math.min(suggested, MAX_GOAL) : 10;

        JTextField number = new JTextField(String.valueOf(start), 3);
        number.setFont(new Font("Serif", Font.PLAIN, 40));
        number.setForeground(HEADER);
        number.setHorizontalAlignment(SwingConstants.CENTER);
        number.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, HEADER));
        number.setMaximumSize(new Dimension(110, 60));
        ((PlainDocument) number.getDocument()).setDocumentFilter(new DigitsOnly());

        JLabel error = new JLabel(" ");
        error.setFont(new Font("SansSerif", Font.PLAIN, 11));
        error.setForeground(new Color(200, 50, 50));
        error.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton minus = round("−");
        JButton plus = round("+");
        minus.addActionListener(event -> nudge(number, error, -1));
        plus.addActionListener(event -> nudge(number, error, +1));

        JPanel stepper = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        stepper.setOpaque(false);
        stepper.add(minus);
        stepper.add(number);
        stepper.add(plus);

        JPanel picks = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        picks.setOpaque(false);
        for (int value : QUICK_PICKS) {
            JButton pick = new JButton(String.valueOf(value));
            pick.setFont(new Font("SansSerif", Font.BOLD, 12));
            pick.setBackground(BLUE);
            pick.setForeground(HEADER);
            pick.setFocusPainted(false);
            pick.setBorder(BorderFactory.createEmptyBorder(5, 16, 5, 16));
            pick.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pick.addActionListener(event -> {
                number.setText(String.valueOf(value));
                error.setText(" ");
            });
            picks.add(pick);
        }

        JLabel question = new JLabel("How many tasks do you want to complete each week?", SwingConstants.CENTER);
        question.setFont(new Font("Serif", Font.PLAIN, 15));
        question.setForeground(HEADER);
        question.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel unit = new JLabel("tasks per week", SwingConstants.CENTER);
        unit.setFont(new Font("SansSerif", Font.PLAIN, 11));
        unit.setForeground(MUTED);
        unit.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel quick = new JLabel("Quick pick", SwingConstants.CENTER);
        quick.setFont(new Font("SansSerif", Font.PLAIN, 11));
        quick.setForeground(MUTED);
        quick.setAlignmentX(Component.CENTER_ALIGNMENT);

        stepper.setAlignmentX(Component.CENTER_ALIGNMENT);
        picks.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(22, 30, 14, 30));
        body.add(question);
        body.add(Box.createVerticalStrut(18));
        body.add(stepper);
        body.add(Box.createVerticalStrut(4));
        body.add(unit);
        body.add(Box.createVerticalStrut(16));
        body.add(quick);
        body.add(Box.createVerticalStrut(6));
        body.add(picks);
        body.add(Box.createVerticalStrut(10));
        body.add(error);

        JButton save = Modal.primaryButton("Save Goal");
        JButton cancel = Modal.secondaryButton("Cancel");

        JButton automatic = null;
        if (current.isPresent()) {
            automatic = link("Use automatic goal");
            automatic.setForeground(MUTED);
        }

        JDialog dialog = Modal.create(owner, "Weekly Goal", body,
                Modal.footerOf(automatic, cancel, save), save);

        cancel.addActionListener(event -> dialog.dispose());
        if (automatic != null) {
            automatic.addActionListener(event -> {
                result[0] = new Result(true, null);
                dialog.dispose();
            });
        }
        save.addActionListener(event -> {
            int value = parse(number.getText());
            if (value < 1 || value > MAX_GOAL) {
                error.setText("Enter a whole number from 1 to " + MAX_GOAL + ".");
                number.requestFocusInWindow();
                return;
            }
            result[0] = new Result(true, value);
            dialog.dispose();
        });

        number.selectAll();
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent event) {
                number.requestFocusInWindow();
                number.selectAll();
            }
        });
        dialog.setVisible(true); // blocks until closed
        return result[0];
    }

    // ---------- helpers ----------

    private static JButton round(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 20));
        button.setForeground(HEADER);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(40, 40));
        button.setBorder(BorderFactory.createLineBorder(new Color(200, 190, 190), 1));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static void nudge(JTextField field, JLabel error, int step) {
        int value = parse(field.getText());
        if (value < 1) {
            value = step > 0 ? 0 : 2;
        }
        field.setText(String.valueOf(Math.max(1, Math.min(MAX_GOAL, value + step))));
        error.setText(" ");
    }

    private static int parse(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Lets only digits (at most three) into the field. */
    private static final class DigitsOnly extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
                throws BadLocationException {
            replace(fb, offset, 0, text, attrs);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String digits = text == null ? "" : text.replaceAll("\\D", "");
            int room = 3 - (fb.getDocument().getLength() - length);
            if (digits.length() > room) {
                digits = digits.substring(0, Math.max(0, room));
            }
            if (!digits.isEmpty() || length > 0) {
                super.replace(fb, offset, length, digits, attrs);
            }
        }
    }
}
