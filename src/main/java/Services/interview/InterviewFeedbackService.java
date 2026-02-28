package Services.interview;

import Models.interview.InterviewFeedback;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InterviewFeedbackService {

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public static void addFeedback(InterviewFeedback f) {
        // Validate decision enum
        if (f.getDecision() == null || (!f.getDecision().equals("ACCEPTED") && !f.getDecision().equals("REJECTED"))) {
            throw new IllegalArgumentException("Decision must be ACCEPTED or REJECTED");
        }

        // Validate overall score if present
        if (f.getOverallScore() != null && (f.getOverallScore() < 0 || f.getOverallScore() > 100)) {
            throw new IllegalArgumentException("Overall score must be between 0 and 100");
        }

        String sql = "INSERT INTO interview_feedback(interview_id, recruiter_id, overall_score, decision, comment) VALUES (?,?,?,?,?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, f.getInterviewId());
            ps.setLong(2, f.getRecruiterId());
            if (f.getOverallScore() != null) {
                ps.setInt(3, f.getOverallScore());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, f.getDecision());
            ps.setString(5, f.getComment());
            ps.executeUpdate();
            System.out.println("Feedback added");
        } catch (SQLException e) {
            System.err.println("Error adding feedback: " + e.getMessage());
            throw new RuntimeException("Failed to add feedback: " + e.getMessage(), e);
        }
    }

    public static void updateFeedback(Long id, InterviewFeedback f) {
        System.out.println("  [SERVICE] updateFeedback called with ID: " + id);
        System.out.println("  [SERVICE] New values - Decision: " + f.getDecision() + ", Score: " + f.getOverallScore());

        // Validate decision enum
        if (f.getDecision() == null || (!f.getDecision().equals("ACCEPTED") && !f.getDecision().equals("REJECTED"))) {
            throw new IllegalArgumentException("Decision must be ACCEPTED or REJECTED");
        }

        // Validate overall score if present
        if (f.getOverallScore() != null && (f.getOverallScore() < 0 || f.getOverallScore() > 100)) {
            throw new IllegalArgumentException("Overall score must be between 0 and 100");
        }

        String sql = "UPDATE interview_feedback SET interview_id=?, recruiter_id=?, overall_score=?, decision=?, comment=? WHERE id=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, f.getInterviewId());
            ps.setLong(2, f.getRecruiterId());
            if (f.getOverallScore() != null) {
                ps.setInt(3, f.getOverallScore());
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setString(4, f.getDecision());
            ps.setString(5, f.getComment());
            ps.setLong(6, id);

            int rowsAffected = ps.executeUpdate();
            System.out.println("  [SERVICE] Update executed. Rows affected: " + rowsAffected);

            if (rowsAffected > 0) {
                System.out.println("  [SERVICE] ✓ Feedback updated successfully in database");
            } else {
                System.err.println("  [SERVICE] ✗ WARNING: No rows were updated! ID may not exist: " + id);
            }
        } catch (SQLException e) {
            System.err.println("  [SERVICE] ✗ SQL Error updating feedback: " + e.getMessage());
            throw new RuntimeException("Failed to update feedback: " + e.getMessage(), e);
        }
    }

    public static void deleteFeedback(Long id) {
        String sql = "DELETE FROM interview_feedback WHERE id=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
            System.out.println("Feedback deleted");
        } catch (SQLException e) {
            System.err.println("Error deleting feedback: " + e.getMessage());
        }
    }

    public static List<InterviewFeedback> getByInterviewId(Long interviewId) {
        List<InterviewFeedback> list = new ArrayList<>();
        String sql = "SELECT * FROM interview_feedback WHERE interview_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, interviewId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InterviewFeedback f = new InterviewFeedback();
                    f.setId(rs.getLong("id"));
                    f.setInterviewId(rs.getLong("interview_id"));
                    f.setRecruiterId(rs.getLong("recruiter_id"));
                    int score = rs.getInt("overall_score");
                    f.setOverallScore(rs.wasNull() ? null : score);
                    f.setDecision(rs.getString("decision"));
                    f.setComment(rs.getString("comment"));
                    list.add(f);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching feedback by interview_id: " + e.getMessage());
        }

        return list;
    }

    public static boolean existsForInterview(Long interviewId) {
        String sql = "SELECT 1 FROM interview_feedback WHERE interview_id = ? LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setLong(1, interviewId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking feedback existence: " + e.getMessage());
            return false;
        }
    }
}
