package Utils;

import java.sql.*;

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
    private static Long cachedRecruiterId = null;
    private static Long cachedCandidateId = null;

    private UserContext() {}

    public static Role getRole() {
        return currentRole;
    }

    public static void setCurrentRole(Role role) {
        currentRole = role;
    }

    public static void toggleRole() {
        currentRole = (currentRole == Role.RECRUITER) ? Role.CANDIDATE : Role.RECRUITER;
    }

    public static String getRoleLabel() {
        return currentRole == Role.RECRUITER ? "Recruiter" : "Candidate";
    }

    // Mock ids for now (replace with authenticated user ids)
    public static Long getRecruiterId() {
        if (cachedRecruiterId == null) {
            cachedRecruiterId = getFirstRecruiterId();
        }
        return cachedRecruiterId;
    }

    public static Long getCandidateId() {
        if (cachedCandidateId == null) {
            cachedCandidateId = getFirstCandidateId();
        }
        return cachedCandidateId;
    }

    private static Long getFirstRecruiterId() {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM recruiter LIMIT 1")) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting recruiter ID: " + e.getMessage());
        }
        return 1L; // Fallback
    }

    private static Long getFirstCandidateId() {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM candidate LIMIT 1")) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting candidate ID: " + e.getMessage());
        }
        return 1L; // Fallback
    }
}


