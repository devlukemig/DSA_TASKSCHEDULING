package DSA1.src.daisyday.ui;

import DSA1.src.daisyday.model.Task;

/** What screens and pages are allowed to ask the main window to do. */
public interface AppController {

    void showScreen(Screen screen);

    void refreshAll();

    /** Shows a styled information window. */
    void message(String text);

    /** Shows a styled warning window (for example, a missing field). */
    void warn(String text);

    /** Shows a styled error window. */
    void error(String text);

    /** Shows a styled "it worked" window. */
    void success(String title, String text);

    /** Asks a yes/no question. Returns true if the user confirmed. */
    boolean confirm(String title, String text, String confirmText, boolean danger);

    /** Opens the task form. Pass null to create a new task. */
    void editTask(Task existing);

    void logout();
}
