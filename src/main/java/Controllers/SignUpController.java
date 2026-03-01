package Controllers;

import Models.Candidate;
import Models.Recruiter;
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

    // ✅ CHANGED: no Role enum
    @FXML private ComboBox<String> cbRole;

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
        // ✅ only these two types in signup
        cbRole.setItems(FXCollections.observableArrayList("CANDIDATE", "RECRUITER"));

        cbRole.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isRecruiter = "RECRUITER".equals(newV);
            boolean isCandidate = "CANDIDATE".equals(newV);

            recruiterBlock.setVisible(isRecruiter);
            recruiterBlock.setManaged(isRecruiter);

            candidateBlock.setVisible(isCandidate);
            candidateBlock.setManaged(isCandidate);
        });

        // hide blocks initially
        recruiterBlock.setVisible(false);
        recruiterBlock.setManaged(false);
        candidateBlock.setVisible(false);
        candidateBlock.setManaged(false);
    }

    @FXML
    private void handleSignUp() {

        // Terms
        if (!cbTerms.isSelected()) {
            showAlert(Alert.AlertType.ERROR, "Error", "You must accept Terms and Conditions.");
            return;
        }

        // Type
        String type = cbRole.getValue();
        if (type == null) {
            showAlert(Alert.AlertType.ERROR, "Error", "Please select account type.");
            return;
        }

        // Full name
        String fullName = txtFullName.getText() == null ? "" : txtFullName.getText().trim();
        if (fullName.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Error", "Full name is required.");
            return;
        }

        String[] parts = fullName.split("\\s+");
        String firstName = parts[0];
        String lastName = parts.length > 1
                ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length))
                : "";

        // Validate names
        String err = utils.InputValidator.validateName(firstName, "First name");
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        if (!lastName.isBlank()) {
            err = utils.InputValidator.validateName(lastName, "Last name");
            if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }
        }

        // Email
        String email = txtEmail.getText() == null ? "" : txtEmail.getText().trim();
        err = utils.InputValidator.validateEmail(email);
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        // Email unique (DB)
        try {
            if (userService.emailExists(email, null)) {
                showAlert(Alert.AlertType.ERROR, "Error", "Email already exists.");
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Database error while checking email.");
            return;
        }

        // Phone (8 digits)
        String phone = txtPhone.getText() == null ? "" : txtPhone.getText().trim();
        err = utils.InputValidator.validatePhone8(phone);
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        // Password
        String pass = txtPassword.getText();
        String confirm = txtConfirmPassword.getText();

        err = utils.InputValidator.validateStrongPassword(pass);
        if (err != null) { showAlert(Alert.AlertType.ERROR, "Error", err); return; }

        if (!pass.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Error", "Passwords do not match.");
            return;
        }

        // Insert user (NO role column in DB)
        try {
            if ("RECRUITER".equals(type)) {
                String companyName = txtCompanyName.getText() == null ? "" : txtCompanyName.getText().trim();
                if (companyName.isEmpty()) {
                    showAlert(Alert.AlertType.ERROR, "Error", "Company name is required for recruiter.");
                    return;
                }

                Recruiter r = new Recruiter(
                        email, pass, firstName, lastName, phone,
                        companyName,
                        txtCompanyLocation.getText() == null ? "" : txtCompanyLocation.getText().trim()
                );

                recruiterService.addRecruiter(r);

            } else { // CANDIDATE
                Integer years = null;
                String yearsText = txtExperienceYears.getText() == null ? "" : txtExperienceYears.getText().trim();
                if (!yearsText.isEmpty()) {
                    try { years = Integer.parseInt(yearsText); }
                    catch (NumberFormatException nfe) {
                        showAlert(Alert.AlertType.ERROR, "Error", "Experience years must be a number.");
                        return;
                    }
                }

                Candidate c = new Candidate(
                        email, pass, firstName, lastName, phone,
                        txtCandidateLocation.getText() == null ? "" : txtCandidateLocation.getText().trim(),
                        txtEducationLevel.getText() == null ? "" : txtEducationLevel.getText().trim(),
                        years,
                        txtCvPath.getText() == null ? "" : txtCvPath.getText().trim()
                );

                candidateService.addCandidate(c);
            }

            showAlert(Alert.AlertType.INFORMATION, "Success", "Account created successfully ✅");
            clearForm();

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Error", "Database error: " + ex.getMessage());
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

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearForm() {
        txtFullName.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtPassword.clear();
        txtConfirmPassword.clear();
        cbRole.setValue(null);
        cbTerms.setSelected(false);

        txtCompanyName.clear();
        txtCompanyLocation.clear();
        txtCandidateLocation.clear();
        txtEducationLevel.clear();
        txtExperienceYears.clear();
        txtCvPath.clear();

        recruiterBlock.setVisible(false);
        recruiterBlock.setManaged(false);
        candidateBlock.setVisible(false);
        candidateBlock.setManaged(false);
    }
}