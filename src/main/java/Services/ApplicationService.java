package Services;

import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal read-only service for applications.
 * We only need retrieval for the UI flows (schedule interview from application).
 */
public class ApplicationService {

    public record ApplicationRow(int id, int candidateId, String status) {}

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public static List<ApplicationRow> getAll() {
        List<ApplicationRow> list = new ArrayList<>();
        String sql = "SELECT id, candidate_id, status FROM application ORDER BY id DESC";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new ApplicationRow(
                        rs.getInt("id"),
                        rs.getInt("candidate_id"),
                        rs.getString("status")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving applications: " + e.getMessage());
        }

        return list;
    }

    public static ApplicationRow getById(int id) {
        String sql = "SELECT id, candidate_id, status FROM application WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ApplicationRow(rs.getInt("id"), rs.getInt("candidate_id"), rs.getString("status"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving application by id: " + e.getMessage());
        }
        return null;
    }

    public static void updateStatus(int applicationId, String status) {
        String sql = "UPDATE application SET status = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, applicationId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update application status: " + e.getMessage(), e);
        }
    }

    public static void reject(int applicationId) {
        updateStatus(applicationId, "REJECTED");
    }

    public static void accept(int applicationId) {
        updateStatus(applicationId, "ACCEPTED");
    }

    public static void delete(int applicationId) {
        String sql = "DELETE FROM application WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete application: " + e.getMessage(), e);
        }
    }
}
