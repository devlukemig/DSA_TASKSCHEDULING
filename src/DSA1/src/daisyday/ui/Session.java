package DSA1.src.daisyday.ui;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Who is logged in and which calendar day is showing.
 * Accounts and tasks are not kept here; they live in the database.
 */
public class Session {

    private static final long NO_ACCOUNT = -1;

    private long accountId = NO_ACCOUNT;
    private String username;
    private LocalDate selectedDate = LocalDate.now();
    private YearMonth displayedMonth = YearMonth.now();

    public boolean isLoggedIn() {
        return accountId != NO_ACCOUNT;
    }

    /** Returns -1 when nobody is logged in, which matches no rows in the database. */
    public long getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public void login(long accountId, String username) {
        this.accountId = accountId;
        this.username = username;
        this.selectedDate = LocalDate.now();
        this.displayedMonth = YearMonth.now();
    }

    public void logout() {
        accountId = NO_ACCOUNT;
        username = null;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public YearMonth getDisplayedMonth() {
        return displayedMonth;
    }

    public void setDisplayedMonth(YearMonth displayedMonth) {
        this.displayedMonth = displayedMonth;
    }
}
