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
 * Point d'entrée BACK OFFICE (Recruiter)
 */
public class MainAppBackOffice extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            User mockUser = new User();
            mockUser.setId(4);
            mockUser.setEmail("hr@acme.com");
            mockUser.setRole(RoleEnum.RECRUITER);
            mockUser.setFirstName("ACME");
            mockUser.setLastName("Corporation");
            SessionManager.setCurrentUser(mockUser);
            Parent root = FXMLLoader.load(getClass().getResource("/GUI/recruiter_dashboard.fxml"));
            Scene scene = new Scene(root, 900, 650);
            primaryStage.setTitle("Back Office - Dashboard Recruteur");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'application:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) { launch(args); }
}
