package Utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SchemaFixer {
    public static void main(String[] args) {
        Connection conn = MyDatabase.getInstance().getConnection();
        if (conn == null) {
            System.out.println("Failed to connect to database.");
            return;
        }

        try {
            System.out.println("Checking schema...");
            DatabaseMetaData meta = conn.getMetaData();

            // Fix recruiter table
            ensureColumn(conn, meta, "recruiter", "company_name", "VARCHAR(255) AFTER id");
            ensureColumn(conn, meta, "recruiter", "company_location", "VARCHAR(255) AFTER company_name");
            ensureColumn(conn, meta, "recruiter", "company_description", "TEXT AFTER company_location");
            ensureColumn(conn, meta, "recruiter", "user_id", "BIGINT AFTER id");

            // Synchronize recruiter user_id if missing
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE recruiter SET user_id = id WHERE user_id IS NULL OR user_id = 0");
            } catch (SQLException e) {
            }

            // Fix candidate table
            ensureColumn(conn, meta, "candidate", "location", "VARCHAR(255) AFTER id");
            ensureColumn(conn, meta, "candidate", "education_level", "VARCHAR(100) AFTER location");
            ensureColumn(conn, meta, "candidate", "experience_years", "INT AFTER education_level");
            ensureColumn(conn, meta, "candidate", "cv_path", "VARCHAR(255) AFTER experience_years");
            ensureColumn(conn, meta, "candidate", "user_id", "BIGINT AFTER id");

            // Synchronize candidate user_id if missing
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("UPDATE candidate SET user_id = id WHERE user_id IS NULL OR user_id = 0");
            } catch (SQLException e) {
            }

            // Force event_type to VARCHAR(255) to avoid ENUM truncation errors
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE recruitment_event MODIFY COLUMN event_type VARCHAR(255)");
                System.out.println("Verified: recruitment_event.event_type is VARCHAR(255).");

                // Fix event_registration table name and status column
                try {
                    stmt.executeUpdate("ALTER TABLE event_registration MODIFY COLUMN attendance_status VARCHAR(255)");
                    System.out.println("Verified: event_registration.attendance_status is VARCHAR(255).");
                } catch (SQLException e) {
                    // Try plural version
                    stmt.executeUpdate("ALTER TABLE event_registrations MODIFY COLUMN attendance_status VARCHAR(255)");
                    System.out.println("Verified: event_registrations.attendance_status is VARCHAR(255).");
                }
            } catch (SQLException e) {
                System.out.println("Note: Tables not found yet or couldn't modify columns: " + e.getMessage());
            }

            System.out.println("Schema check/fix completed.");

            // Create event_review table if it doesn't exist
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS event_review (" +
                    "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "  event_id BIGINT NOT NULL," +
                    "  candidate_id BIGINT NOT NULL," +
                    "  rating INT NOT NULL," +
                    "  comment TEXT," +
                    "  created_at DATETIME NOT NULL," +
                    "  UNIQUE KEY uq_review (event_id, candidate_id)" +
                    ")"
                );
                System.out.println("event_review table ready.");
            } catch (SQLException e) {
                System.out.println("Note: Could not create event_review table: " + e.getMessage());
            }

            // Auto-cancel PENDING registrations for events that have already passed
            try (Statement stmt = conn.createStatement()) {
                int updated = stmt.executeUpdate(
                    "UPDATE event_registration er " +
                    "JOIN recruitment_event re ON er.event_id = re.id " +
                    "SET er.attendance_status = 'CANCELLED' " +
                    "WHERE er.attendance_status IN ('PENDING','REGISTERED') " +
                    "AND re.event_date < NOW()"
                );
                if (updated > 0)
                    System.out.println("Auto-cancelled " + updated + " pending registration(s) for past events.");
            } catch (SQLException e) {
                System.out.println("Note: Could not auto-cancel past pending registrations: " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureColumn(Connection conn, DatabaseMetaData meta, String tableName, String columnName,
            String definition) throws SQLException {
        ResultSet rs = meta.getColumns(null, null, tableName, columnName);
        if (!rs.next()) {
            System.out.println("Adding missing column '" + columnName + "' to table '" + tableName + "'...");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
                System.out.println("Successfully added '" + columnName + "'.");
            }
        } else {
            System.out.println("Column '" + columnName + "' already exists in table '" + tableName + "'.");
        }
    }
}
