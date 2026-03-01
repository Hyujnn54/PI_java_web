package Services.user;

import Utils.MyDatabase;
import Utils.PasswordUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Random;

public class PasswordResetService {

    /** Always get a fresh, valid connection with autoCommit=true. */
    private Connection cnx() throws SQLException {
        Connection c = MyDatabase.getInstance().getConnection();
        // Ensure autoCommit is on — guard against leftover state from other services
        if (!c.getAutoCommit()) {
            try { c.rollback(); } catch (Exception ignored) {}
            c.setAutoCommit(true);
        }
        return c;
    }

    public String generateCode() {
        int code = 100000 + new Random().nextInt(900000);
        return String.valueOf(code);
    }

    public boolean requestReset(String email) throws Exception {
        String code = generateCode();
        LocalDateTime expires = LocalDateTime.now().plusMinutes(10);

        String sql = "UPDATE users SET forget_code=?, forget_code_expires=? WHERE email=?";
        try (PreparedStatement ps = cnx().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setTimestamp(2, Timestamp.valueOf(expires));
            ps.setString(3, email);
            int rows = ps.executeUpdate();
            if (rows == 0) return false; // email not found
        }

        new EmailService().sendResetCode(email, code);
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
                if (!dbCode.trim().equals(code.trim())) return false;
                return exp.toLocalDateTime().isAfter(LocalDateTime.now());
            }
        }
    }

    public boolean resetPassword(String email, String code, String newPasswordPlain) throws SQLException {
        // 1) verify code on a fresh read
        if (!verifyCode(email, code)) {
            System.err.println("[PasswordResetService] resetPassword: invalid or expired code for " + email);
            return false;
        }

        // 2) hash the new password
        String newHash = PasswordUtil.hash(newPasswordPlain);
        System.out.println("[PasswordResetService] Saving new bcrypt hash for: " + email);

        // 3) update — use explicit connection with autoCommit=true
        Connection c = cnx();
        String sql = "UPDATE users SET password=?, forget_code=NULL, forget_code_expires=NULL WHERE email=?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, email);
            int rows = ps.executeUpdate();
            System.out.println("[PasswordResetService] Rows updated: " + rows + " for email: " + email);
            return rows > 0;
        }
    }
}