package Utils;

import Interfaces.IValidator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Input validation utility class
 * Handles validation for common input types with error messages
 */
public class InputValidator implements IValidator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";

    /**
     * Validate if input is a valid integer
     */
    @Override
    public boolean isValidInteger(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        try {
            Integer.parseInt(input.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validate if input is a valid DateTime in format yyyy-MM-dd HH:mm
     */
    @Override
    public boolean isValidDateTime(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDateTime.parse(input.trim(), DATE_TIME_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /**
     * Validate if input is a valid email
     */
    @Override
    public boolean isValidEmail(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        return input.trim().matches(EMAIL_REGEX);
    }

    /**
     * Validate if input is a non-empty string
     */
    @Override
    public boolean isValidString(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Validate if a number is within a specific range
     */
    @Override
    public boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Parse DateTime string to LocalDateTime
     * Returns null if invalid
     */
    public static LocalDateTime parseDateTime(String input) {
        try {
            return LocalDateTime.parse(input.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Format LocalDateTime to string
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Get validation error message
     */
    public static String getValidationError(String fieldName, String type) {
        switch (type) {
            case "integer":
                return fieldName + " must be a valid number";
            case "datetime":
                return fieldName + " must be in format: yyyy-MM-dd HH:mm";
            case "email":
                return fieldName + " must be a valid email address";
            case "required":
                return fieldName + " is required";
            case "range":
                return fieldName + " is out of valid range";
            default:
                return "Invalid input for " + fieldName;
        }
    }
}

