package DSA1.src.daisyday.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.UiFactory.button;

/** A month grid. Days show how many tasks they have. */
public class MonthPanel extends JPanel {

    private static final String[] WEEKDAYS = {
            "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"
    };

    public MonthPanel(
            YearMonth month, LocalDate selected, Map<LocalDate, Integer> taskCounts,
            Consumer<LocalDate> onSelect, Consumer<YearMonth> onMonth) {

        super(new BorderLayout());
        setBackground(HEADER);
        setBorder(BorderFactory.createLineBorder(HEADER, 5));

        add(buildHeading(month, onMonth), BorderLayout.NORTH);
        add(buildGrid(month, selected, taskCounts, onSelect), BorderLayout.CENTER);
    }

    private static JPanel buildHeading(YearMonth month, Consumer<YearMonth> onMonth) {
        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(HEADER);

        JButton previous = button("<");
        JButton next = button(">");
        previous.addActionListener(event -> onMonth.accept(month.minusMonths(1)));
        next.addActionListener(event -> onMonth.accept(month.plusMonths(1)));

        JLabel monthName = new JLabel(
                month.getMonth()
                        .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                        .toUpperCase(Locale.ENGLISH)
                        + " " + month.getYear(),
                SwingConstants.CENTER
        );
        monthName.setForeground(Color.WHITE);
        monthName.setFont(new Font("Serif", Font.BOLD, 22));

        heading.add(previous, BorderLayout.WEST);
        heading.add(monthName, BorderLayout.CENTER);
        heading.add(next, BorderLayout.EAST);
        return heading;
    }

    private static JPanel buildGrid(
            YearMonth month, LocalDate selected,
            Map<LocalDate, Integer> taskCounts, Consumer<LocalDate> onSelect) {

        JPanel grid = new JPanel(new GridLayout(7, 7, 1, 1));
        grid.setBackground(Color.LIGHT_GRAY);

        for (String weekday : WEEKDAYS) {
            JLabel name = new JLabel(weekday, SwingConstants.CENTER);
            name.setOpaque(true);
            name.setBackground(Color.WHITE);
            name.setFont(new Font("SansSerif", Font.BOLD, 9));
            grid.add(name);
        }

        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;

        for (int cell = 0; cell < 42; cell++) {
            int number = cell - offset + 1;

            if (number < 1 || number > month.lengthOfMonth()) {
                JPanel blank = new JPanel();
                blank.setBackground(Color.WHITE);
                grid.add(blank);
                continue;
            }

            LocalDate date = month.atDay(number);
            int count = taskCounts.getOrDefault(date, 0);

            String text = "<html><center>" + number;
            if (count > 0) {
                text += "<br><small>" + count
                        + (count == 1 ? " task" : " tasks") + "</small>";
            }
            text += "</center></html>";

            JButton day = new JButton(text);
            day.setVerticalAlignment(SwingConstants.TOP);
            day.setBackground(
                    date.equals(selected)
                            ? new Color(200, 229, 255) : Color.WHITE
            );
            day.setForeground(Color.BLACK);
            day.setFocusPainted(false);
            day.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            day.addActionListener(event -> onSelect.accept(date));
            grid.add(day);
        }

        return grid;
    }
}
