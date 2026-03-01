package Services.events;

import Models.events.EventCandidate;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidateService {

    private Connection conn() {
        return MyDatabase.getInstance().getConnection();
    }

    public CandidateService() {
    }

    public void add(EventCandidate candidate) throws SQLException {
        String sql = "INSERT INTO candidate (id, user_id, location, education_level, experience_years, cv_path) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setLong(1, candidate.getId());
        ps.setLong(2, candidate.getId());
        ps.setString(3, candidate.getLocation());
        ps.setString(4, candidate.getEducationLevel());
        ps.setInt(5, candidate.getExperienceYears());
        ps.setString(6, candidate.getCvPath());
        ps.executeUpdate();
    }

    public EventCandidate getByUserId(long userId) throws SQLException {
        String sql = "SELECT * FROM candidate WHERE id = ?";
        PreparedStatement ps = conn().prepareStatement(sql);
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
        return null;
    }

    public List<EventCandidate> getAll() throws SQLException {
        String sql = "SELECT * FROM candidate";
        List<EventCandidate> list = new ArrayList<>();
        Statement stmt = conn().createStatement();
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
