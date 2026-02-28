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

    // Navigation buttons
    @FXML private Button btnInterviews;
    @FXML private Button btnApplications;
    @FXML private Button btnJobOffers;
    @FXML private Button btnCalendar;
    @FXML private Button btnStatistics;
    @FXML private Button btnDashboard;
    @FXML private Button btnAdminStats;
    @FXML private Button btnAdminApplications;
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
        if (lblUserName != null) lblUserName.setText(Utils.UserContext.getUserName() != null ? Utils.UserContext.getUserName() : "Utilisateur");
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());
        applyRoleToShell();
        if (Utils.UserContext.getRole() == Utils.UserContext.Role.ADMIN) {
            handleAdminStatsNav();
        } else {
            handleApplicationsNav();
        }
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
    private void handleCalendarNav() {
        CalendarViewController.show();
    }

    @FXML
    private void handleStatistics() {
        activePage = "/AnalyticsDashboard.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnStatistics);
    }

    @FXML
    private void handleDashboardNav() {
        activePage = "/AdminApplicationStatistics.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnDashboard);
    }

    @FXML
    private void handleAdminStatsNav() {
        activePage = "/AdminApplicationStatistics.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnAdminStats);
    }

    @FXML
    private void handleAdminApplicationsNav() {
        activePage = "/AdminApplications.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnAdminApplications);
    }

    // -------------------------------------------------------------------------
    // Top-bar handlers
    // -------------------------------------------------------------------------

    @FXML
    private void handleDisconnect() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Déconnexion");
        confirmAlert.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter ?");
        confirmAlert.setContentText("La fonctionnalité de connexion/déconnexion est visuelle pour le moment.\nRedémarrez l'application pour réinitialiser.");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showInfo("Déconnexion", "Déconnecté", "Redémarrez l'application pour vous reconnecter.");
            }
        });
    }

    @FXML
    private void handleUserProfile() {
        Utils.UserContext.toggleRole();
        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());
        applyRoleToShell();
        if (Utils.UserContext.getRole() == Utils.UserContext.Role.ADMIN) {
            handleAdminStatsNav();
        } else {
            handleApplicationsNav();
        }
    }

    @FXML
    private void handleNotifications() {
        // Build a custom dialog for sending test notifications
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Tester les Notifications");
        dialog.setHeaderText("Envoyer un email / SMS de test à une adresse personnalisée");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(16);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setPrefWidth(420);

        // Email section
        javafx.scene.control.Label emailLbl = new javafx.scene.control.Label("📧 Email de destination");
        emailLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
        javafx.scene.control.TextField emailField = new javafx.scene.control.TextField();
        emailField.setPromptText("ex: destinataire@gmail.com");
        javafx.scene.control.TextField emailNameField = new javafx.scene.control.TextField();
        emailNameField.setPromptText("Nom du destinataire (optionnel)");
        javafx.scene.control.Button btnSendEmail = new javafx.scene.control.Button("📤 Envoyer Email Test");
        btnSendEmail.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        javafx.scene.control.Label emailStatus = new javafx.scene.control.Label("");
        emailStatus.setStyle("-fx-font-size: 12px;");
        btnSendEmail.setOnAction(e -> {
            String email = emailField.getText().trim();
            if (email.isBlank() || !email.contains("@")) {
                emailStatus.setText("⚠ Adresse email invalide.");
                emailStatus.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
                return;
            }
            emailStatus.setText("⏳ Envoi en cours...");
            emailStatus.setStyle("-fx-text-fill: #f0ad4e; -fx-font-size: 12px;");
            String name = emailNameField.getText().trim();
            new Thread(() -> {
                Services.EmailService.sendTestTo(email, name.isEmpty() ? "Test Utilisateur" : name);
                javafx.application.Platform.runLater(() -> {
                    emailStatus.setText("✅ Email envoyé à: " + email);
                    emailStatus.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
                });
            }, "TestEmailThread").start();
        });

        // SMS section
        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();
        javafx.scene.control.Label smsLbl = new javafx.scene.control.Label("📱 Numéro de téléphone SMS");
        smsLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 13px;");
        javafx.scene.control.TextField phoneField = new javafx.scene.control.TextField();
        phoneField.setPromptText("ex: +21653757969  ou  53757969");
        javafx.scene.control.TextField smsNameField = new javafx.scene.control.TextField();
        smsNameField.setPromptText("Nom du destinataire (optionnel)");
        javafx.scene.control.Button btnSendSMS = new javafx.scene.control.Button("📤 Envoyer SMS Test");
        btnSendSMS.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 700; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        javafx.scene.control.Label smsStatus = new javafx.scene.control.Label("");
        smsStatus.setStyle("-fx-font-size: 12px;");
        btnSendSMS.setOnAction(e -> {
            String phone = phoneField.getText().trim();
            if (phone.isBlank()) {
                smsStatus.setText("⚠ Numéro de téléphone requis.");
                smsStatus.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
                return;
            }
            smsStatus.setText("⏳ Envoi en cours...");
            smsStatus.setStyle("-fx-text-fill: #f0ad4e; -fx-font-size: 12px;");
            String name = smsNameField.getText().trim();
            new Thread(() -> {
                Services.SMSService.sendTestTo(phone, name.isEmpty() ? "Test" : name);
                javafx.application.Platform.runLater(() -> {
                    smsStatus.setText("✅ SMS dispatché vers: " + phone);
                    smsStatus.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px;");
                });
            }, "TestSMSThread").start();
        });

        // Scheduler info
        javafx.scene.control.Separator sep2 = new javafx.scene.control.Separator();
        javafx.scene.control.Label schedulerInfo = new javafx.scene.control.Label(
            "⏰ Scheduler actif : " + Services.InterviewReminderScheduler.isRunning()
            + "   |   Rappels envoyés : " + Services.InterviewReminderScheduler.getSentCount());
        schedulerInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        content.getChildren().addAll(
            emailLbl, emailField, emailNameField, btnSendEmail, emailStatus,
            sep, smsLbl, phoneField, smsNameField, btnSendSMS, smsStatus,
            sep2, schedulerInfo
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.showAndWait();
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
        activeBtn.getStyleClass().removeAll("sidebar-nav-btn", "sidebar-button");
        activeBtn.getStyleClass().add("sidebar-nav-btn-active");
    }

    private void resetButtonStyles() {
        Button[] navButtons = {btnInterviews, btnApplications, btnJobOffers,
                               btnCalendar, btnStatistics,
                               btnDashboard, btnAdminStats, btnAdminApplications, btnFullscreenToggle};
        for (Button btn : navButtons) {
            if (btn == null) continue;
            btn.getStyleClass().removeAll("sidebar-nav-btn-active", "sidebar-button-active");
            if (!btn.getStyleClass().contains("sidebar-nav-btn"))
                btn.getStyleClass().add("sidebar-nav-btn");
        }
    }

    private void applyRoleToShell() {
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        boolean isAdmin     = Utils.UserContext.getRole() == Utils.UserContext.Role.ADMIN;
        boolean isCandidate = Utils.UserContext.getRole() == Utils.UserContext.Role.CANDIDATE;

        if (lblUserRole != null) lblUserRole.setText(Utils.UserContext.getRoleLabel());

        if (btnInterviews != null) {
            btnInterviews.setText(isRecruiter || isAdmin ? "📅   Entretiens" : "📅   Entretiens à venir");
            btnInterviews.setVisible(!isAdmin);
            btnInterviews.setManaged(!isAdmin);
        }
        if (btnApplications != null) {
            btnApplications.setText(isRecruiter ? "📋   Candidatures" : "📋   Mes candidatures");
            btnApplications.setVisible(!isAdmin);
            btnApplications.setManaged(!isAdmin);
        }
        if (btnJobOffers != null) {
            if (isAdmin) btnJobOffers.setText("💼   Gérer les offres");
            else if (isRecruiter) btnJobOffers.setText("💼   Mes Offres");
            else btnJobOffers.setText("💼   Offres d'emploi");
            btnJobOffers.setVisible(true);
            btnJobOffers.setManaged(true);
        }
        if (btnCalendar != null) {
            btnCalendar.setVisible(!isAdmin);
            btnCalendar.setManaged(!isAdmin);
        }
        if (btnStatistics != null) {
            btnStatistics.setVisible(!isCandidate);
            btnStatistics.setManaged(!isCandidate);
            if (isAdmin) btnStatistics.setText("📊   Statistiques Globales");
            else         btnStatistics.setText("📊   Mes Statistiques");
        }
        if (btnDashboard != null) { btnDashboard.setVisible(false); btnDashboard.setManaged(false); }
        if (btnAdminStats != null) { btnAdminStats.setVisible(isAdmin); btnAdminStats.setManaged(isAdmin); }
        if (btnAdminApplications != null) { btnAdminApplications.setVisible(isAdmin); btnAdminApplications.setManaged(isAdmin); }
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
