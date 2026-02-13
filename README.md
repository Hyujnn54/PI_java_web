# PI_java_web - Talent Bridge 🌉

Application de gestion RH développée en Java avec JavaFX et MySQL.

## 📋 Description

Talent Bridge est une plateforme de recrutement complète permettant de gérer les offres d'emploi, les candidatures, les entretiens et bien plus encore. Ce projet implémente un système CRUD complet pour la gestion des offres d'emploi et des compétences requises.

## ✨ Fonctionnalités Principales

### 🎯 Gestion des Offres d'Emploi
- ✅ Créer des offres d'emploi avec compétences requises
- ✅ Éditer et mettre à jour les offres existantes
- ✅ Rechercher et filtrer les offres (titre, location, type de contrat)
- ✅ Gérer le statut des offres (OPEN/CLOSED)
- ✅ Supprimer des offres (avec cascade sur les compétences)

### 🎨 Interface Utilisateur
- Interface JavaFX moderne et responsive
- Navigation intuitive avec barre latérale
- Recherche en temps réel
- Formulaires dynamiques
- Gestion des compétences par drag-and-drop style

### 👥 Gestion des Rôles
- Support multi-rôles (Candidat, Recruteur, Admin)
- Interface adaptative selon le rôle
- Contexte utilisateur global

## 🚀 Démarrage Rapide

