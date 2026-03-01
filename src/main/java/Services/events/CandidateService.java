package Services.events;

import Models.events.EventCandidate;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidateService {
    private Connection connection;

    public CandidateService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new SQLException("Pas de connexion à la base de données.");
        }
    }

    public void add(EventCandidate candidate) throws SQLException {
        checkConnection();
        String sql = "INSERT INTO candidate (id, user_id, location, education_level, experience_years, cv_path) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, candidate.getId());
        ps.setLong(2, candidate.getId()); // Shared PK pattern
        ps.setString(3, candidate.getLocation());
        ps.setString(4, candidate.getEducationLevel());
        ps.setInt(5, candidate.getExperienceYears());
        ps.setString(6, candidate.getCvPath());
        ps.executeUpdate();
    }

    public EventCandidate getByUserId(long userId) throws SQLException {
        checkConnection();
        String sql = "SELECT * FROM candidate WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            EventCandidate c = new EventCandidate();
            c.setId(rs.getLong("id"));
            c.setLocation(rs.getString("location"));
            c.setEducationLevel(rs.getString("education_level"));
            c.setExperienceYears(rs.getInt("experience_years"));
            c.setCvPath(rs.getString("cv_path"));
            return c;
        }
        return null; // Retourne null si pas de profil candidat, ce qui est géré par le contrôleur
    }

    public List<EventCandidate> getAll() throws SQLException {
        checkConnection();
        String sql = "SELECT * FROM candidate";
        List<EventCandidate> list = new ArrayList<>();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            EventCandidate c = new EventCandidate();
            c.setId(rs.getLong("id"));
            c.setLocation(rs.getString("location"));
            c.setEducationLevel(rs.getString("education_level"));
            c.setExperienceYears(rs.getInt("experience_years"));
            c.setCvPath(rs.getString("cv_path"));
            list.add(c);
        }
        return list;
    }
}
