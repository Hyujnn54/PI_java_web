package services;

import entities.Recruiter;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecruiterService {
    private Connection connection;

    public RecruiterService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Pas de connexion à la base de données. Vérifiez db.properties.");
        }
    }

    public void add(Recruiter recruiter) throws SQLException {
        String sql = "INSERT INTO recruiter (id, user_id, company_name, company_location, company_description) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, recruiter.getId());
        ps.setLong(2, recruiter.getId()); // Shared PK pattern
        ps.setString(3, recruiter.getCompanyName());
        ps.setString(4, recruiter.getCompanyLocation());
        ps.setString(5, recruiter.getCompanyDescription());
        ps.executeUpdate();
    }

    public Recruiter getByUserId(long userId) throws SQLException {
        checkConnection();
        String sql = "SELECT * FROM recruiter WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setLong(1, userId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Recruiter r = new Recruiter();
            r.setId(rs.getLong("id"));
            r.setCompanyName(rs.getString("company_name"));
            r.setCompanyLocation(rs.getString("company_location"));
            r.setCompanyDescription(rs.getString("company_description"));
            return r;
        }
        return null;
    }

    public List<Recruiter> getAll() throws SQLException {
        String sql = "SELECT * FROM recruiter";
        List<Recruiter> list = new ArrayList<>();
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            Recruiter r = new Recruiter();
            r.setId(rs.getLong("id"));
            r.setCompanyName(rs.getString("company_name"));
            r.setCompanyLocation(rs.getString("company_location"));
            r.setCompanyDescription(rs.getString("company_description"));
            list.add(r);
        }
        return list;
    }
}
