package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String URL = "jdbc:mysql://localhost:3306/rh"
            + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true"
            + "&connectTimeout=5000&socketTimeout=30000";
    private final String USERNAME = "root";
    private final String PASSWORD = "";
    private Connection connection;
    private static MyDatabase instance;

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null) {
                connect();
            } else {
                // isValid() itself can throw if the connection is in a broken state
                boolean valid = false;
                try { valid = !connection.isClosed() && connection.isValid(2); }
                catch (Exception ignored) { valid = false; }
                if (!valid) {
                    System.out.println("Connection stale, reconnecting...");
                    try { connection.close(); } catch (Exception ignored) {}
                    connect();
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking connection: " + e.getMessage());
            try { connection.close(); } catch (Exception ignored) {}
            connect();
        }
        return connection;
    }

    private MyDatabase() {
        connect();
    }

    private void connect() {
        try {
            connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}