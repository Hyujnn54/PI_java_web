package Services;

import entities.RoleEnum;
import entities.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private Connection connection;

    public UserService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private void checkConnection() throws SQLException {
        if (connection == null) {
            throw new SQLException("Pas de connexion à la base de données. Vérifiez db.properties.");
        }
    }

    public void add(User user) throws SQLException {
        String query = "INSERT INTO users (email, password, first_name, last_name, phone, role, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getPassword());
        ps.setString(3, user.getFirstName());
        ps.setString(4, user.getLastName());
        ps.setString(5, user.getPhone());
        ps.setString(6, user.getRole().toString());
        ps.setBoolean(7, user.isActive());
        ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));
        ps.executeUpdate();

        ResultSet rs = ps.getGeneratedKeys();
        if (rs.next()) {
            user.setId(rs.getLong(1));
        }
    }

    public User login(String email, String password) throws SQLException {
        checkConnection();
        String query = "SELECT * FROM users WHERE email = ? AND password = ? AND is_active = 1";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, email);
        ps.setString(2, password);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            user.setPhone(rs.getString("phone"));
            user.setRole(RoleEnum.valueOf(rs.getString("role")));
            user.setActive(rs.getBoolean("is_active"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return user;
        }
        return null; // Login failed
    }

    public User getById(long id) throws SQLException {
        String query = "SELECT * FROM users WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setLong(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            user.setPhone(rs.getString("phone"));
            user.setRole(RoleEnum.valueOf(rs.getString("role")));
            user.setActive(rs.getBoolean("is_active"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            return user;
        }
        return null;
    }

    public List<User> getAll() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM users";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setFirstName(rs.getString("first_name"));
            user.setLastName(rs.getString("last_name"));
            user.setPhone(rs.getString("phone"));
            user.setRole(RoleEnum.valueOf(rs.getString("role")));
            user.setActive(rs.getBoolean("is_active"));
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            users.add(user);
        }
        return users;
    }
    
    public void update(User user) throws SQLException {
        checkConnection();
        String query = "UPDATE users SET email=?, first_name=?, last_name=?, phone=?, role=?, is_active=? WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(query);
        ps.setString(1, user.getEmail());
        ps.setString(2, user.getFirstName());
        ps.setString(3, user.getLastName());
        ps.setString(4, user.getPhone());
        ps.setString(5, user.getRole().toString());
        ps.setBoolean(6, user.isActive());
        ps.setLong(7, user.getId());
        ps.executeUpdate();
    }
}
