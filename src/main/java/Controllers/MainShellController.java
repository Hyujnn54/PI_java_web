package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * The application shell: fixed top bar + sidebar, and a content slot.
 * Team members only implement feature pages (FXML) that load into contentArea.
 */
public class MainShellController {

    @FXML private Button btnInterviews;
    @FXML private Button btnApplications;
    @FXML private Button btnJobOffers;
    @FXML private Button btnFullscreenToggle;

    @FXML private Button btnDisconnect;
    @FXML private Button btnUserProfile;
    @FXML private Button btnNotifications;

    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    @FXML private StackPane contentArea;

    private String activePage = "/InterviewManagement.fxml";

    @FXML
    private void initialize() {
        if (lblUserName != null) lblUserName.setText("Sarah Johnson");
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());

        applyRoleToShell();

        // Default page depends on role
        if (Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER) {
            handleApplicationsNav();
        } else {
            handleApplicationsNav();
        }
    }

    @FXML
    private void handleApplicationsNav() {
        activePage = "/Applications.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnApplications);
    }

    @FXML private void handleInterviewsNav() {
        activePage = "/InterviewManagement.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnInterviews);
    }

    @FXML private void handleJobOffersNav() {
        // Job offers is UI-only placeholder
        activePage = "/JobOffers.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnJobOffers);
    }

    @FXML private void handleDisconnect() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Disconnect");
        alert.setHeaderText("Login removed");
        alert.setContentText("This app currently starts directly in the main shell. Add auth later to enable logout.");
        alert.showAndWait();
    }

    @FXML private void handleUserProfile() {
        Utils.UserContext.toggleRole();
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());

        applyRoleToShell();

        // Navigate to role home
        handleApplicationsNav();
    }

    @FXML private void handleNotifications() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText("You have 3 new notifications");
        alert.setContentText("• Interview scheduled for tomorrow\n• Feedback request pending\n• New job application received");
        alert.showAndWait();
    }

    @FXML
    private void handleFullscreenToggle() {
        try {
            org.example.MainFX.toggleFullscreen();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fullscreen");
            alert.setHeaderText("Could not toggle fullscreen");
            alert.setContentText(String.valueOf(e.getMessage()));
            alert.showAndWait();
        }
    }

    private void loadContentView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent content = loader.load();

            Object controller = loader.getController();
            try {
                controller.getClass().getMethod("setUserRole", String.class)
                        .invoke(controller, Utils.UserContext.getRoleLabel());
            } catch (Exception ignored) {}

            contentArea.getChildren().setAll(content);
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlFile);
            e.printStackTrace();
        }
    }

    private void highlightActiveButton(Button activeBtn) {
        resetButtonStyles();
        if (activeBtn == null) return;
        activeBtn.getStyleClass().removeAll("nav-button-inactive");
        activeBtn.getStyleClass().add("nav-button-active");
    }

    private void resetButtonStyles() {
        Button[] navButtons = {btnInterviews, btnApplications, btnJobOffers};
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().removeAll("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button-inactive")) {
                    btn.getStyleClass().add("nav-button-inactive");
                }
            }
        }
    }

    private void applyRoleToShell() {
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (btnInterviews != null) {
            btnInterviews.setText(isRecruiter ? "📋  Interviews" : "📋  Upcoming Interviews");
        }
        if (btnApplications != null) {
            btnApplications.setText("📨  Applications");
            btnApplications.setVisible(true);
            btnApplications.setManaged(true);
        }
        if (btnJobOffers != null) {
            btnJobOffers.setText("💼  Job Offers");
            btnJobOffers.setVisible(true);
            btnJobOffers.setManaged(true);
        }
    }
}
