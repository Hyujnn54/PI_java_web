package Services;

import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for job_application table.
 * Provides read and status update operations for applications.
 */
public class ApplicationService {

    public record ApplicationRow(
        Long id,
        Long offerId,
        Long candidateId,
        String phone,
        String coverLetter,
        String cvPath,
        LocalDateTime appliedAt,
        String currentStatus,
        String candidateName,
        String candidateEmail,
        String jobTitle
    ) {}

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public static List<ApplicationRow> getAll() {
        List<ApplicationRow> list = new ArrayList<>();
        String sql = "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
                     "ja.applied_at, ja.current_status, " +
                     "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
                     "u.email as candidate_email, " +
                     "jo.title as job_title " +
                     "FROM job_application ja " +
                     "LEFT JOIN users u ON ja.candidate_id = u.id " +
                     "LEFT JOIN job_offer jo ON ja.offer_id = jo.id " +
                     "ORDER BY ja.applied_at DESC";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new ApplicationRow(
                        rs.getLong("id"),
                        rs.getLong("offer_id"),
                        rs.getLong("candidate_id"),
                        rs.getString("phone"),
                        rs.getString("cover_letter"),
                        rs.getString("cv_path"),
                        rs.getTimestamp("applied_at") != null ? rs.getTimestamp("applied_at").toLocalDateTime() : null,
                        rs.getString("current_status"),
                        rs.getString("candidate_name"),
                        rs.getString("candidate_email"),
                        rs.getString("job_title")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving applications: " + e.getMessage());
        }

        return list;
    }

    public static ApplicationRow getById(Long id) {
        String sql = "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
                     "ja.applied_at, ja.current_status, " +
                     "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
                     "u.email as candidate_email, " +
                     "jo.title as job_title " +
                     "FROM job_application ja " +
                     "LEFT JOIN users u ON ja.candidate_id = u.id " +
                     "LEFT JOIN job_offer jo ON ja.offer_id = jo.id " +
                     "WHERE ja.id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ApplicationRow(
                            rs.getLong("id"),
                            rs.getLong("offer_id"),
                            rs.getLong("candidate_id"),
                            rs.getString("phone"),
                            rs.getString("cover_letter"),
                            rs.getString("cv_path"),
                            rs.getTimestamp("applied_at") != null ? rs.getTimestamp("applied_at").toLocalDateTime() : null,
                            rs.getString("current_status"),
                            rs.getString("candidate_name"),
                            rs.getString("candidate_email"),
                            rs.getString("job_title")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving application by id: " + e.getMessage());
        }
        return null;
    }

    public static List<ApplicationRow> getByCandidateId(Long candidateId) {
        List<ApplicationRow> list = new ArrayList<>();
        String sql = "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
                     "ja.applied_at, ja.current_status, " +
                     "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
                     "u.email as candidate_email, " +
                     "jo.title as job_title " +
                     "FROM job_application ja " +
                     "LEFT JOIN users u ON ja.candidate_id = u.id " +
                     "LEFT JOIN job_offer jo ON ja.offer_id = jo.id " +
                     "WHERE ja.candidate_id = ? " +
                     "ORDER BY ja.applied_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, candidateId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(new ApplicationRow(
                        rs.getLong("id"),
                        rs.getLong("offer_id"),
                        rs.getLong("candidate_id"),
                        rs.getString("phone"),
                        rs.getString("cover_letter"),
                        rs.getString("cv_path"),
                        rs.getTimestamp("applied_at") != null ? rs.getTimestamp("applied_at").toLocalDateTime() : null,
                        rs.getString("current_status"),
                        rs.getString("candidate_name"),
                        rs.getString("candidate_email"),
                        rs.getString("job_title")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving applications by candidate: " + e.getMessage());
        }

        return list;
    }

    public static void updateStatus(Long applicationId, String status) {
        // Validate status enum
        List<String> validStatuses = List.of("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        if (!validStatuses.contains(status)) {
            throw new IllegalArgumentException("Invalid status: " + status);
        }

        String sql = "UPDATE job_application SET current_status = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, applicationId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            System.out.println("Application " + applicationId + " status updated to: " + status);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update application status: " + e.getMessage(), e);
        }
    }

    public static void reject(Long applicationId) {
        updateStatus(applicationId, "REJECTED");
    }

    public static void shortlist(Long applicationId) {
        updateStatus(applicationId, "SHORTLISTED");
    }

    public static void setToInterview(Long applicationId) {
        updateStatus(applicationId, "INTERVIEW");
    }

    public static void hire(Long applicationId) {
        updateStatus(applicationId, "HIRED");
    }

    public static void delete(Long applicationId) {
        String sql = "DELETE FROM job_application WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            System.out.println("Application deleted: " + applicationId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete application: " + e.getMessage(), e);
        }
    }
}
