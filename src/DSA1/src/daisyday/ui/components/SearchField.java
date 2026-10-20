package DSA1.src.daisyday.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.MUTED;

/** A text field with a magnifier on the left and a grey hint while it is empty. */
public class SearchField extends JTextField {

    private static final int ICON_SPACE = 28;

    private final String hint;

    public SearchField(String hint) {
        this.hint = hint;
        setFont(new Font("SansSerif", Font.PLAIN, 13));
        setForeground(HEADER);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 190, 190), 1),
                BorderFactory.createEmptyBorder(4, ICON_SPACE, 4, 8)));
        setToolTipText("Search by task title or category");
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // magnifier
        int cy = getHeight() / 2;
        g.setColor(HEADER);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawOval(9, cy - 7, 9, 9);
        g.drawLine(17, cy + 1, 21, cy + 5);

        if (getText().isEmpty()) {
            g.setFont(getFont());
            g.setColor(MUTED);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(hint, ICON_SPACE, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
    }
}
