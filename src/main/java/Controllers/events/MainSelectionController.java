package Controllers.events;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import Utils.SceneManager;
import Utils.SessionManager;
import Models.events.EventUser;
import Models.events.RoleEnum;

public class MainSelectionController {

    @FXML
    private void goToFrontOffice() {
        // Simuler connexion Candidat
        EventUser candidate = new EventUser();
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
        EventUser recruiter = new EventUser();
        recruiter.setId(4); // ID supposé d'un recruteur existant ou mock
        recruiter.setEmail("recruiter@test.com");
        recruiter.setFirstName("Jane");
        recruiter.setLastName("Smith");
        recruiter.setRole(RoleEnum.RECRUITER);

        SessionManager.setCurrentUser(recruiter);

        // Navigation
        SceneManager.switchScene((Stage) Stage.getWindows().get(0), "/GUI/recruiter_dashboard.fxml", "Espace Recruteur");
    }
}
