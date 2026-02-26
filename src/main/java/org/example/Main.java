package org.example;

import Services.InterviewReminderScheduler;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX Application Entry Point
 */
public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Load the main shell
        Parent root = FXMLLoader.load(getClass().getResource("/MainShell.fxml"));

        Scene scene = new Scene(root, 1200, 800);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Talent Bridge - Recruitment Management System");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);

        // ---------------------------------------------------------------
        // Start the background reminder scheduler (email + SMS)
        // Checks every 5 minutes for interviews happening in ~24 hours
        // ---------------------------------------------------------------
        InterviewReminderScheduler.start();

        // Stop scheduler cleanly when the window is closed
        stage.setOnCloseRequest(e -> InterviewReminderScheduler.stop());

        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}


