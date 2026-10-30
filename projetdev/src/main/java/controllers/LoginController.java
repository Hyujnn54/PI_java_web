package controllers;

import entities.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import services.UserService;
import utils.SceneManager;
import utils.SessionManager;

/**
 * Controller simple pour la page de login
 */
public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Label errorLabel;

    private UserService userService;

    public LoginController() {
        userService = new UserService();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();

        // Validation simple
        if (email.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Veuillez remplir tous les champs");
            return;
        }

        // Tentative de connexion
        try {
            User user = userService.login(email, password);

            if (user != null) {
                // Connexion réussie
                SessionManager.setCurrentUser(user);

                // Redirection selon le rôle
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
                // Échec de connexion
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
