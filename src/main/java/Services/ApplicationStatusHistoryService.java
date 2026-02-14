package Services;

import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ApplicationStatusHistoryService {

    public record StatusHistoryRow(
        Long id,
        Long applicationId,
        String status,
        LocalDateTime changedAt,
        Long changedBy,
        String note
    ) {}

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    // CREATE
    public static Long addStatusHistory(Long applicationId, String status, Long changedBy, String note) {
        String sql = "INSERT INTO application_status_history (application_id, status, changed_by, note) " +
                     "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, applicationId);
            ps.setString(2, status);
            ps.setLong(3, changedBy);
            ps.setString(4, note);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating status history: " + e.getMessage());
        }
        return null;
    }

    // READ - By Application ID
    public static List<StatusHistoryRow> getByApplicationId(Long applicationId) {
        List<StatusHistoryRow> list = new ArrayList<>();
        String sql = "SELECT id, application_id, status, changed_at, changed_by, note " +
                     "FROM application_status_history " +
                     "WHERE application_id = ? " +
                     "ORDER BY changed_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new StatusHistoryRow(
                        rs.getLong("id"),
                        rs.getLong("application_id"),
                        rs.getString("status"),
                        rs.getTimestamp("changed_at") != null ? rs.getTimestamp("changed_at").toLocalDateTime() : null,
                        rs.getLong("changed_by"),
                        rs.getString("note")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving status history: " + e.getMessage());
        }

        return list;
    }

    // READ - By ID
    public static StatusHistoryRow getById(Long id) {
        String sql = "SELECT id, application_id, status, changed_at, changed_by, note " +
                     "FROM application_status_history WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new StatusHistoryRow(
                        rs.getLong("id"),
                        rs.getLong("application_id"),
                        rs.getString("status"),
                        rs.getTimestamp("changed_at") != null ? rs.getTimestamp("changed_at").toLocalDateTime() : null,
                        rs.getLong("changed_by"),
                        rs.getString("note")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving history record: " + e.getMessage());
        }

        return null;
    }

    // UPDATE
    public static void updateStatusHistory(Long historyId, String note) {
        String sql = "UPDATE application_status_history SET note = ? WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, note);
            ps.setLong(2, historyId);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("History updated: " + historyId);
            }
        } catch (SQLException e) {
            System.err.println("Error updating history: " + e.getMessage());
        }
    }

    // DELETE
    public static void deleteStatusHistory(Long historyId) {
        String sql = "DELETE FROM application_status_history WHERE id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, historyId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("History deleted: " + historyId);
            }
        } catch (SQLException e) {
            System.err.println("Error deleting history: " + e.getMessage());
        }
    }

    // DELETE - By Application ID (cascade handled by DB)
    public static void deleteByApplicationId(Long applicationId) {
        String sql = "DELETE FROM application_status_history WHERE application_id = ?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, applicationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error deleting history by application: " + e.getMessage());
        }
    }
}

