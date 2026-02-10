package Services;

import Models.InterviewFeedback;
import Utils.MyDatabase;

import java.sql.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InterviewFeedbackService {

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    public static void addFeedback(InterviewFeedback f) {
        String sql = "INSERT INTO interview_feedback(interview_id, recruiter_id, technical_score, communication_score, culture_fit_score, overall_score, decision, comment) VALUES (?,?,?,?,?,?,?,?)";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, f.getInterviewId());
            ps.setInt(2, f.getRecruiterId());
            ps.setInt(3, f.getTechnicalScore());
            ps.setInt(4, f.getCommunicationScore());
            ps.setInt(5, f.getCultureFitScore());
            ps.setInt(6, f.getOverallScore());
            ps.setString(7, f.getDecision());
            ps.setString(8, f.getComment());
            ps.executeUpdate();
            System.out.println("Feedback added");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<InterviewFeedback> getAll() {
        List<InterviewFeedback> list = new ArrayList<>();
        String sql = "SELECT * FROM interview_feedback";

        try (Statement st = getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                InterviewFeedback f = new InterviewFeedback();
                f.setId(rs.getInt("id"));
                f.setInterviewId(rs.getInt("interview_id"));
                f.setRecruiterId(rs.getInt("recruiter_id"));
                f.setTechnicalScore(rs.getInt("technical_score"));
                f.setCommunicationScore(rs.getInt("communication_score"));
                f.setCultureFitScore(rs.getInt("culture_fit_score"));
                f.setOverallScore(rs.getInt("overall_score"));
                f.setDecision(rs.getString("decision"));
                f.setComment(rs.getString("comment"));
                list.add(f);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return list;
    }

    public static void updateFeedback(int id, InterviewFeedback f) {
        String sql = "UPDATE interview_feedback SET interview_id=?, recruiter_id=?, technical_score=?, communication_score=?, culture_fit_score=?, overall_score=?, decision=?, comment=? WHERE id=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, f.getInterviewId());
            ps.setInt(2, f.getRecruiterId());
            ps.setInt(3, f.getTechnicalScore());
            ps.setInt(4, f.getCommunicationScore());
            ps.setInt(5, f.getCultureFitScore());
            ps.setInt(6, f.getOverallScore());
            ps.setString(7, f.getDecision());
            ps.setString(8, f.getComment());
            ps.setInt(9, id);
            ps.executeUpdate();
            System.out.println("Feedback updated");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void deleteFeedback(int id) {
        String sql = "DELETE FROM interview_feedback WHERE id=?";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("Feedback deleted");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<InterviewFeedback> getByInterviewId(int interviewId) {
        List<InterviewFeedback> list = new ArrayList<>();
        String sql = "SELECT * FROM interview_feedback WHERE interview_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, interviewId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InterviewFeedback f = new InterviewFeedback();
                    f.setId(rs.getInt("id"));
                    f.setInterviewId(rs.getInt("interview_id"));
                    f.setRecruiterId(rs.getInt("recruiter_id"));
                    f.setTechnicalScore(rs.getInt("technical_score"));
                    f.setCommunicationScore(rs.getInt("communication_score"));
                    f.setCultureFitScore(rs.getInt("culture_fit_score"));
                    f.setOverallScore(rs.getInt("overall_score"));
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

    public static boolean existsForInterview(int interviewId) {
        String sql = "SELECT 1 FROM interview_feedback WHERE interview_id = ? LIMIT 1";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, interviewId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking feedback existence: " + e.getMessage());
            return false;
        }
    }

    public static InterviewFeedback getById(int id) {
        String sql = "SELECT * FROM interview_feedback WHERE id = ?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    InterviewFeedback f = new InterviewFeedback();
                    f.setId(rs.getInt("id"));
                    f.setInterviewId(rs.getInt("interview_id"));
                    f.setRecruiterId(rs.getInt("recruiter_id"));
                    f.setTechnicalScore(rs.getInt("technical_score"));
                    f.setCommunicationScore(rs.getInt("communication_score"));
                    f.setCultureFitScore(rs.getInt("culture_fit_score"));
                    f.setOverallScore(rs.getInt("overall_score"));
                    f.setDecision(rs.getString("decision"));
                    f.setComment(rs.getString("comment"));
                    return f;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching feedback by id: " + e.getMessage());
        }
        return null;
    }

    // Optional convenience for dialogs: create or update depending on id.
    public static void save(InterviewFeedback f) {
        if (f.getId() > 0) {
            updateFeedback(f.getId(), f);
        } else {
            addFeedback(f);
        }
    }
}
