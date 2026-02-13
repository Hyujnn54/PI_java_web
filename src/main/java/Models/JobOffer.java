package Models;

import java.time.LocalDateTime;

public class JobOffer {
    private Long id;
    private Long recruiterId;
    private String title;
    private String description;
    private String location;
    private ContractType contractType;
    private LocalDateTime createdAt;
    private LocalDateTime deadline;
    private Status status;

    public enum ContractType {
        CDI, CDD, INTERNSHIP, FREELANCE, PART_TIME, FULL_TIME
    }

    public enum Status {
        OPEN, CLOSED
    }

    // Constructors
    public JobOffer() {
        this.createdAt = LocalDateTime.now();
        this.status = Status.OPEN;
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
    }

    // Getters and Setters
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

    @Override
    public String toString() {
        return "JobOffer{" +
                "id=" + id +
                ", recruiterId=" + recruiterId +
                ", title='" + title + '\'' +
                ", location='" + location + '\'' +
                ", contractType=" + contractType +
                ", status=" + status +
                '}';
    }
}

