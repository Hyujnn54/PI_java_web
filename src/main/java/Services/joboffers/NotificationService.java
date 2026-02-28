package Services.joboffers;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Service de notifications desktop - affiche des notifications toast
 * 100% local, sans API externe
 */
public class NotificationService {

    private static NotificationService instance;
    private static final Queue<NotificationData> notificationQueue = new LinkedList<>();
    private static boolean isShowing = false;
    private static final int MAX_VISIBLE = 3;
    private static int currentVisible = 0;

    public enum NotificationType {
        SUCCESS, WARNING, ERROR, INFO
    }

    private static class NotificationData {
        String title;
        String message;
        NotificationType type;
        int durationSeconds;

        NotificationData(String title, String message, NotificationType type, int durationSeconds) {
            this.title = title;
            this.message = message;
            this.type = type;
            this.durationSeconds = durationSeconds;
        }
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    /**
     * Affiche une notification de succès
     */
    public static void showSuccess(String title, String message) {
        show(title, message, NotificationType.SUCCESS, 4);
    }

    /**
     * Affiche une notification d'information
     */
    public static void showInfo(String title, String message) {
        show(title, message, NotificationType.INFO, 5);
    }

    /**
     * Affiche une notification d'avertissement
     */
    public static void showWarning(String title, String message) {
        show(title, message, NotificationType.WARNING, 6);
    }

    /**
     * Affiche une notification d'erreur
     */
    public static void showError(String title, String message) {
        show(title, message, NotificationType.ERROR, 8);
    }

    /**
     * Affiche une notification personnalisée
     */
    public static void show(String title, String message, NotificationType type, int durationSeconds) {
        try {
            Platform.runLater(() -> {
                try {
                    notificationQueue.add(new NotificationData(title, message, type, durationSeconds));
                    processQueue();
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'affichage de la notification: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur Platform.runLater: " + e.getMessage());
        }
    }

    private static void processQueue() {
        if (notificationQueue.isEmpty() || currentVisible >= MAX_VISIBLE) {
            return;
        }

        NotificationData data = notificationQueue.poll();
        if (data != null) {
            currentVisible++;
            try {
                showNotificationWindow(data);
            } catch (Exception e) {
                System.err.println("Erreur showNotificationWindow: " + e.getMessage());
                currentVisible--;
            }
        }
    }

    private static void showNotificationWindow(NotificationData data) {
        Stage notificationStage = new Stage();
        notificationStage.initStyle(StageStyle.TRANSPARENT);
        notificationStage.setAlwaysOnTop(true);

        // Conteneur principal
        VBox container = new VBox(8);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(getContainerStyle(data.type));
        container.setPrefWidth(350);
        container.setMinHeight(80);

        // Icône et titre
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(getIcon(data.type));
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label titleLabel = new Label(data.title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: " + getTitleColor(data.type) + ";");

        // Bouton fermer
        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-font-size: 14px; -fx-text-fill: #999; -fx-cursor: hand; -fx-padding: 0 0 0 10;");
        closeBtn.setOnMouseClicked(e -> {
            closeNotification(notificationStage);
        });
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-font-size: 14px; -fx-text-fill: #333; -fx-cursor: hand; -fx-padding: 0 0 0 10;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-font-size: 14px; -fx-text-fill: #999; -fx-cursor: hand; -fx-padding: 0 0 0 10;"));

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        headerBox.getChildren().addAll(iconLabel, titleLabel, spacer, closeBtn);

        // Message
        Label messageLabel = new Label(data.message);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #555; -fx-wrap-text: true;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);

        // Barre de progression du temps
        javafx.scene.layout.StackPane progressContainer = new javafx.scene.layout.StackPane();
        progressContainer.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 2;");
        progressContainer.setPrefHeight(3);

        javafx.scene.layout.Region progressBar = new javafx.scene.layout.Region();
        progressBar.setStyle("-fx-background-color: " + getProgressColor(data.type) + "; -fx-background-radius: 2;");
        progressBar.setPrefHeight(3);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        progressContainer.getChildren().add(progressBar);
        javafx.scene.layout.StackPane.setAlignment(progressBar, Pos.CENTER_LEFT);

        container.getChildren().addAll(headerBox, messageLabel, progressContainer);

        Scene scene = new Scene(container);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        notificationStage.setScene(scene);

        // Position en bas à droite
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        notificationStage.setX(screenBounds.getMaxX() - 370);
        notificationStage.setY(screenBounds.getMaxY() - 100 - (currentVisible - 1) * 100);

        // Animation d'entrée
        container.setOpacity(0);
        notificationStage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), container);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();

        // Animation de la barre de progression
        javafx.animation.Timeline progressAnimation = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(Duration.ZERO,
                new javafx.animation.KeyValue(progressBar.prefWidthProperty(), 350)),
            new javafx.animation.KeyFrame(Duration.seconds(data.durationSeconds),
                new javafx.animation.KeyValue(progressBar.prefWidthProperty(), 0))
        );
        progressAnimation.play();

        // Fermeture automatique
        PauseTransition delay = new PauseTransition(Duration.seconds(data.durationSeconds));
        delay.setOnFinished(e -> closeNotification(notificationStage));
        delay.play();
    }

    private static void closeNotification(Stage stage) {
        VBox container = (VBox) stage.getScene().getRoot();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), container);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            stage.close();
            currentVisible--;
            processQueue();
        });
        fadeOut.play();
    }

    private static String getContainerStyle(NotificationType type) {
        String borderColor = switch (type) {
            case SUCCESS -> "#28a745";
            case WARNING -> "#ffc107";
            case ERROR -> "#dc3545";
            case INFO -> "#17a2b8";
        };

        return "-fx-background-color: white; -fx-background-radius: 10; " +
               "-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 4; -fx-border-radius: 10; " +
               "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 15, 0, 0, 5);";
    }

    private static String getIcon(NotificationType type) {
        return switch (type) {
            case SUCCESS -> "✅";
            case WARNING -> "⚠️";
            case ERROR -> "❌";
            case INFO -> "ℹ️";
        };
    }

    private static String getTitleColor(NotificationType type) {
        return switch (type) {
            case SUCCESS -> "#28a745";
            case WARNING -> "#856404";
            case ERROR -> "#dc3545";
            case INFO -> "#17a2b8";
        };
    }

    private static String getProgressColor(NotificationType type) {
        return switch (type) {
            case SUCCESS -> "#28a745";
            case WARNING -> "#ffc107";
            case ERROR -> "#dc3545";
            case INFO -> "#17a2b8";
        };
    }
}


