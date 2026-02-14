package Utils;

/**
 * UserContext - Singleton pour gérer le contexte de l'utilisateur connecté
 * Stocke les informations de l'utilisateur actuel et son rôle
 */
public class UserContext {

    private static UserContext instance;

    // User information
    private Long userId;
    private String userName;
    private String userEmail;
    private Role currentRole;

    // Role enum
    public enum Role {
        CANDIDATE("Candidate"),
        RECRUITER("Recruiter"),
        ADMIN("Admin");

        private final String label;

        Role(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    // Private constructor for Singleton
    private UserContext() {
        // Default values - start as Recruiter with ID 1
        this.currentRole = Role.RECRUITER;
        this.userName = "Demo Recruiter";
        this.userEmail = "recruiter@talentbridge.com";
        this.userId = 1L; // Recruiter ID
    }

    // Get singleton instance
    public static UserContext getInstance() {
        if (instance == null) {
            instance = new UserContext();
        }
        return instance;
    }

    // Static convenience methods
    public static Role getRole() {
        return getInstance().currentRole;
    }

    public static String getRoleLabel() {
        return getInstance().currentRole.getLabel();
    }

    public static void setRole(Role role) {
        getInstance().currentRole = role;
    }

    public static void toggleRole() {
        UserContext ctx = getInstance();
        // Toggle between CANDIDATE and RECRUITER for demo purposes
        if (ctx.currentRole == Role.CANDIDATE) {
            // Switch to Recruiter
            ctx.currentRole = Role.RECRUITER;
            ctx.userId = 1L; // Recruiter ID
            ctx.userName = "Demo Recruiter";
            ctx.userEmail = "recruiter@talentbridge.com";
        } else if (ctx.currentRole == Role.RECRUITER) {
            // Switch to Candidate
            ctx.currentRole = Role.CANDIDATE;
            ctx.userId = 2L; // Candidate ID (different user)
            ctx.userName = "John Doe";
            ctx.userEmail = "candidate@example.com";
        }
    }

    public static Long getUserId() {
        return getInstance().userId;
    }

    public static void setUserId(Long userId) {
        getInstance().userId = userId;
    }

    public static String getUserName() {
        return getInstance().userName;
    }

    public static void setUserName(String userName) {
        getInstance().userName = userName;
    }

    public static String getUserEmail() {
        return getInstance().userEmail;
    }

    public static void setUserEmail(String userEmail) {
        getInstance().userEmail = userEmail;
    }

    public static Long getRecruiterId() {
        // Si l'utilisateur est un recruteur, retourne son ID
        // Sinon retourne null ou un ID par défaut
        if (getInstance().currentRole == Role.RECRUITER) {
            return getInstance().userId;
        }
        return 1L; // ID par défaut pour les tests
    }

    public static Long getCandidateId() {
        // Si l'utilisateur est un candidat, retourne son ID
        // Sinon retourne null ou un ID par défaut
        if (getInstance().currentRole == Role.CANDIDATE) {
            return getInstance().userId;
        }
        return 1L; // ID par défaut pour les tests
    }

    /**
     * Login - Définit l'utilisateur connecté
     */
    public static void login(Long userId, String userName, String email, Role role) {
        UserContext ctx = getInstance();
        ctx.userId = userId;
        ctx.userName = userName;
        ctx.userEmail = email;
        ctx.currentRole = role;
    }

    /**
     * Logout - Réinitialise le contexte
     */
    public static void logout() {
        UserContext ctx = getInstance();
        ctx.userId = null;
        ctx.userName = null;
        ctx.userEmail = null;
        ctx.currentRole = null;
    }

    /**
     * Vérifie si un utilisateur est connecté
     */
    public static boolean isLoggedIn() {
        return getInstance().userId != null && getInstance().currentRole != null;
    }

    /**
     * Vérifie si l'utilisateur a un rôle spécifique
     */
    public static boolean hasRole(Role role) {
        return getInstance().currentRole == role;
    }

    @Override
    public String toString() {
        return "UserContext{" +
                "userId=" + userId +
                ", userName='" + userName + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", currentRole=" + currentRole +
                '}';
    }
}



