package Models;

import java.time.LocalDateTime;

public class JobApplication {
    private Long id;
    private Long offerId;
    private Long candidateId;
    private String phone;
    private String coverLetter;
    private String cvPath;
    private LocalDateTime appliedAt;
    private String currentStatus;

    public JobApplication() {
    }

    public JobApplication(Long offerId, Long candidateId, String phone, String coverLetter, String cvPath) {
        this.offerId = offerId;
        this.candidateId = candidateId;
        this.phone = phone;
        this.coverLetter = coverLetter;
        this.cvPath = cvPath;
        this.currentStatus = "SUBMITTED";
        this.appliedAt = LocalDateTime.now();
    }

    public JobApplication(Long id, Long offerId, Long candidateId, String phone, String coverLetter,
                         String cvPath, LocalDateTime appliedAt, String currentStatus) {
        this.id = id;
        this.offerId = offerId;
        this.candidateId = candidateId;
        this.phone = phone;
        this.coverLetter = coverLetter;
        this.cvPath = cvPath;
        this.appliedAt = appliedAt;
        this.currentStatus = currentStatus;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCoverLetter() {
        return coverLetter;
    }

    public void setCoverLetter(String coverLetter) {
        this.coverLetter = coverLetter;
    }

    public String getCvPath() {
        return cvPath;
    }

    public void setCvPath(String cvPath) {
        this.cvPath = cvPath;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(LocalDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    @Override
    public String toString() {
        return "JobApplication{" +
                "id=" + id +
                ", offerId=" + offerId +
                ", candidateId=" + candidateId +
                ", phone='" + phone + '\'' +
                ", coverLetter='" + (coverLetter != null ? coverLetter.substring(0, Math.min(30, coverLetter.length())) : "null") + '\'' +
                ", cvPath='" + cvPath + '\'' +
                ", appliedAt=" + appliedAt +
                ", currentStatus='" + currentStatus + '\'' +
                '}';
    }
}

