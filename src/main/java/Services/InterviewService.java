package Services;

import Models.Interview;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InterviewService {

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?://)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([/\\w \\.-]*)*/?$",
        Pattern.CASE_INSENSITIVE
    );

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public static void addInterview(Interview i) {
        // Validate enum values before database operation
        if (!isValidMode(i.getMode())) {
            throw new IllegalArgumentException("Invalid mode: " + i.getMode() + ". Must be ONLINE or ON_SITE");
        }
        if (!isValidStatus(i.getStatus())) {
            System.out.println("Warning: Potentially invalid status: " + i.getStatus());
        }

        // Validate foreign key constraints
        if (i.getApplicationId() == null || !isValidApplicationId(i.getApplicationId())) {
            throw new RuntimeException("Invalid or missing application_id. Please select a valid job application.");
        }

        if (i.getRecruiterId() == null || !isValidRecruiterId(i.getRecruiterId())) {
            throw new RuntimeException("Invalid or missing recruiter_id. Please ensure recruiter is logged in.");
        }

        // Validate scheduled date is in the future
        if (i.getScheduledAt() != null && i.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Interview cannot be scheduled in the past.");
        }

        // Require meeting link or location depending on mode
        if ("ONLINE".equals(i.getMode())) {
            if (i.getMeetingLink() == null || i.getMeetingLink().isBlank()) {
                throw new IllegalArgumentException("Meeting link is required for ONLINE interviews.");
            }
            // Validate meeting link format
            if (!isValidUrl(i.getMeetingLink())) {
                throw new IllegalArgumentException("Invalid meeting link URL format.");
            }
            // Clear location for ONLINE
            i.setLocation(null);
        } else if ("ON_SITE".equals(i.getMode())) {
            if (i.getLocation() == null || i.getLocation().isBlank()) {
                throw new IllegalArgumentException("Location is required for ON_SITE interviews.");
            }
            if (i.getLocation().length() > 255) {
                throw new IllegalArgumentException("Location is too long (max 255 characters).");
            }
            // Clear meeting link for ON_SITE
            i.setMeetingLink(null);
        }

        // Validate duration
        if (i.getDurationMinutes() <= 0 || i.getDurationMinutes() > 480) {
            throw new IllegalArgumentException("Duration must be between 1 and 480 minutes.");
        }

        String sql = "INSERT INTO interview(application_id, recruiter_id, scheduled_at, duration_minutes, mode, meeting_link, location, notes, status) VALUES (?,?,?,?,?,?,?,?,?)";

        System.out.println("Adding interview with data:");
        System.out.println("Application ID: " + i.getApplicationId());
        System.out.println("Recruiter ID: " + i.getRecruiterId());
        System.out.println("Scheduled At: " + i.getScheduledAt());
        System.out.println("Duration: " + i.getDurationMinutes());
        System.out.println("Mode: '" + i.getMode() + "'");
        System.out.println("Status: '" + i.getStatus() + "'");

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, i.getApplicationId());
            ps.setLong(2, i.getRecruiterId());
            ps.setTimestamp(3, Timestamp.valueOf(i.getScheduledAt()));
            ps.setInt(4, i.getDurationMinutes());
            ps.setString(5, i.getMode());
            ps.setString(6, i.getMeetingLink());
            ps.setString(7, i.getLocation());
            ps.setString(8, i.getNotes());
            ps.setString(9, i.getStatus());

            int rowsAffected = ps.executeUpdate();
            System.out.println("Interview added successfully. Rows affected: " + rowsAffected);
        } catch (SQLException e) {
            System.err.println("SQL Error adding interview: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw new RuntimeException("Failed to add interview: " + e.getMessage(), e);
        }
    }

    private static boolean isValidMode(String mode) {
        return mode != null && (mode.equals("ONLINE") || mode.equals("ON_SITE"));
    }

    private static boolean isValidStatus(String status) {
        if (status == null) return false;
        return status.equals("SCHEDULED") || status.equals("CANCELLED") || status.equals("DONE");
    }

    private static boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) return false;
        return URL_PATTERN.matcher(url).matches();
    }

    private static boolean isValidApplicationId(Long applicationId) {
        String sql = "SELECT COUNT(*) FROM job_application WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error validating application ID: " + e.getMessage());
        }
        return false;
    }

    private static boolean isValidRecruiterId(Long recruiterId) {
        String sql = "SELECT COUNT(*) FROM recruiter WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, recruiterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error validating recruiter ID: " + e.getMessage());
        }
        return false;
    }

    public static List<Interview> getAll() {
        List<Interview> list = new ArrayList<>();
        String sql = "SELECT * FROM interview ORDER BY scheduled_at DESC";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Interview i = new Interview();
                i.setId(rs.getLong("id"));
                i.setApplicationId(rs.getLong("application_id"));
                i.setRecruiterId(rs.getLong("recruiter_id"));
                i.setScheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime());
                i.setDurationMinutes(rs.getInt("duration_minutes"));
                i.setMode(rs.getString("mode"));
                i.setMeetingLink(rs.getString("meeting_link"));
                i.setLocation(rs.getString("location"));
                i.setNotes(rs.getString("notes"));
                i.setStatus(rs.getString("status"));
                list.add(i);
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving interviews: " + e.getMessage());
        }
        return list;
    }

    public static Interview getById(Long id) {
        String sql = "SELECT * FROM interview WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Interview i = new Interview();
                i.setId(rs.getLong("id"));
                i.setApplicationId(rs.getLong("application_id"));
                i.setRecruiterId(rs.getLong("recruiter_id"));
                i.setScheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime());
                i.setDurationMinutes(rs.getInt("duration_minutes"));
                i.setMode(rs.getString("mode"));
                i.setMeetingLink(rs.getString("meeting_link"));
                i.setLocation(rs.getString("location"));
                i.setNotes(rs.getString("notes"));
                i.setStatus(rs.getString("status"));
                return i;
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving interview by id: " + e.getMessage());
        }
        return null;
    }

    public static void updateInterview(Long id, Interview i) {
        // Validate enum values before database operation
        if (!isValidMode(i.getMode())) {
            throw new IllegalArgumentException("Invalid mode: " + i.getMode() + ". Must be ONLINE or ON_SITE");
        }
        if (!isValidStatus(i.getStatus())) {
            System.out.println("Warning: Potentially invalid status: " + i.getStatus());
        }

        // Validate scheduled date is in the future (only if status is SCHEDULED)
        if ("SCHEDULED".equals(i.getStatus()) && i.getScheduledAt() != null && i.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Interview cannot be scheduled in the past.");
        }

        // Require meeting link or location depending on mode
        if ("ONLINE".equals(i.getMode())) {
            if (i.getMeetingLink() == null || i.getMeetingLink().isBlank()) {
                throw new IllegalArgumentException("Meeting link is required for ONLINE interviews.");
            }
            if (!isValidUrl(i.getMeetingLink())) {
                throw new IllegalArgumentException("Invalid meeting link URL format.");
            }
            i.setLocation(null);
        } else if ("ON_SITE".equals(i.getMode())) {
            if (i.getLocation() == null || i.getLocation().isBlank()) {
                throw new IllegalArgumentException("Location is required for ON_SITE interviews.");
            }
            if (i.getLocation().length() > 255) {
                throw new IllegalArgumentException("Location is too long (max 255 characters).");
            }
            i.setMeetingLink(null);
        }

        // Validate duration
        if (i.getDurationMinutes() <= 0 || i.getDurationMinutes() > 480) {
            throw new IllegalArgumentException("Duration must be between 1 and 480 minutes.");
        }

        String sql = "UPDATE interview SET application_id=?, recruiter_id=?, scheduled_at=?, duration_minutes=?, mode=?, meeting_link=?, location=?, notes=?, status=? WHERE id=?";

        System.out.println("Updating interview ID " + id + " with data:");
        System.out.println("Application ID: " + i.getApplicationId());
        System.out.println("Recruiter ID: " + i.getRecruiterId());
        System.out.println("Scheduled At: " + i.getScheduledAt());
        System.out.println("Duration: " + i.getDurationMinutes());
        System.out.println("Mode: '" + i.getMode() + "'");
        System.out.println("Status: '" + i.getStatus() + "'");

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, i.getApplicationId());
            ps.setLong(2, i.getRecruiterId());
            ps.setTimestamp(3, Timestamp.valueOf(i.getScheduledAt()));
            ps.setInt(4, i.getDurationMinutes());
            ps.setString(5, i.getMode());
            ps.setString(6, i.getMeetingLink());
            ps.setString(7, i.getLocation());
            ps.setString(8, i.getNotes());
            ps.setString(9, i.getStatus());
            ps.setLong(10, id);

            int rowsAffected = ps.executeUpdate();
            System.out.println("Interview updated successfully. Rows affected: " + rowsAffected);

            if (rowsAffected == 0) {
                System.err.println("WARNING: No rows were updated. Interview ID " + id + " may not exist.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Error updating interview: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            e.printStackTrace();
            throw new RuntimeException("Failed to update interview: " + e.getMessage(), e);
        }
    }

    public static void delete(Long id) {
        String sql = "DELETE FROM interview WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            System.out.println("Interview deleted: " + id);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<Interview> getByApplicationId(Long applicationId) {
        List<Interview> list = new ArrayList<>();
        String sql = "SELECT * FROM interview WHERE application_id = ? ORDER BY scheduled_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Interview i = new Interview();
                i.setId(rs.getLong("id"));
                i.setApplicationId(rs.getLong("application_id"));
                i.setRecruiterId(rs.getLong("recruiter_id"));
                i.setScheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime());
                i.setDurationMinutes(rs.getInt("duration_minutes"));
                i.setMode(rs.getString("mode"));
                i.setMeetingLink(rs.getString("meeting_link"));
                i.setLocation(rs.getString("location"));
                i.setNotes(rs.getString("notes"));
                i.setStatus(rs.getString("status"));
                list.add(i);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving interviews by application: " + e.getMessage());
        }
        return list;
    }
}
