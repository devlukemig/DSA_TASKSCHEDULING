package DSA1.src.daisyday;

import DSA1.src.daisyday.data.DataAccessException;
import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.ui.dialogs.Modal;
import DSA1.src.daisyday.ui.MainFrame;

import javax.swing.SwingUtilities;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Database database = openDatabase();
        if (database == null) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(database::close));
        SwingUtilities.invokeLater(() -> new MainFrame(database).setVisible(true));
    }

    private static Database openDatabase() {
        try {
            return Database.open();
        } catch (DataAccessException e) {
            Modal.error(null, e.getMessage());
            return null;
        }
    }
}
