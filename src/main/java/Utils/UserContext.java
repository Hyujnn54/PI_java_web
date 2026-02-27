package Utils;

import java.sql.*;

/**
 * UI-only user context.
 * Replace with real authentication + session when login is implemented.
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
    private static Long cachedAdminId     = null;

    private UserContext() {}

    public static Role getRole() {
        return currentRole;
    }

    public static void setCurrentRole(Role role) {
        currentRole = role;
        cachedRecruiterId = null;
        cachedCandidateId = null;
        cachedAdminId     = null;
    }

    public static void toggleRole() {
        currentRole = switch (currentRole) {
            case RECRUITER -> Role.CANDIDATE;
            case CANDIDATE -> Role.ADMIN;
            case ADMIN     -> Role.RECRUITER;
        };
        cachedRecruiterId = null;
        cachedCandidateId = null;
        cachedAdminId     = null;
    }

    public static String getRoleLabel() {
        return switch (currentRole) {
            case RECRUITER -> "Recruteur";
            case CANDIDATE -> "Candidat";
            case ADMIN     -> "Admin";
        };
    }

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
            if (rs.next()) return rs.getLong("id");
        } catch (SQLException e) {
            System.err.println("Error getting recruiter ID: " + e.getMessage());
        }
        return 1L;
    }

    private static Long getFirstCandidateId() {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM candidate LIMIT 1")) {
            if (rs.next()) return rs.getLong("id");
        } catch (SQLException e) {
            System.err.println("Error getting candidate ID: " + e.getMessage());
        }
        return 1L;
    }

    private static Long getFirstAdminId() {
        // Try the admin sub-table first
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM admin LIMIT 1")) {
            if (rs.next()) return rs.getLong("id");
        } catch (SQLException e) {
            System.err.println("Error getting admin ID from admin table: " + e.getMessage());
        }
        // Fallback: look in users table for role = ADMIN
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id FROM users WHERE role = 'ADMIN' LIMIT 1")) {
            if (rs.next()) return rs.getLong("id");
        } catch (SQLException e) {
            System.err.println("Error getting admin ID from users table: " + e.getMessage());
        }
        return 1L; // last resort fallback
    }
}
