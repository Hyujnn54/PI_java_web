package Utils;

public class ValidationUtils {

    /**
     * Validates Tunisian phone number
     * Tunisian format: +216 XXXXXXXX or 216XXXXXXXX or 0XXXXXXXX or XXXXXXXX
     */
    public static boolean isValidTunisianPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;

        String cleaned = phone.replaceAll("[^0-9+]", "");

        // Check various Tunisian formats
        // +216XXXXXXXX
        if (cleaned.startsWith("+216") && cleaned.length() == 12) {
            return cleaned.substring(4).matches("\\d{8}");
        }
        // 216XXXXXXXX
        if (cleaned.startsWith("216") && cleaned.length() == 11) {
            return cleaned.substring(3).matches("\\d{8}");
        }
        // 0XXXXXXXX
        if (cleaned.startsWith("0") && cleaned.length() == 9) {
            return cleaned.substring(1).matches("\\d{8}");
        }
        // XXXXXXXX
        if (cleaned.length() == 8) {
            return cleaned.matches("\\d{8}");
        }

        return false;
    }

    /**
     * Validates French phone number
     * French format: +33 XXXXXXXXX or 33XXXXXXXXX or 0XXXXXXXXX
     */
    public static boolean isValidFrenchPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;

        String cleaned = phone.replaceAll("[^0-9+]", "");

        // Check various French formats
        // +33XXXXXXXXX
        if (cleaned.startsWith("+33") && cleaned.length() == 12) {
            return cleaned.substring(3).matches("\\d{9}");
        }
        // 33XXXXXXXXX
        if (cleaned.startsWith("33") && cleaned.length() == 11) {
            return cleaned.substring(2).matches("\\d{9}");
        }
        // 0XXXXXXXXX
        if (cleaned.startsWith("0") && cleaned.length() == 10) {
            return cleaned.substring(1).matches("\\d{9}");
        }

        return false;
    }

    /**
     * Validates cover letter
     * Requirements: not empty, not only spaces, min 50 chars, max 2000 chars
     */
    public static boolean isValidCoverLetter(String coverLetter) {
        if (coverLetter == null || coverLetter.trim().isEmpty()) {
            return false;
        }

        String trimmed = coverLetter.trim();
        return trimmed.length() >= 50 && trimmed.length() <= 2000;
    }

    /**
     * Get error message for cover letter
     */
    public static String getCoverLetterErrorMessage(String coverLetter) {
        if (coverLetter == null || coverLetter.trim().isEmpty()) {
            return "Cover letter cannot be empty";
        }

        String trimmed = coverLetter.trim();
        if (trimmed.length() < 50) {
            return "Cover letter must be at least 50 characters (current: " + trimmed.length() + ")";
        }

        if (trimmed.length() > 2000) {
            return "Cover letter must not exceed 2000 characters (current: " + trimmed.length() + ")";
        }

        return "";
    }

    /**
     * Get error message for phone number
     */
    public static String getPhoneErrorMessage(String country, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "Phone number cannot be empty";
        }

        if ("TN".equals(country)) {
            return "Please enter a valid Tunisian phone number (8 digits format)";
        } else if ("FR".equals(country)) {
            return "Please enter a valid French phone number (9 digits format)";
        }

        return "Please select a valid country";
    }

    /**
     * Validates note for status history
     * Requirements: not empty, not only spaces, min 5 chars, max 255 chars
     */
    public static boolean isValidNote(String note) {
        if (note == null || note.trim().isEmpty()) {
            return false;
        }

        String trimmed = note.trim();
        return trimmed.length() >= 5 && trimmed.length() <= 255;
    }

    /**
     * Get error message for note
     */
    public static String getNoteErrorMessage(String note) {
        if (note == null || note.trim().isEmpty()) {
            return "Note cannot be empty";
        }

        String trimmed = note.trim();
        if (trimmed.length() < 5) {
            return "Note must be at least 5 characters (current: " + trimmed.length() + ")";
        }

        if (trimmed.length() > 255) {
            return "Note must not exceed 255 characters (current: " + trimmed.length() + ")";
        }

        return "";
    }
}

