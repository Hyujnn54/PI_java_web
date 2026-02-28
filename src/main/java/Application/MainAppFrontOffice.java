package Application;

import Models.User;
import Models.RoleEnum;
import Utils.SessionManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Point d'entrée FRONT OFFICE (Candidat)
 */
public class MainAppFrontOffice extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Simuler un utilisateur candidat connecté (john_doe)
            User mockUser = new User();
            mockUser.setId(2);  // ID de john_doe dans la base
            mockUser.setEmail("john.doe@example.com");
            mockUser.setRole(RoleEnum.CANDIDATE);
            mockUser.setFirstName("John");
            mockUser.setLastName("Doe");
            
            SessionManager.setCurrentUser(mockUser);
            
            // Charger directement le dashboard candidat
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/candidate_dashboard.fxml"));
            Scene scene = new Scene(root, 900, 650);
            
            primaryStage.setTitle("Front Office - Dashboard Candidat");
            primaryStage.setScene(scene);
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
