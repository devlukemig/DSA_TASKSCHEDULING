package DSA1.src.daisyday.ui.components;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import static DSA1.src.daisyday.ui.Theme.HEADER;

/** A thin rounded progress bar. */
public class GoalBar extends JComponent {

    private final double fraction;

    public GoalBar(double fraction) {
        this.fraction = Math.max(0, Math.min(1, fraction));
        setPreferredSize(new Dimension(10, 10));
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int h = getHeight();
        g.setColor(new Color(229, 224, 224));
        g.fillRoundRect(0, 0, getWidth(), h, h, h);
        int filled = (int) Math.round(getWidth() * fraction);
        if (filled > 0) {
            g.setColor(HEADER);
            g.fillRoundRect(0, 0, Math.max(filled, h), h, h, h);
        }
        g.dispose();
    }
}
