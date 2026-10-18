package DSA1.src.daisyday.ui.components;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import static DSA1.src.daisyday.ui.Theme.BLUE;
import static DSA1.src.daisyday.ui.Theme.GRID;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;

/** Vertical bars, one per label (used for tasks completed on each day of the week). */
public class WeekBarChart extends JPanel {

    private final String[] labels;
    private final int[] values;
    private final int highlight;

    /** @param highlight index of the label to draw in bold (today), or -1 */
    public WeekBarChart(String[] labels, int[] values, int highlight, int height) {
        this.labels = labels;
        this.values = values;
        this.highlight = highlight;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(10, height));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        ChartAxis axis = new ChartAxis(max);

        Font font = new Font("SansSerif", Font.PLAIN, 11);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        int left = 12 + fm.stringWidth(String.valueOf(axis.top)) + 8;
        int right = getWidth() - 14;
        int top = 18;
        int bottom = getHeight() - 24;
        int plotHeight = bottom - top;

        // grid lines + y labels
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{3f, 4f}, 0f));
        for (int tick = 0; tick <= axis.top; tick += axis.step) {
            int y = bottom - (int) Math.round(plotHeight * tick / (double) axis.top);
            g.setColor(GRID);
            g.drawLine(left, y, right, y);
            g.setColor(MUTED);
            String text = String.valueOf(tick);
            g.drawString(text, left - 8 - fm.stringWidth(text), y + 4);
        }

        // bars
        int slot = (right - left) / labels.length;
        int barWidth = Math.max(8, Math.min(34, slot - 14));
        for (int i = 0; i < labels.length; i++) {
            int cx = left + slot * i + slot / 2;

            if (values[i] > 0) {
                int barHeight = (int) Math.round(plotHeight * values[i] / (double) axis.top);
                double x = cx - barWidth / 2.0;
                double y = bottom - barHeight;
                // rounded top corners, flat bottom
                Area bar = new Area(new RoundRectangle2D.Double(x, y, barWidth, barHeight + 8, 10, 10));
                bar.intersect(new Area(new Rectangle2D.Double(x, y, barWidth, barHeight)));
                g.setColor(BLUE);
                g.fill(bar);

                g.setColor(HEADER);
                g.setFont(font.deriveFont(Font.BOLD));
                String value = String.valueOf(values[i]);
                g.drawString(value, cx - g.getFontMetrics().stringWidth(value) / 2, (int) y - 4);
            }

            g.setFont(font.deriveFont(i == highlight ? Font.BOLD : Font.PLAIN));
            g.setColor(i == highlight ? HEADER : MUTED);
            g.drawString(labels[i], cx - g.getFontMetrics().stringWidth(labels[i]) / 2, getHeight() - 7);
        }
        g.dispose();
    }
}
