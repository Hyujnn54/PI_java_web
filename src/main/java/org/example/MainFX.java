package org.example;

import Utils.UserContext;
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
            // Initialize default user context (Recruiter with ID 1)
            UserContext.login(1L, "Recruteur Démo", "recruteur@talentbridge.com", UserContext.Role.RECRUITER);
            System.out.println("Contexte utilisateur initialisé : " + UserContext.getRoleLabel() + " (ID: " + UserContext.getUserId() + ")");

            System.out.println("Chargement de MainShell.fxml...");
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/MainShell.fxml"));
            Parent root = fxmlLoader.load();
            System.out.println("MainShell chargé avec succès");

            Scene scene = new Scene(root, WINDOWED_WIDTH, WINDOWED_HEIGHT);

            // Add global CSS styling with error handling
            try {
                String cssPath = getClass().getResource("/styles.css").toExternalForm();
                scene.getStylesheets().add(cssPath);
                System.out.println("CSS chargé avec succès : " + cssPath);
            } catch (Exception e) {
                System.err.println("Avertissement : Impossible de charger styles.css : " + e.getMessage());
            }

            // Initialize centralized navigation
            Utils.SceneManager.init(stage, scene);

            stage.setTitle("Talent Bridge - Tableau de bord");
            stage.setScene(scene);
            stage.setResizable(true);
            stage.centerOnScreen();
            stage.show();

            System.out.println("Application démarrée avec succès");

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'application : " + e.getMessage());
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
