package DSA1.src.daisyday.data;

/** Thrown when something goes wrong while reading or writing the database. */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage(), cause);
    }
}
