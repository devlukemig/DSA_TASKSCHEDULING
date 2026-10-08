package DSA1.src.daisyday.ui.screens;

import DSA1.src.daisyday.data.Database;
import DSA1.src.daisyday.ui.AppController;
import DSA1.src.daisyday.ui.Session;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static DSA1.src.daisyday.ui.Theme.PAGE_BG;

/** Base class for the three pages shown beside the sidebar. */
public abstract class Page extends JPanel {

    protected final Session session;
    protected final Database database;
    protected final AppController app;

    protected Page(Session session, Database database, AppController app) {
        super(new BorderLayout(12, 14));
        this.session = session;
        this.database = database;
        this.app = app;
        setBackground(PAGE_BG);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
    }

    protected long accountId() {
        return session.getAccountId();
    }

    /** Reloads the page from the database. */
    public abstract void refresh();
}
