package Controllers;

import Models.Role;
import Models.User;
import Services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class SignUpController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<Role> cbRole;
    @FXML private CheckBox cbTerms;

    private final UserService userService = new UserService();

    @FXML
    public void initialize() {
        cbRole.setItems(FXCollections.observableArrayList(Role.values()));
    }

    @FXML
    private void handleSignUp() {

        if (!cbTerms.isSelected()) {
            showAlert("Error", "You must accept terms.");
            return;
        }

        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            showAlert("Error", "Passwords do not match.");
            return;
        }

        try {

            String fullName = txtFullName.getText();
            String[] parts = fullName.split(" ");

            String firstName = parts[0];
            String lastName = parts.length > 1 ? parts[1] : "";

            User user = new User(
                    txtEmail.getText(),
                    txtPassword.getText(),
                    cbRole.getValue(),
                    firstName,
                    lastName,
                    txtPhone.getText()
            );

            userService.addUser(user);

            showAlert("Success", "Account created successfully ✅");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Database error.");
        }
    }

    @FXML
    private void handleShowLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
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
