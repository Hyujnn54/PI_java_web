package org.example;

import Utils.SceneManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

        try {
            System.out.println("Loading Login.fxml...");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = fxmlLoader.load();
            System.out.println("Login page loaded successfully");

            Scene scene = new Scene(root, 550, 650);

            // Add global CSS styling with error handling
            try {
                String cssPath = getClass().getResource("/styles.css").toExternalForm();
                scene.getStylesheets().add(cssPath);
                System.out.println("CSS loaded successfully: " + cssPath);
            } catch (Exception e) {
                System.err.println("Warning: Could not load styles.css: " + e.getMessage());
            }

            // Initialize centralized navigation
            SceneManager.init(stage, scene);

            stage.setTitle("Talent Bridge - Login");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.show();

            System.out.println("Application started successfully");

        } catch (Exception e) {
            System.err.println("Error loading application: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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
