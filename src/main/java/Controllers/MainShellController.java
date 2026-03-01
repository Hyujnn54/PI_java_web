package Controllers;

import Models.Admin;
import Models.Candidate;
import Models.Recruiter;
import Models.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import Utils.Session;

public class MainShellController {

    private static MainShellController instance;
    public static MainShellController getInstance() { return instance; }

    public void repatchDark(javafx.scene.layout.VBox container) { /* no-op */ }

    @FXML private StackPane contentArea;
    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    @FXML private Button btnApplications;
    @FXML private Button btnJobOffers;
    @FXML private Button btnInterviews;
    @FXML private Button btnEvents;
    @FXML private Button btnPastEvents;
    @FXML private Button btnDashboard;
    @FXML private Button btnUserProfile;
    @FXML private Button btnDisconnect;
    @FXML private Button btnFullscreenToggle;

    @FXML
    public void initialize() {
        instance = this;
        User u = Session.getCurrentUser();

        // Top bar
        String displayName = (u != null && u.getFirstName() != null && !u.getFirstName().isBlank())
                ? u.getFirstName() + (u.getLastName() != null ? " " + u.getLastName() : "")
                : (u != null ? u.getEmail() : "User");
        if (lblUserName != null) lblUserName.setText(displayName);
        if (lblUserRole != null) lblUserRole.setText(getRoleLabel(u));

        // Sidebar visibility by role
        boolean isAdmin     = u instanceof Admin;
        boolean isRecruiter = u instanceof Recruiter;
        boolean isCandidate = u instanceof Candidate;

        // Admin Dashboard — admin only
        setVisible(btnDashboard, isAdmin);

        // Events — recruiter + candidate, NOT admin
        setVisible(btnEvents, !isAdmin);

        // Past Events — candidate only
        setVisible(btnPastEvents, isCandidate);

        // Interviews — recruiter + candidate
        setVisible(btnInterviews, !isAdmin);

        // Job Offers — all
        setVisible(btnJobOffers, true);

        // Applications — all
        setVisible(btnApplications, true);

        // Default view
        if (isAdmin) {
            loadContent("/AdminDashboard.fxml");
        } else {
            loadContent("/views/application/Applications.fxml");
        }
    }

    private void setVisible(Button btn, boolean show) {
        if (btn != null) { btn.setVisible(show); btn.setManaged(show); }
    }

    private String getRoleLabel(User u) {
        if (u instanceof Admin)     return "Admin";
        if (u instanceof Recruiter) return "Recruteur";
        if (u instanceof Candidate) return "Candidat";
        return "Utilisateur";
    }

    public void loadContent(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (Exception e) {
            System.err.println("Error loading view: " + fxml + " — " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Top bar ──────────────────────────────────────────────
    @FXML private void handleNotifications() {}

    @FXML
    private void handleUserProfile() { loadContent("/Profile.fxml"); }

    @FXML
    private void handleDisconnect() {
        try {
            Session.clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            contentArea.getScene().setRoot(loader.load());
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── Sidebar navigation ───────────────────────────────────
    @FXML
    private void handleApplicationsNav() {
        User u = Session.getCurrentUser();
        if (u instanceof Admin) loadContent("/views/application/AdminApplications.fxml");
        else                    loadContent("/views/application/Applications.fxml");
    }

    @FXML
    private void handleJobOffersNav() {
        User u = Session.getCurrentUser();
        if      (u instanceof Admin)     loadContent("/views/joboffers/JobOffersAdmin.fxml");
        else if (u instanceof Recruiter) loadContent("/views/joboffers/JobOffers.fxml");
        else                             loadContent("/views/joboffers/JobOffersBrowse.fxml");
    }

    @FXML
    private void handleInterviewsNav() {
        loadContent("/views/interview/InterviewManagement.fxml");
    }

    @FXML
    private void handleEventsNav() {
        User u = Session.getCurrentUser();
        if      (u instanceof Recruiter) loadContent("/views/events/RecruiterEvents.fxml");
        else if (u instanceof Candidate) loadContent("/views/events/CandidateEvents.fxml");
        else                             loadContent("/views/events/Events.fxml");
    }

    @FXML
    private void handlePastEventsNav() {
        loadContent("/views/events/PastEvents.fxml");
    }

    @FXML
    private void handleDashboardNav() {
        loadContent("/AdminDashboard.fxml");
    }

    @FXML
    private void handleFullscreenToggle() {
        try {
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setFullScreen(!stage.isFullScreen());
        } catch (Exception e) { e.printStackTrace(); }
    }
}