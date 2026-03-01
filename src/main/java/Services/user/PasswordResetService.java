package Services.user;

import Utils.MyDatabase;
import Utils.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

public class PasswordResetService {

    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    public String generateCode() {
        int code = 100000 + new Random().nextInt(900000);
        return String.valueOf(code);
    }

    public boolean requestReset(String email) throws Exception {
        Long userId = getUserIdByEmail(email);
        if (userId == null) return false;

        String code = generateCode();
        LocalDateTime expires = LocalDateTime.now().plusMinutes(10);

        String sql = "UPDATE users SET forget_code=?, forget_code_expires=? WHERE id=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setTimestamp(2, Timestamp.valueOf(expires));
            ps.setLong(3, userId);
            ps.executeUpdate();
        }

        new EmailService().sendResetCode(email, code); // javax.mail
        return true;
    }

    public boolean verifyCode(String email, String code) throws SQLException {
        String sql = "SELECT forget_code, forget_code_expires FROM users WHERE email=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                String dbCode = rs.getString("forget_code");
                Timestamp exp = rs.getTimestamp("forget_code_expires");
                if (dbCode == null || exp == null) return false;
                if (!dbCode.equals(code)) return false;
                return exp.toLocalDateTime().isAfter(LocalDateTime.now());
            }
        }
    }

    public boolean resetPassword(String email, String code, String newPasswordPlain) throws SQLException {
        if (!verifyCode(email, code)) return false;

        String newHash = PasswordUtil.hash(newPasswordPlain);

        String sql = "UPDATE users SET password=?, forget_code=NULL, forget_code_expires=NULL WHERE email=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        }
    }

    private Long getUserIdByEmail(String email) throws SQLException {
        try (PreparedStatement ps = cnx().prepareStatement("SELECT id FROM users WHERE email=?")) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getLong("id");
            }
        }
    }
}