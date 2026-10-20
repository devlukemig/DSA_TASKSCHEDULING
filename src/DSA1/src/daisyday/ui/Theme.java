package DSA1.src.daisyday.ui;

import java.awt.Color;

public final class Theme {

    public static final Color LOGIN_BG = new Color(235, 225, 225);
    public static final Color MAROON = new Color(100, 24, 14);
    public static final Color SIDEBAR = new Color(85, 12, 10);
    public static final Color HEADER = new Color(86, 53, 53);
    public static final Color PAGE_BG = new Color(193, 178, 178);
    public static final Color BLUE = new Color(151, 202, 255);
    public static final Color GREEN = new Color(36, 195, 110);
    public static final Color GRID = new Color(235, 230, 230);
    public static final Color MUTED = new Color(120, 105, 105);

    private Theme() {
    }

    private static final Color[] CATEGORY_COLORS = {
            new Color(244, 143, 204),
            new Color(229, 217, 132),
            new Color(147, 225, 157),
            new Color(0, 175, 233),
            new Color(255, 178, 120),
            new Color(190, 160, 235),
            new Color(120, 210, 200),
            new Color(235, 135, 135)
    };

    /** The color a category is shown in, on the category row and on its tasks. */
    public static Color categoryColor(int index) {
        return CATEGORY_COLORS[Math.floorMod(index, CATEGORY_COLORS.length)];
    }

    /** Colors of the three task statuses in the dashboard charts. */
    public static Color statusColor(String status) {
        return switch (status) {
            case "Completed" -> GREEN;
            case "In Progress" -> new Color(90, 160, 235);
            default -> new Color(255, 199, 70);
        };
    }

    public static Color priorityColor(String priority) {
        return switch (priority) {
            case "High" -> new Color(255, 48, 88);
            case "Medium" -> new Color(255, 199, 70);
            default -> new Color(120, 218, 147);
        };
    }
}
