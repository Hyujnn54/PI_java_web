package Utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TableInspector {
    public static void main(String[] args) {
        Connection conn = MyDatabase.getInstance().getConnection();
        if (conn != null) {
            try {
                DatabaseMetaData meta = conn.getMetaData();
                inspectTable(meta, "recruiter");
                inspectTable(meta, "candidate");
                inspectTable(meta, "users");
                inspectTable(meta, "recruitment_event");
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        } else {
            System.out.println("Failed to connect to database.");
        }
    }

    private static void inspectTable(DatabaseMetaData meta, String tableName) throws SQLException {
        System.out.println("\n--- Table: " + tableName + " ---");
        ResultSet rs = meta.getColumns(null, null, tableName, null);
        boolean found = false;
        while (rs.next()) {
            found = true;
            String columnName = rs.getString("COLUMN_NAME");
            String columnType = rs.getString("TYPE_NAME");
            int columnSize = rs.getInt("COLUMN_SIZE");
            System.out.println(
                    " - " + columnName + " (" + columnType + (columnSize > 0 ? "[" + columnSize + "]" : "") + ")");
        }
        if (!found) {
            System.out.println("Table '" + tableName + "' not found!");
        }
    }
}
