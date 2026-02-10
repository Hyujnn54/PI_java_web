package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Main application entry point for Interview Management System (Talent Bridge)
 * Color Scheme: White (#FFFFFF) and Blue (#0066CC)
 */
public class MainFX extends Application {

    private static Stage primaryStage;
    private static boolean isFullscreen = false;
    private static final double WINDOWED_WIDTH = 1400;
    private static final double WINDOWED_HEIGHT = 800;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MainMenu.fxml"));
        Parent root = fxmlLoader.load();
        Scene scene = new Scene(root, WINDOWED_WIDTH, WINDOWED_HEIGHT);

        // Add global CSS styling
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        stage.setTitle("Talent Bridge - Interview Management System");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Toggle between fullscreen and windowed mode
     */
    public static void toggleFullscreen() {
        if (primaryStage != null) {
            isFullscreen = !isFullscreen;

            if (isFullscreen) {
                primaryStage.setFullScreen(true);
                primaryStage.setResizable(true);
            } else {
                primaryStage.setFullScreen(false);
                primaryStage.setResizable(false);
                primaryStage.setWidth(WINDOWED_WIDTH);
                primaryStage.setHeight(WINDOWED_HEIGHT);

                // Center window on screen
                Screen screen = Screen.getPrimary();
                primaryStage.setX((screen.getVisualBounds().getWidth() - WINDOWED_WIDTH) / 2);
                primaryStage.setY((screen.getVisualBounds().getHeight() - WINDOWED_HEIGHT) / 2);
            }
        }
    }

    /**
     * Get the current fullscreen state
     */
    public static boolean isFullscreenMode() {
        return isFullscreen;
    }

    public static void main(String[] args) {
        launch(args);
    }
}





