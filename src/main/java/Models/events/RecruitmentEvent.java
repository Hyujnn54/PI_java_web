package Models.events;

import java.time.LocalDateTime;

public class RecruitmentEvent {
    private long id;
    private long recruiterId;
    private String title;
    private String description;
    private String eventType;
    private String location;
    private LocalDateTime eventDate;
    private int capacity;
    private String meetLink;
    private LocalDateTime createdAt;
    
    // Optional: Recruiter object for display purposes
    private Recruiter recruiter;

    // Default constructor
    public RecruitmentEvent() {
    }

    // Full constructor
    public RecruitmentEvent(long id, long recruiterId, String title, String description, 
                           String eventType, String location, LocalDateTime eventDate, 
                           int capacity, LocalDateTime createdAt) {
        this.id = id;
        this.recruiterId = recruiterId;
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.location = location;
        this.eventDate = eventDate;
        this.capacity = capacity;
        this.createdAt = createdAt;
    }

    // Constructor without ID (for creation)
    public RecruitmentEvent(long recruiterId, String title, String description, 
                           String eventType, String location, LocalDateTime eventDate, 
                           int capacity, LocalDateTime createdAt) {
        this.recruiterId = recruiterId;
        this.title = title;
        this.description = description;
        this.eventType = eventType;
        this.location = location;
        this.eventDate = eventDate;
        this.capacity = capacity;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(long recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public String getMeetLink() { return meetLink; }
    public void setMeetLink(String meetLink) { this.meetLink = meetLink; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Recruiter getRecruiter() {
        return recruiter;
    }

    public void setRecruiter(Recruiter recruiter) {
        this.recruiter = recruiter;
    }

    @Override
    public String toString() {
        return "RecruitmentEvent{" +
                "id=" + id +
                ", recruiterId=" + recruiterId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", eventType='" + eventType + '\'' +
                ", location='" + location + '\'' +
                ", eventDate=" + eventDate +
                ", capacity=" + capacity +
                ", createdAt=" + createdAt +
                '}';
    }
}
