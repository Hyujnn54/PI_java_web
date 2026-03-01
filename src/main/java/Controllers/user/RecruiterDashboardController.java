package Controllers.user;

import Controllers.MainShellController;
import Utils.MyDatabase;
import Utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class RecruiterDashboardController {

    @FXML private Label lblOffers;
    @FXML private Label lblApplications;
    @FXML private Label lblInterviews;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        try {
            Long recruiterId = Session.getUserId();
            if (recruiterId == null) return;
            Connection conn = MyDatabase.getInstance().getConnection();

            // Job offers count
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM job_offer WHERE recruiter_id = ?")) {
                ps.setLong(1, recruiterId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblOffers.setText(String.valueOf(rs.getInt(1)));
            }

            // Applications received
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM job_application ja " +
                    "JOIN job_offer jo ON ja.offer_id = jo.id " +
                    "WHERE jo.recruiter_id = ?")) {
                ps.setLong(1, recruiterId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblApplications.setText(String.valueOf(rs.getInt(1)));
            }

            // Scheduled interviews
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COUNT(*) FROM interview i " +
                    "JOIN job_application ja ON i.application_id = ja.id " +
                    "JOIN job_offer jo ON ja.offer_id = jo.id " +
                    "WHERE jo.recruiter_id = ? AND i.status = 'SCHEDULED'")) {
                ps.setLong(1, recruiterId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) lblInterviews.setText(String.valueOf(rs.getInt(1)));
            }

        } catch (Exception e) {
            System.err.println("RecruiterDashboard stats error: " + e.getMessage());
        }
    }

    @FXML private void handleCreateOffer() {
        MainShellController shell = MainShellController.getInstance();
        if (shell != null) shell.loadContentView("/views/joboffers/JobOffers.fxml");
    }

    @FXML private void handleViewApplications() {
        MainShellController shell = MainShellController.getInstance();
        if (shell != null) shell.loadContentView("/views/application/Applications.fxml");
    }

    @FXML private void handleScheduleInterview() {
        MainShellController shell = MainShellController.getInstance();
        if (shell != null) shell.loadContentView("/views/interview/InterviewManagement.fxml");
    }
}


