package Controllers;

import Services.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import Services.LoggedUser;


public class LoginController {

    @FXML private TextField txtEmail;
    @FXML private PasswordField txtPassword;
    @FXML private CheckBox cbRememberMe;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin(ActionEvent event) {
        String email = txtEmail.getText();
        String password = txtPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please fill all fields.");
            return;
        }

        try {
            LoggedUser logged = authService.login(email, password);

            if (logged != null) {
                // start session
                utils.Session.start(logged.id, logged.role, logged.email);

                // go to main shell (or dashboard)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainShell.fxml"));
                Stage stage = (Stage) txtEmail.getScene().getWindow();
                stage.setScene(new Scene(loader.load()));
            } else {
                showAlert("Error", "Invalid email or password.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Login failed", "Error: " + e.getMessage());
        }
    }



    @FXML
    private void handleShowSignUp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SignUp.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
