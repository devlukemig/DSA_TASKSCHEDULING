package DSA1.src.daisyday.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * Puts the children side by side, sharing the width by weight (2, 1 gives a
 * two-thirds / one-third split). Every child gets the height of the tallest one.
 */
public class RowLayout implements LayoutManager {

    private final int gap;
    private final double[] weights;

    public RowLayout(int gap, double... weights) {
        this.gap = gap;
        this.weights = weights;
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
    }

    @Override
    public void removeLayoutComponent(Component component) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int width = 0;
        int height = 0;
        for (Component child : parent.getComponents()) {
            Dimension size = child.getPreferredSize();
            width += size.width + gap;
            height = Math.max(height, size.height);
        }
        Insets in = parent.getInsets();
        return new Dimension(width + in.left + in.right, height + in.top + in.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(0, preferredLayoutSize(parent).height);
    }

    @Override
    public void layoutContainer(Container parent) {
        Component[] children = parent.getComponents();
        Insets in = parent.getInsets();
        int count = Math.min(children.length, weights.length);
        if (count == 0) {
            return;
        }

        double totalWeight = 0;
        for (int i = 0; i < count; i++) {
            totalWeight += weights[i];
        }

        int available = parent.getWidth() - in.left - in.right - gap * (count - 1);
        int height = parent.getHeight() - in.top - in.bottom;
        double x = in.left;

        for (int i = 0; i < count; i++) {
            int width = (int) Math.round(available * weights[i] / totalWeight);
            children[i].setBounds((int) Math.round(x), in.top, width, height);
            x += width + gap;
        }
    }
}
