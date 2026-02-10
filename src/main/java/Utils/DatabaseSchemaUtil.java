package Utils;

import java.sql.*;

/**
 * Database Schema Utility for fixing and verifying database schema
 */
public class DatabaseSchemaUtil {

    public static void fixInterviewTableSchema() {
        Connection conn = MyDatabase.getInstance().getConnection();

        try {
            System.out.println("Checking and fixing interview table schema...");

            // Check current schema
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "interview", null);

            System.out.println("Current interview table structure:");
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                System.out.println("  " + columnName + ": " + dataType + "(" + columnSize + ")");
            }
            columns.close();

            // Fix mode and status columns if needed
            String[] fixQueries = {
                "ALTER TABLE interview MODIFY COLUMN mode VARCHAR(20)",
                "ALTER TABLE interview MODIFY COLUMN status VARCHAR(20) DEFAULT 'SCHEDULED'"
            };

            for (String query : fixQueries) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(query);
                    System.out.println("Executed: " + query);
                } catch (SQLException e) {
                    System.out.println("Query already applied or error: " + e.getMessage());
                }
            }

            System.out.println("Schema fix completed.");

        } catch (SQLException e) {
            System.err.println("Error fixing schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void verifyInterviewData() {
        String sql = "SELECT COUNT(*) as count FROM interview";

        try (Statement stmt = MyDatabase.getInstance().getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                int count = rs.getInt("count");
                System.out.println("Current number of interviews in database: " + count);

                if (count > 0) {
                    // Show sample data
                    String sampleSql = "SELECT id, mode, status, scheduled_at FROM interview LIMIT 5";
                    try (ResultSet sampleRs = stmt.executeQuery(sampleSql)) {
                        System.out.println("Sample interview data:");
                        while (sampleRs.next()) {
                            System.out.println("  ID: " + sampleRs.getInt("id") +
                                             ", Mode: '" + sampleRs.getString("mode") +
                                             "', Status: '" + sampleRs.getString("status") +
                                             "', Date: " + sampleRs.getTimestamp("scheduled_at"));
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.err.println("Error verifying data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void cleanupCorruptedData() {
        String sql = "DELETE FROM interview WHERE mode NOT IN ('ONLINE', 'ON_SITE') OR mode IS NULL OR mode = ''";

        try (Statement stmt = MyDatabase.getInstance().getConnection().createStatement()) {
            int deletedRows = stmt.executeUpdate(sql);
            System.out.println("Cleaned up " + deletedRows + " corrupted interview records");

            if (deletedRows > 0) {
                System.out.println("Corrupted data has been removed. You can now create new interviews.");
            }

        } catch (SQLException e) {
            System.err.println("Error cleaning up data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
