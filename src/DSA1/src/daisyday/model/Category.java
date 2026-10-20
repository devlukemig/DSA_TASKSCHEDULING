package DSA1.src.daisyday.model;

/** A category name and the color it is shown in (an index into the category palette). */
public record Category(String name, int colorIndex) {

    /** How many different category colors there are. */
    public static final int PALETTE_SIZE = 8;
}
