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
import java.util.List;

import static DSA1.src.daisyday.ui.Theme.BLUE;
import static DSA1.src.daisyday.ui.Theme.GRID;
import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;

/** Horizontal bars with a label on the left (used for tasks per category). */
public class HorizontalBarChart extends JPanel {

    private static final int ROW = 38;

    private final List<String> labels;
    private final List<Integer> values;

    public HorizontalBarChart(List<String> labels, List<Integer> values) {
        this.labels = labels;
        this.values = values;
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(10, Math.max(150, labels.size() * ROW + 34)));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (labels.isEmpty()) {
            g.setColor(MUTED);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("No tasks yet", 14, 28);
            g.dispose();
            return;
        }

        int max = 0;
        for (int value : values) {
            max = Math.max(max, value);
        }
        ChartAxis axis = new ChartAxis(max);

        Font font = new Font("SansSerif", Font.PLAIN, 12);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();

        int labelWidth = 0;
        for (String label : labels) {
            labelWidth = Math.max(labelWidth, fm.stringWidth(label));
        }
        labelWidth = Math.min(labelWidth, getWidth() / 3);

        int left = 12 + labelWidth + 14;
        int right = getWidth() - 30;
        int top = 8;
        int bottom = getHeight() - 22;
        int plotWidth = right - left;

        // vertical grid lines + x labels
        g.setFont(font.deriveFont(11f));
        FontMetrics small = g.getFontMetrics();
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                10f, new float[]{3f, 4f}, 0f));
        for (int tick = 0; tick <= axis.top; tick += axis.step) {
            int x = left + (int) Math.round(plotWidth * tick / (double) axis.top);
            g.setColor(GRID);
            g.drawLine(x, top, x, bottom);
            g.setColor(MUTED);
            String text = String.valueOf(tick);
            g.drawString(text, x - small.stringWidth(text) / 2, getHeight() - 6);
        }

        int rowHeight = (bottom - top) / labels.size();
        int barHeight = Math.min(26, rowHeight - 10);
        for (int i = 0; i < labels.size(); i++) {
            int cy = top + rowHeight * i + rowHeight / 2;

            g.setFont(font);
            g.setColor(HEADER);
            String label = clip(labels.get(i), g.getFontMetrics(), labelWidth);
            g.drawString(label, 12 + labelWidth - g.getFontMetrics().stringWidth(label), cy + 4);

            int barWidth = (int) Math.round(plotWidth * values.get(i) / (double) axis.top);
            if (barWidth > 0) {
                double y = cy - barHeight / 2.0;
                // rounded right end, flat left end
                Area bar = new Area(new RoundRectangle2D.Double(left - 8, y, barWidth + 8, barHeight, 10, 10));
                bar.intersect(new Area(new Rectangle2D.Double(left, y, barWidth, barHeight)));
                g.setColor(BLUE);
                g.fill(bar);
            }

            g.setFont(font.deriveFont(Font.BOLD));
            g.setColor(HEADER);
            g.drawString(String.valueOf(values.get(i)), left + barWidth + 6, cy + 4);
        }
        g.dispose();
    }

    private static String clip(String text, FontMetrics fm, int width) {
        if (fm.stringWidth(text) <= width) {
            return text;
        }
        String clipped = text;
        while (clipped.length() > 1 && fm.stringWidth(clipped + "…") > width) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }
        return clipped + "…";
    }
}
