package entities;

public enum AttendanceStatusEnum {
    // Original DB values
    REGISTERED,
    ATTENDED,
    CANCELLED,
    NO_SHOW,
    
    // Synonyms / New names used in logic
    PENDING,
    CONFIRMED
}
