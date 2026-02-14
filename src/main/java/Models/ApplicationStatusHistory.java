package Models;

import java.time.LocalDateTime;

public class ApplicationStatusHistory {
    private Long id;
    private Long applicationId;
    private String status;
    private LocalDateTime changedAt;
    private Long changedBy;
    private String note;

    public ApplicationStatusHistory() {
    }

    public ApplicationStatusHistory(Long applicationId, String status, Long changedBy, String note) {
        this.applicationId = applicationId;
        this.status = status;
        this.changedBy = changedBy;
        this.note = note;
        this.changedAt = LocalDateTime.now();
    }

    public ApplicationStatusHistory(Long id, Long applicationId, String status,
                                  LocalDateTime changedAt, Long changedBy, String note) {
        this.id = id;
        this.applicationId = applicationId;
        this.status = status;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "ApplicationStatusHistory{" +
                "id=" + id +
                ", applicationId=" + applicationId +
                ", status='" + status + '\'' +
                ", changedAt=" + changedAt +
                ", changedBy=" + changedBy +
                ", note='" + note + '\'' +
                '}';
    }
}

