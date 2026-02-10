package Models;

import java.time.LocalDateTime;

public class Interview {

    private int id;
    private int applicationId;
    private int recruiterId;
    private LocalDateTime scheduledAt;
    private int durationMinutes;
    private String mode;
    private String status;

    public Interview() {}

    public Interview(int applicationId, int recruiterId,
                     LocalDateTime scheduledAt, int durationMinutes, String mode) {
        this.applicationId = applicationId;
        this.recruiterId = recruiterId;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.mode = mode;
        this.status = "SCHEDULED";
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getApplicationId() { return applicationId; }
    public void setApplicationId(int applicationId) { this.applicationId = applicationId; }

    public int getRecruiterId() { return recruiterId; }
    public void setRecruiterId(int recruiterId) { this.recruiterId = recruiterId; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
