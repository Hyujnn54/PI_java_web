package Services;

import Utils.MyDatabase;
import java.sql.*;

public class UserService {

    public static class UserInfo {
        public final String firstName;
        public final String lastName;
        public final String email;
        public final String phone;
        public final String educationLevel;
        public final Integer experienceYears;

        public UserInfo(String firstName, String lastName, String email, String phone,
                        String educationLevel, Integer experienceYears) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.educationLevel = educationLevel;
            this.experienceYears = experienceYears;
        }

        public String firstName() { return firstName; }
        public String lastName() { return lastName; }
        public String email() { return email; }
        public String phone() { return phone; }
        public String educationLevel() { return educationLevel; }
        public Integer experienceYears() { return experienceYears; }
    }

    /**
     * Get user information for a candidate
     */
    public static UserInfo getUserInfo(Long userId) {
        try (Connection conn = MyDatabase.getInstance().getConnection()) {
            String query = "SELECT u.first_name, u.last_name, u.email, u.phone, " +
                          "c.education_level, c.experience_years " +
                          "FROM users u " +
                          "LEFT JOIN candidate c ON u.id = c.id " +
                          "WHERE u.id = ? AND u.role = 'CANDIDATE'";

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new UserInfo(
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("education_level"),
                    rs.getObject("experience_years") != null ? rs.getInt("experience_years") : null
                );
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user info: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get recruiter company name
     */
    public static String getRecruiterCompanyName(Long recruiterId) {
        try (Connection conn = MyDatabase.getInstance().getConnection()) {
            String query = "SELECT company_name FROM recruiter WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1, recruiterId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("company_name");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recruiter info: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get candidate skills from database
     */
    public static java.util.List<String> getCandidateSkills(Long candidateId) {
        java.util.List<String> skills = new java.util.ArrayList<>();
        try (Connection conn = MyDatabase.getInstance().getConnection()) {
            String query = "SELECT skill_name, level FROM candidate_skill WHERE candidate_id = ? ORDER BY level DESC, skill_name ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setLong(1, candidateId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String skillName = rs.getString("skill_name");
                String level = rs.getString("level");
                skills.add(skillName + " (" + level + ")");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching candidate skills: " + e.getMessage());
            e.printStackTrace();
        }
        return skills;
    }
}



