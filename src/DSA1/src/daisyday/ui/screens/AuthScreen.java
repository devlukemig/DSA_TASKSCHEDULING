package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.ui.components.BrandIcon;
import DSA1.src.daisyday.ui.components.LettersOnly;
import DSA1.src.daisyday.ui.components.PasswordInput;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import static DSA1.src.daisyday.ui.Theme.LOGIN_BG;
import static DSA1.src.daisyday.ui.UiFactory.label;

/** Shared look and helpers for the login and registration screens. */
abstract class AuthScreen extends JPanel {

    protected AuthScreen() {
        super(null);
        setBackground(LOGIN_BG);
        setPreferredSize(new Dimension(594, 415));

        JLabel logo = new JLabel(new BrandIcon());
        logo.setBounds(32, 17, 84, 82);
        add(logo);

        JLabel brand = label("DaisyDay", 36);
        brand.setFont(new Font("Segoe Script", Font.BOLD, 36));
        brand.setBounds(130, 24, 245, 58);
        add(brand);
    }

    protected void placeLabel(
            String text, int x, int y, int width, int height, int size) {

        JLabel result = label(text, size);
        result.setBounds(x, y, width, height);
        add(result);
    }

    protected JTextField textField(int x, int y, int width) {
        JTextField field = new JTextField();
        field.setBounds(x, y, width, 24);
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 7));
        add(field);
        return field;
    }

    /** A text field for the username: letters only. */
    protected JTextField usernameField(int x, int y, int width) {
        JTextField field = textField(x, y, width);
        LettersOnly.install(field);
        return field;
    }

    protected PasswordInput passwordInput(int x, int y, int width) {
        PasswordInput input = new PasswordInput();
        input.setBounds(x, y, width, 24);
        add(input);
        return input;
    }
}
