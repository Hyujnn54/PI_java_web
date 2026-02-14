package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    @FXML private Button btnDashboard;
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
        // Load different views based on user role
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        activePage = isRecruiter ? "/JobOffers.fxml" : "/JobOffersBrowse.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnJobOffers);
    }

    @FXML private void handleDashboardNav() {
        activePage = "/AdminDashboard.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnDashboard);
    }

    @FXML private void handleDisconnect() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Disconnect");
        confirmAlert.setHeaderText("Are you sure you want to logout?");
        confirmAlert.setContentText("You will be redirected to the login page.");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Load login page
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
                    Parent root = loader.load();

                    Stage stage = (Stage) btnDisconnect.getScene().getWindow();
                    stage.setScene(new Scene(root, 550, 650));
                    stage.setTitle("Talent Bridge - Login");
                    stage.centerOnScreen();

                } catch (IOException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Error");
                    errorAlert.setHeaderText("Failed to logout");
                    errorAlert.setContentText(e.getMessage());
                    errorAlert.showAndWait();
                }
            }
        });
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
        activeBtn.getStyleClass().removeAll("sidebar-button");
        activeBtn.getStyleClass().add("sidebar-button-active");
    }

    private void resetButtonStyles() {
        Button[] navButtons = {btnInterviews, btnApplications, btnJobOffers, btnDashboard};
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
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        boolean isAdmin = Utils.UserContext.getRole() == Utils.UserContext.Role.ADMIN;

        // Update button labels for interviews
        if (btnInterviews != null) {
            btnInterviews.setText(isRecruiter ? "📋  Interviews" : "📋  Upcoming Interviews");
            // Show for recruiter and admin, hide for candidate
            if (isAdmin) {
                btnInterviews.setVisible(true);
                btnInterviews.setManaged(true);
            } else if (!isRecruiter) {
                btnInterviews.setVisible(true);
                btnInterviews.setManaged(true);
            } else {
                btnInterviews.setVisible(true);
                btnInterviews.setManaged(true);
            }
        }

        // Applications button
        if (btnApplications != null) {
            btnApplications.setText("📨  Applications");
            btnApplications.setVisible(true);
            btnApplications.setManaged(true);
        }

        // Job Offers button
        if (btnJobOffers != null) {
            btnJobOffers.setText("💼  Job Offers");
            // Hide for admin in job offers view (show dashboard instead)
            if (isAdmin) {
                btnJobOffers.setVisible(false);
                btnJobOffers.setManaged(false);
            } else {
                btnJobOffers.setVisible(true);
                btnJobOffers.setManaged(true);
            }
        }

        // Dashboard button - only show for admin
        if (btnDashboard != null) {
            if (isAdmin) {
                btnDashboard.setText("👨‍💼  Dashboard");
                btnDashboard.setVisible(true);
                btnDashboard.setManaged(true);
            } else {
                btnDashboard.setVisible(false);
                btnDashboard.setManaged(false);
            }
        }
    }
}
