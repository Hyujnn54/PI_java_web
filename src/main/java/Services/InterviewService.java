package Services;

import Models.Interview;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InterviewService {

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
        if (!isValidApplicationId(i.getApplicationId())) {
            System.err.println("Invalid application_id: " + i.getApplicationId() + ". Using first available application.");
            int validAppId = getFirstValidApplicationId();
            if (validAppId == -1) {
                throw new RuntimeException("No valid applications found in database. Please create an application first.");
            }
            i.setApplicationId(validAppId);
        }

        if (!isValidRecruiterId(i.getRecruiterId())) {
            System.err.println("Invalid recruiter_id: " + i.getRecruiterId() + ". Using first available recruiter.");
            int validRecruiterId = getFirstValidRecruiterId();
            if (validRecruiterId == -1) {
                throw new RuntimeException("No valid recruiters found in database. Please create a recruiter first.");
            }
            i.setRecruiterId(validRecruiterId);
        }

        // Require meeting link or location depending on mode
        if ("ONLINE".equals(i.getMode())) {
            if (i.getMeetingLink() == null || i.getMeetingLink().isBlank()) {
                throw new IllegalArgumentException("Meeting link is required for ONLINE interviews.");
            }
            // Clear location for ONLINE
            i.setLocation(null);
        } else if ("ON_SITE".equals(i.getMode())) {
            if (i.getLocation() == null || i.getLocation().isBlank()) {
                throw new IllegalArgumentException("Location is required for ON_SITE interviews.");
            }
            // Clear meeting link for ON_SITE
            i.setMeetingLink(null);
        }

        String sql = "INSERT INTO interview(application_id, recruiter_id, scheduled_at, duration_minutes, mode, meeting_link, location, notes, status) VALUES (?,?,?,?,?,?,?,?,?)";

        System.out.println("Adding interview with data:");
        System.out.println("Application ID: " + i.getApplicationId());
        System.out.println("Recruiter ID: " + i.getRecruiterId());
        System.out.println("Scheduled At: " + i.getScheduledAt());
        System.out.println("Duration: " + i.getDurationMinutes());
        System.out.println("Mode: '" + i.getMode() + "' (length: " + (i.getMode() != null ? i.getMode().length() : "null") + ")");
        System.out.println("Status: '" + i.getStatus() + "' (length: " + (i.getStatus() != null ? i.getStatus().length() : "null") + ")");

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, i.getApplicationId());
            ps.setInt(2, i.getRecruiterId());
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
        return status.equals("SCHEDULED") || status.equals("RESCHEDULED") ||
               status.equals("CANCELLED") || status.equals("DONE");
    }

    private static boolean isValidApplicationId(int applicationId) {
        String sql = "SELECT COUNT(*) FROM application WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, applicationId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error validating application ID: " + e.getMessage());
        }
        return false;
    }

    private static boolean isValidRecruiterId(int recruiterId) {
        String sql = "SELECT COUNT(*) FROM recruiter WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, recruiterId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error validating recruiter ID: " + e.getMessage());
        }
        return false;
    }

    private static int getFirstValidApplicationId() {
        String sql = "SELECT id FROM application LIMIT 1";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("Using application ID: " + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Error getting first application ID: " + e.getMessage());
        }
        return -1;
    }

    private static int getFirstValidRecruiterId() {
        String sql = "SELECT id FROM recruiter LIMIT 1";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int id = rs.getInt("id");
                System.out.println("Using recruiter ID: " + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Error getting first recruiter ID: " + e.getMessage());
        }
        return -1;
    }

    public static List<Interview> getAll() {
        List<Interview> list = new ArrayList<>();
        String sql = "SELECT * FROM interview";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Interview i = new Interview();
                i.setId(rs.getInt("id"));
                i.setApplicationId(rs.getInt("application_id"));
                i.setRecruiterId(rs.getInt("recruiter_id"));
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

    public static void updateInterview(int id, Interview i) {
        // Validate enum values before database operation
        if (!isValidMode(i.getMode())) {
            throw new IllegalArgumentException("Invalid mode: " + i.getMode() + ". Must be ONLINE or ON_SITE");
        }
        if (!isValidStatus(i.getStatus())) {
            System.out.println("Warning: Potentially invalid status: " + i.getStatus());
        }

        // Require meeting link or location depending on mode
        if ("ONLINE".equals(i.getMode())) {
            if (i.getMeetingLink() == null || i.getMeetingLink().isBlank()) {
                throw new IllegalArgumentException("Meeting link is required for ONLINE interviews.");
            }
            i.setLocation(null);
        } else if ("ON_SITE".equals(i.getMode())) {
            if (i.getLocation() == null || i.getLocation().isBlank()) {
                throw new IllegalArgumentException("Location is required for ON_SITE interviews.");
            }
            i.setMeetingLink(null);
        }

        String sql = "UPDATE interview SET application_id=?, recruiter_id=?, scheduled_at=?, duration_minutes=?, mode=?, meeting_link=?, location=?, notes=?, status=? WHERE id=?";

        System.out.println("Updating interview ID " + id + " with data:");
        System.out.println("Application ID: " + i.getApplicationId());
        System.out.println("Recruiter ID: " + i.getRecruiterId());
        System.out.println("Scheduled At: " + i.getScheduledAt());
        System.out.println("Duration: " + i.getDurationMinutes());
        System.out.println("Mode: '" + i.getMode() + "' (length: " + (i.getMode() != null ? i.getMode().length() : "null") + ")");
        System.out.println("Status: '" + i.getStatus() + "' (length: " + (i.getStatus() != null ? i.getStatus().length() : "null") + ")");

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, i.getApplicationId());
            ps.setInt(2, i.getRecruiterId());
            ps.setTimestamp(3, Timestamp.valueOf(i.getScheduledAt()));
            ps.setInt(4, i.getDurationMinutes());
            ps.setString(5, i.getMode());
            ps.setString(6, i.getMeetingLink());
            ps.setString(7, i.getLocation());
            ps.setString(8, i.getNotes());
            ps.setString(9, i.getStatus());
            ps.setInt(10, id);

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

    public static void delete(int id) {
        String sql = "DELETE FROM interview WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Interview deleted: " + id);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
