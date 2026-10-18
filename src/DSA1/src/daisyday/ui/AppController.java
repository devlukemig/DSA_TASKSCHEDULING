package DSA1.src.daisyday.ui;

import DSA1.src.daisyday.model.Task;

/** What screens and pages are allowed to ask the main window to do. */
public interface AppController {

    void showScreen(Screen screen);

    void refreshAll();

    void message(String text);

    /** Opens the task form. Pass null to create a new task. */
    void editTask(Task existing);

    void logout();
}
