package Controllers;

import Models.Candidate;
import Models.Recruiter;
import Models.Role;
import Models.User;
import Services.CandidateService;
import Services.RecruiterService;
import Services.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SignUpController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<Role> cbRole;
    @FXML private CheckBox cbTerms;

    // blocks
    @FXML private VBox recruiterBlock;
    @FXML private VBox candidateBlock;

    // recruiter fields
    @FXML private TextField txtCompanyName;
    @FXML private TextField txtCompanyLocation;

    // candidate fields
    @FXML private TextField txtCandidateLocation;
    @FXML private TextField txtEducationLevel;
    @FXML private TextField txtExperienceYears;
    @FXML private TextField txtCvPath;

    private final UserService userService = new UserService();
    private final RecruiterService recruiterService = new RecruiterService();
    private final CandidateService candidateService = new CandidateService();

    @FXML
    public void initialize() {
        // ✅ Admin not allowed in signup
        cbRole.setItems(FXCollections.observableArrayList(Role.CANDIDATE, Role.RECRUITER));

        cbRole.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isRecruiter = newV == Role.RECRUITER;
            boolean isCandidate = newV == Role.CANDIDATE;

            recruiterBlock.setVisible(isRecruiter);
            recruiterBlock.setManaged(isRecruiter);

            candidateBlock.setVisible(isCandidate);
            candidateBlock.setManaged(isCandidate);
        });
    }

    @FXML
    private void handleSignUp() {
        if (!cbTerms.isSelected()) {
            showAlert("Error", "You must accept terms.");
            return;
        }

        if (cbRole.getValue() == null) {
            showAlert("Error", "Please select a role.");
            return;
        }

        if (!txtPassword.getText().equals(txtConfirmPassword.getText())) {
            showAlert("Error", "Passwords do not match.");
            return;
        }

        try {
            String fullName = txtFullName.getText().trim();
            if (fullName.isEmpty()) {
                showAlert("Error", "Full name is required.");
                return;
            }

            String[] parts = fullName.split("\\s+");
            String firstName = parts[0];
            String lastName = parts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)) : "";

            Role role = cbRole.getValue();

            if (role == Role.RECRUITER) {
                if (txtCompanyName.getText().trim().isEmpty()) {
                    showAlert("Error", "Company name is required for recruiter.");
                    return;
                }

                Recruiter r = new Recruiter(
                        txtEmail.getText().trim(),
                        txtPassword.getText(),
                        firstName,
                        lastName,
                        txtPhone.getText().trim(),
                        txtCompanyName.getText().trim(),
                        txtCompanyLocation.getText().trim()
                );

                recruiterService.addRecruiter(r); // ✅ users + recruiter

            } else if (role == Role.CANDIDATE) {
                Integer years = null;
                String yearsText = txtExperienceYears.getText().trim();
                if (!yearsText.isEmpty()) {
                    try { years = Integer.parseInt(yearsText); }
                    catch (NumberFormatException nfe) {
                        showAlert("Error", "Experience years must be a number.");
                        return;
                    }
                }

                Candidate c = new Candidate(
                        txtEmail.getText().trim(),
                        txtPassword.getText(),
                        firstName,
                        lastName,
                        txtPhone.getText().trim(),
                        txtCandidateLocation.getText().trim(),
                        txtEducationLevel.getText().trim(),
                        years,
                        txtCvPath.getText().trim()
                );

                candidateService.addCandidate(c); // ✅ users + candidate
            } else {
                // should never happen because admin not in combobox
                User u = new User(
                        txtEmail.getText().trim(),
                        txtPassword.getText(),
                        role,
                        firstName,
                        lastName,
                        txtPhone.getText().trim()
                );
                userService.addUser(u);
            }

            showAlert("Success", "Account created successfully ✅");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error: " + e.getMessage());
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
