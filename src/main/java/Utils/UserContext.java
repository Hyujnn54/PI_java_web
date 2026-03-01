package Utils;

import Models.Admin;
import Models.Candidate;
import Models.Recruiter;
import Models.User;

/**
 * UserContext — bridges to Utils.Session for backward compatibility.
 * All role/id lookups go through Session.getCurrentUser().
 */
public class UserContext {

    public enum Role { CANDIDATE, RECRUITER, ADMIN }

    private UserContext() {}

    public static Role getRole() {
        User u = Session.getCurrentUser();
        if (u instanceof Admin)     return Role.ADMIN;
        if (u instanceof Recruiter) return Role.RECRUITER;
        if (u instanceof Candidate) return Role.CANDIDATE;
        return Role.CANDIDATE;
    }

    public static String getRoleLabel() {
        Role r = getRole();
        return switch (r) {
            case ADMIN     -> "Admin";
            case RECRUITER -> "Recruteur";
            case CANDIDATE -> "Candidat";
        };
    }

    public static Long getUserId() {
        User u = Session.getCurrentUser();
        return u == null ? null : u.getId();
    }

    public static String getUserName() {
        User u = Session.getCurrentUser();
        if (u == null) return null;
        String name = (u.getFirstName() != null ? u.getFirstName() : "") +
                      (u.getLastName()  != null ? " " + u.getLastName() : "");
        return name.isBlank() ? u.getEmail() : name.trim();
    }

    public static String getUserEmail() {
        User u = Session.getCurrentUser();
        return u == null ? null : u.getEmail();
    }

    public static void setUserName(String name) {
        // No-op — name lives in Session/User object
    }

    public static boolean isLoggedIn() {
        return Session.isLoggedIn();
    }

    public static boolean isAdmin()     { return getRole() == Role.ADMIN; }
    public static boolean isRecruiter() { return getRole() == Role.RECRUITER; }
    public static boolean isCandidate() { return getRole() == Role.CANDIDATE; }

    public static Long getRecruiterId() {
        User u = Session.getCurrentUser();
        return (u instanceof Recruiter) ? u.getId() : null;
    }

    public static Long getCandidateId() {
        User u = Session.getCurrentUser();
        return (u instanceof Candidate) ? u.getId() : null;
    }

    public static Long getAdminId() {
        User u = Session.getCurrentUser();
        return (u instanceof Admin) ? u.getId() : null;
    }

    /** Kept for FXML/legacy compatibility — no-op */
    public static void login(Long userId, String userName, String email, Role role) {}
    public static void logout() { Session.clear(); }
    public static void toggleRole() {}
    public static void setCurrentRole(Role role) {}
    public static boolean hasRole(Role role) { return getRole() == role; }
}
