package DSA1.src.daisyday.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MAROON;

/** Small helpers that build the styled Swing components used everywhere. */
public final class UiFactory {

    private UiFactory() {
    }

    public static JLabel label(String text, int size) {
        JLabel result = new JLabel(text);
        result.setFont(new Font("Serif", Font.PLAIN, size));
        result.setForeground(MAROON);
        return result;
    }

    public static JButton button(String text) {
        JButton result = new JButton(text);
        result.setFont(new Font("Serif", Font.BOLD, 14));
        result.setBackground(HEADER);
        result.setForeground(Color.WHITE);
        result.setFocusPainted(false);
        result.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12));
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return result;
    }

    public static JButton link(String text) {
        JButton result = new JButton(text);
        result.setForeground(MAROON);
        result.setFont(new Font("SansSerif", Font.PLAIN, 12));
        result.setContentAreaFilled(false);
        result.setBorder(BorderFactory.createEmptyBorder());
        result.setFocusPainted(false);
        result.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return result;
    }

    public static JLabel tag(String text, Color color) {
        JLabel result = new JLabel(text, SwingConstants.CENTER);
        result.setOpaque(true);
        result.setBackground(color);
        result.setForeground(Color.BLACK);
        result.setFont(new Font("SansSerif", Font.BOLD, 10));
        result.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
        return result;
    }

    public static JPanel section(String title, JComponent body) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(
                new Color(229, 224, 224), 3
        ));

        JLabel heading = new JLabel(title);
        heading.setOpaque(true);
        heading.setBackground(HEADER);
        heading.setForeground(Color.WHITE);
        heading.setFont(new Font("Serif", Font.PLAIN, 15));
        heading.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        panel.add(heading, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    public static String html(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
