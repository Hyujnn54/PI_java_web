package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.example.MainFX;

import java.io.IOException;

/**
 * Main menu controller for Talent Bridge application
 * Handles navigation between different modules
 */
public class MainMenuController {

    @FXML
    private Button btnInterviews;

    @FXML
    private Button btnJobOffers;

    @FXML
    private Button btnFeedback;

    @FXML
    private Button btnReschedule;

    @FXML
    private Button btnEvents;

    @FXML
    private Button btnSettings;

    @FXML
    private Button btnFullscreen;

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox welcomeScreen;

    @FXML
    private void handleInterviewsNav() {
        loadContentView("/InterviewManagement.fxml");
    }

    @FXML
    private void handleJobOffersNav() {
        loadContentView("/JobOffers.fxml");
    }

    @FXML
    private void handleFeedbackNav() {
        loadContentView("/FeedbackManagement.fxml");
    }

    @FXML
    private void handleRescheduleNav() {
        loadContentView("/RescheduleManagement.fxml");
    }

    @FXML
    private void handleEventsNav() {
        loadContentView("/Events.fxml");
    }

    @FXML
    private void handleSettingsNav() {
        loadContentView("/Settings.fxml");
    }

    @FXML
    private void handleFullscreenToggle() {
        MainFX.toggleFullscreen();
        updateFullscreenButtonText();
    }

    private void updateFullscreenButtonText() {
        if (btnFullscreen != null) {
            if (MainFX.isFullscreenMode()) {
                btnFullscreen.setText("⊡  Windowed");
            } else {
                btnFullscreen.setText("⛶  Fullscreen");
            }
        }
    }

    private void loadContentView(String fxmlFile) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            VBox content = loader.load();

            // Clear existing content and add the new view
            contentArea.getChildren().clear();
            contentArea.getChildren().add(content);
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlFile);
            e.printStackTrace();
        }
    }
}

