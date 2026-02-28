package Utils;

import java.sql.*;

/**
 * UserContext - manages the connected user's session.
 * Uses a singleton instance internally but exposes static methods for convenience.
 */
public class UserContext {

    private static UserContext instance;

    private Long userId;
    private String userName;
    private String userEmail;
    private Role currentRole;

    // Cached DB IDs
    private Long cachedRecruiterId = null;
    private Long cachedCandidateId = null;
    private Long cachedAdminId     = null;

    public enum Role {
        CANDIDATE("Candidat"),
        RECRUITER("Recruteur"),
        ADMIN("Administrateur");

        private final String label;
        Role(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private UserContext() {
        this.currentRole = Role.RECRUITER;
        this.userName    = "Utilisateur";
        this.userEmail   = "";
        this.userId      = null;
    }

    public static UserContext getInstance() {
        if (instance == null) instance = new UserContext();
        return instance;
    }

    // -------------------------------------------------------------------------
    // Login / Logout
    // -------------------------------------------------------------------------

    public static void login(Long userId, String userName, String email, Role role) {
        UserContext ctx = getInstance();
        ctx.userId      = userId;
        ctx.userName    = userName;
        ctx.userEmail   = email;
        ctx.currentRole = role;
        ctx.cachedRecruiterId = null;
        ctx.cachedCandidateId = null;
        ctx.cachedAdminId     = null;
    }

    public static void logout() {
        UserContext ctx = getInstance();
        ctx.userId      = null;
        ctx.userName    = null;
        ctx.userEmail   = null;
        ctx.currentRole = null;
    }

    public static boolean isLoggedIn() {
        return getInstance().userId != null && getInstance().currentRole != null;
    }

    // -------------------------------------------------------------------------
    // Role helpers
    // -------------------------------------------------------------------------

    public static Role getRole()              { return getInstance().currentRole; }
    public static String getRoleLabel()       { return getInstance().currentRole != null ? getInstance().currentRole.getLabel() : ""; }
    public static boolean isAdmin()           { return getInstance().currentRole == Role.ADMIN; }
    public static boolean isRecruiter()       { return getInstance().currentRole == Role.RECRUITER; }
    public static boolean isCandidate()       { return getInstance().currentRole == Role.CANDIDATE; }
    public static boolean hasRole(Role role)  { return getInstance().currentRole == role; }

    public static void setCurrentRole(Role role) {
        getInstance().currentRole = role;
        getInstance().cachedRecruiterId = null;
        getInstance().cachedCandidateId = null;
        getInstance().cachedAdminId     = null;
    }

    public static void toggleRole() {
        UserContext ctx = getInstance();
        Role newRole = switch (ctx.currentRole) {
            case RECRUITER -> Role.CANDIDATE;
            case CANDIDATE -> Role.ADMIN;
            case ADMIN     -> Role.RECRUITER;
        };
        ctx.currentRole = newRole;
        ctx.cachedRecruiterId = null;
        ctx.cachedCandidateId = null;
        ctx.cachedAdminId     = null;

        // Switch to a real DB user matching the new role
        switchToRoleUser(ctx, newRole);
    }

    /**
     * Looks up the first user for the given role in the DB and updates userId/userName/userEmail.
     * Falls back to role-specific tables if no match found in users table.
     */
    private static void switchToRoleUser(UserContext ctx, Role newRole) {
        String roleStr = newRole.name(); // "RECRUITER", "CANDIDATE", "ADMIN"
        Long foundId = queryFirstId(
            "SELECT id FROM users WHERE UPPER(role) = '" + roleStr + "' LIMIT 1", null);
        if (foundId != null) {
            ctx.userId = foundId;
            ctx.userName = queryFirstString(
                "SELECT CONCAT(COALESCE(first_name,''), ' ', COALESCE(last_name,'')) FROM users WHERE id = " + foundId,
                newRole.getLabel());
            ctx.userEmail = queryFirstString(
                "SELECT email FROM users WHERE id = " + foundId, "");
        } else {
            // Fallback: try role-specific table
            String table = switch (newRole) {
                case RECRUITER -> "recruiter";
                case CANDIDATE -> "candidate";
                case ADMIN     -> "admin";
            };
            Long tableId = queryFirstId("SELECT id FROM " + table + " LIMIT 1", null);
            if (tableId != null) {
                ctx.userId = tableId;
            }
        }
    }

    private static String queryFirstString(String sql, String fallback) {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            if (rs.next()) {
                String val = rs.getString(1);
                return (val != null && !val.isBlank()) ? val.trim() : fallback;
            }
        } catch (SQLException e) {
            System.err.println("UserContext DB error: " + e.getMessage());
        }
        return fallback;
    }

