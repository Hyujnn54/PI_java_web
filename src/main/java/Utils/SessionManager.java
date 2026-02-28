package Utils;

import Models.events.User;

/**
 * Classe simple pour stocker l'utilisateur connecté
 */
public class SessionManager {
    private static User currentUser = null;

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void logout() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static Long getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }

    public static String getCurrentUserRole() {
        return currentUser != null ? currentUser.getRole().name() : null;
    }
}
