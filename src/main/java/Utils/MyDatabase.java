package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL      = "jdbc:mysql://localhost:3306/rh"
            + "?autoReconnect=true"
            + "&useSSL=false"
            + "&allowPublicKeyRetrieval=true"
            + "&serverTimezone=UTC"
            + "&connectionTimeout=30000"
            + "&socketTimeout=60000";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private Connection connection;
    private static MyDatabase instance;

    private MyDatabase() {
        connect();
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    /** Always returns a live connection, reconnecting if needed. */
    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("Connection is closed or invalid, reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.out.println("Error checking connection, reconnecting: " + e.getMessage());
            connect();
        }
        return connection;
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            connection.setAutoCommit(true); // always start with autoCommit=true
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.out.println("Failed to connect to database: " + e.getMessage());
            connection = null;
        }
    }
}
