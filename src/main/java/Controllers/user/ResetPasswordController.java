package Controllers.user;

import Services.user.PasswordResetService;
import Utils.InputValidator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ResetPasswordController {

    @FXML private TextField     txtEmail;
    @FXML private TextField     txtCode;
    @FXML private PasswordField txtNewPassword;
    @FXML private TextField     txtNewPasswordVisible;
    @FXML private Button        btnToggleNew;
    @FXML private PasswordField txtConfirm;
    @FXML private TextField     txtConfirmVisible;
    @FXML private Button        btnToggleConfirm;
    @FXML private Label         lblStatus;

    private boolean newVisible     = false;
    private boolean confirmVisible = false;

    private final PasswordResetService service = new PasswordResetService();

    public void setEmail(String email) {
        txtEmail.setText(email);
        txtEmail.setEditable(false);
    }

    // ── Toggle helpers ────────────────────────────────────────────────────────

    @FXML
    private void handleToggleNew() {
        newVisible = !newVisible;
        if (newVisible) {
            txtNewPasswordVisible.setText(txtNewPassword.getText());
            txtNewPasswordVisible.setVisible(true);  txtNewPasswordVisible.setManaged(true);
            txtNewPassword.setVisible(false);         txtNewPassword.setManaged(false);
            btnToggleNew.setText("🙈");
        } else {
            txtNewPassword.setText(txtNewPasswordVisible.getText());
            txtNewPassword.setVisible(true);          txtNewPassword.setManaged(true);
            txtNewPasswordVisible.setVisible(false);  txtNewPasswordVisible.setManaged(false);
            btnToggleNew.setText("👁");
        }
    }

    @FXML
    private void handleToggleConfirm() {
        confirmVisible = !confirmVisible;
        if (confirmVisible) {
            txtConfirmVisible.setText(txtConfirm.getText());
            txtConfirmVisible.setVisible(true);  txtConfirmVisible.setManaged(true);
            txtConfirm.setVisible(false);         txtConfirm.setManaged(false);
            btnToggleConfirm.setText("🙈");
        } else {
            txtConfirm.setText(txtConfirmVisible.getText());
            txtConfirm.setVisible(true);          txtConfirm.setManaged(true);
            txtConfirmVisible.setVisible(false);  txtConfirmVisible.setManaged(false);
            btnToggleConfirm.setText("👁");
        }
    }

    private String getNewPassword()     { return newVisible     ? txtNewPasswordVisible.getText() : txtNewPassword.getText(); }
    private String getConfirmPassword() { return confirmVisible ? txtConfirmVisible.getText()     : txtConfirm.getText(); }

    // ── Reset ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleReset() {
        String email   = txtEmail.getText().trim();
        String code    = txtCode.getText().trim();
        String pass    = getNewPassword();
        String confirm = getConfirmPassword();

        if (code.isEmpty())  { lblStatus.setText("Enter the code."); return; }

        String err = InputValidator.validateStrongPassword(pass);
        if (err != null)     { lblStatus.setText(err); return; }

        if (!pass.equals(confirm)) { lblStatus.setText("Passwords do not match."); return; }

        try {
            boolean ok = service.resetPassword(email, code, pass);
            if (!ok) {
                lblStatus.setText("Invalid or expired code. Request a new one.");
                return;
            }
            lblStatus.setStyle("-fx-text-fill:#16a34a; -fx-font-size:12px;");
            lblStatus.setText("Password changed ✅  Redirecting to login…");

            // Short delay then go to login
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/Login.fxml"));
                    Stage stage = (Stage) txtEmail.getScene().getWindow();
                    stage.setScene(new Scene(loader.load()));
                } catch (Exception ex) { ex.printStackTrace(); }
            });
            pause.play();

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/Login.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}