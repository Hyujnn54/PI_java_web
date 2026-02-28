package Models.joboffers;

import java.time.LocalDateTime;

/**
 * Modèle représentant un avertissement/signalement envoyé par l'admin
 * concernant une offre d'emploi
 */
public class JobOfferWarning {

    public enum WarningStatus {
        SENT,       // Envoyé (en attente que le recruteur le voie)
        SEEN,       // Vu par le recruteur
        RESOLVED,   // Résolu par le recruteur
        DISMISSED   // Rejeté/Annulé
    }

    private Long id;
    private Long jobOfferId;
    private Long recruiterId;
    private Long adminId;
    private String reason;
    private String message;
    private WarningStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime seenAt;
    private LocalDateTime resolvedAt;

    // Constructeurs
    public JobOfferWarning() {
        this.status = WarningStatus.SENT;
        this.createdAt = LocalDateTime.now();
    }

    public JobOfferWarning(Long jobOfferId, Long recruiterId, Long adminId, String reason, String message) {
        this.jobOfferId = jobOfferId;
        this.recruiterId = recruiterId;
        this.adminId = adminId;
        this.reason = reason;
        this.message = message;
        this.status = WarningStatus.SENT;
        this.createdAt = LocalDateTime.now();
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public WarningStatus getStatus() {
        return status;
    }

    public void setStatus(WarningStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSeenAt() {
        return seenAt;
    }

    public void setSeenAt(LocalDateTime seenAt) {
        this.seenAt = seenAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    /**
     * Retourne le libellé français du statut
     */
    public String getStatusLabel() {
        return switch (status) {
            case SENT -> "Envoyé";
            case SEEN -> "Vu";
            case RESOLVED -> "Résolu";
            case DISMISSED -> "Annulé";
        };
    }

    @Override
    public String toString() {
        return "JobOfferWarning{" +
                "id=" + id +
                ", jobOfferId=" + jobOfferId +
                ", reason='" + reason + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}


