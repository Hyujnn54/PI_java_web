package Models.joboffers;

import java.time.LocalDateTime;

/**
 * Modèle représentant une correction soumise par un recruteur
 * suite à un signalement d'offre
 */
public class WarningCorrection {

    public enum CorrectionStatus {
        PENDING,    // En attente de validation par l'admin
        APPROVED,   // Approuvée par l'admin
        REJECTED    // Rejetée par l'admin
    }

    private Long id;
    private Long warningId;
    private Long jobOfferId;
    private Long recruiterId;
    private String correctionNote;      // Note du recruteur expliquant la correction
    private String oldTitle;            // Ancien titre (avant correction)
    private String newTitle;            // Nouveau titre (après correction)
    private String oldDescription;      // Ancienne description
    private String newDescription;      // Nouvelle description
    private CorrectionStatus status;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private String adminNote;           // Note de l'admin lors de la revue

    public WarningCorrection() {
        this.status = CorrectionStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
    }

    public WarningCorrection(Long warningId, Long jobOfferId, Long recruiterId, String correctionNote) {
        this.warningId = warningId;
        this.jobOfferId = jobOfferId;
        this.recruiterId = recruiterId;
        this.correctionNote = correctionNote;
        this.status = CorrectionStatus.PENDING;
        this.submittedAt = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWarningId() {
        return warningId;
    }

    public void setWarningId(Long warningId) {
        this.warningId = warningId;
    }

    public Long getJobOfferId() {
        return jobOfferId;
    }

    public void setJobOfferId(Long jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    public Long getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(Long recruiterId) {
        this.recruiterId = recruiterId;
    }

    public String getCorrectionNote() {
        return correctionNote;
    }

    public void setCorrectionNote(String correctionNote) {
        this.correctionNote = correctionNote;
    }

    public String getOldTitle() {
        return oldTitle;
    }

    public void setOldTitle(String oldTitle) {
        this.oldTitle = oldTitle;
    }

    public String getNewTitle() {
        return newTitle;
    }

    public void setNewTitle(String newTitle) {
        this.newTitle = newTitle;
    }

    public String getOldDescription() {
        return oldDescription;
    }

    public void setOldDescription(String oldDescription) {
        this.oldDescription = oldDescription;
    }

    public String getNewDescription() {
        return newDescription;
    }

    public void setNewDescription(String newDescription) {
        this.newDescription = newDescription;
    }

    public CorrectionStatus getStatus() {
        return status;
    }

    public void setStatus(CorrectionStatus status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getAdminNote() {
        return adminNote;
    }

    public void setAdminNote(String adminNote) {
        this.adminNote = adminNote;
    }

    public String getStatusLabel() {
        return switch (status) {
            case PENDING -> "En attente de validation";
            case APPROVED -> "Approuvée";
            case REJECTED -> "Rejetée";
        };
    }
}

