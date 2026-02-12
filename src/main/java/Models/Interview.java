package Models;

import java.time.LocalDateTime;

public class Interview {

    private Long id;
    private Long applicationId;
    private Long recruiterId;
    private LocalDateTime scheduledAt;
    private int durationMinutes;
    private String mode;
    private String status;
    private String meetingLink;
    private String location;
    private String notes;

    public Interview() {}

    public Interview(Long applicationId, Long recruiterId,
                     LocalDateTime scheduledAt, int durationMinutes, String mode) {
        this.applicationId = applicationId;
        this.recruiterId = recruiterId;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.mode = mode;
        this.status = "SCHEDULED";
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public Long getRecruiterId() { return recruiterId; }
    public void setRecruiterId(Long recruiterId) { this.recruiterId = recruiterId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMeetingLink() {
        return meetingLink;
    }

    public void setMeetingLink(String meetingLink) {
        this.meetingLink = meetingLink;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
