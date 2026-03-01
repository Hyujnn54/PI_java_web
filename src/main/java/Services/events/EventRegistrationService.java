package Services.events;

import Models.events.*;
import Utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventRegistrationService {

    private Connection connection;

    public EventRegistrationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Pas de connexion à la base de données. Vérifiez db.properties.");
        }
    }

    public boolean isAlreadyRegistered(long eventId, long candidateId) throws SQLException {
        checkConnection();
        String query = "SELECT COUNT(*) FROM event_registration WHERE event_id = ? AND candidate_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, eventId);
        ps.setLong(2, candidateId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }

    public void apply(EventRegistration registration) throws SQLException {
        checkConnection();

        if (isAlreadyRegistered(registration.getEventId(), registration.getCandidateId())) {
            throw new SQLException("Vous êtes déjà inscrit à cet événement.");
        }

        String query = "INSERT INTO event_registration (event_id, candidate_id, registered_at, attendance_status) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, registration.getEventId());
        ps.setLong(2, registration.getCandidateId());
        ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(4, AttendanceStatusEnum.PENDING.name());
        ps.executeUpdate();
    }

    public void update(EventRegistration registration) throws SQLException {
        checkConnection();
        String query = "UPDATE event_registration SET attendance_status=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, registration.getAttendanceStatus().name());
        ps.setLong(2, registration.getId());
        ps.executeUpdate();
    }

    public void delete(long id) throws SQLException {
        checkConnection();
        String query = "DELETE FROM event_registration WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    public int getConfirmedCount(long eventId) throws SQLException {
        checkConnection();
        String query = "SELECT COUNT(*) FROM event_registration WHERE event_id = ? AND attendance_status = 'CONFIRMED'";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, eventId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    public List<EventRegistration> getAll() throws SQLException {
        checkConnection();
        List<EventRegistration> registrations = new ArrayList<>();
        String query = "SELECT er.*, re.title, re.event_date FROM event_registration er JOIN recruitment_event re ON er.event_id = re.id";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
            EventRegistration reg = new EventRegistration();
            reg.setId(rs.getLong("id"));
            reg.setEventId(rs.getLong("event_id"));
            reg.setCandidateId(rs.getLong("candidate_id"));
            reg.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
            reg.setAttendanceStatus(safeParseStatus(rs.getString("attendance_status")));

            RecruitmentEvent event = new RecruitmentEvent();
            event.setId(rs.getLong("event_id"));
            event.setTitle(rs.getString("title"));
            event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
            reg.setEvent(event);

            registrations.add(reg);
        }
        return registrations;
    }

    public List<EventRegistration> getByCandidate(long candidateId) throws SQLException {
        checkConnection();
        List<EventRegistration> registrations = new ArrayList<>();
        String query = "SELECT er.*, re.title, re.event_date, re.location, re.description FROM event_registration er JOIN recruitment_event re ON er.event_id = re.id WHERE er.candidate_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, candidateId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            EventRegistration reg = new EventRegistration();
            reg.setId(rs.getLong("id"));
            reg.setEventId(rs.getLong("event_id"));
            reg.setCandidateId(rs.getLong("candidate_id"));
            reg.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
            reg.setAttendanceStatus(safeParseStatus(rs.getString("attendance_status")));

            RecruitmentEvent event = new RecruitmentEvent();
            event.setId(rs.getLong("event_id"));
            event.setTitle(rs.getString("title"));
            event.setDescription(rs.getString("description"));
            event.setLocation(rs.getString("location"));
            event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
            reg.setEvent(event);

            registrations.add(reg);
        }
        return registrations;
    }

    public List<EventRegistration> getByEvent(long eventId) throws SQLException {
        checkConnection();
        List<EventRegistration> registrations = new ArrayList<>();
        // Using LEFT JOINs to be more robust if candidate profile is incomplete
        String query = "SELECT er.*, u.first_name, u.last_name, u.email, re.title as event_title, re.event_date FROM event_registration er " +
                "JOIN recruitment_event re ON er.event_id = re.id " +
                "LEFT JOIN candidate c ON er.candidate_id = c.id " +
                "JOIN users u ON (er.candidate_id = u.id OR c.user_id = u.id) " +
                "WHERE er.event_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, eventId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            registrations.add(mapResultSetToRegistration(rs));
        }
        return registrations;
    }

    public List<EventRegistration> getByRecruiter(long recruiterId) throws SQLException {
        checkConnection();
        List<EventRegistration> registrations = new ArrayList<>();
        String query = "SELECT er.*, u.first_name, u.last_name, u.email, re.title as event_title, re.event_date FROM event_registration er " +
                "JOIN recruitment_event re ON er.event_id = re.id " +
                "LEFT JOIN candidate c ON er.candidate_id = c.id " +
                "JOIN users u ON (er.candidate_id = u.id OR c.user_id = u.id) " +
                "WHERE re.recruiter_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, recruiterId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            registrations.add(mapResultSetToRegistration(rs));
        }
        return registrations;
    }

    private EventRegistration mapResultSetToRegistration(ResultSet rs) throws SQLException {
        EventRegistration reg = new EventRegistration();
        reg.setId(rs.getLong("id"));
        reg.setEventId(rs.getLong("event_id"));
        reg.setCandidateId(rs.getLong("candidate_id"));
        reg.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
        reg.setAttendanceStatus(safeParseStatus(rs.getString("attendance_status")));

        // Optional columns (from JOINS)
        try { reg.setFirstName(rs.getString("first_name")); } catch (SQLException ignored) {}
        try { reg.setLastName(rs.getString("last_name")); } catch (SQLException ignored) {}
        try { reg.setEmail(rs.getString("email")); } catch (SQLException ignored) {}
        if (reg.getFirstName() != null || reg.getLastName() != null) {
            reg.setCandidateName(
                (reg.getFirstName() != null ? reg.getFirstName() : "") + " " +
                (reg.getLastName()  != null ? reg.getLastName()  : ""));
        }
        try {
            RecruitmentEvent event = new RecruitmentEvent();
            event.setId(reg.getEventId());
            event.setTitle(rs.getString("event_title"));
            try {
                if (rs.getTimestamp("event_date") != null) {
                    event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
                }
            } catch (SQLException ignored) {}
            reg.setEvent(event);
        } catch (SQLException ignored) {}

        Candidate cand = new Candidate();
        cand.setId(rs.getLong("candidate_id"));
        reg.setCandidate(cand);

        return reg;
    }

    private AttendanceStatusEnum safeParseStatus(String value) {
        if (value == null || value.isBlank()) return AttendanceStatusEnum.PENDING;
        try {
            return AttendanceStatusEnum.valueOf(value.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            return AttendanceStatusEnum.PENDING;
        }
    }

    /**
     * Get the distinct event types that a candidate has registered for in the past.
     * Used for the recommendation system.
     */
    public java.util.List<String> getEventTypesForCandidate(long candidateId) throws SQLException {
        java.util.List<String> types = new java.util.ArrayList<>();
        String sql = "SELECT DISTINCT re.event_type FROM event_registration er "
                   + "JOIN recruitment_event re ON er.event_id = re.id "
                   + "WHERE er.candidate_id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, candidateId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String t = rs.getString("event_type");
            if (t != null && !t.isBlank()) types.add(t);
        }
        return types;
    }
}
