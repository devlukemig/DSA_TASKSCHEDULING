package DSA1.src.daisyday.util;

public final class TextFormat {

    private TextFormat() {
    }

    /** "Course work for statistics" with limit 12 becomes "Course work...". */
    public static String shorten(String text, int limit) {
        if (text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit).trim() + "...";
    }
}
