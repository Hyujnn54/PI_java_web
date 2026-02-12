package Controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Sign Up Controller
 * Handles new user registration
 */
public class SignUpController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private CheckBox cbTerms;
    @FXML private Button btnSignUp;
    @FXML private Hyperlink linkLogin;

    @FXML
    public void initialize() {
        // Setup role combo box
        if (cbRole != null) {
            cbRole.getItems().addAll("Candidate", "Recruiter");
            cbRole.setValue("Candidate");
        }
    }

    @FXML
    private void handleSignUp() {
        // Validation
        if (!validateInput()) {
            return;
        }

        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String password = txtPassword.getText().trim();
        String role = cbRole.getValue();

        // TODO: Replace with actual user creation in database
        // For now, just show success and navigate to login

        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
        successAlert.setTitle("Success");
        successAlert.setHeaderText("Account Created!");
        successAlert.setContentText("Your account has been created successfully. Please login to continue.");
        successAlert.showAndWait();

        // Navigate to login
        handleShowLogin();
    }

    private boolean validateInput() {
        if (txtFullName.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter your full name.", Alert.AlertType.WARNING);
            return false;
        }

        String email = txtEmail.getText().trim();
        if (email.isEmpty()) {
            showAlert("Validation Error", "Please enter your email.", Alert.AlertType.WARNING);
            return false;
        }

        if (!email.contains("@") || !email.contains(".")) {
            showAlert("Validation Error", "Please enter a valid email address.", Alert.AlertType.WARNING);
            return false;
        }

        String password = txtPassword.getText().trim();
        if (password.isEmpty()) {
            showAlert("Validation Error", "Please enter a password.", Alert.AlertType.WARNING);
            return false;
        }

        if (password.length() < 6) {
            showAlert("Validation Error", "Password must be at least 6 characters long.", Alert.AlertType.WARNING);
            return false;
        }

        String confirmPassword = txtConfirmPassword.getText().trim();
        if (!password.equals(confirmPassword)) {
            showAlert("Validation Error", "Passwords do not match.", Alert.AlertType.WARNING);
            return false;
        }

        if (cbRole.getValue() == null) {
            showAlert("Validation Error", "Please select your role.", Alert.AlertType.WARNING);
            return false;
        }

        if (!cbTerms.isSelected()) {
            showAlert("Validation Error", "Please agree to the Terms and Conditions.", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    @FXML
    private void handleShowLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) linkLogin.getScene().getWindow();
            stage.setScene(new Scene(root, 550, 650));
            stage.setTitle("Talent Bridge - Login");
            stage.centerOnScreen();

        } catch (IOException e) {
            showAlert("Error", "Failed to load login page: " + e.getMessage(), Alert.AlertType.ERROR);
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