    // -------------------------------------------------------------------------
    // User info
    // -------------------------------------------------------------------------

    public static Long getUserId()              { return getInstance().userId; }
    public static void setUserId(Long id)       { getInstance().userId = id; }
    public static String getUserName()          { return getInstance().userName; }
    public static void setUserName(String n)    { getInstance().userName = n; }
    public static String getUserEmail()         { return getInstance().userEmail; }
    public static void setUserEmail(String e)   { getInstance().userEmail = e; }

    // -------------------------------------------------------------------------
    // DB-backed ID resolvers
    // -------------------------------------------------------------------------

    public static Long getRecruiterId() {
        UserContext ctx = getInstance();
        if (ctx.currentRole == Role.RECRUITER && ctx.userId != null) {
            // Verify user actually exists in recruiter table
            Long rid = queryFirstId("SELECT id FROM recruiter WHERE id = " + ctx.userId, null);
            if (rid != null) return rid;
            // Fallback: look up by user_id foreign key if schema differs
            rid = queryFirstId("SELECT id FROM recruiter WHERE user_id = " + ctx.userId + " LIMIT 1", null);
            if (rid != null) return rid;
            // Last resort: first recruiter in DB
            rid = queryFirstId("SELECT id FROM recruiter LIMIT 1", null);
            if (rid != null) return rid;
            return ctx.userId;
        }
        if (ctx.cachedRecruiterId == null) {
            ctx.cachedRecruiterId = queryFirstId("SELECT id FROM users WHERE UPPER(role) = 'RECRUITER' LIMIT 1", null);
            if (ctx.cachedRecruiterId == null) {
                ctx.cachedRecruiterId = queryFirstId("SELECT id FROM recruiter LIMIT 1", 1L);
            }
        }
        return ctx.cachedRecruiterId;
    }

    public static Long getCandidateId() {
        UserContext ctx = getInstance();
        if (ctx.currentRole == Role.CANDIDATE && ctx.userId != null) {
            // Verify user actually exists in candidate table
            Long cid = queryFirstId("SELECT id FROM candidate WHERE id = " + ctx.userId, null);
            if (cid != null) return cid;
            // Fallback: look up by user_id foreign key if schema differs
            cid = queryFirstId("SELECT id FROM candidate WHERE user_id = " + ctx.userId + " LIMIT 1", null);
            if (cid != null) return cid;
            // Last resort: first candidate in DB
            cid = queryFirstId("SELECT id FROM candidate LIMIT 1", null);
            if (cid != null) return cid;
            return ctx.userId;
        }
        if (ctx.cachedCandidateId == null) {
            ctx.cachedCandidateId = queryFirstId("SELECT id FROM users WHERE UPPER(role) = 'CANDIDATE' LIMIT 1", null);
            if (ctx.cachedCandidateId == null) {
                ctx.cachedCandidateId = queryFirstId("SELECT id FROM candidate LIMIT 1", 1L);
            }
        }
        return ctx.cachedCandidateId;
    }

    public static Long getAdminId() {
        UserContext ctx = getInstance();
        if (ctx.currentRole == Role.ADMIN && ctx.userId != null) return ctx.userId;
        if (ctx.cachedAdminId == null) {
            ctx.cachedAdminId = queryFirstId("SELECT id FROM users WHERE UPPER(role) = 'ADMIN' LIMIT 1", null);
            if (ctx.cachedAdminId == null) {
                ctx.cachedAdminId = queryFirstId("SELECT id FROM admin LIMIT 1", 1L);
            }
        }
        return ctx.cachedAdminId;
    }

    private static Long queryFirstId(String sql, Long fallback) {
        try (Connection conn = MyDatabase.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong("id");
        } catch (SQLException e) {
            System.err.println("UserContext DB error: " + e.getMessage());
        }
        return fallback;
    }

    @Override
    public String toString() {
        return "UserContext{userId=" + userId + ", userName='" + userName
                + "', role=" + currentRole + '}';
    }
}
