package Utils;

import java.sql.*;

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
    private static Long cachedRecruiterId = null;
    private static Long cachedCandidateId = null;
    private static Long cachedAdminId = null;

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

    public static Long getAdminId() {
        if (cachedAdminId == null) {
            cachedAdminId = getFirstAdminId();
        }
        return cachedAdminId;
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

    private static Long getFirstAdminId() {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE role = 'ADMIN' LIMIT 1")) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            System.err.println("Error getting admin ID: " + e.getMessage());
        }
        return 1L; // Fallback
    }
}


