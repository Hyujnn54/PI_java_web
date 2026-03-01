package Services.events;

import Models.events.EventReview;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewService {

    private Connection conn() { return MyDatabase.getInstance().getConnection(); }

    public ReviewService() {}

    public void add(EventReview review) throws SQLException {
        String sql = "INSERT INTO event_review (event_id, candidate_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setLong(1, review.getEventId());
        ps.setLong(2, review.getCandidateId());
        ps.setInt(3, review.getRating());
        ps.setString(4, review.getComment() != null ? review.getComment() : "");
        ps.setTimestamp(5, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.executeUpdate();
    }

    public boolean hasReviewed(long eventId, long candidateId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM event_review WHERE event_id = ? AND candidate_id = ?";
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setLong(1, eventId);
        ps.setLong(2, candidateId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public List<EventReview> getByEvent(long eventId) throws SQLException {
        List<EventReview> list = new ArrayList<>();
        String sql = "SELECT er.*, CONCAT(u.first_name, ' ', u.last_name) AS candidate_name " +
                     "FROM event_review er " +
                     "LEFT JOIN users u ON er.candidate_id = u.id " +
                     "WHERE er.event_id = ? ORDER BY er.created_at DESC";
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setLong(1, eventId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            EventReview r = new EventReview();
            r.setId(rs.getLong("id"));
            r.setEventId(rs.getLong("event_id"));
            r.setCandidateId(rs.getLong("candidate_id"));
            r.setRating(rs.getInt("rating"));
            r.setComment(rs.getString("comment"));
            if (rs.getTimestamp("created_at") != null)
                r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            try { r.setCandidateName(rs.getString("candidate_name")); } catch (SQLException ignored) {}
            list.add(r);
        }
        return list;
    }

    public double getAverageRating(long eventId) throws SQLException {
        String sql = "SELECT AVG(rating) FROM event_review WHERE event_id = ?";
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setLong(1, eventId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            double avg = rs.getDouble(1);
            return rs.wasNull() ? 0 : avg;
        }
        return 0;
    }
}
