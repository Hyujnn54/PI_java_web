package Application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée de l'application JavaFX
 * Application de gestion de recrutement avec authentification multi-rôles
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Charger directement le Dashboard Recruteur (Bypass Login)
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/recruiter_dashboard.fxml"));
            Scene scene = new Scene(root, 1000, 700);

            primaryStage.setTitle("Système de Recrutement - Accueil");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMaximized(true);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'application:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
