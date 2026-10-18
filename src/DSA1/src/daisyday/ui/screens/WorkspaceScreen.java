package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Screen;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import static DSA1.src.daisyday.ui.Theme.HEADER;
import static DSA1.src.daisyday.ui.Theme.PAGE_BG;
import static DSA1.src.daisyday.ui.Theme.SIDEBAR;
import static DSA1.src.daisyday.ui.UiFactory.button;

/** Sidebar + one page. The current screen's menu item is highlighted. */
public class WorkspaceScreen extends JPanel {

    private final AppController app;
    private final Screen current;

    public WorkspaceScreen(Screen current, AppController app, Page page) {
        super(new BorderLayout());
        this.current = current;
        this.app = app;

        setBackground(PAGE_BG);
        add(buildSidebar(), BorderLayout.WEST);
        add(page, BorderLayout.CENTER);
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR);
        sidebar.setPreferredSize(new Dimension(240, 500));

        sidebar.add(buildTitle());
        sidebar.add(Box.createVerticalStrut(20));
        sidebar.add(menu("DASHBOARD", Screen.DASHBOARD));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(menu(
                "<html>TASK LIST &<br>TASK CATEGORIES</html>", Screen.TASKS
        ));
        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(menu("CALENDAR VIEW", Screen.CALENDAR));
        sidebar.add(Box.createVerticalGlue());

        JButton logout = button("LOG OUT");
        logout.setAlignmentX(Component.CENTER_ALIGNMENT);
        logout.addActionListener(event -> app.logout());

        sidebar.add(logout);
        sidebar.add(Box.createVerticalStrut(35));
        return sidebar;
    }

    private JPanel buildTitle() {
        JLabel title = new JLabel("TASK SCHEDULE");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.PLAIN, 26));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(HEADER);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
        titlePanel.setMaximumSize(new Dimension(240, 80));
        titlePanel.add(title);
        return titlePanel;
    }

    private JButton menu(String text, Screen target) {
        JButton result = button(text);
        result.setFont(new Font("Serif", Font.PLAIN, 17));
        result.setAlignmentX(Component.CENTER_ALIGNMENT);
        result.setMaximumSize(new Dimension(205, 58));
        result.setHorizontalAlignment(SwingConstants.LEFT);

        boolean selected = target == current;
        result.setBackground(selected ? new Color(222, 217, 215) : SIDEBAR);
        result.setForeground(selected ? HEADER : Color.WHITE);
        result.addActionListener(event -> {
            app.refreshAll();
            app.showScreen(target);
        });
        return result;
    }
}
