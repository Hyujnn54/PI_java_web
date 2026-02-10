package Interfaces;

import javafx.scene.control.TextField;

/**
 * Validation interface for input fields
 * Implement this to add custom validation logic
 */
public interface IValidator {
    /**
     * Validate an integer field
     */
    boolean isValidInteger(String input);

    /**
     * Validate a date field (yyyy-MM-dd HH:mm format)
     */
    boolean isValidDateTime(String input);

    /**
     * Validate an email field
     */
    boolean isValidEmail(String input);

    /**
     * Validate a non-empty string
     */
    boolean isValidString(String input);

    /**
     * Validate a number is within range
     */
    boolean isInRange(int value, int min, int max);
}

