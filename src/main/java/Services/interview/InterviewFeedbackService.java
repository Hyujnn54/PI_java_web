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
            System.out.println("[FeedbackService] Feedback added");
        } catch (SQLException e) {
            System.err.println("Error adding feedback: " + e.getMessage());
            throw new RuntimeException("Failed to add feedback: " + e.getMessage(), e);
        }

        // Send acceptance email via Brevo if candidate is accepted
        if ("ACCEPTED".equals(f.getDecision())) {
            sendAcceptanceEmailAsync(f.getInterviewId());
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

        // Send acceptance email via Brevo if candidate is now accepted
        if ("ACCEPTED".equals(f.getDecision())) {
            sendAcceptanceEmailAsync(f.getInterviewId());
        }
    }

    // ── Acceptance email (Brevo via InterviewEmailService) ───────────────────

    /**
     * Looks up the candidate's contact + job offer details from DB,
     * then sends the acceptance notification via Brevo on a background thread.
     *
     * DB path:
     *   interview.id → interview.application_id
     *   → job_application.candidate_id → users (email, first_name, last_name)
     *   → job_application.offer_id     → job_offer (title, location, contract_type, description)
     */
    private static void sendAcceptanceEmailAsync(Long interviewId) {
        new Thread(() -> {
            try {
                String q = "SELECT u.email, u.first_name, u.last_name, " +
                           "jo.title, jo.location, jo.contract_type, jo.description " +
                           "FROM interview i " +
                           "JOIN job_application ja ON i.application_id = ja.id " +
                           "JOIN users u ON ja.candidate_id = u.id " +
                           "JOIN job_offer jo ON ja.offer_id = jo.id " +
                           "WHERE i.id = ?";
                try (PreparedStatement ps = getConnection().prepareStatement(q)) {
                    ps.setLong(1, interviewId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.err.println("[FeedbackService] Acceptance email: no data for interview #" + interviewId);
                            return;
                        }
                        String email       = rs.getString("email");
                        String firstName   = rs.getString("first_name");
                        String lastName    = rs.getString("last_name");
                        String fullName    = (firstName + " " + lastName).trim();
                        String jobTitle    = rs.getString("title");
                        String location    = rs.getString("location");
                        String contract    = rs.getString("contract_type");
                        String description = rs.getString("description");

                        boolean sent = InterviewEmailService.sendAcceptanceNotification(
                                email, fullName, jobTitle, location, contract, description);
                        if (sent)
                            System.out.println("[FeedbackService] Acceptance email sent to " + email);
                        else
                            System.err.println("[FeedbackService] Acceptance email failed for " + email);
                    }
                }
            } catch (Exception ex) {
                System.err.println("[FeedbackService] Acceptance email exception: " + ex.getMessage());
            }
        }, "acceptance-email").start();
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

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