### Prérequis
- Java 17 ou supérieur
- MySQL 8.0+
- IntelliJ IDEA (recommandé) ou Eclipse
- Maven (optionnel, intégré à l'IDE)

### Installation

1. **Cloner le projet**
```bash
git clone https://github.com/votre-username/PI_java_web.git
cd PI_java_web
```

2. **Configurer la base de données**
```sql
# Créer la base de données
CREATE DATABASE rh;

# Importer le schéma
mysql -u root -p rh < src/main/java/Utils/rh.sql
```

3. **Configurer la connexion BDD**
Éditez `src/main/java/Utils/MyDatabase.java` si nécessaire :
```java
private static final String URL = "jdbc:mysql://localhost:3306/rh";
private static final String USER = "root";
private static final String PASSWORD = ""; // Votre mot de passe
```

4. **Lancer l'application**
- Via IntelliJ: Run > Run 'Main'
- Via Maven: `mvn javafx:run`

5. **Se connecter**
   - Email: `demo@talentbridge.com`
   - Password: `demo123`
   - Role: Sélectionnez votre rôle (Recruiter/Candidate/Admin)

### 🔧 Résolution des Erreurs

#### Erreur "Location is not set" (Login.fxml)
✅ **Résolu !** Le fichier `Login.fxml` a été créé. Voir [LOGIN_FIX.md](LOGIN_FIX.md)

#### Erreur "Cannot find symbol: UserContext"

**Solution Rapide:**
1. File > Invalidate Caches / Restart
2. Invalidate and Restart

Voir [QUICK_FIX.md](QUICK_FIX.md) pour plus de détails.

## 📁 Structure du Projet

```
PI_java_web/
├── src/main/java/
│   ├── Controllers/          # Contrôleurs JavaFX
│   │   ├── JobOffersController.java
│   │   └── MainShellController.java
│   ├── Models/               # Modèles de données
│   │   ├── JobOffer.java
│   │   └── OfferSkill.java
│   ├── Services/             # Logique métier (CRUD)
│   │   ├── JobOfferService.java
│   │   └── OfferSkillService.java
│   ├── Utils/                # Utilitaires
│   │   ├── MyDatabase.java
│   │   ├── UserContext.java
│   │   ├── SceneManager.java
│   │   └── rh.sql
│   └── org/example/
│       ├── Main.java
│       └── MainFX.java
├── src/main/resources/       # Fichiers FXML et CSS
│   ├── JobOffers.fxml
│   ├── JobOffersBrowse.fxml
│   ├── MainShell.fxml
│   └── styles.css
└── pom.xml                   # Configuration Maven
```

## 📚 Documentation

- **[CRUD_README.md](CRUD_README.md)** - Guide complet du système CRUD
- **[SUMMARY.md](SUMMARY.md)** - Résumé détaillé du projet
- **[USERCONTEXT_FIX.md](USERCONTEXT_FIX.md)** - Gestion du contexte utilisateur
- **[QUICK_FIX.md](QUICK_FIX.md)** - Solutions aux problèmes courants
- **[CHECKLIST.md](CHECKLIST.md)** - Liste de vérification complète

## 🗄️ Base de Données

### Tables Principales

#### job_offer
Stocke les offres d'emploi publiées par les recruteurs.
- id, recruiter_id, title, description, location
- contract_type (CDI, CDD, INTERNSHIP, etc.)
- created_at, deadline, status (OPEN/CLOSED)

#### offer_skill
Stocke les compétences requises pour chaque offre.
- id, offer_id (FK), skill_name
- level_required (BEGINNER, INTERMEDIATE, ADVANCED)

Voir le fichier SQL complet : [rh.sql](src/main/java/Utils/rh.sql)

## 🎯 Fonctionnalités CRUD

### Create (Créer)
```java
JobOffer newJob = new JobOffer();
newJob.setTitle("Développeur Java");
// ... configurer les autres champs
JobOffer saved = jobOfferService.createJobOffer(newJob);
```

### Read (Lire)
```java
// Toutes les offres
List<JobOffer> all = jobOfferService.getAllJobOffers();

// Par ID
JobOffer job = jobOfferService.getJobOfferById(1L);

// Recherche
List<JobOffer> results = jobOfferService.searchJobOffers("Java", "title");
```

### Update (Mettre à jour)
```java
job.setTitle("Nouveau titre");
boolean updated = jobOfferService.updateJobOffer(job);
```

### Delete (Supprimer)
```java
boolean deleted = jobOfferService.deleteJobOffer(jobId);
```

## 👥 Gestion des Utilisateurs

### UserContext
Singleton pour gérer le contexte utilisateur :

```java
// Récupérer le rôle
UserContext.Role role = UserContext.getRole();

// Basculer le rôle (démo)
UserContext.toggleRole();

// Vérifier les permissions
if (UserContext.getRole() == UserContext.Role.RECRUITER) {
    // Actions recruteur
}

// Récupérer l'ID pour les opérations
Long recruiterId = UserContext.getRecruiterId();
```

## 🎨 Technologies Utilisées

- **Java 17** - Langage de programmation
- **JavaFX 17** - Interface utilisateur
- **MySQL 8.0** - Base de données
- **Maven** - Gestion des dépendances
- **JDBC** - Connectivité base de données

## 📦 Dépendances Maven

```xml
<dependencies>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.33</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-fxml</artifactId>
        <version>17</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>17</version>
    </dependency>
</dependencies>
```

## 🧪 Tests

Pour tester le système CRUD :

1. **Test Création**
   - Naviguer vers "Job Offers"
   - Cliquer sur "Create Job Offer"
   - Remplir le formulaire
   - Ajouter des compétences
   - Valider

2. **Test Lecture**
   - Vérifier la liste des offres
   - Sélectionner une offre
   - Vérifier les détails et compétences

3. **Test Mise à Jour**
   - Cliquer sur "Edit"
   - Modifier les champs
   - Ajouter/Supprimer des compétences
   - Sauvegarder

4. **Test Suppression**
   - Sélectionner une offre
   - Cliquer sur "Delete"
   - Confirmer la suppression

## 🐛 Dépannage

### Erreur: "Cannot find symbol: UserContext"
➡️ Solution: Invalidate Caches dans IntelliJ ([QUICK_FIX.md](QUICK_FIX.md))

### Erreur: "Cannot connect to database"
➡️ Vérifiez que MySQL est démarré et que la BDD `rh` existe

### Erreur: "JavaFX components not found"
➡️ Maven doit télécharger les dépendances (pom.xml)

## 🚧 Roadmap

- [ ] Authentification réelle (Login/Register)
- [ ] Système de candidatures
- [ ] Gestion des entretiens
- [ ] Notifications en temps réel
- [ ] Export PDF/Excel
- [ ] Statistiques et tableaux de bord
- [ ] API REST
- [ ] Tests unitaires et d'intégration

## 👨‍💻 Auteurs

Projet développé dans le cadre du cours de développement Java Web.

## 📄 Licence

Ce projet est sous licence MIT.

## 🙏 Remerciements

- Équipe pédagogique
- Contributeurs du projet
- Communauté JavaFX

---

**Version:** 1.0.0  
**Dernière mise à jour:** 13 février 2026  
**Status:** ✅ Production Ready

🎉 **Système CRUD Complet et Opérationnel !** 🎉

