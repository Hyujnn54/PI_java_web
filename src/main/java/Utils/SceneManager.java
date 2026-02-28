package Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Centralized navigation so the whole team uses the same shell and transitions.
 */
public final class SceneManager {

    private static Stage stage;
    private static Scene scene;

    private SceneManager() {}

    public static void init(Stage primaryStage, Scene primaryScene) {
        stage = primaryStage;
        scene = primaryScene;
    }

    public static void setRoot(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            scene.setRoot(root);
        } catch (Exception e) {
            System.err.println("Failed to load scene: " + fxmlPath + " -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Load a new scene with specified dimensions
     */
    public static void loadScene(String fxmlPath, String title, double width, double height) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene newScene = new Scene(root, width, height);

        // Copy stylesheets from current scene if any
        if (scene != null && !scene.getStylesheets().isEmpty()) {
            newScene.getStylesheets().addAll(scene.getStylesheets());
        }

        stage.setScene(newScene);
        stage.setTitle(title);
        stage.centerOnScreen();

        // Update the scene reference
        scene = newScene;
    }

    public static Stage getStage() {
        return stage;
    }
}
