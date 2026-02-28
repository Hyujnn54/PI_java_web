package Controllers.events;

import Models.events.User;
import Services.events.UserService;
import Utils.SceneManager;
import Utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * Controller simple pour la page de login
 */
public class LoginController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label errorLabel;

    private UserService userService;

    public LoginController() {
        userService = new UserService();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs");
            return;
        }
        try {
            User user = userService.login(email, password);
            if (user != null) {
                SessionManager.setCurrentUser(user);
                Stage stage = (Stage) loginButton.getScene().getWindow();
                String role = user.getRole().name();
                if (role.equals("ADMIN")) {
                    SceneManager.switchScene(stage, "/GUI/admin_dashboard.fxml", "Admin Dashboard");
                } else if (role.equals("RECRUITER")) {
                    SceneManager.switchScene(stage, "/GUI/recruiter_dashboard.fxml", "Recruiter Dashboard");
                } else if (role.equals("CANDIDATE")) {
                    SceneManager.switchScene(stage, "/GUI/candidate_dashboard.fxml", "Candidate Dashboard");
                }
            } else {
                errorLabel.setText("Email ou mot de passe incorrect");
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            errorLabel.setText("Erreur de base de données : " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToSignup() {
        Stage stage = (Stage) loginButton.getScene().getWindow();
        SceneManager.switchScene(stage, "/GUI/signup.fxml", "Inscription");
    }
}
