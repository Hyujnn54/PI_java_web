package Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe simple pour gérer la navigation entre les vues
 */
public class SceneManager {

    /**
     * Charge un fichier FXML et change la scène
     * @param stage Le stage JavaFX
     * @param fxmlPath Le chemin vers le fichier FXML (ex: "/GUI/login.fxml")
     */
    public static void switchScene(Stage stage, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(SceneManager.class.getResource(fxmlPath));
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la scène: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Charge un fichier FXML avec un titre spécifique
     */
    public static void switchScene(Stage stage, String fxmlPath, String title) {
        switchScene(stage, fxmlPath);
        stage.setTitle(title);
    }
}
