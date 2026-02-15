package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private static String url;
    private static String user;
    private static String password;
    private Connection connection;
    private static MyDatabase instance;

    // Bloc statique pour charger les propriétés
    static {
        try {
            java.util.Properties props = new java.util.Properties();
            java.io.InputStream is = MyDatabase.class.getClassLoader().getResourceAsStream("db.properties");
            if (is == null) {
                System.err.println("ERREUR CRITIQUE: db.properties introuvable dans le classpath!");
                // Valeurs par défaut en cas d'erreur
                url = "jdbc:mysql://localhost:3306/rh";
                user = "root";
                password = "";
            } else {
                props.load(is);
                url = props.getProperty("db.url");
                user = props.getProperty("db.user");
                password = props.getProperty("db.password");
            }
        } catch (java.io.IOException e) {
            System.err.println("Erreur lors du chargement de db.properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Singleton thread-safe avec double-checked locking
    public static MyDatabase getInstance() {
        if (instance == null) {
            synchronized (MyDatabase.class) {
                if (instance == null) {
                    instance = new MyDatabase();
                }
            }
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Connected to database successfully");
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            // Ne pas throw RuntimeException ici pour permettre à l'app de continuer (et
            // afficher l'erreur UI)
            // ou bien on laisse planter. Mieux vaut logger.
            e.printStackTrace();
        }
    }

    // Méthode pour fermer la connexion proprement
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Database connection closed");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}