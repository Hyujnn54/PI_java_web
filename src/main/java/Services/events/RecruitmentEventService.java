package Services.events;

import Models.events.RecruitmentEvent;
import Models.events.Recruiter;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecruitmentEventService {

    private Connection connection;

    public RecruitmentEventService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Pas de connexion à la base de données. Vérifiez db.properties.");
        }
    }

    public void add(RecruitmentEvent event) throws SQLException {
        checkConnection();
        String query = "INSERT INTO recruitment_event (recruiter_id, title, description, event_type, location, event_date, capacity, meet_link, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setLong(1, event.getRecruiterId());
        ps.setString(2, event.getTitle());
        ps.setString(3, event.getDescription());
        ps.setString(4, event.getEventType());
        ps.setString(5, event.getLocation());
        ps.setTimestamp(6, Timestamp.valueOf(event.getEventDate()));
        ps.setInt(7, event.getCapacity());
        ps.setString(8, event.getMeetLink());
        ps.setTimestamp(9, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        if (keys.next()) event.setId(keys.getLong(1));
    }

    public void update(RecruitmentEvent event) throws SQLException {
        checkConnection();
        String query = "UPDATE recruitment_event SET title=?, description=?, event_type=?, location=?, event_date=?, capacity=?, meet_link=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, event.getTitle());
        ps.setString(2, event.getDescription());
        ps.setString(3, event.getEventType());
        ps.setString(4, event.getLocation());
        ps.setTimestamp(5, Timestamp.valueOf(event.getEventDate()));
        ps.setInt(6, event.getCapacity());
        ps.setString(7, event.getMeetLink());
        ps.setLong(8, event.getId());
        ps.executeUpdate();
    }

    public void delete(long id) throws SQLException {
        String query = "DELETE FROM recruitment_event WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, id);
        ps.executeUpdate();
    }

    public List<RecruitmentEvent> getAll() throws SQLException {
        List<RecruitmentEvent> events = new ArrayList<>();
        String query = "SELECT e.*, r.company_name, r.company_location, r.company_description FROM recruitment_event e JOIN recruiter r ON e.recruiter_id = r.id";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
            RecruitmentEvent event = new RecruitmentEvent();
            event.setId(rs.getLong("id"));
            event.setRecruiterId(rs.getLong("recruiter_id"));
            event.setTitle(rs.getString("title"));
            event.setDescription(rs.getString("description"));
            event.setEventType(rs.getString("event_type"));
            event.setLocation(rs.getString("location"));
            event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
            event.setCapacity(rs.getInt("capacity"));
            event.setMeetLink(rs.getString("meet_link"));
            event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

            Recruiter recruiter = new Recruiter();
            recruiter.setId(rs.getLong("recruiter_id"));
            recruiter.setCompanyName(rs.getString("company_name"));
            recruiter.setCompanyLocation(rs.getString("company_location"));
            event.setRecruiter(recruiter);

            events.add(event);
        }
        return events;
    }

    public List<RecruitmentEvent> getByRecruiter(long recruiterId) throws SQLException {
        List<RecruitmentEvent> events = new ArrayList<>();
        String query = "SELECT * FROM recruitment_event WHERE recruiter_id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, recruiterId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            RecruitmentEvent event = new RecruitmentEvent();
            event.setId(rs.getLong("id"));
            event.setRecruiterId(rs.getLong("recruiter_id"));
            event.setTitle(rs.getString("title"));
            event.setDescription(rs.getString("description"));
            event.setEventType(rs.getString("event_type"));
            event.setLocation(rs.getString("location"));
            event.setEventDate(rs.getTimestamp("event_date").toLocalDateTime());
            event.setCapacity(rs.getInt("capacity"));
            try { event.setMeetLink(rs.getString("meet_link")); } catch (SQLException ignored) {}
            event.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            events.add(event);
        }
        return events;
    }
}
