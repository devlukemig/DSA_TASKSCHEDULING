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
import java.awt.geom.Arc2D;
import java.util.List;

import static DSA1.src.daisyday.ui.Theme.GRID;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;

/** Donut chart with a legend and a list of counts underneath. */
public class DonutChart extends JPanel {

    private static final Font LEGEND = new Font("SansSerif", Font.PLAIN, 12);
    private static final Font LEGEND_BOLD = new Font("SansSerif", Font.BOLD, 12);
    private static final int ROW = 22;

    private final List<String> names;
    private final int[] counts;
    private final Color[] colors;
    private final int total;

    public DonutChart(List<String> names, int[] counts, Color[] colors) {
        this.names = names;
        this.counts = counts;
        this.colors = colors;
        int sum = 0;
        for (int count : counts) {
            sum += count;
        }
        this.total = sum;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(10, 330));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int listHeight = names.size() * ROW + 14;
        int legendHeight = 30;
        int size = Math.max(60, Math.min(Math.min(width - 60, 180), getHeight() - listHeight - legendHeight - 24));

        drawDonut(g, (width - size) / 2, 10, size);
        drawLegend(g, 10 + size + 12, width);
        drawCounts(g, getHeight() - listHeight, width);
        g.dispose();
    }

    private void drawDonut(Graphics2D g, int x, int y, int size) {
        float thickness = size * 0.24f;
        double inset = thickness / 2;
        double diameter = size - thickness;
        g.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));

        if (total == 0) {
            g.setColor(GRID);
            g.draw(new Arc2D.Double(x + inset, y + inset, diameter, diameter, 0, 360, Arc2D.OPEN));
        } else {
            double angle = 90;
            for (int i = 0; i < counts.length; i++) {
                if (counts[i] == 0) {
                    continue;
                }
                double extent = -360.0 * counts[i] / total;
                g.setColor(colors[i]);
                g.draw(new Arc2D.Double(x + inset, y + inset, diameter, diameter, angle, extent, Arc2D.OPEN));
                angle += extent;
            }
            // thin white gaps between slices
            if (nonEmpty() > 1) {
                g.setStroke(new BasicStroke(2f));
                g.setColor(Color.WHITE);
                angle = 90;
                for (int i = 0; i < counts.length; i++) {
                    if (counts[i] == 0) {
                        continue;
                    }
                    double rad = Math.toRadians(angle);
                    double cx = x + size / 2.0;
                    double cy = y + size / 2.0;
                    g.drawLine(
                            (int) Math.round(cx + Math.cos(rad) * (size / 2.0 - thickness - 1)),
                            (int) Math.round(cy - Math.sin(rad) * (size / 2.0 - thickness - 1)),
                            (int) Math.round(cx + Math.cos(rad) * (size / 2.0 + 1)),
                            (int) Math.round(cy - Math.sin(rad) * (size / 2.0 + 1)));
                    angle -= 360.0 * counts[i] / total;
                }
            }
        }

        g.setStroke(new BasicStroke(1f)); // the ring stroke is thick; don't let it leak into later drawing

        // total in the middle
        String number = String.valueOf(total);
        g.setColor(HEADER);
        g.setFont(new Font("Serif", Font.PLAIN, Math.max(16, size / 5)));
        FontMetrics big = g.getFontMetrics();
        int cy = y + size / 2;
        g.drawString(number, x + (size - big.stringWidth(number)) / 2, cy + 2);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(MUTED);
        FontMetrics small = g.getFontMetrics();
        String caption = total == 1 ? "task" : "tasks";
        g.drawString(caption, x + (size - small.stringWidth(caption)) / 2, cy + 2 + small.getHeight());
    }

    private int nonEmpty() {
        int n = 0;
        for (int count : counts) {
            if (count > 0) {
                n++;
            }
        }
        return n;
    }

    private void drawLegend(Graphics2D g, int y, int width) {
        g.setFont(LEGEND);
        FontMetrics fm = g.getFontMetrics();
        int itemsWidth = 0;
        for (String name : names) {
            itemsWidth += 14 + fm.stringWidth(name) + 18;
        }
        int x = Math.max(8, (width - itemsWidth + 18) / 2);
        for (int i = 0; i < names.size(); i++) {
            g.setColor(colors[i]);
            g.fillOval(x, y + 4, 9, 9);
            g.setColor(HEADER);
            g.drawString(names.get(i), x + 14, y + 13);
            x += 14 + fm.stringWidth(names.get(i)) + 18;
        }
    }

    private void drawCounts(Graphics2D g, int top, int width) {
        g.setColor(GRID);
        g.drawLine(12, top, width - 12, top);

        int y = top + 12;
        for (int i = 0; i < names.size(); i++) {
            g.setColor(colors[i]);
            g.fillOval(14, y + 5, 8, 8);
            g.setColor(HEADER);
            g.setFont(LEGEND);
            g.drawString(names.get(i), 30, y + 14);
            g.setFont(LEGEND_BOLD);
            String value = counts[i] + (total == 0 ? "" : "  ·  " + Math.round(counts[i] * 100.0 / total) + "%");
            g.drawString(value, width - 14 - g.getFontMetrics().stringWidth(value), y + 14);
            y += ROW;
        }
    }
}
