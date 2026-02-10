package Utils;

/**
 * UI-only user context.
 * Later you can replace this with real authentication + session.
 */
public final class UserContext {

    public enum Role {
        RECRUITER,
        CANDIDATE
    }

    private static Role currentRole = Role.RECRUITER;

    private UserContext() {}

    public static Role getRole() {
        return currentRole;
    }

    public static void toggleRole() {
        currentRole = (currentRole == Role.RECRUITER) ? Role.CANDIDATE : Role.RECRUITER;
    }

    public static String getRoleLabel() {
        return currentRole == Role.RECRUITER ? "Recruiter" : "Candidate";
    }

    // Mock ids for now (replace with authenticated user ids)
    public static int getRecruiterId() {
        return 1;
    }

    public static int getCandidateId() {
        return 1;
    }
}

