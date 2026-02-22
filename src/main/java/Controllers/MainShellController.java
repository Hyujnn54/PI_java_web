package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * The application shell: fixed top bar + sidebar, and a content slot.
 * Team members only implement feature pages (FXML) that load into contentArea.
 */
public class MainShellController {

    @FXML private Button btnInterviews;
    @FXML private Button btnApplications;
    @FXML private Button btnJobOffers;
    @FXML private Button btnStatistics;
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
        if (lblUserName != null) lblUserName.setText("User"); // Will be updated dynamically
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());

        applyRoleToShell();

        // Default page: Load Job Offers
        handleJobOffersNav();
    }

    @FXML
    private void handleApplicationsNav() {
        // Applications.fxml not yet implemented - redirect to Job Offers
        showNotImplementedAlert("Applications Management");
        handleJobOffersNav();
    }

    @FXML private void handleInterviewsNav() {
        // InterviewManagement.fxml not yet implemented - redirect to Job Offers
        showNotImplementedAlert("Interview Management");
        handleJobOffersNav();
    }

    @FXML private void handleJobOffersNav() {
        // Load different views based on user role
        Utils.UserContext.Role role = Utils.UserContext.getRole();

        if (role == Utils.UserContext.Role.ADMIN) {
            activePage = "/JobOffersAdmin.fxml";
        } else if (role == Utils.UserContext.Role.RECRUITER) {
            activePage = "/JobOffers.fxml";
        } else {
            activePage = "/JobOffersBrowse.fxml";
        }

        loadContentView(activePage);
        highlightActiveButton(btnJobOffers);
    }

    @FXML
    private void handleStatistics() {
        activePage = "/Statistics.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnStatistics);
    }

    @FXML private void handleDisconnect() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Disconnect");
        confirmAlert.setHeaderText("Are you sure you want to exit?");
        confirmAlert.setContentText("The application will close.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Close the application
                Stage stage = (Stage) btnDisconnect.getScene().getWindow();
                stage.close();
            }
        });
    }

    @FXML private void handleUserProfile() {
        Utils.UserContext.toggleRole();
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());

        applyRoleToShell();

        // Navigate to Job Offers page
        handleJobOffersNav();
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

    private void showNotImplementedAlert(String featureName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feature Not Implemented");
        alert.setHeaderText(featureName + " - Coming Soon");
        alert.setContentText("This feature is not yet implemented.\nRedirecting to Job Offers...");
        alert.show();
    }

    private void highlightActiveButton(Button activeBtn) {
        resetButtonStyles();
        if (activeBtn == null) return;
        activeBtn.getStyleClass().removeAll("sidebar-button");
        activeBtn.getStyleClass().add("sidebar-button-active");
    }

    private void resetButtonStyles() {
        Button[] navButtons = {btnInterviews, btnApplications, btnJobOffers, btnStatistics};
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.getStyleClass().removeAll("sidebar-button-active");
                if (!btn.getStyleClass().contains("sidebar-button")) {
                    btn.getStyleClass().add("sidebar-button");
                }
            }
        }
    }

    private void applyRoleToShell() {
        Utils.UserContext.Role role = Utils.UserContext.getRole();
        boolean isRecruiter = role == Utils.UserContext.Role.RECRUITER;
        boolean isAdmin = role == Utils.UserContext.Role.ADMIN;

        if (btnInterviews != null) {
            if (isAdmin) {
                btnInterviews.setText("⚙️  System");
            } else if (isRecruiter) {
                btnInterviews.setText("📋  Interviews");
            } else {
                btnInterviews.setText("📋  Upcoming Interviews");
            }
        }
        if (btnApplications != null) {
            if (isAdmin) {
                btnApplications.setText("📊  Reports");
            } else {
                btnApplications.setText("📨  Applications");
            }
            btnApplications.setVisible(true);
            btnApplications.setManaged(true);
        }
        if (btnJobOffers != null) {
            if (isAdmin) {
                btnJobOffers.setText("⚙️  Manage All Jobs");
            } else {
                btnJobOffers.setText("💼  Job Offers");
            }
            btnJobOffers.setVisible(true);
            btnJobOffers.setManaged(true);
        }
    }
}
