package Utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * UTILITAIRE DE TEST DE CONNEXION
 * Exécutez ce fichier pour vérifier si la base de données est accessible.
 */
public class DatabaseTester {
    public static void main(String[] args) {
        System.out.println("--- TEST DE CONNEXION BASE DE DONNÉES ---");
        
        String url = "jdbc:mysql://localhost:3306/rh";
        String user = "root";
        String password = ""; // Mot de passe vide comme demandé

        System.out.println("Tentative de connexion à : " + url);
        System.out.println("Utilisateur : " + user);
        System.out.println("Mot de passe : (vide)");
        
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("✅ SUCCÈS ! La connexion fonctionne.");
            System.out.println("Base de données trouvée : " + conn.getCatalog());
        } catch (SQLException e) {
            System.err.println("❌ ÉCHEC DE LA CONNEXION !");
            System.err.println("Message : " + e.getMessage());
            System.err.println("Code d'erreur : " + e.getErrorCode());
            
            if (e.getMessage().contains("Access denied")) {
                System.out.println("\n👉 PISTE : Le mot de passe ou l'utilisateur est incorrect.");
            } else if (e.getMessage().contains("Unknown database")) {
                System.out.println("\n👉 PISTE : La base de données 'rh' n'existe pas.");
                System.out.println("Avez-vous importé le fichier database_schema.sql ?");
            } else if (e.getMessage().contains("Communications link failure")) {
                System.out.println("\n👉 PISTE : Le serveur MySQL n'est pas démarré.");
            }
        }
    }
}
