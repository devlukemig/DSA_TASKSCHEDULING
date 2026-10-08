package DSA1.src.daisyday.ui;

import java.awt.Color;

public final class Theme {

    public static final Color LOGIN_BG = new Color(235, 225, 225);
    public static final Color MAROON = new Color(100, 24, 14);
    public static final Color SIDEBAR = new Color(85, 12, 10);
    public static final Color HEADER = new Color(86, 53, 53);
    public static final Color PAGE_BG = new Color(193, 178, 178);
    public static final Color BLUE = new Color(151, 202, 255);

    private Theme() {
    }

    public static Color priorityColor(String priority) {
        return switch (priority) {
            case "High" -> new Color(255, 48, 88);
            case "Medium" -> new Color(255, 199, 70);
            default -> new Color(120, 218, 147);
        };
    }
}
