package Models.events;

import java.time.LocalDateTime;

public class EventRegistration {
    private long id;
    private long eventId; // FK vers recruitment_event
    private long candidateId; // FK vers candidate
    private LocalDateTime registeredAt;
    private AttendanceStatusEnum attendanceStatus;

    private RecruitmentEvent event;
    private EventCandidate candidate;
    private String candidateName; // Full name (fallback)
    private String firstName;
    private String lastName;
    private String email;

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // Constructeur par défaut
    public EventRegistration() {
    }

    // Constructeur complet
    public EventRegistration(long id, long eventId, long candidateId,
            LocalDateTime registeredAt, AttendanceStatusEnum attendanceStatus) {
        this.id = id;
        this.eventId = eventId;
        this.candidateId = candidateId;
        this.registeredAt = registeredAt;
        this.attendanceStatus = attendanceStatus;
    }

    // Constructeur sans ID
    public EventRegistration(long eventId, long candidateId,
            LocalDateTime registeredAt, AttendanceStatusEnum attendanceStatus) {
        this.eventId = eventId;
        this.candidateId = candidateId;
        this.registeredAt = registeredAt;
        this.attendanceStatus = attendanceStatus;
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getEventId() {
        return eventId;
    }

    public void setEventId(long eventId) {
        this.eventId = eventId;
    }

    public long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(long candidateId) {
        this.candidateId = candidateId;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public AttendanceStatusEnum getAttendanceStatus() {
        return attendanceStatus;
    }

    public void setAttendanceStatus(AttendanceStatusEnum attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public RecruitmentEvent getEvent() {
        return event;
    }

    public void setEvent(RecruitmentEvent event) {
        this.event = event;
    }

    public EventCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(EventCandidate candidate) {
        this.candidate = candidate;
    }

    @Override
    public String toString() {
        return "EventRegistration{" +
                "id=" + id +
                ", eventId=" + eventId +
                ", candidateId=" + candidateId +
                ", registeredAt=" + registeredAt +
                ", attendanceStatus=" + attendanceStatus +
                '}';
    }
}
