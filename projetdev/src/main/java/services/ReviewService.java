package services;

import entities.EventReview;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewService {

    private Connection connection;

    public ReviewService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void add(EventReview review) throws SQLException {
        String query = "INSERT INTO event_review (event_id, candidate_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, review.getEventId());
        ps.setLong(2, review.getCandidateId());
        ps.setInt(3, review.getRating());
        ps.setString(4, review.getComment());
        ps.setTimestamp(5, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.executeUpdate();
    }

    public List<EventReview> getByEvent(long eventId) throws SQLException {
        List<EventReview> reviews = new ArrayList<>();
        String query = "SELECT * FROM event_review WHERE event_id = ? ORDER BY created_at DESC";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, eventId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            EventReview review = new EventReview();
            review.setId(rs.getLong("id"));
            review.setEventId(rs.getLong("event_id"));
            review.setCandidateId(rs.getLong("candidate_id"));
            review.setRating(rs.getInt("rating"));
            review.setComment(rs.getString("comment"));
            review.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            reviews.add(review);
        }
        return reviews;
    }

    public boolean hasCandidateReviewed(long eventId, long candidateId) throws SQLException {
        String query = "SELECT COUNT(*) FROM event_review WHERE event_id = ? AND candidate_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, eventId);
        ps.setLong(2, candidateId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }
}
