package Controllers;

import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Login Controller
 * Handles user authentication and navigation to main shell
 */
public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox cbRememberMe;
    @FXML private Button btnLogin;
    @FXML private Hyperlink linkSignUp;

    @FXML
    public void initialize() {
        // Add enter key listener for quick login
        if (txtPassword != null) {
            txtPassword.setOnAction(e -> handleLogin());
        }
    }

    @FXML
    private void handleLogin() {
        String email = txtEmail.getText().trim();
        String password = txtPassword.getText().trim();

        // Basic validation
        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Validation Error", "Please enter both email and password.", Alert.AlertType.WARNING);
            return;
        }

        // TODO: Replace with actual authentication
        // For now, any credentials will log in
        try {
            // Mock authentication - Set user context based on email
            if (email.toLowerCase().contains("recruiter")) {
                UserContext.setCurrentRole(UserContext.Role.RECRUITER);
            } else {
                UserContext.setCurrentRole(UserContext.Role.CANDIDATE);
            }

            // Load Main Shell
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainShell.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnLogin.getScene().getWindow();
            stage.setScene(new Scene(root, 1400, 800));
            stage.setTitle("Talent Bridge - " + UserContext.getRoleLabel());
            stage.centerOnScreen();

            System.out.println("Login successful for: " + email);

        } catch (IOException e) {
            showAlert("Error", "Failed to load main application: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowSignUp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignUp.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) linkSignUp.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 750));
            stage.setTitle("Talent Bridge - Sign Up");
            stage.centerOnScreen();

        } catch (IOException e) {
            showAlert("Error", "Failed to load sign up page: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}



