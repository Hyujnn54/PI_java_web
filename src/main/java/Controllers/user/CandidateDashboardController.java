package Controllers.user;

import Controllers.MainShellController;
import Utils.MyDatabase;
import Utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ButtonType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CandidateDashboardController {

    @FXML private Label lblApplications;
    @FXML private Label lblInterviews;
    @FXML private Label lblOffers;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        try {
            Long candidateId = Session.getUserId();
            if (candidateId == null) return;
            Connection conn = MyDatabase.getInstance().getConnection();

            // Applications count
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM job_application WHERE candidate_id = ?")) {
                ps.setLong(1, candidateId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblApplications.setText(String.valueOf(rs.getInt(1)));
            }

            // Interviews count (scheduled)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM interview i " +
                    "JOIN job_application ja ON i.application_id = ja.id " +
                    "WHERE ja.candidate_id = ? AND i.status = 'SCHEDULED'")) {
                ps.setLong(1, candidateId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblInterviews.setText(String.valueOf(rs.getInt(1)));
            }

            // Available offers count
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM job_offer WHERE status = 'OPEN'")) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblOffers.setText(String.valueOf(rs.getInt(1)));
            }

        } catch (Exception e) {
            System.err.println("CandidateDashboard stats error: " + e.getMessage());
        }
    }

    @FXML private void handleBrowseJobs() {
        MainShellController shell = MainShellController.getInstance();
        if (shell != null) shell.loadContentView("/views/joboffers/JobOffersBrowse.fxml");
    }

    @FXML private void handleViewMyApplications() {
        MainShellController shell = MainShellController.getInstance();
        if (shell != null) shell.loadContentView("/views/application/Applications.fxml");
    }

    @FXML
    private void handleSkills() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/CandidateSkillsDialog.fxml"));
            DialogPane pane = loader.load();
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("My Skills");
            dialog.setDialogPane(pane);
            if (!pane.getButtonTypes().contains(ButtonType.CLOSE))
                pane.getButtonTypes().add(ButtonType.CLOSE);
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

