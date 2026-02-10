package Services;

import Models.Interview;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InterviewService {

    private static final Connection cnx =
            MyDatabase.getInstance().getConnection();

    public static void addInterview(Interview i) {
        String sql = "INSERT INTO interview(application_id, recruiter_id, scheduled_at, duration_minutes, mode, status) VALUES (?,?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, i.getApplicationId());
            ps.setInt(2, i.getRecruiterId());
            ps.setTimestamp(3, Timestamp.valueOf(i.getScheduledAt()));
            ps.setInt(4, i.getDurationMinutes());
            ps.setString(5, i.getMode());
            ps.setString(6, i.getStatus());
            ps.executeUpdate();
            System.out.println("Interview added");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<Interview> getAll() {
        List<Interview> list = new ArrayList<>();
        String sql = "SELECT * FROM interview";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Interview i = new Interview();
                i.setId(rs.getInt("id"));
                i.setApplicationId(rs.getInt("application_id"));
                i.setRecruiterId(rs.getInt("recruiter_id"));
                i.setScheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime());
                i.setDurationMinutes(rs.getInt("duration_minutes"));
                i.setMode(rs.getString("mode"));
                i.setStatus(rs.getString("status"));
                list.add(i);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public static void updateInterview(int id, Interview i) {
        String sql = "UPDATE interview SET application_id=?, recruiter_id=?, scheduled_at=?, duration_minutes=?, mode=?, status=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, i.getApplicationId());
            ps.setInt(2, i.getRecruiterId());
            ps.setTimestamp(3, Timestamp.valueOf(i.getScheduledAt()));
            ps.setInt(4, i.getDurationMinutes());
            ps.setString(5, i.getMode());
            ps.setString(6, i.getStatus());
            ps.setInt(7, id);
            ps.executeUpdate();
            System.out.println("Interview updated");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void approveRescheduleRequest(int interviewId, LocalDateTime newDate) {
        String sql = "UPDATE interview SET scheduled_at=?, status='APPROVED' WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(newDate));
            ps.setInt(2, interviewId);
            ps.executeUpdate();
            System.out.println("Reschedule request approved");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void rejectRescheduleRequest(int interviewId) {
        String sql = "UPDATE interview SET status='REJECTED' WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, interviewId);
            ps.executeUpdate();
            System.out.println("Reschedule request rejected");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void delete(int id) {
        String sql = "DELETE FROM interview WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Interview deleted");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
