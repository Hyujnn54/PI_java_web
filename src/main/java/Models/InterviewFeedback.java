package Models;

public class InterviewFeedback {

    private int id;
    private int interviewId;
    private int recruiterId;
    private int technicalScore;
    private int communicationScore;
    private int cultureFitScore;
    private int overallScore;
    private String decision;
    private String comment;

    public InterviewFeedback() {}

    public InterviewFeedback(int interviewId, int recruiterId,
                             int technicalScore, int communicationScore,
                             int cultureFitScore, String decision, String comment) {
        this.interviewId = interviewId;
        this.recruiterId = recruiterId;
        this.technicalScore = technicalScore;
        this.communicationScore = communicationScore;
        this.cultureFitScore = cultureFitScore;
        this.overallScore = (technicalScore + communicationScore + cultureFitScore) / 3;
        this.decision = decision;
        this.comment = comment;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInterviewId() { return interviewId; }
    public void setInterviewId(int interviewId) { this.interviewId = interviewId; }

    public int getRecruiterId() { return recruiterId; }
    public void setRecruiterId(int recruiterId) { this.recruiterId = recruiterId; }

    public int getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(int technicalScore) { this.technicalScore = technicalScore; }

    public int getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(int communicationScore) { this.communicationScore = communicationScore; }

    public int getCultureFitScore() { return cultureFitScore; }
    public void setCultureFitScore(int cultureFitScore) { this.cultureFitScore = cultureFitScore; }

    public int getOverallScore() { return overallScore; }
    public void setOverallScore(int overallScore) { this.overallScore = overallScore; }

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
