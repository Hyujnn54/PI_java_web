package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * The application shell: fixed top bar + sidebar, and a content slot.
 * Team members only implement feature pages (FXML) that load into contentArea.
 */
public class MainShellController {

    // Navigation buttons
    @FXML private Button btnInterviews;
    @FXML private Button btnApplications;
    @FXML private Button btnJobOffers;
    @FXML private Button btnDashboard;
    @FXML private Button btnFullscreenToggle;

    // Top-bar buttons
    @FXML private Button btnDisconnect;
    @FXML private Button btnUserProfile;
    @FXML private Button btnNotifications;

    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;

    @FXML private StackPane contentArea;

    private String activePage = "/Applications.fxml";

    @FXML
    private void initialize() {
        if (lblUserName != null) lblUserName.setText("Utilisateur");
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());
        applyRoleToShell();
        handleApplicationsNav();
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    @FXML
    private void handleApplicationsNav() {
        activePage = "/Applications.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnApplications);
    }

    @FXML
    private void handleInterviewsNav() {
        activePage = "/InterviewManagement.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnInterviews);
    }

    @FXML
    private void handleJobOffersNav() {
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        activePage = isRecruiter ? "/JobOffers.fxml" : "/JobOffersBrowse.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnJobOffers);
    }

    @FXML
    private void handleDashboardNav() {
        activePage = "/AdminApplicationStatistics.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnDashboard);
    }

    // -------------------------------------------------------------------------
    // Top-bar handlers
    // -------------------------------------------------------------------------

    @FXML
    private void handleDisconnect() {
        showInfo("Déconnexion", "Fonctionnalité de Déconnexion",
            "La fonctionnalité de connexion/déconnexion est désactivée pour le moment.\nRedémarrez l'application pour réinitialiser.");
    }

    @FXML
    private void handleUserProfile() {
        Utils.UserContext.toggleRole();
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());
        applyRoleToShell();
        handleApplicationsNav();
    }

    @FXML
    private void handleNotifications() {
        Services.InterviewReminderScheduler.printDiagnostics();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Notifications & Rappels");
        alert.setHeaderText("Statut du scheduler de rappels");
        alert.setContentText(
            "Scheduler actif : " + Services.InterviewReminderScheduler.isRunning() + "\n"
            + "Rappels envoyés cette session : " + Services.InterviewReminderScheduler.getSentCount() + "\n\n"
            + "Cliquez OK pour forcer l'envoi des rappels maintenant."
        );
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                Services.InterviewReminderScheduler.resetAllReminders();
                Services.InterviewReminderScheduler.runTestNow();
                showInfo("Rappels envoyés", "Test terminé",
                    "Vérifiez la console pour le résultat détaillé.");
            }
        });
    }

    @FXML
    private void handleFullscreenToggle() {
        try {
            org.example.MainFX.toggleFullscreen();
        } catch (Exception e) {
            showError("Plein Écran", "Impossible d'activer le plein écran : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
            System.err.println("Error loading view: " + fxmlFile + " — " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void highlightActiveButton(Button activeBtn) {
        resetButtonStyles();
        if (activeBtn == null) return;
        activeBtn.getStyleClass().removeAll("sidebar-nav-btn");
        activeBtn.getStyleClass().add("sidebar-nav-btn-active");
    }

    private void resetButtonStyles() {
        Button[] navButtons = {btnInterviews, btnApplications, btnJobOffers, btnDashboard, btnFullscreenToggle};
        for (Button btn : navButtons) {
            if (btn == null) continue;
            btn.getStyleClass().removeAll("sidebar-nav-btn-active");
            if (!btn.getStyleClass().contains("sidebar-nav-btn"))
                btn.getStyleClass().add("sidebar-nav-btn");
        }
    }

    private void applyRoleToShell() {
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (btnInterviews != null) {
            btnInterviews.setText(isRecruiter ? "📅  Entretiens" : "📅  Entretiens à venir");
            btnInterviews.setVisible(true); btnInterviews.setManaged(true);
        }
        if (btnApplications != null) {
            btnApplications.setText(isRecruiter ? "📋  Candidatures" : "📋  Mes candidatures");
            btnApplications.setVisible(true); btnApplications.setManaged(true);
        }
        if (btnJobOffers != null) {
            btnJobOffers.setText("💼  Offres d'emploi");
            btnJobOffers.setVisible(true); btnJobOffers.setManaged(true);
        }
        if (btnDashboard != null) {
            btnDashboard.setVisible(false); btnDashboard.setManaged(false);
        }
    }

    private void showInfo(String title, String header, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(header); a.setContentText(content); a.showAndWait();
    }

    private void showError(String header, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erreur"); a.setHeaderText(header); a.setContentText(content); a.showAndWait();
    }
}
