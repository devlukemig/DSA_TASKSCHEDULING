package DSA1.src.daisyday.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.JViewport;

/**
 * Left-to-right layout that starts a new row when the next component would
 * not fit. A component wider than the container is shrunk to fit.
 */
public class WrapLayout implements LayoutManager {

    private final int hgap;
    private final int vgap;

    public WrapLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    public int verticalGap() {
        return vgap;
    }

    /** How many rows the children need in a container that is this wide. */
    public int rowCount(Container parent, int width) {
        int[] rows = new int[1];
        arrange(parent, width, false, rows);
        return rows[0];
    }

    /** Height of the tallest child (every row is this tall). */
    public int rowHeight(Container parent) {
        int height = 0;
        for (Component child : parent.getComponents()) {
            if (child.isVisible()) {
                height = Math.max(height, child.getPreferredSize().height);
            }
        }
        return height;
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
    }

    @Override
    public void removeLayoutComponent(Component component) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int width = parent.getParent() instanceof JViewport view ? view.getWidth() : parent.getWidth();
        if (width <= 0) {
            width = 600;
        }
        return new Dimension(width, arrange(parent, width, false, new int[1]));
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(0, preferredLayoutSize(parent).height);
    }

    @Override
    public void layoutContainer(Container parent) {
        arrange(parent, parent.getWidth(), true, new int[1]);
    }

    /** Places (or just measures) the children and returns the total height. */
    private int arrange(Container parent, int width, boolean place, int[] rows) {
        Insets in = parent.getInsets();
        int available = Math.max(1, width - in.left - in.right);

        int x = 0;
        int y = in.top;
        int rowHeight = 0;
        rows[0] = 0;

        for (Component child : parent.getComponents()) {
            if (!child.isVisible()) {
                continue;
            }
            Dimension size = child.getPreferredSize();
            int w = Math.min(size.width, available);

            if (rows[0] == 0) {
                rows[0] = 1;
            } else if (x + w > available) {
                x = 0;
                y += rowHeight + vgap;
                rowHeight = 0;
                rows[0]++;
            }

            if (place) {
                child.setBounds(in.left + x, y, w, size.height);
            }
            x += w + hgap;
            rowHeight = Math.max(rowHeight, size.height);
        }
        return y + rowHeight + in.bottom;
    }
}
