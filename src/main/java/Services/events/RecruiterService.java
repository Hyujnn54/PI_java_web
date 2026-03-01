package Services.events;

import Models.events.EventRecruiter;
import Utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecruiterService {
    private Connection connection;

    public RecruiterService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
        if (connection == null) {
            throw new SQLException("Pas de connexion à la base de données. Vérifiez db.properties.");
        }
    }

    public void add(EventRecruiter recruiter) throws SQLException {
        checkConnection();
        String sql = "INSERT INTO recruiter (id, user_id, company_name, company_location, company_description) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, recruiter.getId());
        ps.setLong(2, recruiter.getId()); // Shared PK pattern
        ps.setString(3, recruiter.getCompanyName());
        ps.setString(4, recruiter.getCompanyLocation());
        ps.setString(5, recruiter.getCompanyDescription());
        ps.executeUpdate();
    }

    public EventRecruiter getByUserId(long userId) throws SQLException {
        checkConnection();
        String sql = "SELECT * FROM recruiter WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            EventRecruiter r = new EventRecruiter();
            r.setId(rs.getLong("id"));
            r.setCompanyName(rs.getString("company_name"));
            r.setCompanyLocation(rs.getString("company_location"));
            r.setCompanyDescription(rs.getString("company_description"));
            return r;
        }
        return null;
    }

    public List<EventRecruiter> getAll() throws SQLException {
        checkConnection();
        String sql = "SELECT * FROM recruiter";
        List<EventRecruiter> list = new ArrayList<>();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            EventRecruiter r = new EventRecruiter();
            r.setId(rs.getLong("id"));
            r.setCompanyName(rs.getString("company_name"));
            r.setCompanyLocation(rs.getString("company_location"));
            r.setCompanyDescription(rs.getString("company_description"));
            list.add(r);
        }
        return list;
    }
}
