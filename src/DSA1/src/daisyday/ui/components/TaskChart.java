package DSA1.src.daisyday.ui.components;

import DSA1.src.daisyday.model.TaskSummary;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;

import static DSA1.src.daisyday.ui.Theme.HEADER;

/** Bar or pie chart of completed / pending / overdue tasks. */
public class TaskChart extends JPanel {

    private static final Color[] COLORS = {
            new Color(36, 195, 110),
            new Color(224, 244, 0),
            new Color(255, 40, 40)
    };
    private static final String[] NAMES = {"Completed", "Pending*", "Overdue"};

    private final boolean pie;
    private final TaskSummary summary;

    public TaskChart(boolean pie, TaskSummary summary) {
        this.pie = pie;
        this.summary = summary;
        setBackground(Color.WHITE);
        setToolTipText("Pending includes In Progress tasks that are not overdue.");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        int[] counts = {summary.completed(), summary.pending(), summary.overdue()};
        int total = summary.total();

        if (total == 0) {
            g.setColor(Color.GRAY);
            g.drawString("No tasks yet", 20, 40);
        } else if (pie) {
            drawPie(g, counts, total);
        } else {
            drawBars(g, counts);
        }

        drawLegend(g);
        g.dispose();
    }

    private void drawPie(Graphics2D g, int[] counts, int total) {
        int size = Math.max(10, Math.min(getWidth() - 35, getHeight() - 32));
        int x = (getWidth() - size) / 2;
        int start = 90;
        int drawn = 0;

        for (int i = 0; i < counts.length; i++) {
            int angle = i == counts.length - 1
                    ? 360 - drawn
                    : (int) Math.round(counts[i] * 360.0 / total);

            g.setColor(COLORS[i]);
            g.fillArc(x, 4, size, size, start, angle);
            start += angle;
            drawn += angle;
        }
    }

    private void drawBars(Graphics2D g, int[] counts) {
        int maximum = Math.max(1, Arrays.stream(counts).max().orElse(1));

        for (int i = 0; i < counts.length; i++) {
            int y = 12 + i * 32;
            g.setColor(COLORS[i]);
            g.fillOval(12, y + 5, 10, 10);
            int width = counts[i] * Math.max(0, getWidth() - 65) / maximum;
            g.fillRect(30, y, width, 22);
            g.setColor(HEADER);
            g.drawString(String.valueOf(counts[i]), getWidth() - 25, y + 16);
        }
    }

    private void drawLegend(Graphics2D g) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 10));

        for (int i = 0; i < NAMES.length; i++) {
            int x = 5 + i * getWidth() / 3;
            g.setColor(COLORS[i]);
            g.fillOval(x, getHeight() - 16, 7, 7);
            g.setColor(HEADER);
            g.drawString(NAMES[i], x + 10, getHeight() - 9);
        }
    }
}
