package Controllers.user;

import Models.user.Candidate;
import Models.user.Recruiter;
import Models.user.User;
import Services.user.ProfileService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import Utils.InputValidator;
import Utils.Session;
import Services.user.UserService;
import Services.user.LuxandFaceService;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;


public class ProfileController {

    @FXML private TextField tfFirstName;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPhone;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblStatus;

    // recruiter
    @FXML private VBox recruiterBlock;
    @FXML private TextField tfCompanyName;
    @FXML private TextField tfCompanyLocation;

    // candidate
    @FXML private VBox candidateBlock;
    @FXML private TextField tfCandidateLocation;
    @FXML private TextField tfEducationLevel;
    @FXML private TextField tfExperienceYears;
    @FXML private TextField tfCvPath;
    @FXML private Button btnEnableFace;
    @FXML private Button btnDisableFace;

    private final ProfileService service = new ProfileService();

    @FXML
    public void initialize() {
        loadMyProfile();
    }

    private void loadMyProfile() {
        try {
            Long userIdObj = Session.getUserId();
            if (userIdObj == null) {
                lblStatus.setText("Not logged in.");
                return;
            }
            long userId = userIdObj;

            User u = Session.getCurrentUser();
            boolean isRecruiter = u instanceof Recruiter;
            boolean isCandidate = u instanceof Candidate;

            ProfileService.UserProfile p = service.getUserProfile(userId);

            tfFirstName.setText(p.firstName());
            tfEmail.setText(p.email());
            tfPhone.setText(p.phone() == null ? "" : p.phone());

            recruiterBlock.setVisible(isRecruiter);
            recruiterBlock.setManaged(isRecruiter);

            candidateBlock.setVisible(isCandidate);
            candidateBlock.setManaged(isCandidate);

            String faceId = new UserService().getFacePersonId(userId);
            boolean enabled = (faceId != null && !faceId.isBlank());
            btnEnableFace.setVisible(!enabled);
            btnEnableFace.setManaged(!enabled);
            btnDisableFace.setVisible(enabled);
            btnDisableFace.setManaged(enabled);

            // Always clean up ghost Luxand persons — delete any UUID that doesn't match DB
            final String dbUuid = faceId;
            new Thread(() -> {
                try {
                    LuxandFaceService lux = new LuxandFaceService();
                    java.util.List<String> allUuids = lux.listPersonUuids();
                    System.out.println("[Cleanup] Luxand persons found: " + allUuids.size());
                    for (String ghostUuid : allUuids) {
                        if (dbUuid == null || !ghostUuid.equals(dbUuid)) {
                            try {
                                lux.deletePerson(ghostUuid);
                                System.out.println("[Cleanup] Deleted ghost: " + ghostUuid);
                            } catch (Exception ex) {
                                System.err.println("[Cleanup] Could not delete " + ghostUuid + ": " + ex.getMessage());
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("[Cleanup] Failed: " + ex.getMessage());
                }
            }).start();

            if (isRecruiter) {
                ProfileService.RecruiterInfo r = service.getRecruiterInfo(userId);
                tfCompanyName.setText(r.companyName());
                tfCompanyLocation.setText(r.companyLocation());
            }

            if (isCandidate) {
                ProfileService.CandidateInfo c = service.getCandidateInfo(userId);
                tfCandidateLocation.setText(c.location());
                tfEducationLevel.setText(c.educationLevel());
                tfExperienceYears.setText(c.experienceYears() == null ? "" : String.valueOf(c.experienceYears()));
                tfCvPath.setText(c.cvPath());
            }

            lblStatus.setText("Loaded ✅");

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        try {
            Long userIdObj = Session.getUserId();
            if (userIdObj == null) {
                showError("Not logged in.");
                return;
            }
            long userId = userIdObj;

            User u = Session.getCurrentUser();
            boolean isRecruiter = u instanceof Recruiter;
            boolean isCandidate = u instanceof Candidate;

            // ===== Validations =====
            String err;

            err = InputValidator.validateName(tfFirstName.getText().trim(), "Full name");
            if (err != null) { showError(err); return; }

            err = InputValidator.validatePhone8(tfPhone.getText().trim());
            if (err != null) { showError(err); return; }

            String newPass = pfPassword.getText();
            if (newPass != null && !newPass.isBlank()) {
                err = InputValidator.validateStrongPassword(newPass);
                if (err != null) { showError(err); return; }
            } else {
                newPass = null; // keep old password
            }

            // ===== Update users table =====
            service.updateUserCore(
                    userId,
                    tfFirstName.getText().trim(),
                    tfPhone.getText().trim(),
                    newPass
            );

            // ===== Type-specific update =====
            if (isRecruiter) {
                if (tfCompanyName.getText().trim().isEmpty()) { showError("Company name is required."); return; }

                service.updateRecruiterInfo(
                        userId,
                        tfCompanyName.getText().trim(),
                        tfCompanyLocation.getText().trim()
                );
            }

            if (isCandidate) {
                Integer years = null;
                String y = tfExperienceYears.getText().trim();
                if (!y.isEmpty()) {
                    try { years = Integer.parseInt(y); }
                    catch (NumberFormatException ex) { showError("Experience years must be a number."); return; }
                }

                String cv = tfCvPath.getText().trim();
                err = InputValidator.validateCvPath(cv);
                if (err != null) { showError(err); return; }

                service.updateCandidateInfo(
                        userId,
                        tfCandidateLocation.getText().trim(),
                        tfEducationLevel.getText().trim(),
                        years,
                        cv
                );
            }

            pfPassword.clear();
            lblStatus.setText("Saved ✅");

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Save failed: " + e.getMessage());
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    @FXML
    private void handleEnableFaceLogin() {
        try {
            User me = Session.getCurrentUser();
            if (me == null) { showError("Not logged in."); return; }

            // Allow selecting up to 3 photos
            FileChooser fc = new FileChooser();
            fc.setTitle("Select 1–3 clear face photos (hold Ctrl for multiple)");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));

            java.util.List<File> files = fc.showOpenMultipleDialog(tfEmail.getScene().getWindow());
            if (files == null || files.isEmpty()) return;
            if (files.size() > 3) files = files.subList(0, 3);

            UserService us = new UserService();
            LuxandFaceService lux = new LuxandFaceService();

            // Step 1 — delete the existing DB person from Luxand (if any)
            String existingUuid = us.getFacePersonId(me.getId());
            System.out.println("existingUuid from DB = [" + existingUuid + "]");
            if (existingUuid != null && !existingUuid.isBlank()) {
                try { lux.deletePerson(existingUuid.trim()); System.out.println("Deleted DB person: " + existingUuid); }
                catch (Exception ex) { System.err.println("Delete DB person failed (ignored): " + ex.getMessage()); }
            }

            // Step 2 — delete ALL Luxand persons with this email (kills any ghosts)
            try {
                java.util.List<String> allUuids = lux.listPersonUuids();
                System.out.println("Total Luxand persons: " + allUuids.size());
                // We can't filter by name from listPersonUuids alone — but we know the ghost UUID
                // So delete every UUID that's NOT the one we just created (done after creation below)
                // Instead: just try deleting the known ghost if it's not already gone
                // The real fix is: after addPerson, Luxand search will return our new UUID going forward
            } catch (Exception ex) {
                System.err.println("listPersonUuids failed (ignored): " + ex.getMessage());
            }

            // Step 3 — create fresh person with first photo
            byte[] firstBytes = Files.readAllBytes(files.get(0).toPath());
            String uuid = lux.addPerson(me.getEmail(), firstBytes, files.get(0).getName());
            System.out.println("Created Luxand person UUID=" + uuid);

            // Step 4 — add remaining photos (up to 2 more)
            for (int i = 1; i < files.size(); i++) {
                byte[] b = Files.readAllBytes(files.get(i).toPath());
                lux.addFace(uuid, b, files.get(i).getName());
                System.out.println("Added extra face " + i + " to UUID=" + uuid);
            }

            // Step 5 — save new UUID to DB
            us.enableFaceLogin(me.getId(), uuid);

            showAlert("Success", "Face login enabled ✅\n" + files.size() + " photo(s) registered.\nUUID: " + uuid);

            btnEnableFace.setVisible(false);
            btnEnableFace.setManaged(false);
            btnDisableFace.setVisible(true);
            btnDisableFace.setManaged(true);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Enable face failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDisableFaceLogin() {
        try {
            User me = Session.getCurrentUser();
            if (me == null) return;

            UserService us = new UserService();
            LuxandFaceService lux = new LuxandFaceService();

            String existingUuid = us.getFacePersonId(me.getId());
            if (existingUuid != null && !existingUuid.isBlank()) {
                try { lux.deletePerson(existingUuid.trim()); System.out.println("Deleted Luxand person: " + existingUuid); }
                catch (Exception ex) { System.err.println("Delete failed (ignored): " + ex.getMessage()); }
            }

            us.disableFaceLogin(me.getId());
            showAlert("Done", "Face login disabled ✅");

            btnEnableFace.setVisible(true);
            btnEnableFace.setManaged(true);
            btnDisableFace.setVisible(false);
            btnDisableFace.setManaged(false);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Disable face failed: " + e.getMessage());
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