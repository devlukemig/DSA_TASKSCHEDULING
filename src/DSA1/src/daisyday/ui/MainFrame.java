package DSA1.src.daisyday.ui;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.model.Task;
import DSA1.src.daisyday.ui.dialogs.TaskDialog;
import DSA1.src.daisyday.ui.screens.CalendarPage;
import DSA1.src.daisyday.ui.screens.DashboardPage;
import DSA1.src.daisyday.ui.screens.LoginScreen;
import DSA1.src.daisyday.ui.screens.RegistrationScreen;
import DSA1.src.daisyday.ui.screens.TasksPage;
import DSA1.src.daisyday.ui.screens.WorkspaceScreen;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/** The app window. Owns the state and switches between screens. */
public class MainFrame extends JFrame implements AppController {

    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);
    private final Database database;
    private final Session session = new Session();

    private final LoginScreen loginScreen;
    private final DashboardPage dashboardPage;
    private final TasksPage tasksPage;
    private final CalendarPage calendarPage;

    public MainFrame(Database database) {
        this.database = database;

        setTitle("DaisyDay");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        loginScreen = new LoginScreen(session, database, this);
        dashboardPage = new DashboardPage(session, database, this);
        tasksPage = new TasksPage(session, database, this);
        calendarPage = new CalendarPage(session, database, this);

        screens.add(loginScreen, Screen.LOGIN.name());
        screens.add(new RegistrationScreen(database, this, loginScreen), Screen.REGISTER.name());
        screens.add(new WorkspaceScreen(Screen.DASHBOARD, this, dashboardPage), Screen.DASHBOARD.name());
        screens.add(new WorkspaceScreen(Screen.TASKS, this, tasksPage), Screen.TASKS.name());
        screens.add(new WorkspaceScreen(Screen.CALENDAR, this, calendarPage), Screen.CALENDAR.name());

        setContentPane(screens);
        refreshAll();
        showScreen(Screen.LOGIN);
    }

    @Override
    public void showScreen(Screen screen) {
        cards.show(screens, screen.name());

        screens.setPreferredSize(
                screen.isAuthentication()
                        ? new Dimension(594, 415)
                        : new Dimension(920, 500)
        );

        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void refreshAll() {
        try {
            dashboardPage.refresh();
            tasksPage.refresh();
            calendarPage.refresh();
        } catch (DataAccessException e) {
            message(e.getMessage());
        }
    }

    @Override
    public void message(String text) {
        JOptionPane.showMessageDialog(
                this, text, "DaisyDay", JOptionPane.INFORMATION_MESSAGE
        );
    }

    @Override
    public void editTask(Task existing) {
        if (!session.isLoggedIn()) {
            return;
        }
        new TaskDialog(this, session, database, existing, this::refreshAll).setVisible(true);
    }

    @Override
    public void logout() {
        session.logout();
        loginScreen.resetPassword();
        refreshAll();
        showScreen(Screen.LOGIN);
    }
}
