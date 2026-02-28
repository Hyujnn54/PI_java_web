package Models.joboffers;

import java.time.LocalDateTime;

public class JobOffer {
    private Long id;
    private Long recruiterId;
    private String title;
    private String description;
    private String location;
    private Double latitude;
    private Double longitude;
    private ContractType contractType;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
    private Status status;
    private boolean isFlagged;
    private LocalDateTime flaggedAt;


    // Constructors
    public JobOffer() {
        this.createdAt = LocalDateTime.now();
        this.status = Status.OPEN;
        this.isFlagged = false;
    }

    public JobOffer(Long id, Long recruiterId, String title, String description, String location,
                    ContractType contractType, LocalDateTime createdAt, LocalDateTime deadline, Status status) {
        this.id = id;
        this.recruiterId = recruiterId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.contractType = contractType;
        this.createdAt = createdAt;
        this.deadline = deadline;
        this.status = status;
        this.isFlagged = false;
    }

    public JobOffer(Long id, Long recruiterId, String title, String description, String location,
                    ContractType contractType, LocalDateTime createdAt, LocalDateTime deadline, Status status,
                    boolean isFlagged, LocalDateTime flaggedAt) {
        this.id = id;
        this.recruiterId = recruiterId;
        this.title = title;
        this.description = description;
        this.location = location;
        this.contractType = contractType;
        this.createdAt = createdAt;
        this.deadline = deadline;
        this.status = status;
        this.isFlagged = isFlagged;
        this.flaggedAt = flaggedAt;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecruiterId() {
        return recruiterId;
    }

    public void setRecruiterId(Long recruiterId) {
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDateTime deadline) {
        this.deadline = deadline;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public boolean isFlagged() {
        return isFlagged;
    }

    public void setFlagged(boolean flagged) {
        isFlagged = flagged;
    }

    public LocalDateTime getFlaggedAt() {
        return flaggedAt;
    }

    public void setFlaggedAt(LocalDateTime flaggedAt) {
        this.flaggedAt = flaggedAt;
    }

    @Override
    public String toString() {
        return "JobOffer{" +
                "id=" + id +
                ", recruiterId=" + recruiterId +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", contractType=" + contractType +
                ", status=" + status +
                ", isFlagged=" + isFlagged +
                '}';
    }
}

