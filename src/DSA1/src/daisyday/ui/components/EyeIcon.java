package DSA1.src.daisyday.ui.components;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

class EyeIcon implements Icon {

    private final boolean slashed;

    EyeIcon(boolean slashed) {
        this.slashed = slashed;
    }

    @Override
    public int getIconWidth() {
        return 27;
    }

    @Override
    public int getIconHeight() {
        return 20;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        Color iconColor = new Color(226, 228, 228);
        g.setColor(iconColor);
        g.setStroke(new BasicStroke(
                2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
        ));

        Path2D eye = new Path2D.Double();
        eye.moveTo(2, 10);
        eye.curveTo(8, 2, 19, 2, 25, 10);
        eye.curveTo(19, 18, 8, 18, 2, 10);

        g.draw(eye);
        g.fillOval(10, 6, 8, 8);

        if (slashed) {
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(
                    5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
            ));
            g.drawLine(5, 2, 23, 18);

            g.setColor(iconColor);
            g.setStroke(new BasicStroke(
                    2.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
            ));
            g.drawLine(5, 2, 23, 18);
        }

        g.dispose();
    }
}
