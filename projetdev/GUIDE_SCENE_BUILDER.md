# Guide d'Intégration de Scene Builder

## 📥 1. Télécharger Scene Builder

**Lien officiel:** https://gluonhq.com/products/scene-builder/

Choisissez la version compatible avec votre OS (Windows, Mac, Linux).

**Version recommandée:** Scene Builder 19.0.0 ou supérieure

---

## 🔧 2. Intégration avec IntelliJ IDEA

### Étape 1: Installer Scene Builder
1. Téléchargez et installez Scene Builder
2. Notez le chemin d'installation (ex: `C:\Program Files\SceneBuilder\SceneBuilder.exe`)

### Étape 2: Configurer IntelliJ
1. Ouvrez **File** → **Settings** (ou `Ctrl+Alt+S`)
2. Naviguez vers **Languages & Frameworks** → **JavaFX**
3. Dans le champ **Path to SceneBuilder**, cliquez sur **Browse**
4. Sélectionnez le fichier exécutable de Scene Builder :
   - Path: `C:\Users\rayan\AppData\Local\SceneBuilder\SceneBuilder.exe`
5. Cliquez sur **Apply** puis **OK**

### Étape 3: Ouvrir un fichier FXML
1. Faites un clic droit sur n'importe quel fichier `.fxml`
2. Sélectionnez **Open in SceneBuilder**
3. Scene Builder s'ouvrira avec votre interface

---

## 🔧 3. Intégration avec Eclipse

### Étape 1: Installer e(fx)clipse
1. Allez dans **Help** → **Eclipse Marketplace**
2. Recherchez "e(fx)clipse"
3. Installez le plugin **e(fx)clipse**
4. Redémarrez Eclipse

### Étape 2: Configurer Scene Builder
1. Ouvrez **Window** → **Preferences**
2. Naviguez vers **JavaFX**
3. Dans **SceneBuilder executable**, cliquez sur **Browse**
4. Sélectionnez l'exécutable de Scene Builder
5. Cliquez sur **Apply and Close**

### Étape 3: Ouvrir un fichier FXML
1. Faites un clic droit sur un fichier `.fxml`
2. Sélectionnez **Open with SceneBuilder**

---

## 🔧 4. Intégration avec VS Code

### Étape 1: Installer l'extension
1. Ouvrez VS Code
2. Allez dans **Extensions** (`Ctrl+Shift+X`)
3. Recherchez "SceneBuilder extension for Visual Studio Code"
4. Installez l'extension

### Étape 2: Configurer
1. Ouvrez **File** → **Preferences** → **Settings**
2. Recherchez "SceneBuilder"
3. Définissez le chemin vers l'exécutable Scene Builder

### Étape 3: Utiliser
1. Clic droit sur un fichier `.fxml`
2. Sélectionnez **Open in SceneBuilder**

---

## 🎨 Utilisation de Scene Builder avec Vos Fichiers

Vos fichiers FXML sont dans: `src/main/resources/GUI/`

- ✅ `login.fxml`
- ✅ `admin_dashboard.fxml`
- ✅ `recruiter_dashboard.fxml`
- ✅ `candidate_dashboard.fxml`

### Tips pour Scene Builder:

1. **Modifier visuellement** - Drag & drop des composants
2. **Propriétés** - Panel de droite pour modifier les styles
3. **Controller** - Assurez-vous que le controller est bien défini
4. **fx:id** - Doit correspondre aux @FXML dans le controller
5. **onAction** - Doit correspondre aux méthodes dans le controller

---

## ⚡ Raccourcis Clavier Scene Builder

- `Ctrl+Z` - Annuler
- `Ctrl+Y` - Refaire
- `Ctrl+C` / `Ctrl+V` - Copier / Coller
- `Del` - Supprimer composant sélectionné
- `Ctrl+D` - Dupliquer composant

---

## 🚀 Workflow Recommandé

1. **Ouvrir le FXML** dans Scene Builder
2. **Modifier visuellement** l'interface
3. **Sauvegarder** (`Ctrl+S`)
4. **Vérifier** que les fx:id correspondent au controller
5. **Tester** en lançant l'application JavaFX

---

## ✅ Vérification

Pour vérifier que Scene Builder est bien intégré:

1. Ouvrez `src/main/resources/GUI/login.fxml`
2. Faites un clic droit → **Open in SceneBuilder**
3. Si Scene Builder s'ouvre → ✅ Intégration réussie
4. Sinon → Revérifiez le chemin dans les settings

---

**Astuce:** Vous pouvez modifier le design directement dans Scene Builder et le code sera automatiquement mis à jour dans le fichier FXML!
