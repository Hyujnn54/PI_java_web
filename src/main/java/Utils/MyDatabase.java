package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
<<<<<<< HEAD
    private final String URL = "jdbc:mysql://localhost:3306/rh?autoReconnect=true&useSSL=false";
>>>>>>> offer
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
            // Check if connection is valid, if not, reconnect
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
<<<<<<< HEAD
                System.out.println("Connection is closed or invalid, reconnecting...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Error checking connection validity: " + e.getMessage());
>>>>>>> offer
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
<<<<<<< HEAD
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
>>>>>>> offer
            e.printStackTrace();
        }
    }
}