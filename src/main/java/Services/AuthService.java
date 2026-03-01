package Services;

import Models.*;
import Utils.MyDatabase;
import Utils.PasswordUtil;

import java.sql.*;

public class AuthService {
    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public User login(String email, String password) throws SQLException {

        String sql = """
            SELECT id, email, password, first_name, last_name, phone
            FROM users
            WHERE email=? AND is_active=1
        """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                String storedHash = rs.getString("password");

                // ✅ verify hash
                if (!PasswordUtil.verify(password, storedHash)) return null;

                long id = rs.getLong("id");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String phone = rs.getString("phone");

                // We keep password as hash inside objects (never plain)
                String safePasswordValue = storedHash;

                // 1) ADMIN?
                Admin a = findAdmin(id, email, safePasswordValue, firstName, lastName, phone);
                if (a != null) return a;

                // 2) RECRUITER?
                Recruiter r = findRecruiter(id, email, safePasswordValue, firstName, lastName, phone);
                if (r != null) return r;

                // 3) CANDIDATE?
                Candidate c = findCandidate(id, email, safePasswordValue, firstName, lastName, phone);
                if (c != null) return c;

                // 4) fallback base user
                User u = new User(email, safePasswordValue, firstName, lastName, phone);
                u.setId(id);
                return u;
            }
        }
    }

    private Admin findAdmin(long id, String email, String password,
                            String firstName, String lastName, String phone) throws SQLException {
        String sql = "SELECT assigned_area FROM admin WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Admin a = new Admin(email, password, firstName, lastName, phone, rs.getString("assigned_area"));
                a.setId(id);
                return a;
            }
        }
    }

    private Recruiter findRecruiter(long id, String email, String password,
                                    String firstName, String lastName, String phone) throws SQLException {
        String sql = "SELECT company_name, company_location FROM recruiter WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Recruiter r = new Recruiter(
                        email, password, firstName, lastName, phone,
                        rs.getString("company_name"),
                        rs.getString("company_location")
                );
                r.setId(id);
                return r;
            }
        }
    }

    private Candidate findCandidate(long id, String email, String password,
                                    String firstName, String lastName, String phone) throws SQLException {
        String sql = "SELECT location, education_level, experience_years, cv_path FROM candidate WHERE id=?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Candidate c = new Candidate(
                        email, password, firstName, lastName, phone,
                        rs.getString("location"),
                        rs.getString("education_level"),
                        (Integer) rs.getObject("experience_years"),
                        rs.getString("cv_path")
                );
                c.setId(id);
                return c;
            }
        }
    }

    public User loginWithFacePersonId(String personId) throws SQLException {
        String sql = """
        SELECT id, email, password, first_name, last_name, phone
        FROM users
        WHERE face_enabled=1 AND face_person_id=? AND is_active=1
    """;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, personId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                long id = rs.getLong("id");
                String email = rs.getString("email");
                String storedHash = rs.getString("password");
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String phone = rs.getString("phone");

                Admin a = findAdmin(id, email, storedHash, firstName, lastName, phone);
                if (a != null) return a;

                Recruiter r = findRecruiter(id, email, storedHash, firstName, lastName, phone);
                if (r != null) return r;

                Candidate c = findCandidate(id, email, storedHash, firstName, lastName, phone);
                if (c != null) return c;

                User u = new User(email, storedHash, firstName, lastName, phone);
                u.setId(id);
                return u;
            }
        }
    }
}