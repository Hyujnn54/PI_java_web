package Controllers.events;

import Models.events.Candidate;
import Models.events.Recruiter;
import Models.events.RoleEnum;
import Models.events.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import Services.events.CandidateService;
import Services.events.RecruiterService;
import Services.events.UserService;
import Utils.SceneManager;

import java.sql.SQLException;

public class SignupController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField phoneField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private Label errorLabel;
    @FXML
    private Button signupButton;

    private UserService userService;
    private CandidateService candidateService;
    private RecruiterService recruiterService;

    public SignupController() {
        userService = new UserService();
        candidateService = new CandidateService();
        recruiterService = new RecruiterService();
    }

    @FXML
    private void handleSignup() {
        String firstName = firstNameField.getText();
        String lastName = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String password = passwordField.getText();
        String roleStr = roleComboBox.getValue();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || roleStr == null) {
            errorLabel.setText("Veuillez remplir les champs obligatoires");
            return;
        }

        try {
            // 1. Créer l'utilisateur de base
            User user = new User();
            user.setEmail(email);
            user.setPassword(password);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhone(phone);
            user.setRole(RoleEnum.valueOf(roleStr));
            user.setActive(true);

            userService.add(user); // Cette méthode hydrate l'ID généré

            // 2. Créer le profil spécifique (Shared PK)
            if (user.getRole() == RoleEnum.CANDIDATE) {
                Candidate candidate = new Candidate(user.getId(), "", "", 0, "");
                candidateService.add(candidate);
            } else if (user.getRole() == RoleEnum.RECRUITER) {
                Recruiter recruiter = new Recruiter(user.getId(), "", "", "");
                recruiterService.add(recruiter);
            }

            // 3. Succès -> Aller au login
            showAlert("Succès", "Compte créé avec succès ! Connectez-vous.");
            goToLogin();

        } catch (SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Erreur BDD : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Erreur inattendue : " + e.getMessage());
        }
    }

    @FXML
    private void goToLogin() {
        Stage stage = (Stage) signupButton.getScene().getWindow();
        SceneManager.switchScene(stage, "/GUI/login.fxml", "Connexion");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
