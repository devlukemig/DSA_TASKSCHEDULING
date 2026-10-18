package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Screen;
import DSA1.src.daisyday.ui.Session;
import DSA1.src.daisyday.ui.components.PasswordInput;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTextField;
import java.awt.Font;
import java.util.Arrays;
import java.util.OptionalLong;

import static DSA1.src.daisyday.ui.Theme.MAROON;
import static DSA1.src.daisyday.ui.Theme.SIDEBAR;
import static DSA1.src.daisyday.ui.UiFactory.button;
import static DSA1.src.daisyday.ui.UiFactory.link;

public class LoginScreen extends AuthScreen {

    private final Session session;
    private final Database database;
    private final AppController app;

    private final JTextField username;
    private final PasswordInput password;
    private final JCheckBox showPassword;

    public LoginScreen(Session session, Database database, AppController app) {
        this.session = session;
        this.database = database;
        this.app = app;

        placeLabel("WELCOME !", 250, 101, 200, 30, 25);
        placeLabel("Login to continue", 255, 126, 200, 18, 12);
        placeLabel("Username:", 92, 165, 90, 24, 14);
        placeLabel("Password:", 92, 205, 90, 24, 14);

        username = textField(181, 166, 314);
        password = passwordInput(181, 206, 314);

        showPassword = new JCheckBox("Show password");
        showPassword.setBounds(181, 235, 150, 20);
        showPassword.setOpaque(false);
        showPassword.setForeground(MAROON);
        showPassword.setFont(new Font("SansSerif", Font.PLAIN, 12));

        password.setVisibilityListener(visible -> showPassword.setSelected(visible));
        showPassword.addActionListener(event ->
                password.setPasswordVisible(showPassword.isSelected())
        );
        add(showPassword);

        JButton login = button("LOGIN");
        login.setBackground(SIDEBAR);
        login.setBounds(264, 284, 90, 29);
        login.addActionListener(event -> handleLogin());
        add(login);

        password.getField().addActionListener(event -> handleLogin());

        JButton register = link(
                "<html>don’t have an account yet? <b>register</b> now</html>"
        );
        register.setBounds(170, 339, 265, 22);
        register.addActionListener(event -> app.showScreen(Screen.REGISTER));
        add(register);
    }

    public void setUsername(String name) {
        username.setText(name);
    }

    public void resetPassword() {
        password.clearAndHide();
        showPassword.setSelected(false);
    }

    private void handleLogin() {
        String name = username.getText().trim();
        char[] entered = password.getField().getPassword();

        try {
            if (name.isEmpty() || entered.length == 0) {
                app.warn("Please enter your username and password.");
                return;
            }

            OptionalLong accountId = database.accounts().authenticate(name, entered);

            if (accountId.isEmpty()) {
                app.error("Incorrect username or password.");
                return;
            }

            session.login(accountId.getAsLong(), name);

            resetPassword();
            app.refreshAll();
            app.showScreen(Screen.DASHBOARD);
            app.success("Welcome Back", "You are now logged in as " + name + ".");

        } catch (DataAccessException e) {
            app.error(e.getMessage());

        } finally {
            Arrays.fill(entered, '\0');
        }
    }
}
