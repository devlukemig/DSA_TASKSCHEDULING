package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Screen;
import DSA1.src.daisyday.ui.components.PasswordInput;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;

import static DSA1.src.daisyday.ui.Theme.MAROON;
import static DSA1.src.daisyday.ui.Theme.SIDEBAR;
import static DSA1.src.daisyday.ui.UiFactory.button;
import static DSA1.src.daisyday.ui.UiFactory.link;

public class RegistrationScreen extends AuthScreen {

    private final Database database;
    private final AppController app;
    private final LoginScreen loginScreen;

    private final JTextField username;
    private final PasswordInput password;
    private final PasswordInput confirm;

    public RegistrationScreen(
            Database database, AppController app, LoginScreen loginScreen) {

        this.database = database;
        this.app = app;
        this.loginScreen = loginScreen;

        placeLabel("REGISTRATION", 214, 93, 260, 30, 25);
        placeLabel("Fill out this Information", 214, 124, 240, 18, 12);
        placeLabel("Username:", 124, 158, 160, 24, 14);
        placeLabel("Password:", 124, 212, 160, 24, 14);

        username = textField(169, 180, 314);
        password = passwordInput(169, 235, 314);

        JLabel hint = new JLabel("Minimum 8 characters");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        hint.setForeground(MAROON);
        hint.setBounds(169, 261, 250, 16);
        add(hint);

        placeLabel("Confirm Password:", 124, 281, 180, 24, 14);
        confirm = passwordInput(169, 304, 314);

        JButton signup = button("SIGN UP");
        signup.setBackground(SIDEBAR);
        signup.setBounds(275, 345, 100, 29);
        signup.addActionListener(event -> handleSignUp());
        add(signup);

        add(buildReturnLink(signup));
    }

    private JPanel buildReturnLink(JButton signup) {
        JPanel returnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        returnPanel.setOpaque(false);
        returnPanel.setBounds(
                signup.getX() - 40,
                signup.getY() + signup.getHeight() + 12,
                signup.getWidth() + 80,
                22
        );

        JLabel returnText = new JLabel("Return to ");
        returnText.setForeground(MAROON);
        returnText.setFont(new Font("SansSerif", Font.PLAIN, 12));

        // Bold Login, no underline, still clickable.
        JButton loginLink = link("Login");
        loginLink.setFont(new Font("SansSerif", Font.BOLD, 12));
        loginLink.addActionListener(event -> {
            password.clearAndHide();
            confirm.clearAndHide();
            loginScreen.resetPassword();
            app.showScreen(Screen.LOGIN);
        });

        returnPanel.add(returnText);
        returnPanel.add(loginLink);
        return returnPanel;
    }

    private void handleSignUp() {
        String name = username.getText().trim();
        char[] first = password.getField().getPassword();
        char[] second = confirm.getField().getPassword();

        try {
            if (name.isEmpty() || first.length == 0 || second.length == 0) {
                app.warn("Please fill out all fields.");
                return;
            }

            int length = Character.codePointCount(first, 0, first.length);

            if (length < 8) {
                app.warn("Password must contain at least 8 characters.");
                password.getField().requestFocusInWindow();
                return;
            }

            if (!Arrays.equals(first, second)) {
                app.warn("Passwords do not match.");
                confirm.getField().requestFocusInWindow();
                return;
            }

            if (database.accounts().exists(name)) {
                app.warn("This username is already registered.");
                return;
            }

            database.accounts().create(name, first);
            loginScreen.setUsername(name);

            username.setText("");
            password.clearAndHide();
            confirm.clearAndHide();
            loginScreen.resetPassword();

            app.showScreen(Screen.LOGIN);
            app.success("Account Created", "Registration successful! You can now log in.");

        } catch (DataAccessException e) {
            app.error(e.getMessage());

        } finally {
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
        }
    }
}
