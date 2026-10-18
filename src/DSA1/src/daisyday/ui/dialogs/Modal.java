package DSA1.src.daisyday.ui.dialogs;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

import static DSA1.src.daisyday.ui.Theme.GRID;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.UiFactory.html;

/**
 * The app's own message windows: success, warning, error, info and
 * yes/no confirmations, all in the same maroon style as the rest of DaisyDay.
 * Other dialogs (like the weekly goal) reuse {@link #create} for the frame.
 */
public final class Modal {

    public enum Kind { SUCCESS, ERROR, WARNING, INFO, QUESTION }

    private static final Color DANGER = new Color(200, 50, 50);

    private Modal() {
    }

    // ---------- ready-made messages ----------

    public static void success(Component owner, String title, String message) {
        show(owner, Kind.SUCCESS, title, message, "OK", null, false);
    }

    public static void info(Component owner, String title, String message) {
        show(owner, Kind.INFO, title, message, "OK", null, false);
    }

    public static void warning(Component owner, String message) {
        show(owner, Kind.WARNING, "Check Your Input", message, "OK", null, false);
    }

    public static void error(Component owner, String message) {
        show(owner, Kind.ERROR, "Something Went Wrong", message, "OK", null, false);
    }

    /** Yes/no question. Returns true only if the user pressed the confirm button. */
    public static boolean confirm(
            Component owner, String title, String message, String confirmText, boolean danger) {
        return show(owner, danger ? Kind.WARNING : Kind.QUESTION,
                title, message, confirmText, "Cancel", danger);
    }

    // ---------- building blocks ----------

    /** The message content (icon + text) without a window around it. */
    static JPanel messageBody(Kind kind, String message) {
        JLabel text = new JLabel("<html><body style='width:250px'>"
                + html(message).replace("\n", "<br>") + "</body></html>");
        text.setFont(new Font("Serif", Font.PLAIN, 15));
        text.setForeground(HEADER);

        JPanel icon = new JPanel(new BorderLayout());
        icon.setOpaque(false);
        icon.add(new ModalIcon(kind), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 26));
        body.add(icon, BorderLayout.WEST);
        body.add(text, BorderLayout.CENTER);
        return body;
    }

    /** The footer strip that holds the buttons. */
    static JPanel footerOf(JComponent left, JComponent... buttons) {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        for (JComponent button : buttons) {
            right.add(button);
        }

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(250, 247, 247));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, GRID),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        if (left != null) {
            footer.add(left, BorderLayout.WEST);
        }
        footer.add(right, BorderLayout.EAST);
        return footer;
    }

    /**
     * An undecorated, modal window with a maroon title bar, your body and footer.
     * Esc closes it; Enter presses {@code defaultButton}.
     */
    public static JDialog create(
            Component owner, String heading, JComponent body, JComponent footer, JButton defaultButton) {

        Window window = owner == null ? null
                : owner instanceof Window w ? w : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(window, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        JLabel title = new JLabel(heading.toUpperCase(Locale.ENGLISH));
        title.setOpaque(true);
        title.setBackground(HEADER);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.PLAIN, 15));
        title.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        title.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        // the window has no title bar, so let the maroon bar drag it
        Point[] grab = new Point[1];
        title.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                grab[0] = event.getPoint();
            }
        });
        title.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                Point at = dialog.getLocation();
                dialog.setLocation(at.x + event.getX() - grab[0].x, at.y + event.getY() - grab[0].y);
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(HEADER, 1),
                BorderFactory.createLineBorder(new Color(229, 224, 224), 3)));
        root.add(title, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.getRootPane().registerKeyboardAction(
                event -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (defaultButton != null) {
            dialog.getRootPane().setDefaultButton(defaultButton);
        }
        dialog.pack();
        dialog.setLocationRelativeTo(window);
        return dialog;
    }

    // ---------- buttons ----------

    public static JButton primaryButton(String text) {
        return styled(text, HEADER, Color.WHITE, HEADER);
    }

    public static JButton dangerButton(String text) {
        return styled(text, DANGER, Color.WHITE, DANGER);
    }

    public static JButton secondaryButton(String text) {
        return styled(text, Color.WHITE, HEADER, new Color(200, 190, 190));
    }

    private static JButton styled(String text, Color background, Color foreground, Color border) {
        JButton button = new JButton(text);
        button.setFont(new Font("Serif", Font.BOLD, 14));
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(7, 18, 7, 18)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ---------- showing ----------

    private static boolean show(
            Component owner, Kind kind, String title, String message,
            String primaryText, String secondaryText, boolean danger) {

        boolean[] confirmed = {false};

        JButton primary = danger ? dangerButton(primaryText) : primaryButton(primaryText);
        JDialog[] holder = new JDialog[1];
        primary.addActionListener(event -> {
            confirmed[0] = true;
            holder[0].dispose();
        });

        JComponent footer;
        if (secondaryText == null) {
            footer = footerOf(null, primary);
        } else {
            JButton secondary = secondaryButton(secondaryText);
            secondary.addActionListener(event -> holder[0].dispose());
            footer = footerOf(null, secondary, primary);
        }

        holder[0] = create(owner, title, messageBody(kind, message), footer, primary);
        holder[0].setVisible(true); // blocks until closed
        return confirmed[0];
    }
}
