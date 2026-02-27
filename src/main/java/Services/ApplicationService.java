package Services;

import Utils.MyDatabase;

import java.io.File;
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
        String jobTitle,
        boolean isArchived
    ) {}

    private static FileService fileService = new FileService();

        String jobTitle
    ) {}

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Check if candidate has already applied to this offer
     */
    public static boolean hasAlreadyApplied(Long offerId, Long candidateId) {
        String sql = "SELECT COUNT(*) as count FROM job_application " +
                     "WHERE offer_id = ? AND candidate_id = ? AND is_archived = 0";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, offerId);
            ps.setLong(2, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if candidate already applied: " + e.getMessage());
        }
        return false;
    }

    /**
     * Create application with PDF file upload
     */
    public static Long createWithPDF(Long offerId, Long candidateId, String phone, String coverLetter, File pdfFile) {
        // Check if candidate has already applied to this offer
        if (hasAlreadyApplied(offerId, candidateId)) {
            System.err.println("Candidate " + candidateId + " has already applied to offer " + offerId);
            return null;
        }

        String cvPath = "";

        // Upload PDF if provided
        if (pdfFile != null && pdfFile.exists()) {
            try {
                cvPath = fileService.uploadPDF(pdfFile);
            } catch (Exception e) {
                System.err.println("Error uploading PDF: " + e.getMessage());
                // Continue without PDF
            }
        }

        return create(offerId, candidateId, phone, coverLetter, cvPath);
    }

    /**
     * Download PDF for application
     */
    public static File downloadPDF(Long applicationId) throws Exception {
        ApplicationRow app = getById(applicationId);
        if (app == null || app.cvPath() == null || app.cvPath().isEmpty()) {
            throw new Exception("Application or PDF not found");
        }
        return fileService.downloadPDF(app.cvPath());
    }

    // CREATE
    public static Long create(Long offerId, Long candidateId, String phone, String coverLetter, String cvPath) {
        String sql = "INSERT INTO job_application (offer_id, candidate_id, phone, cover_letter, cv_path, current_status) " +
                     "VALUES (?, ?, ?, ?, ?, 'SUBMITTED')";

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, offerId);
            ps.setLong(2, candidateId);
            ps.setString(3, phone);
            ps.setString(4, coverLetter);
            ps.setString(5, cvPath);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    Long applicationId = rs.getLong(1);
                    ApplicationStatusHistoryService.addStatusHistory(applicationId, "SUBMITTED", candidateId, "Application submitted");
                    System.out.println("Application created: " + applicationId);
                    return applicationId;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating application: " + e.getMessage());
        }
        return null;
    }

    // READ - All
    public static List<ApplicationRow> getAll() {
        List<ApplicationRow> list = new ArrayList<>();
        String sql = "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
                     "ja.applied_at, ja.current_status, ja.is_archived, " +
                     "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
                     "u.email as candidate_email, jo.title as job_title " +
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
                    rs.getString("job_title"),
                    rs.getBoolean("is_archived")
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

    // READ - By ID
    public static ApplicationRow getById(Long id) {
        String sql = "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
                     "ja.applied_at, ja.current_status, ja.is_archived, " +
                     "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
                     "u.email as candidate_email, jo.title as job_title " +
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
                        rs.getString("job_title"),
                        rs.getBoolean("is_archived")
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
            System.err.println("Error retrieving application: " + e.getMessage());
            System.err.println("Error retrieving application by id: " + e.getMessage());
        }
        return null;
    }

    // READ - By Candidate ID
    public static List<ApplicationRow> getByCandidateId(Long candidateId) {
        List<ApplicationRow> list = new ArrayList<>();
        String sql = "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
                     "ja.applied_at, ja.current_status, ja.is_archived, " +
                     "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
                     "u.email as candidate_email, jo.title as job_title " +
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
            try (ResultSet rs = ps.executeQuery()) {
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
                        rs.getString("job_title"),
                        rs.getBoolean("is_archived")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving candidate applications: " + e.getMessage());
        }

        return list;
    }

    // READ - By Offer ID
    public static List<ApplicationRow> getByOfferId(Long offerId) {
        List<ApplicationRow> list = new ArrayList<>();
        String sql = "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
                     "ja.applied_at, ja.current_status, ja.is_archived, " +
                     "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
                     "u.email as candidate_email, jo.title as job_title " +
                     "FROM job_application ja " +
                     "LEFT JOIN users u ON ja.candidate_id = u.id " +
                     "LEFT JOIN job_offer jo ON ja.offer_id = jo.id " +
                     "WHERE ja.offer_id = ? " +
                     "ORDER BY ja.applied_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ApplicationRow(
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
                        rs.getString("job_title"),
                        rs.getBoolean("is_archived")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving offer applications: " + e.getMessage());
                        rs.getString("job_title")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving applications by candidate: " + e.getMessage());
        }

        return list;
    }

    // UPDATE
    public static void update(Long applicationId, String phone, String coverLetter, String cvPath) {
        String sql = "UPDATE job_application SET phone = ?, cover_letter = ?, cv_path = ? WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, phone);
            ps.setString(2, coverLetter);
            ps.setString(3, cvPath);
            ps.setLong(4, applicationId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Application updated: " + applicationId);
            }
        } catch (SQLException e) {
            System.err.println("Error updating application: " + e.getMessage());
        }
    }

    // UPDATE Status
    public static void updateStatus(Long applicationId, String status, Long changedBy, String note) {
        String sql = "UPDATE job_application SET current_status = ? WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, applicationId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                ApplicationStatusHistoryService.addStatusHistory(applicationId, status, changedBy, note);
                System.out.println("Application status updated: " + applicationId + " -> " + status);
            }
        } catch (SQLException e) {
            System.err.println("Error updating status: " + e.getMessage());
        }
    }

    // Archive / Unarchive
    public static void setArchived(Long applicationId, boolean archived, Long changedBy) {
        String sql = "UPDATE job_application SET is_archived = ? WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, archived ? 1 : 0);
            ps.setLong(2, applicationId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                String status = archived ? "ARCHIVED" : "UNARCHIVED";
                String note = archived ? "Application archived by admin" : "Application unarchived by admin";
                // Add a history entry to record the archive action
                ApplicationStatusHistoryService.addStatusHistory(applicationId, status, changedBy, note);
                System.out.println("Application archive flag updated: " + applicationId + " -> " + archived);
            }
        } catch (SQLException e) {
            System.err.println("Error setting archive flag: " + e.getMessage());
        }
    }

    // BULK UPDATE Status
    public static void bulkUpdateStatus(List<Long> applicationIds, String status, Long changedBy, String note) {
        if (applicationIds == null || applicationIds.isEmpty()) {
            System.err.println("No applications to update");
            return;
        }

        String sql = "UPDATE job_application SET current_status = ? WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            for (Long applicationId : applicationIds) {
                ps.setString(1, status);
                ps.setLong(2, applicationId);
                ps.addBatch();
            }

            int[] results = ps.executeBatch();
            int updatedCount = 0;

            // Add history entries for each updated application
            for (int i = 0; i < results.length; i++) {
                if (results[i] > 0) {
                    updatedCount++;
                    ApplicationStatusHistoryService.addStatusHistory(applicationIds.get(i), status, changedBy, note);
                }
            }

            System.out.println("Bulk status update completed: " + updatedCount + " applications updated to " + status);
        } catch (SQLException e) {
            System.err.println("Error in bulk status update: " + e.getMessage());
        }
    }

    // DELETE
    public static void delete(Long applicationId) {
        // First, get the application to retrieve PDF path
        ApplicationRow app = getById(applicationId);

        // Delete PDF file if it exists
        if (app != null && app.cvPath() != null && !app.cvPath().isEmpty()) {
            try {
                fileService.deletePDF(app.cvPath());
                System.out.println("PDF file deleted: " + app.cvPath());
            } catch (Exception e) {
                System.err.println("Error deleting PDF file: " + e.getMessage());
                // Continue with database deletion even if file deletion fails
            }
        }

        // Delete from database
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
            if (rows > 0) {
                System.out.println("Application deleted: " + applicationId);
            }
        } catch (SQLException e) {
            System.err.println("Error deleting application: " + e.getMessage());
        }
    }

    // Get distinct education levels from database
    public static List<String> getEducationLevels() {
        List<String> levels = new ArrayList<>();
        String sql = "SELECT DISTINCT education_level FROM users WHERE education_level IS NOT NULL ORDER BY education_level";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                String level = rs.getString("education_level");
                if (level != null && !level.trim().isEmpty()) {
                    levels.add(level);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving education levels: " + e.getMessage());
        }

        return levels;
    }

    // SEARCH Applications by various criteria
    public static List<ApplicationRow> searchApplications(
            List<Long> offerIds,
            String searchCriteria,
            String searchText) {

        List<ApplicationRow> list = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
            "ja.applied_at, ja.current_status, ja.is_archived, " +
            "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
            "u.email as candidate_email, jo.title as job_title " +
            "FROM job_application ja " +
            "LEFT JOIN users u ON ja.candidate_id = u.id " +
            "LEFT JOIN job_offer jo ON ja.offer_id = jo.id " +
            "WHERE 1=1"
        );

        List<Object> params = new ArrayList<>();

        // Filter by offer IDs (for recruiter's offers)
        if (offerIds != null && !offerIds.isEmpty()) {
            sql.append(" AND ja.offer_id IN (");
            for (int i = 0; i < offerIds.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(")");
            params.addAll(offerIds);
        }

        // Add search criteria
        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchValue = "%" + searchText.trim() + "%";

            if (searchCriteria != null) {
                switch (searchCriteria) {
                    case "Candidate Name":
                        sql.append(" AND (CONCAT(u.first_name, ' ', u.last_name) LIKE ? OR u.first_name LIKE ? OR u.last_name LIKE ?)");
                        params.add(searchValue);
                        params.add(searchValue);
                        params.add(searchValue);
                        break;
                    case "Candidate Email":
                        sql.append(" AND u.email LIKE ?");
                        params.add(searchValue);
                        break;
                    case "Offer Title":
                        sql.append(" AND jo.title LIKE ?");
                        params.add(searchValue);
                        break;
                    case "Company Name":
                        sql.append(" AND jo.company_name LIKE ?");
                        params.add(searchValue);
                        break;
                    case "Status":
                        sql.append(" AND ja.current_status LIKE ?");
                        params.add(searchValue);
                        break;
                }
            }
        }

        sql.append(" ORDER BY ja.applied_at DESC");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                if (params.get(i) instanceof Long) {
                    ps.setLong(i + 1, (Long) params.get(i));
                } else if (params.get(i) instanceof String) {
                    ps.setString(i + 1, (String) params.get(i));
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
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
                        rs.getString("job_title"),
                        rs.getBoolean("is_archived")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching applications: " + e.getMessage());
            return getByOfferIds(offerIds);
        }

        return list;
    }

    // Helper method to get applications by offer IDs
    private static List<ApplicationRow> getByOfferIds(List<Long> offerIds) {
        List<ApplicationRow> list = new ArrayList<>();
        if (offerIds == null || offerIds.isEmpty()) {
            return list;
        }

        StringBuilder sql = new StringBuilder(
            "SELECT ja.id, ja.offer_id, ja.candidate_id, ja.phone, ja.cover_letter, ja.cv_path, " +
            "ja.applied_at, ja.current_status, ja.is_archived, " +
            "CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.last_name, '')) as candidate_name, " +
            "u.email as candidate_email, jo.title as job_title " +
            "FROM job_application ja " +
            "LEFT JOIN users u ON ja.candidate_id = u.id " +
            "LEFT JOIN job_offer jo ON ja.offer_id = jo.id " +
            "WHERE ja.offer_id IN ("
        );

        for (int i = 0; i < offerIds.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(") ORDER BY ja.applied_at DESC");

        try (PreparedStatement ps = getConnection().prepareStatement(sql.toString())) {
            for (int i = 0; i < offerIds.size(); i++) {
                ps.setLong(i + 1, offerIds.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
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
                        rs.getString("job_title"),
                        rs.getBoolean("is_archived")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving applications by offer IDs: " + e.getMessage());
        }

        return list;
    }
}

            if (rows == 0) {
                throw new RuntimeException("Application not found: " + applicationId);
            }
            System.out.println("Application deleted: " + applicationId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete application: " + e.getMessage(), e);
        }
    }
}
