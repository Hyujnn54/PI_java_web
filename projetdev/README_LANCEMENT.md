# 🚀 Comment Lancer l'Application - Guide Simple

## ✅ **MÉTHODE SIMPLE - AVEC INTELLIJ (RECOMMANDÉE)**

Vous avez maintenant **3 fichiers Launcher** pour lancer facilement l'application :

---

### **1️⃣ Launcher.java - Application Complète (avec Login)**

**Fichier :** `src/main/java/Application/Launcher.java`

**Comment lancer :**
1. Dans IntelliJ, ouvrez le fichier `Launcher.java`
2. **Clic droit** sur le fichier (dans l'éditeur ou dans l'arborescence)
3. Sélectionnez **"Run 'Launcher.main()'"**
4. L'application démarre avec la page de login

**Comptes de test :**
- 👑 Admin: `admin@rh.com` / `admin123`
- 👨‍🎓 Candidat: `john.doe@example.com` / `password123`
- 🧑‍💼 Recruteur: `hr@acme.com` / `password123`

---

### **2️⃣ LauncherFrontOffice.java - Dashboard Candidat (SKIP LOGIN)**

**Fichier :** `src/main/java/Application/LauncherFrontOffice.java`

**Comment lancer :**
1. Dans IntelliJ, ouvrez le fichier `LauncherFrontOffice.java`
2. **Clic droit** sur le fichier
3. Sélectionnez **"Run 'LauncherFrontOffice.main()'"**
4. L'application démarre directement sur le dashboard candidat

**Utilisateur simulé :** John Doe (candidat)

**Fonctionnalités :**
- ✅ Voir les événements disponibles
- ✅ S'inscrire aux événements
- ✅ Gérer ses inscriptions

---

### **3️⃣ LauncherBackOffice.java - Dashboard Recruteur (SKIP LOGIN)**

**Fichier :** `src/main/java/Application/LauncherBackOffice.java`

**Comment lancer :**
1. Dans IntelliJ, ouvrez le fichier `LauncherBackOffice.java`
2. **Clic droit** sur le fichier
3. Sélectionnez **"Run 'LauncherBackOffice.main()'"**
4. L'application démarre directement sur le dashboard recruteur

**Utilisateur simulé :** ACME Corporation (recruteur)

**Fonctionnalités :**
- ✅ Créer des événements de recrutement
- ✅ Voir ses événements
- ✅ Supprimer des événements

---

## ⚙️ **CONFIGURATION INTELLIJ (PREMIÈRE FOIS)**

Si c'est la première fois que vous lancez l'application :

### **Étape 1 : Vérifier Maven**

1. Clic droit sur le fichier `pom.xml`
2. Sélectionnez **"Maven"** → **"Reload Project"**
3. Attendez que Maven télécharge les dépendances

### **Étape 2 : Vérifier la Base de Données**

Assurez-vous que MySQL est démarré et que la base `rh` existe :

```bash
mysql -u root -p < database_schema.sql
```

Ou utilisez MySQL Workbench / phpMyAdmin pour importer `database_schema.sql`

### **Étape 3 : Vérifier MyDatabase.java**

Ouvrez `src/main/java/utils/MyDatabase.java` et vérifiez :

```java
private static final String PASSWORD = "";  // ⚠️ Mettez votre mot de passe MySQL !
```

---

## 🐛 **DÉPANNAGE**

### **Erreur : "Could not find or load main class"**

**Solution :**
1. Clic droit sur le projet → **"Rebuild Project"**
2. Re-lancez le Launcher

---

### **Erreur : "JavaFX runtime components are missing"**

**Solution :**
1. Vérifiez que le `pom.xml` contient les dépendances JavaFX
2. Clic droit sur `pom.xml` → **"Maven"** → **"Reload Project"**
3. Attendez que Maven se synchronise

---

### **Erreur : "Communications link failure" (MySQL)**

**Solution :**
1. Vérifiez que MySQL est démarré
2. Windows : `net start MySQL80`
3. Vérifiez le mot de passe dans `MyDatabase.java`

---

## 🎯 **QUELLE VERSION LANCER ?**

- **Développement/Test complet** → `Launcher.java` (avec login)
- **Test du Front Office** → `LauncherFrontOffice.java` (candidat)
- **Test du Back Office** → `LauncherBackOffice.java` (recruteur)

---

## ✅ **C'EST TOUT !**

Vous pouvez maintenant lancer l'application en **un seul clic** ! 🎉

**Conseil :** Ajoutez les Launchers à vos **Run Configurations** favorites dans IntelliJ pour un accès encore plus rapide !
