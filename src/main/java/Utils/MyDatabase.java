package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private final String URL = "jdbc:mysql://localhost:3306/rh";
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
                System.out.println("La connexion est fermée ou invalide, reconnexion en cours...");
                connect();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification de la connexion : " + e.getMessage());
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
            System.out.println("Connexion à la base de données réussie");
        } catch (SQLException e) {
            System.err.println("Échec de la connexion à la base de données : " + e.getMessage());
            e.printStackTrace();
        }
    }
}