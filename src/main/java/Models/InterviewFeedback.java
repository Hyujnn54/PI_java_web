package Models;

public class InterviewFeedback {

    private Long id;
    private Long interviewId;
    private Long recruiterId;
    private Integer overallScore;
    private String decision;
    private String comment;

    public InterviewFeedback() {}

    public InterviewFeedback(Long interviewId, Long recruiterId,
                             Integer overallScore, String decision, String comment) {
        this.interviewId = interviewId;
        this.recruiterId = recruiterId;
        this.overallScore = overallScore;
        this.decision = decision;
        this.comment = comment;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInterviewId() { return interviewId; }
    public void setInterviewId(Long interviewId) { this.interviewId = interviewId; }

    public Long getRecruiterId() { return recruiterId; }
    public void setRecruiterId(Long recruiterId) { this.recruiterId = recruiterId; }

    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
