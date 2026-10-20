package DSA1.src.daisyday.ui;

public enum Screen {
    LOGIN, REGISTER, DASHBOARD, TASKS, CALENDAR, ARCHIVE;

    public boolean isAuthentication() {
        return this == LOGIN || this == REGISTER;
    }
}
