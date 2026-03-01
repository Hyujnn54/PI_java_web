package Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SceneManager {

    private static Stage stage;
    private static Scene scene;

    private SceneManager() {}

    public static void init(Stage primaryStage, Scene primaryScene) {
        stage = primaryStage;
        scene = primaryScene;
    }

    public static void setRoot(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(org.example.Main.class.getResource(fxml));
            Parent root = loader.load();
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Switch the scene on the given stage to a new FXML root. */
    public static void switchScene(Stage targetStage, String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene s = targetStage.getScene();
            if (s != null) {
                s.setRoot(root);
            } else {
                targetStage.setScene(new Scene(root));
            }
            if (title != null) targetStage.setTitle(title);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Load a new scene from FXML and set it on the primary stage. */
    public static void loadScene(String fxmlPath, String title, double width, double height) throws Exception {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Parent root = loader.load();
        Scene s = new Scene(root, width, height);
        if (stage != null) {
            stage.setScene(s);
            if (title != null) stage.setTitle(title);
            scene = s;
        }
    }

    public static Stage getStage() { return stage; }
    public static Scene getScene() { return scene; }
}