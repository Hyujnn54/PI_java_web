package Models.events;

import java.time.LocalDateTime;

public class EventReview {
    private long id;
    private long eventId;
    private long candidateId;
    private int rating; // 1 to 5
    private String comment;
    private LocalDateTime createdAt;
    // denormalized for display
    private String candidateName;

    public EventReview() {}

    public EventReview(long eventId, long candidateId, int rating, String comment) {
        this.eventId = eventId;
        this.candidateId = candidateId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getEventId() { return eventId; }
    public void setEventId(long eventId) { this.eventId = eventId; }
    public long getCandidateId() { return candidateId; }
    public void setCandidateId(long candidateId) { this.candidateId = candidateId; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
}

