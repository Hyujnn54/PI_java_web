package Services;

import Models.InterviewRescheduleRequest;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InterviewRescheduleService {

    private static final Connection cnx =
            MyDatabase.getInstance().getConnection();

    public static void addRequest(InterviewRescheduleRequest r) {
        String sql = "INSERT INTO interview_reschedule_request(interview_id, candidate_id, requested_datetime, reason, status) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getInterviewId());
            ps.setInt(2, r.getCandidateId());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(r.getRequestedDateTime()));
            ps.setString(4, r.getReason());
            ps.setString(5, r.getStatus());
            ps.executeUpdate();
            System.out.println("Reschedule request sent");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<InterviewRescheduleRequest> getAll() {
        List<InterviewRescheduleRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM interview_reschedule_request";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                InterviewRescheduleRequest r = new InterviewRescheduleRequest();
                r.setId(rs.getInt("id"));
                r.setInterviewId(rs.getInt("interview_id"));
                r.setCandidateId(rs.getInt("candidate_id"));
                r.setRequestedDateTime(rs.getTimestamp("requested_datetime").toLocalDateTime());
                r.setReason(rs.getString("reason"));
                r.setStatus(rs.getString("status"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public static void updateRequest(int id, InterviewRescheduleRequest r) {
        String sql = "UPDATE interview_reschedule_request SET interview_id=?, candidate_id=?, requested_datetime=?, reason=?, status=? WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, r.getInterviewId());
            ps.setInt(2, r.getCandidateId());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(r.getRequestedDateTime()));
            ps.setString(4, r.getReason());
            ps.setString(5, r.getStatus());
            ps.setInt(6, id);
            ps.executeUpdate();
            System.out.println("Reschedule request updated");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void deleteRequest(int id) {
        String sql = "DELETE FROM interview_reschedule_request WHERE id=?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Reschedule request deleted");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
