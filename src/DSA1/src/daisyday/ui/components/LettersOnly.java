package DSA1.src.daisyday.ui.components;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/** Lets only letters into a text field (no digits, symbols or spaces), also when pasting. */
public final class LettersOnly extends DocumentFilter {

    private LettersOnly() {
    }

    public static void install(JTextField field) {
        ((PlainDocument) field.getDocument()).setDocumentFilter(new LettersOnly());
        field.setToolTipText("Letters only");
    }

    /** True if the text is not empty and every character is a letter. */
    public static boolean isValid(String text) {
        return !text.isEmpty() && text.codePoints().allMatch(Character::isLetter);
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attrs)
            throws BadLocationException {
        replace(fb, offset, 0, text, attrs);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text == null) {
            super.replace(fb, offset, length, null, attrs);
            return;
        }

        StringBuilder letters = new StringBuilder();
        text.codePoints()
                .filter(Character::isLetter)
                .forEach(letters::appendCodePoint);

        if (letters.length() > 0 || length > 0 || text.isEmpty()) {
            super.replace(fb, offset, length, letters.toString(), attrs);
        }
    }
}
