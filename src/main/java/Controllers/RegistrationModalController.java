package Controllers;

import entities.Candidate;
import entities.EventRegistration;
import entities.RecruitmentEvent;
import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import services.EventRegistrationService;
import services.UserService;

import java.sql.SQLException;

public class RegistrationModalController {

    @FXML
    private TextField eventTitleField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField emailField;

    private RecruitmentEvent event;
    private Candidate candidate;
    private CandidateDashboardController parentController;
    private EventRegistrationService registrationService = new EventRegistrationService();
    private UserService userService = new UserService();

    public void setEvent(RecruitmentEvent event) {
        this.event = event;
        eventTitleField.setText(event.getTitle());
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
        try {
            User user = userService.getById(candidate.getId());
            if (user != null) {
                lastNameField.setText(user.getLastName());
                firstNameField.setText(user.getFirstName());
                emailField.setText(user.getEmail());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setParentController(CandidateDashboardController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleConfirm() {
        try {
            EventRegistration reg = new EventRegistration();
            reg.setEventId(event.getId());
            reg.setCandidateId(candidate.getId());
            registrationService.apply(reg);
            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Votre inscription à l'événement \"" + event.getTitle() + "\" a été confirmée !");

            if (parentController != null) {
                parentController.onRegistrationSuccess();
            }
            close();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) eventTitleField.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
