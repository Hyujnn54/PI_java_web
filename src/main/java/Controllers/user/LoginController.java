package Controllers.user;

import Models.user.User;
import Services.user.AuthService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import Services.user.LuxandFaceService;
import Services.user.UserService;

import Utils.CameraUtil;

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
            Models.user.User logged = authService.login(email, password);

            if (logged != null) {
                System.out.println("LOGIN SUCCESS - sending email...");

                new Thread(() -> {
                    try {
                        new Services.user.EmailService()
                                .sendLoginSuccess(logged.getEmail(), logged.getFirstName());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
                Utils.Session.start(logged);

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/SignUp.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            Scene current = stage.getScene();
            scene(stage, loader, current.getWidth(), current.getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scene(Stage stage, FXMLLoader loader, double w, double h) throws Exception {
        Scene s = new Scene(loader.load(), w, h);
        stage.setScene(s);
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/ForgotPassword.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            scene(stage, loader, stage.getScene().getWidth(), stage.getScene().getHeight());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void handleLoginWithFace(ActionEvent event) {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Select your face photo");
            fc.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
            );

            File file = fc.showOpenDialog(txtEmail.getScene().getWindow());
            if (file == null) return;

            byte[] bytes = Files.readAllBytes(file.toPath());

            LuxandFaceService lux = new LuxandFaceService();
            LuxandFaceService.FaceMatch match = lux.searchBestMatch(bytes, file.getName());

            if (match == null || match.probability < 0.85) {
                showAlert("Face login failed", "No confident match. Try a clearer photo.");
                return;
            }

            User user = new UserService().getByFacePersonId(match.uuid);
            System.out.println("DB user = " + (user == null ? "null" : user.getEmail()));
            if (user == null) {
                showAlert("Face login failed", "Matched face is not linked to any account (or face login not enabled).");
                return;
            }

            // ✅ login success
            Utils.Session.start(user);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MainShell.fxml"));
            Stage stage = (Stage) txtEmail.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Face login failed", e.getMessage());
        }
    }

    @FXML
    private void handleLoginWithCamera(ActionEvent event) {
        try {
            // 1) Capture multiple shots
            java.util.List<byte[]> shots = Utils.CameraUtil.captureMultipleJpegsWithPreview(
                    txtEmail.getScene().getWindow(),
                    5,      // capture 5 shots (more stable)
                    250     // 250ms between shots
            );

            if (shots == null || shots.isEmpty()) {
                showAlert("Face login failed", "Capture cancelled.");
                return;
            }

            System.out.println("Captured shots = " + shots.size());
            for (int i = 0; i < shots.size(); i++) {
                System.out.println("SHOT[" + i + "] bytes=" + shots.get(i).length);
            }

            // 2) Run Luxand + DB in background thread
            new Thread(() -> {
                try {
                    System.out.println("=== FACE LOGIN START ===");

                    LuxandFaceService lux = new LuxandFaceService();

                    LuxandFaceService.LivenessResult bestLive = null;
                    byte[] bestBytes = null;

                    // 3) Check liveness for each shot
                    for (int i = 0; i < shots.size(); i++) {
                        byte[] b = shots.get(i);

                        LuxandFaceService.LivenessResult live = lux.isLive(b, "capture.jpg");

                        System.out.println("SHOT " + i +
                                " => isReal=" + live.isReal +
                                " score=" + live.score);

                        if (bestLive == null || live.score > bestLive.score) {
                            bestLive = live;
                            bestBytes = b;
                        }

                        // stop early if excellent
                        if (live.isReal && live.score >= 0.70) break;
                    }

                    if (bestLive == null) {
                        javafx.application.Platform.runLater(() ->
                                showAlert("Face login failed", "Liveness detection error.")
                        );
                        return;
                    }

                    // 4) Acceptance policy
                    double THRESHOLD = 0.60; // adjust if needed
                    boolean accepted = bestLive.isReal || bestLive.score >= THRESHOLD;

                    if (!accepted) {

                        LuxandFaceService.LivenessResult finalBestLive = bestLive;

                        javafx.application.Platform.runLater(() ->
                                showAlert("Face login failed",
                                        "Liveness failed (best score="
                                                + String.format("%.2f", finalBestLive.score)
                                                + "). Blink + move head slightly + better light.")
                        );
                        return;
                    }

                    System.out.println("LIVENESS ACCEPTED score=" + bestLive.score);

                    // 5) Crop face using rectangle
                    byte[] faceOnly = Utils.CameraUtil.cropToFace(
                            bestBytes,
                            bestLive.left,
                            bestLive.top,
                            bestLive.right,
                            bestLive.bottom
                    );

                    if (faceOnly == null || faceOnly.length < 2000)
                        faceOnly = bestBytes;

                    // 6) Search match
                    LuxandFaceService.FaceMatch match =
                            lux.searchBestMatch(faceOnly, "face.jpg");

                    if (match == null) {
                        javafx.application.Platform.runLater(() ->
                                showAlert("Face login failed",
                                        "Face not recognized. Enable face login in profile first.")
                        );
                        return;
                    }

                    if (match.probability < 0.85) {
                        double pct = Math.round(match.probability * 1000.0) / 10.0;
                        javafx.application.Platform.runLater(() ->
                                showAlert("Face login failed",
                                        "Low confidence (" + pct + "%). Try better lighting.")
                        );
                        return;
                    }

                    // 7) Check DB link
                    User user = new UserService().getByFacePersonId(match.uuid);

                    if (user == null) {
                        javafx.application.Platform.runLater(() ->
                                showAlert("Face login failed",
                                        "Matched face is not linked to any account.")
                        );
                        return;
                    }

                    // 8) SUCCESS LOGIN
                    javafx.application.Platform.runLater(() -> {
                        try {
                            Utils.Session.start(user);

                            FXMLLoader loader = new FXMLLoader(
                                    getClass().getResource("/MainShell.fxml")
                            );

                            Stage stage = (Stage) txtEmail.getScene().getWindow();
                            stage.setScene(new Scene(loader.load()));

                        } catch (Exception ex) {
                            ex.printStackTrace();
                            showAlert("Face login failed", "UI error: " + ex.getMessage());
                        }
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() ->
                            showAlert("Face login failed", "System error: " + ex.getMessage())
                    );
                } finally {
                    System.out.println("=== FACE LOGIN END ===");
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Face login failed", "Capture error: " + e.getMessage());
        }
    }
}






