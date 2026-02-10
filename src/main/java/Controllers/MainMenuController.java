package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class MainMenuController {

    @FXML
    private Button btnInterviews;

    @FXML
    private Button btnFeedback;

    @FXML
    private Button btnReschedule;

    @FXML
    private void handleInterviewsButton() {
        loadView("/InterviewManagement.fxml", "Interview Management");
    }

    @FXML
    private void handleFeedbackButton() {
        loadView("/FeedbackManagement.fxml", "Feedback Management");
    }

    @FXML
    private void handleRescheduleButton() {
        loadView("/RescheduleManagement.fxml", "Reschedule Management");
    }

    private void loadView(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Scene scene = new Scene(loader.load(), 900, 600);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

