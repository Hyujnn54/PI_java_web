package utils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

public class NotificationService {

    public static void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                    .title(title)
                    .text(message)
                    .hideAfter(Duration.seconds(5))
                    .position(Pos.TOP_RIGHT)
                    .showInformation();
        });
    }

    public static void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                    .title(title)
                    .text(message)
                    .hideAfter(Duration.seconds(5))
                    .position(Pos.TOP_RIGHT)
                    .showWarning();
        });
    }

    public static void showError(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                    .title(title)
                    .text(message)
                    .hideAfter(Duration.seconds(5))
                    .position(Pos.TOP_RIGHT)
                    .showError();
        });
    }

    public static void showSuccess(String title, String message) {
        Platform.runLater(() -> {
            Notifications.create()
                    .title(title)
                    .text(message)
                    .hideAfter(Duration.seconds(5))
                    .position(Pos.TOP_RIGHT)
                    .showInformation(); // Using Information for success as it's most reliable
        });
    }
}
