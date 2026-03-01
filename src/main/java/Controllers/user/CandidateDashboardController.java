package Controllers.user;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonType;


public class CandidateDashboardController {

    @FXML private Label lblApplications;
    @FXML private Label lblInterviews;
    @FXML private Label lblOffers;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        statusLabel.setText("Welcome candidate ✅");
    }

    @FXML private void handleBrowseJobs() { statusLabel.setText("Browse Job Offers clicked"); }
    @FXML private void handleViewMyApplications() { statusLabel.setText("View My Applications clicked"); }
    @FXML
    private void handleSkills() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/CandidateSkillsDialog.fxml"));
            DialogPane pane = loader.load();

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("My Skills");
            dialog.setDialogPane(pane);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
