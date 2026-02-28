package Controllers;

import Controllers.interview.CalendarViewController;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;

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
    @FXML private Button btnThemeToggle;

    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Label lblSidebarUserName;
    @FXML private Label lblSidebarUserRole;

    @FXML private StackPane contentArea;
    @FXML private BorderPane rootPane;

    private String activePage = "/views/application/Applications.fxml";
    private boolean darkMode  = false;

    @FXML
    public void initialize() {
        String name = Utils.UserContext.getUserName() != null ? Utils.UserContext.getUserName() : "Utilisateur";
        String role = Utils.UserContext.getRoleLabel();
        if (lblUserName != null) lblUserName.setText(name);
        if (lblUserRole != null) lblUserRole.setText(role);
        if (lblSidebarUserName != null) lblSidebarUserName.setText(name);
        if (lblSidebarUserRole != null) lblSidebarUserRole.setText(role);
        applyRoleToShell();

        // Animate sidebar nav buttons in with staggered fade+slide
        Button[] navBtns = {btnApplications, btnInterviews, btnJobOffers,
                            btnCalendar, btnStatistics, btnAdminStats,
                            btnAdminApplications, btnFullscreenToggle};
        for (int i = 0; i < navBtns.length; i++) {
            Button btn = navBtns[i];
            if (btn == null || !btn.isManaged()) continue;
            btn.setOpacity(0);
            btn.setTranslateX(-12);
            final int delay = i * 45;
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(delay)),
                new KeyFrame(Duration.millis(delay + 280),
                    new KeyValue(btn.opacityProperty(), 1.0),
                    new KeyValue(btn.translateXProperty(), 0))
            );
            tl.play();
        }

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
        activePage = "/views/application/Applications.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnApplications);
    }

    @FXML
    private void handleInterviewsNav() {
        activePage = "/views/interview/InterviewManagement.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnInterviews);
    }

    @FXML
    private void handleJobOffersNav() {
        Utils.UserContext.Role role = Utils.UserContext.getRole();
        if (role == Utils.UserContext.Role.ADMIN) {
            activePage = "/views/joboffers/JobOffersAdmin.fxml";
        } else if (role == Utils.UserContext.Role.RECRUITER) {
            activePage = "/views/joboffers/JobOffers.fxml";
        } else {
            activePage = "/views/joboffers/JobOffersBrowse.fxml";
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
        activePage = "/views/joboffers/AnalyticsDashboard.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnStatistics);
    }

    @FXML
    private void handleDashboardNav() {
        activePage = "/views/application/AdminApplicationStatistics.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnDashboard);
    }

    @FXML
    private void handleAdminStatsNav() {
        activePage = "/views/application/AdminApplicationStatistics.fxml";
        loadContentView(activePage);
        highlightActiveButton(btnAdminStats);
    }

    @FXML
    private void handleAdminApplicationsNav() {
        activePage = "/views/application/AdminApplications.fxml";
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
        String name = Utils.UserContext.getUserName() != null ? Utils.UserContext.getUserName() : "Utilisateur";
        String role = Utils.UserContext.getRoleLabel();
        if (lblUserName != null) lblUserName.setText(name);
        if (lblUserRole != null) lblUserRole.setText(role);
        if (lblSidebarUserName != null) lblSidebarUserName.setText(name);
        if (lblSidebarUserRole != null) lblSidebarUserRole.setText(role);
        applyRoleToShell();
        if (Utils.UserContext.getRole() == Utils.UserContext.Role.ADMIN) {
            handleAdminStatsNav();
        } else {
            handleApplicationsNav();
        }
    }

    @FXML
    private void handleNotifications() {
        // Show system status info (no debug email/SMS)
        boolean schedulerRunning = Services.interview.InterviewReminderScheduler.isRunning();
        int remindersSent = Services.interview.InterviewReminderScheduler.getSentCount();

        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Notifications Systeme");
        dialog.setHeaderText(null);

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(14);
        content.setPadding(new javafx.geometry.Insets(22));
        content.setPrefWidth(360);
        content.setStyle("-fx-background-color: white;");

        // Title
        javafx.scene.control.Label title = new javafx.scene.control.Label("Statut du Systeme");
        title.setStyle("-fx-font-size:17px; -fx-font-weight:700; -fx-text-fill:#2c3e50;");

        javafx.scene.control.Separator sep = new javafx.scene.control.Separator();

        // Scheduler status
        javafx.scene.layout.HBox schedulerRow = new javafx.scene.layout.HBox(12);
        schedulerRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        schedulerRow.setStyle("-fx-background-color:" + (schedulerRunning ? "#D4EDDA" : "#F8D7DA")
                + "; -fx-background-radius:10; -fx-padding:12 16;");
        javafx.scene.control.Label schedulerIcon = new javafx.scene.control.Label(schedulerRunning ? "✅" : "⛔");
        schedulerIcon.setStyle("-fx-font-size:18px;");
        javafx.scene.layout.VBox schedulerText = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label schedulerLabel = new javafx.scene.control.Label(
                "Planificateur de rappels: " + (schedulerRunning ? "ACTIF" : "INACTIF"));
        schedulerLabel.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:"
                + (schedulerRunning ? "#155724" : "#721C24") + ";");
        javafx.scene.control.Label reminderLabel = new javafx.scene.control.Label(
                remindersSent + " rappel(s) envoye(s) cette session");
        reminderLabel.setStyle("-fx-font-size:11px; -fx-text-fill:" + (schedulerRunning ? "#155724" : "#721C24") + ";");
        schedulerText.getChildren().addAll(schedulerLabel, reminderLabel);
        schedulerRow.getChildren().addAll(schedulerIcon, schedulerText);

        // DB status
        boolean dbOk = Utils.MyDatabase.getInstance().getConnection() != null;
        javafx.scene.layout.HBox dbRow = new javafx.scene.layout.HBox(12);
        dbRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        dbRow.setStyle("-fx-background-color:" + (dbOk ? "#D4EDDA" : "#F8D7DA")
                + "; -fx-background-radius:10; -fx-padding:12 16;");
        javafx.scene.control.Label dbIcon = new javafx.scene.control.Label(dbOk ? "🗄" : "⚠");
        dbIcon.setStyle("-fx-font-size:18px;");
        javafx.scene.control.Label dbLabel = new javafx.scene.control.Label(
                "Base de donnees: " + (dbOk ? "CONNECTEE" : "DECONNECTEE"));
        dbLabel.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:"
                + (dbOk ? "#155724" : "#721C24") + ";");
        dbRow.getChildren().addAll(dbIcon, dbLabel);

        // User info
        javafx.scene.layout.HBox userRow = new javafx.scene.layout.HBox(12);
        userRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        userRow.setStyle("-fx-background-color:#DCEEFB; -fx-background-radius:10; -fx-padding:12 16;");
        javafx.scene.control.Label userIcon = new javafx.scene.control.Label("👤");
        userIcon.setStyle("-fx-font-size:18px;");
        javafx.scene.layout.VBox userText = new javafx.scene.layout.VBox(2);
        javafx.scene.control.Label userLabel = new javafx.scene.control.Label(
                Utils.UserContext.getUserName() != null ? Utils.UserContext.getUserName() : "Utilisateur");
        userLabel.setStyle("-fx-font-weight:700; -fx-font-size:13px; -fx-text-fill:#1565C0;");
        javafx.scene.control.Label roleLabel = new javafx.scene.control.Label(
                "Role: " + Utils.UserContext.getRoleLabel());
        roleLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#1565C0;");
        userText.getChildren().addAll(userLabel, roleLabel);
        userRow.getChildren().addAll(userIcon, userText);

        content.getChildren().addAll(title, sep, schedulerRow, dbRow, userRow);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: white;");
        dialog.showAndWait();
    }

    @FXML
    private void handleThemeToggle() {
        if (rootPane == null) return;
        darkMode = !darkMode;

        if (darkMode) {
            if (!rootPane.getStyleClass().contains("dark"))
                rootPane.getStyleClass().add("dark");
            rootPane.setStyle("-fx-background-color: #0F1923;");
            if (contentArea != null) contentArea.setStyle("-fx-background-color:#0F1923; -fx-padding:24;");
            if (btnThemeToggle != null) {
                btnThemeToggle.setText("☀");
                btnThemeToggle.setStyle("-fx-background-color:#1A2535; -fx-font-size:16px; -fx-cursor:hand;"
                    + "-fx-background-radius:22; -fx-min-width:40; -fx-min-height:40;"
                    + "-fx-border-color:#243044; -fx-border-width:1; -fx-border-radius:22; -fx-text-fill:#F5C518;");
            }
        } else {
            rootPane.getStyleClass().remove("dark");
            rootPane.setStyle("-fx-background-color: #EBF0F8;");
            if (contentArea != null) contentArea.setStyle("-fx-background-color:#EBF0F8; -fx-padding:24;");
            if (btnThemeToggle != null) {
                btnThemeToggle.setText("🌙");
                btnThemeToggle.setStyle("-fx-background-color:#F0F4FA; -fx-font-size:16px; -fx-cursor:hand;"
                    + "-fx-background-radius:22; -fx-min-width:40; -fx-min-height:40;"
                    + "-fx-border-color:#E4EBF5; -fx-border-width:1; -fx-border-radius:22;");
            }
        }

        // Re-apply theme to the currently displayed page
        if (contentArea != null && !contentArea.getChildren().isEmpty()) {
            javafx.scene.Node current = contentArea.getChildren().get(0);
            if (current instanceof Parent p) {
                if (darkMode) {
                    if (DARK_CSS_URL != null && !p.getStylesheets().contains(DARK_CSS_URL))
                        p.getStylesheets().add(DARK_CSS_URL);
                    if (!p.getStyleClass().contains("dark"))
                        p.getStyleClass().add("dark");
                    patchNodeDark(p);
                } else {
                    p.getStyleClass().remove("dark");
                    p.getStylesheets().remove(DARK_CSS_URL);
                }
            }
        }

        // Bounce animation on toggle button
        if (btnThemeToggle != null) {
            ScaleTransition st = new ScaleTransition(Duration.millis(180), btnThemeToggle);
            st.setFromX(0.85); st.setFromY(0.85);
            st.setToX(1.0);    st.setToY(1.0);
            st.play();
        }

        // Reload active page so dynamic Java-built UI also gets patched
        if (activePage != null && !activePage.isBlank()) {
            loadContentView(activePage);
        }
    }

    @FXML
    private void handleFullscreenToggle() {
        try {
            org.example.MainFX.toggleFullscreen();
        } catch (Exception e) {
            showError("Plein Ecran", "Impossible d'activer le plein ecran : " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    // ── Dark theme CSS URL (loaded once) ─────────────────────────────────────
    private static String DARK_CSS_URL = null;
    private static String LIGHT_CSS_URL = null;
    private static {
        try {
            DARK_CSS_URL  = MainShellController.class.getResource("/dark-theme.css").toExternalForm();
            LIGHT_CSS_URL = MainShellController.class.getResource("/styles.css").toExternalForm();
        } catch (Exception ignored) {}
    }

    private void loadContentView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent newContent = loader.load();

            // Inject dark theme into the loaded page
            applyThemeToPage(newContent);

            // Try to pass role to the loaded controller
            Object controller = loader.getController();
            try {
                controller.getClass().getMethod("setUserRole", String.class)
                    .invoke(controller, Utils.UserContext.getRoleLabel());
            } catch (Exception ignored) {}

            // Animate: fade out old, slide+fade in new
            if (!contentArea.getChildren().isEmpty()) {
                Parent old = (Parent) contentArea.getChildren().get(0);
                FadeTransition fadeOut = new FadeTransition(Duration.millis(120), old);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    contentArea.getChildren().setAll(newContent);
                    newContent.setOpacity(0);
                    newContent.setTranslateY(14);
                    Timeline tl = new Timeline(
                        new KeyFrame(Duration.millis(220),
                            new KeyValue(newContent.opacityProperty(), 1.0),
                            new KeyValue(newContent.translateYProperty(), 0))
                    );
                    tl.play();
                });
                fadeOut.play();
            } else {
                contentArea.getChildren().setAll(newContent);
                newContent.setOpacity(0);
                newContent.setTranslateY(14);
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(250),
                        new KeyValue(newContent.opacityProperty(), 1.0),
                        new KeyValue(newContent.translateYProperty(), 0))
                );
                tl.play();
            }
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlFile + " — " + e.getMessage());
        }
    }

    /**
     * Applies the current theme to a freshly-loaded page:
     * - Adds dark-theme.css to the page's stylesheets
     * - Adds/removes "dark" CSS class on the root node
     * - Patches inline background styles directly on all nodes
     */
    private void applyThemeToPage(Parent page) {
        if (page == null) return;
        if (darkMode) {
            // Ensure dark-theme.css is in the page's stylesheet list
            if (DARK_CSS_URL != null
                    && !page.getStylesheets().contains(DARK_CSS_URL)) {
                page.getStylesheets().add(DARK_CSS_URL);
            }
            // Add "dark" CSS class so selectors like .dark .sidebar work
            if (!page.getStyleClass().contains("dark"))
                page.getStyleClass().add("dark");
            // Recursively patch inline white/light backgrounds
            patchNodeDark(page);
        } else {
            page.getStyleClass().remove("dark");
            page.getStylesheets().remove(DARK_CSS_URL);
        }
    }

    /** Walk every node and replace white/light inline background-color with dark equivalents */
    private void patchNodeDark(javafx.scene.Node node) {
        if (node == null) return;
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            // Replace white/near-white inline backgrounds
            style = style.replaceAll("(?i)-fx-background-color:\\s*white\\b",           "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#fff\\b",            "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#ffffff\\b",         "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#FFFFFF\\b",         "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#f8f9fa\\b",         "-fx-background-color:#151F2E");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#F5F6F8\\b",         "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#EBF0F8\\b",         "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#F0F4FA\\b",         "-fx-background-color:#0F1923");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#E4EBF5\\b",         "-fx-background-color:#151F2E");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#F7FAFF\\b",         "-fx-background-color:#1A2535");
            style = style.replaceAll("(?i)-fx-background-color:\\s*#DCEEFB\\b",         "-fx-background-color:#1B3C60");
            // Replace light text colors with dark-mode text
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#2c3e50\\b",                "-fx-text-fill:#E8EEF5");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#495057\\b",                "-fx-text-fill:#C8D8E8");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#8FA3B8\\b",                "-fx-text-fill:#5A7090");
            style = style.replaceAll("(?i)-fx-text-fill:\\s*#A0B0C0\\b",                "-fx-text-fill:#3D5270");
            // Light borders → dark
            style = style.replaceAll("(?i)-fx-border-color:\\s*#E8EEF8\\b",             "-fx-border-color:#243044");
            style = style.replaceAll("(?i)-fx-border-color:\\s*#E4EBF5\\b",             "-fx-border-color:#243044");
            style = style.replaceAll("(?i)-fx-border-color:\\s*#D4DCE8\\b",             "-fx-border-color:#1F2D40");
            style = style.replaceAll("(?i)-fx-border-color:\\s*#dee2e6\\b",             "-fx-border-color:#243044");
            node.setStyle(style);
        }
        // Recurse into children
        if (node instanceof javafx.scene.Parent p) {
            for (javafx.scene.Node child : p.getChildrenUnmodifiable()) {
                patchNodeDark(child);
            }
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

