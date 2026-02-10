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

    public static Stage getStage() {
        return stage;
    }
}

