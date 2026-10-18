package DSA1.src.daisyday.ui.dialogs;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** The round icon at the left of a message modal: check, cross, "!" , "i" or "?". */
final class ModalIcon extends JComponent {

    private final Modal.Kind kind;

    ModalIcon(Modal.Kind kind) {
        this.kind = kind;
        setPreferredSize(new Dimension(58, 58));
        setOpaque(false);
    }

    static Color colorOf(Modal.Kind kind) {
        return switch (kind) {
            case SUCCESS -> new Color(36, 195, 110);
            case ERROR -> new Color(214, 64, 64);
            case WARNING -> new Color(245, 166, 35);
            case INFO -> new Color(90, 160, 235);
            case QUESTION -> new Color(86, 53, 53);
        };
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int size = Math.min(getWidth(), getHeight());
        Color color = colorOf(kind);

        // soft halo, then the solid circle
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
        g.fillOval(0, 0, size, size);
        int inset = 7;
        g.setColor(color);
        g.fillOval(inset, inset, size - 2 * inset, size - 2 * inset);

        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(size / 12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double c = size / 2.0;
        double u = size / 100.0; // 1 unit = 1% of the icon

        switch (kind) {
            case SUCCESS -> {
                g.drawLine(px(c - 14 * u), px(c + 1 * u), px(c - 4 * u), px(c + 11 * u));
                g.drawLine(px(c - 4 * u), px(c + 11 * u), px(c + 15 * u), px(c - 10 * u));
            }
            case ERROR -> {
                g.drawLine(px(c - 11 * u), px(c - 11 * u), px(c + 11 * u), px(c + 11 * u));
                g.drawLine(px(c + 11 * u), px(c - 11 * u), px(c - 11 * u), px(c + 11 * u));
            }
            case WARNING -> {
                g.drawLine(px(c), px(c - 14 * u), px(c), px(c + 3 * u));
                g.fillOval(px(c - 3 * u), px(c + 9 * u), px(6 * u), px(6 * u));
            }
            case INFO -> {
                g.drawLine(px(c), px(c - 2 * u), px(c), px(c + 14 * u));
                g.fillOval(px(c - 3 * u), px(c - 15 * u), px(6 * u), px(6 * u));
            }
            case QUESTION -> {
                g.setFont(new Font("Serif", Font.BOLD, (int) (size * 0.55)));
                FontMetrics fm = g.getFontMetrics();
                g.drawString("?", px(c - fm.stringWidth("?") / 2.0), px(c + fm.getAscent() / 2.0 - 3 * u));
            }
        }
        g.dispose();
    }

    private static int px(double value) {
        return (int) Math.round(value);
    }
}
