package Services.user;

import Utils.MyDatabase;
// Utils.PasswordUtil used inline below

import java.sql.*;

public class ProfileService {

    private Connection cnx() { return MyDatabase.getInstance().getConnection(); }

    // DTOs
    public record UserProfile(long id, String email, String firstName, String phone) {}
    public record RecruiterInfo(String companyName, String companyLocation) {}
    public record CandidateInfo(String location, String educationLevel, Integer experienceYears, String cvPath) {}

    public UserProfile getUserProfile(long userId) throws SQLException {
        String sql = "SELECT id, email, first_name, phone FROM users WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("User not found");
                return new UserProfile(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("first_name"),
                        rs.getString("phone")
                );
            }
        }
    }

    public RecruiterInfo getRecruiterInfo(long userId) throws SQLException {
        String sql = "SELECT company_name, company_location FROM recruiter WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new RecruiterInfo("", "");
                return new RecruiterInfo(rs.getString("company_name"), rs.getString("company_location"));
            }
        }
    }

    public CandidateInfo getCandidateInfo(long userId) throws SQLException {
        // ⚠️ If your candidate table columns differ, change them here
        String sql = "SELECT location, education_level, experience_years, cv_path FROM candidate WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return new CandidateInfo("", "", null, "");
                Integer years = (Integer) rs.getObject("experience_years");
                return new CandidateInfo(
                        rs.getString("location"),
                        rs.getString("education_level"),
                        years,
                        rs.getString("cv_path")
                );
            }
        }
    }

    public void updateUserCore(long userId, String first, String phone, String newPasswordOrNull) throws SQLException {
        String sql = (newPasswordOrNull == null)
                ? "UPDATE users SET first_name=?, phone=? WHERE id=?"
                : "UPDATE users SET first_name=?, phone=?, password=? WHERE id=?";

        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, first);
            ps.setString(i++, phone);

            if (newPasswordOrNull != null) {
                String hashed = Utils.PasswordUtil.hash(newPasswordOrNull);
                ps.setString(i++, hashed);
            }

            ps.setLong(i, userId);
            ps.executeUpdate();
        }
    }

    public void updateRecruiterInfo(long userId, String companyName, String companyLocation) throws SQLException {
        String sql = "UPDATE recruiter SET company_name=?, company_location=? WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, companyName);
            ps.setString(2, companyLocation);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void updateCandidateInfo(long userId, String location, String edu, Integer years, String cvPath) throws SQLException {
        String sql = "UPDATE candidate SET location=?, education_level=?, experience_years=?, cv_path=? WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, location);
            ps.setString(2, edu);
            if (years == null) ps.setNull(3, Types.INTEGER);
            else ps.setInt(3, years);
            ps.setString(4, cvPath);
            ps.setLong(5, userId);
            ps.executeUpdate();
        }
    }
}
