package Services;

import Utils.MyDatabase;
import Utils.UserContext;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing all users in the system.
 * Provides CRUD operations for users across all roles (Recruiter, Candidate, Admin).
 */
public class UserManagementService {

    /**
     * Record representing a user with their details and role information
     */
    public record UserRow(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        Boolean isActive,
        String additionalInfo  // company_name for recruiter or location for candidate
    ) {}

    private static Connection getConnection() {
        return MyDatabase.getInstance().getConnection();
    }

    /**
     * Get all users in the system
     */
    public static List<UserRow> getAllUsers() {
        List<UserRow> users = new ArrayList<>();
        String sql = "SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.role, u.is_active, " +
                     "COALESCE(r.company_name, c.location, '') as additional_info " +
                     "FROM users u " +
                     "LEFT JOIN recruiter r ON u.id = r.id AND u.role = 'RECRUITER' " +
                     "LEFT JOIN candidate c ON u.id = c.id AND u.role = 'CANDIDATE' " +
                     "ORDER BY u.first_name, u.last_name";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(new UserRow(
                    rs.getLong("id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("role"),
                    rs.getBoolean("is_active"),
                    rs.getString("additional_info")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Get users filtered by role
     */
    public static List<UserRow> getUsersByRole(UserContext.Role role) {
        List<UserRow> users = new ArrayList<>();
        String roleStr = role.name();
        String sql = "SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.role, u.is_active, " +
                     "COALESCE(r.company_name, c.location, '') as additional_info " +
                     "FROM users u " +
                     "LEFT JOIN recruiter r ON u.id = r.id " +
                     "LEFT JOIN candidate c ON u.id = c.id " +
                     "WHERE u.role = ? " +
                     "ORDER BY u.first_name, u.last_name";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, roleStr);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(new UserRow(
                    rs.getLong("id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("role"),
                    rs.getBoolean("is_active"),
                    rs.getString("additional_info")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getting users by role: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Get a specific user by ID
     */
    public static UserRow getUserById(Long userId) {
        String sql = "SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.role, u.is_active, " +
                     "COALESCE(r.company_name, c.location, '') as additional_info " +
                     "FROM users u " +
                     "LEFT JOIN recruiter r ON u.id = r.id " +
                     "LEFT JOIN candidate c ON u.id = c.id " +
                     "WHERE u.id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new UserRow(
                    rs.getLong("id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("role"),
                    rs.getBoolean("is_active"),
                    rs.getString("additional_info")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Search users by email or name
     */
    public static List<UserRow> searchUsers(String query, UserContext.Role roleFilter) {
        List<UserRow> users = new ArrayList<>();
        String searchTerm = "%" + query + "%";

        String sql = "SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.role, u.is_active, " +
                     "COALESCE(r.company_name, c.location, '') as additional_info " +
                     "FROM users u " +
                     "LEFT JOIN recruiter r ON u.id = r.id " +
                     "LEFT JOIN candidate c ON u.id = c.id " +
                     "WHERE (u.first_name LIKE ? OR u.last_name LIKE ? OR u.email LIKE ?)";

        List<Object> params = new ArrayList<>();
        params.add(searchTerm);
        params.add(searchTerm);
        params.add(searchTerm);

        if (roleFilter != null) {
            sql += " AND u.role = ?";
            params.add(roleFilter.name());
        }

        sql += " ORDER BY u.first_name, u.last_name";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                if (params.get(i) instanceof String) {
                    pstmt.setString(i + 1, (String) params.get(i));
                }
            }
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(new UserRow(
                    rs.getLong("id"),
                    rs.getString("first_name"),
                    rs.getString("last_name"),
                    rs.getString("email"),
                    rs.getString("phone"),
                    rs.getString("role"),
                    rs.getBoolean("is_active"),
                    rs.getString("additional_info")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Create a new user
     */
    public static Long createUser(String firstName, String lastName, String email, String phone,
                                   String password, String role, Boolean isActive) {
        String sql = "INSERT INTO users (first_name, last_name, email, phone, password, role, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, phone);
            pstmt.setString(5, password != null ? password : "default123");
            pstmt.setString(6, role);
            pstmt.setBoolean(7, isActive != null ? isActive : true);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        Long userId = generatedKeys.getLong(1);

                        // Create role-specific profiles
                        if ("RECRUITER".equals(role)) {
                            createRecruiterProfile(userId, "");
                        } else if ("CANDIDATE".equals(role)) {
                            createCandidateProfile(userId, "");
                        }

                        return userId;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Update an existing user
     */
    public static boolean updateUser(Long userId, String firstName, String lastName, String email,
                                     String phone, String role, Boolean isActive) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, phone = ?, role = ?, is_active = ? " +
                     "WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, email);
            pstmt.setString(4, phone);
            pstmt.setString(5, role);
            pstmt.setBoolean(6, isActive != null ? isActive : true);
            pstmt.setLong(7, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Delete a user (soft delete by setting is_active to false)
     */
    public static boolean deleteUser(Long userId) {
        String sql = "UPDATE users SET is_active = false WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Permanently delete a user (hard delete)
     */
    public static boolean permanentlyDeleteUser(Long userId) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error permanently deleting user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Create recruiter profile for a new recruiter user
     */
    private static void createRecruiterProfile(Long userId, String companyName) {
        String sql = "INSERT INTO recruiter (id, company_name, company_location) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, companyName != null ? companyName : "");
            pstmt.setString(3, "");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating recruiter profile: " + e.getMessage());
        }
    }

    /**
     * Create candidate profile for a new candidate user
     */
    private static void createCandidateProfile(Long userId, String location) {
        String sql = "INSERT INTO candidate (id, location, education_level, experience_years, cv_path) " +
                     "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            pstmt.setString(2, location != null ? location : "");
            pstmt.setString(3, "");
            pstmt.setInt(4, 0);
            pstmt.setString(5, "");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error creating candidate profile: " + e.getMessage());
        }
    }
}

