package Controllers;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import utils.SceneManager;
import utils.SessionManager;
import entities.User;
import entities.RoleEnum;

public class MainSelectionController {

    @FXML
    private void goToFrontOffice() {
        // Simuler connexion Candidat
        User candidate = new User();
        candidate.setId(2); // ID supposé d'un candidat existant ou mock
        candidate.setEmail("candidate@test.com");
        candidate.setFirstName("John");
        candidate.setLastName("Doe");
        candidate.setRole(RoleEnum.CANDIDATE);

        SessionManager.setCurrentUser(candidate);

        // Navigation
        SceneManager.switchScene((Stage) Stage.getWindows().get(0), "/GUI/candidate_dashboard.fxml", "Espace Candidat");
    }

    @FXML
    private void goToBackOffice() {
        // Simuler connexion Recruteur
        User recruiter = new User();
        recruiter.setId(4); // ID supposé d'un recruteur existant ou mock
        recruiter.setEmail("recruiter@test.com");
        recruiter.setFirstName("Jane");
        recruiter.setLastName("Smith");
        recruiter.setRole(RoleEnum.RECRUITER);

        SessionManager.setCurrentUser(recruiter);

        // Navigation
        SceneManager.switchScene((Stage) Stage.getWindows().get(0), "/GUI/recruiter_dashboard.fxml",
                "Espace Recruteur");
    }
}
