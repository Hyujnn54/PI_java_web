package Models;

import java.time.LocalDateTime;

public class InterviewRescheduleRequest {

    private int id;
    private int interviewId;
    private int candidateId;
    private LocalDateTime requestedDateTime;
    private String reason;
    private String status;

    public InterviewRescheduleRequest() {}

    public InterviewRescheduleRequest(int interviewId, int candidateId,
                                      LocalDateTime requestedDateTime, String reason) {
        this.interviewId = interviewId;
        this.candidateId = candidateId;
        this.requestedDateTime = requestedDateTime;
        this.reason = reason;
        this.status = "PENDING";
    }

    public int getInterviewId() { return interviewId; }
    public void setInterviewId(int interviewId) { this.interviewId = interviewId; }

    public int getCandidateId() { return candidateId; }
    public void setCandidateId(int candidateId) { this.candidateId = candidateId; }

    public LocalDateTime getRequestedDateTime() { return requestedDateTime; }
    public void setRequestedDateTime(LocalDateTime requestedDateTime) { this.requestedDateTime = requestedDateTime; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
