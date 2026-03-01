package Utils;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {}

    /** Hash a plain-text password using BCrypt. */
    public static String hash(String plain) {
        if (plain == null) throw new IllegalArgumentException("Password cannot be null");
        return BCrypt.hashpw(plain, BCrypt.gensalt(12));
    }

    /**
     * Verify plain password vs stored value.
     * Supports both:
     *   - BCrypt hashed passwords ($2a$, $2b$, $2y$ prefix)
     *   - Legacy plain-text passwords (stored as-is for old seeded users)
     */
    public static boolean verify(String plain, String stored) {
        if (plain == null || stored == null) return false;
        if (looksLikeBCryptHash(stored)) {
            try {
                return BCrypt.checkpw(plain, stored);
            } catch (Exception e) {
                return false;
            }
        }
        // Legacy: plain-text comparison for old seeded users
        return plain.equals(stored);
    }

    /** Returns true if the string looks like a BCrypt hash. */
    public static boolean looksLikeBCryptHash(String s) {
        if (s == null) return false;
        return s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$");
    }
}