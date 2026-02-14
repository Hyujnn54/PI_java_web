package Utils;

/**
 * UI-only user context.
 * Later you can replace this with real authentication + session.
 */
public final class UserContext {

    public enum Role {
        RECRUITER,
        CANDIDATE,
        ADMIN
    }

    private static Role currentRole = Role.RECRUITER;
    // Using static IDs for testing: candidate=1, recruiter=2, admin=3

    private UserContext() {}

    public static Role getRole() {
        return currentRole;
    }

    public static void setCurrentRole(Role role) {
        currentRole = role;
    }

    public static void toggleRole() {
        currentRole = switch (currentRole) {
            case RECRUITER -> Role.CANDIDATE;
            case CANDIDATE -> Role.ADMIN;
            case ADMIN -> Role.RECRUITER;
        };
    }

    public static String getRoleLabel() {
        return switch (currentRole) {
            case RECRUITER -> "Recruiter";
            case CANDIDATE -> "Candidate";
            case ADMIN -> "Admin";
        };
    }

    // Mock ids for now (replace with authenticated user ids)
    public static Long getRecruiterId() {
        return 2L; // Static ID for testing
    }

    public static Long getCandidateId() {
        return 1L; // Static ID for testing
    }

    public static Long getAdminId() {
        return 3L; // Static ID for testing
    }
}


