package Application;

/**
 * LAUNCHER PRINCIPAL - EXÉCUTEZ CE FICHIER !
 * 
 * Ce fichier lance l'application JavaFX complète avec login.
 * 
 * Pour exécuter dans IntelliJ :
 * 1. Clic droit sur ce fichier
 * 2. Run 'Launcher.main()'
 * 
 * Comptes de test :
 * - Admin: admin@rh.com / admin123
 * - Candidat: john.doe@example.com / password123
 * - Recruteur: hr@acme.com / password123
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args); // Lancer avec login
        // MainAppBackOffice.main(args); // Lancer directement le back office
        // (recruteur)
    }
}
