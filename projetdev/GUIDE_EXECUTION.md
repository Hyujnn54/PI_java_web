# Guide d'Exécution - Application JavaFX Recrutement

## 🗄️ Étape 1: Initialiser la Base de Données

### Option A: MySQL en ligne de commande

```bash
mysql -u root -p < database_schema.sql
```

Entrez votre mot de passe MySQL quand demandé.

### Option B: MySQL Workbench

1. Ouvrez MySQL Workbench
2. Connectez-vous à votre serveur
3. **File** → **Open SQL Script**
4. Sélectionnez `database_schema.sql`
5. Cliquez sur **Execute** (éclair ⚡)

### Option C: phpMyAdmin

1. Ouvrez phpMyAdmin
2. Cliquez sur **Import**
3. Choisissez le fichier `database_schema.sql`
4. Cliquez sur **Go**

### ✅ Vérification

La base de données `rh` doit contenir:
- ✅ 5 tables (users, candidate, recruiter, recruitment_event, event_registrations)
- ✅ 5 utilisateurs de test
- ✅ 2 candidats, 2 recruteurs
- ✅ 3 événements, 4 inscriptions

---

## ⚙️ Étape 2: Vérifier la Configuration MySQL

Ouvrez `src/main/java/utils/MyDatabase.java` et vérifiez:

```java
private static final String URL = "jdbc:mysql://localhost:3306/rh";
private static final String USER = "root";
private static final String PASSWORD = "";  // ⚠️ Mettez votre mot de passe!
```

**Si votre mot de passe MySQL n'est pas vide, modifiez cette ligne!**

---

## 🚀 Étape 3: Compiler le Projet

```bash
mvn clean compile
```

---

## 🎯 Étape 4: Lancer l'Application

Vous avez 3 options d'exécution:

### Option 1: Avec Login (Application Complète)

```bash
mvn exec:java -Dexec.mainClass="Application.MainApp"
```

Utilisez les comptes de test:
- Admin: `admin@rh.com` / `admin123`
- Candidat: `john.doe@example.com` / `password123`
- Recruteur: `hr@acme.com` / `password123`

---

### Option 2: Front Office (Dashboard Candidat Direct)

```bash
mvn exec:java -Dexec.mainClass="Application.MainAppFrontOffice"
```

✅ **Skip login** - démarre directement sur le dashboard candidat
- Utilisateur simulé: John Doe (candidat)
- Fonctionnalités:
  - Voir les événements disponibles
  - S'inscrire aux événements
  - Gérer ses inscriptions

---

### Option 3: Back Office (Dashboard Recruteur Direct)

```bash
mvn exec:java -Dexec.mainClass="Application.MainAppBackOffice"
```

✅ **Skip login** - démarre directement sur le dashboard recruteur
- Utilisateur simulé: ACME Corporation (recruteur)
- Fonctionnalités:
  - Créer des événements de recrutement
  - Voir ses événements
  - Supprimer des événements

---

## 🔧 Étape 5: Depuis votre IDE

### IntelliJ IDEA

1. Ouvrez le projet
2. Clic droit sur la classe Main désirée:
   - `MainApp.java` (avec login)
   - `MainAppFrontOffice.java` (candidat direct)
   - `MainAppBackOffice.java` (recruteur direct)
3. Sélectionnez **Run 'MainApp.main()'**

### Eclipse

1. Ouvrez le projet
2. Naviguez vers le package `Application`
3. Clic droit sur la classe Main
4. **Run As** → **Java Application**

### VS Code

1. Ouvrez le projet
2. Allez dans la classe Main
3. Cliquez sur **Run** au-dessus de `public static void main`

---

## 🐛 Dépannage

### Erreur: "Communications link failure"

**Problème:** MySQL n'est pas démarré

**Solution:**
```bash
# Windows
net start MySQL80

# Mac/Linux
sudo service mysql start
```

---

### Erreur: "Access denied for user 'root'@'localhost'"

**Problème:** Mot de passe MySQL incorrect

**Solution:** Modifiez `MyDatabase.java` ligne 9 avec votre mot de passe

---

### Erreur: "Unknown database 'rh'"

**Problème:** Base de données non créée

**Solution:** Exécutez `database_schema.sql` (voir Étape 1)

---

### Erreur: "javafx.fxml.LoadException"

**Problème:** Fichier FXML ou controller incorrect

**Solution:** 
1. Vérifiez que le fichier FXML existe dans `src/main/resources/GUI/`
2. Vérifiez que le controller est bien défini: `fx:controller="controllers.XxxController"`

---

## 📝 Notes Importantes

1. **Front Office** = Interface Candidat
2. **Back Office** = Interface Recruteur
3. **Admin Dashboard** = Accessible uniquement via login avec compte admin

---

## 🎉 Vous êtes prêt!

Lancez l'application avec la commande de votre choix et testez les fonctionnalités!
