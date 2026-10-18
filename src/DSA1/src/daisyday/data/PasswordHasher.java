package DSA1.src.daisyday.data;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Salted PBKDF2 hashing, so real passwords are never stored. */
final class PasswordHasher {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    static String newSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    static String hash(char[] password, String salt) {
        PBEKeySpec spec = new PBEKeySpec(
                password, Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS
        );
        try {
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is unavailable", e);
        } finally {
            spec.clearPassword();
        }
    }

    static boolean matches(char[] password, String salt, String expectedHash) {
        byte[] actual = hash(password, salt).getBytes();
        return MessageDigest.isEqual(actual, expectedHash.getBytes());
    }
}
