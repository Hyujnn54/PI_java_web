package Application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée pour le TEST DIRECT du Dashboard Candidat
 */
public class MainAppCandidate extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger directement le Dashboard Candidat
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/candidate_dashboard.fxml"));
            Scene scene = new Scene(root, 1000, 700);

            primaryStage.setTitle("Talent Bridge - Dashboard Candidat (Test)");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Erreur chargement Dashboard Candidat:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
