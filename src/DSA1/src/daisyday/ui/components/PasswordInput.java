package DSA1.src.daisyday.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Consumer;

import static DSA1.src.daisyday.ui.Theme.MAROON;

/** Password field with a show/hide eye button. */
public class PasswordInput extends JPanel {

    private final JPasswordField field = new JPasswordField();
    private final JButton eyeButton = new JButton();
    private boolean passwordVisible;
    private Consumer<Boolean> visibilityListener;

    public PasswordInput() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder());

        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(MAROON);
        field.setBackground(Color.WHITE);
        field.setBorder(BorderFactory.createEmptyBorder(1, 7, 1, 2));
        field.setEchoChar('\u2022');

        eyeButton.setPreferredSize(new Dimension(32, 24));
        eyeButton.setBackground(Color.WHITE);
        eyeButton.setOpaque(true);
        eyeButton.setContentAreaFilled(false);
        eyeButton.setBorder(BorderFactory.createEmptyBorder());
        eyeButton.setFocusPainted(false);
        eyeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        eyeButton.addActionListener(event -> setPasswordVisible(!passwordVisible));

        add(field, BorderLayout.CENTER);
        add(eyeButton, BorderLayout.EAST);
        setPasswordVisible(false);
    }

    public JPasswordField getField() {
        return field;
    }

    public void setPasswordVisible(boolean visible) {
        passwordVisible = visible;
        field.setEchoChar(visible ? (char) 0 : '\u2022');
        eyeButton.setIcon(new EyeIcon(!visible));
        eyeButton.setToolTipText(visible ? "Hide password" : "Show password");
        eyeButton.getAccessibleContext().setAccessibleName(
                visible ? "Hide password" : "Show password"
        );

        if (visibilityListener != null) {
            visibilityListener.accept(visible);
        }
    }

    public void setVisibilityListener(Consumer<Boolean> listener) {
        visibilityListener = listener;
    }

    public void clearAndHide() {
        field.setText("");
        setPasswordVisible(false);
    }
}
