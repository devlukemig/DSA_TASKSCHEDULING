package DSA1.src.daisyday.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeFormat {

    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter FULL_DATE =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.ENGLISH);

    private TimeFormat() {
    }

    public static String time(LocalTime time) {
        return time.format(TIME);
    }

    public static String fullDate(LocalDate date) {
        return date.format(FULL_DATE);
    }
}
