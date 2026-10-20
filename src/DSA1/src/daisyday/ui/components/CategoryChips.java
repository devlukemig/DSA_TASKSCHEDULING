package DSA1.src.daisyday.ui.components;

import DSA1.src.daisyday.model.Category;
import DSA1.src.daisyday.util.TextFormat;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.categoryColor;

/**
 * The category badges. Long names are shortened ("Course work...") and one
 * badge at a time can be clicked open to show its full name. Badges wrap onto
 * more rows when they run out of width: up to three rows, then it scrolls.
 */
public class CategoryChips extends JScrollPane {

    private static final int MAX_ROWS = 3;
    private static final int NAME_LIMIT = 12;
    private final WrapLayout layout = new WrapLayout(8, 6);
    private final ChipPanel panel = new ChipPanel();
    private final Consumer<String> onDelete;

    private List<Category> categories = List.of();
    private String expanded;
    private int lastHeight = -1;

    /** @param onDelete called with the category name when its × is clicked */
    public CategoryChips(Consumer<String> onDelete) {
        super(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER);
        this.onDelete = onDelete;

        panel.setLayout(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        setViewportView(panel);
        setBorder(BorderFactory.createEmptyBorder());
        getVerticalScrollBar().setUnitIncrement(30);

        // the number of rows depends on the width, which is only known once shown
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                if (heightWanted(getWidth()) != lastHeight) {
                    revalidate();
                }
            }
        });
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
        if (expanded != null && categories.stream().noneMatch(c -> c.name().equals(expanded))) {
            expanded = null;
        }
        rebuild();
    }

    // ---------- sizing ----------

    @Override
    public Dimension getPreferredSize() {
        int width = getWidth() > 0 ? getWidth()
                : getParent() != null && getParent().getWidth() > 0 ? getParent().getWidth() : 600;
        lastHeight = heightWanted(width);
        return new Dimension(width, lastHeight);
    }

    /** Height for as many rows as are needed, but never more than three. */
    private int heightWanted(int width) {
        int rows = Math.max(1, Math.min(MAX_ROWS, layout.rowCount(panel, width)));
        int measured = layout.rowHeight(panel);
        int rowHeight = measured > 0 ? measured : 28;
        return rows * rowHeight + (rows - 1) * layout.verticalGap()
                + panel.getInsets().top + panel.getInsets().bottom;
    }

    // ---------- chips ----------

    private void rebuild() {
        panel.removeAll();
        for (Category category : categories) {
            panel.add(chip(category.name(), categoryColor(category.colorIndex())));
        }
        panel.revalidate();
        panel.repaint();
        revalidate();
    }

    private JPanel chip(String name, Color color) {
        boolean open = name.equals(expanded);
        boolean longName = name.length() > NAME_LIMIT;

        JLabel text = new JLabel(open ? name : TextFormat.shorten(name, NAME_LIMIT));
        text.setFont(new Font("Serif", Font.PLAIN, 13));
        text.setForeground(Color.BLACK);
        if (longName) {
            text.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            text.setToolTipText(open ? "Click to shorten" : "Click to show the full name");
            text.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                    expanded = open ? null : name; // only one open at a time
                    rebuild();
                }
            });
        }

        JLabel delete = new JLabel("×");
        delete.setFont(new Font("SansSerif", Font.BOLD, 15));
        delete.setForeground(HEADER);
        delete.setBorder(BorderFactory.createEmptyBorder(0, 2, 1, 2));
        delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delete.setToolTipText("Delete this category");
        delete.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                onDelete.accept(name);
            }
        });

        JPanel chip = new JPanel(new BorderLayout(6, 0));
        chip.setBackground(color);
        chip.setBorder(BorderFactory.createEmptyBorder(3, 14, 3, 8));
        chip.add(text, BorderLayout.CENTER);
        chip.add(delete, BorderLayout.EAST);
        return chip;
    }

    /** Holds the chips; as wide as the viewport so that they wrap. */
    private final class ChipPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return layout.rowHeight(this) + layout.verticalGap();
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(30, visible.height - 10);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
