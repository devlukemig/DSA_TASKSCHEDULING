package DSA1.src.daisyday.ui.components;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;

import static DSA1.src.daisyday.ui.Theme.MAROON;

/** The DaisyDay logo drawn in code. */
public class BrandIcon implements Icon {

    @Override
    public int getIconWidth() {
        return 84;
    }

    @Override
    public int getIconHeight() {
        return 82;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setColor(MAROON);
        g.fillOval(0, 0, 82, 80);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(2.3f));

        g.drawRoundRect(24, 21, 27, 38, 3, 3);
        g.drawLine(29, 36, 40, 36);
        g.drawLine(29, 43, 38, 43);
        g.drawLine(29, 50, 40, 50);

        Polygon pen = new Polygon(
                new int[]{39, 55, 60, 44},
                new int[]{44, 28, 33, 49}, 4
        );

        g.setColor(MAROON);
        g.fillPolygon(pen);
        g.setColor(Color.WHITE);
        g.drawPolygon(pen);
        g.drawLine(52, 31, 57, 36);
        g.drawLine(39, 44, 37, 51);
        g.drawLine(37, 51, 44, 49);
        g.dispose();
    }
}
