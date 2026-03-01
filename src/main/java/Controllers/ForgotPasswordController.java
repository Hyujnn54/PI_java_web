package Controllers;

import Services.PasswordResetService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML private TextField txtEmail;
    @FXML private Label lblStatus;

    private final PasswordResetService service = new PasswordResetService();

    @FXML
    private void handleSendCode() {
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        if (email.isEmpty()) {
            lblStatus.setText("Enter your email.");
            return;
        }

        try {
            boolean ok = service.requestReset(email);
            if (!ok) {
                lblStatus.setText("Email not found.");
                return;
            }

            lblStatus.setText("Code sent ✅ Check your inbox.");

            // go to reset page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ResetPassword.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));

            // pass email to reset controller
            ResetPasswordController ctrl = loader.getController();
            ctrl.setEmail(email);

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}