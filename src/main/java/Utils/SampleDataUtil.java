package Utils;

import java.sql.*;

/**
 * Utility class to ensure we have valid sample data for testing interviews
 */
public class SampleDataUtil {

    public static void ensureSampleDataExists() {
        createSampleApplicationIfNeeded();
        createSampleRecruiterIfNeeded();
    }

    private static void createSampleApplicationIfNeeded() {
        Connection conn = MyDatabase.getInstance().getConnection();
        
        // Check if any applications exist
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM application")) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("No applications found. Creating sample application...");
                
                // First ensure we have a candidate
                createSampleCandidateIfNeeded();
                
                // Get first candidate ID
                try (ResultSet candidateRs = stmt.executeQuery("SELECT id FROM candidate LIMIT 1")) {
                    if (candidateRs.next()) {
                        int candidateId = candidateRs.getInt("id");
                        
                        // Create sample application
                        String insertSql = "INSERT INTO application (candidate_id, status) VALUES (?, 'PENDING')";
                        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                            ps.setInt(1, candidateId);
                            ps.executeUpdate();
                            System.out.println("Sample application created successfully.");
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring sample application: " + e.getMessage());
        }
    }

    private static void createSampleRecruiterIfNeeded() {
        Connection conn = MyDatabase.getInstance().getConnection();
        
        // Check if any recruiters exist
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM recruiter")) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("No recruiters found. Creating sample recruiter...");
                
                // Create sample recruiter
                String insertSql = "INSERT INTO recruiter (full_name, email) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, "John Recruiter");
                    ps.setString(2, "john.recruiter@talentbridge.com");
                    ps.executeUpdate();
                    System.out.println("Sample recruiter created successfully.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring sample recruiter: " + e.getMessage());
        }
    }

    private static void createSampleCandidateIfNeeded() {
        Connection conn = MyDatabase.getInstance().getConnection();
        
        // Check if any candidates exist
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM candidate")) {
            
            if (rs.next() && rs.getInt(1) == 0) {
                System.out.println("No candidates found. Creating sample candidate...");
                
                // Create sample candidate
                String insertSql = "INSERT INTO candidate (full_name, email) VALUES (?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, "Jane Candidate");
                    ps.setString(2, "jane.candidate@example.com");
                    ps.executeUpdate();
                    System.out.println("Sample candidate created successfully.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error ensuring sample candidate: " + e.getMessage());
        }
    }

    public static void showAvailableData() {
        Connection conn = MyDatabase.getInstance().getConnection();
        
        try (Statement stmt = conn.createStatement()) {
            // Show available candidates
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM candidate");
            if (rs.next()) {
                System.out.println("Available candidates: " + rs.getInt(1));
            }
            rs.close();
            
            // Show available recruiters
            rs = stmt.executeQuery("SELECT COUNT(*) FROM recruiter");
            if (rs.next()) {
                System.out.println("Available recruiters: " + rs.getInt(1));
            }
            rs.close();
            
            // Show available applications
            rs = stmt.executeQuery("SELECT COUNT(*) FROM application");
            if (rs.next()) {
                System.out.println("Available applications: " + rs.getInt(1));
            }
            rs.close();
            
            // Show sample IDs
            rs = stmt.executeQuery("SELECT id FROM application LIMIT 1");
            if (rs.next()) {
                System.out.println("Sample application ID: " + rs.getInt("id"));
            }
            rs.close();
            
            rs = stmt.executeQuery("SELECT id FROM recruiter LIMIT 1");
            if (rs.next()) {
                System.out.println("Sample recruiter ID: " + rs.getInt("id"));
            }
            rs.close();
            
        } catch (SQLException e) {
            System.err.println("Error showing available data: " + e.getMessage());
        }
    }
}
