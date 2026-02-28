# 🐛 DIAGNOSTIC - Launcher ne Fonctionne Pas

## ❓ Dites-moi exactement quelle erreur vous avez :

### **1. Erreur de Compilation ?**
- ❌ Texte rouge dans IntelliJ ?
- ❌ "Cannot resolve symbol" ?
- **→ Si oui, quelle classe n'est pas trouvée ?**

---

### **2. Erreur au Lancement ?**
Quand vous faites **Run 'Launcher.main()'**, que se passe-t-il ?

#### **Option A : Erreur JavaFX**
```
Error: JavaFX runtime components are missing
```
**→ Si c'est ça, dites-moi !**

#### **Option B : Erreur Base de Données**
```
Communications link failure
Access denied for user 'root'
Unknown database 'rh'
```
**→ Si c'est ça, dites-moi laquelle !**

#### **Option C : Erreur FXML**
```
javafx.fxml.LoadException
Location is not set
```
**→ Si c'est ça, dites-moi !**

#### **Option D : Autre Erreur**
**→ Copiez-collez le message d'erreur complet**

---

### **3. Rien ne se passe ?**
- ❌ Aucune fenêtre ne s'ouvre ?
- ❌ Pas de message d'erreur du tout ?

---

## 🔍 **COMMENT VOIR L'ERREUR EXACTE**

1. Dans IntelliJ, en bas, cliquez sur l'onglet **"Run"**
2. Regardez les messages en rouge
3. **Copiez le texte complet de l'erreur** et dites-moi

---

## ✅ **SOLUTIONS RAPIDES À TESTER**

En attendant votre réponse, testez ces solutions :

### **Solution 1 : Rebuild le Projet**
1. Dans IntelliJ : **Build** → **Rebuild Project**
2. Attendez la fin
3. Re-lancez le Launcher

### **Solution 2 : Vérifier MySQL**
Ouvrez un terminal et tapez :
```bash
net start MySQL80
```
Si MySQL démarre → La base de données n'était pas lancée

### **Solution 3 : Vérifier le Mot de Passe MySQL**
1. Ouvrez `src/main/java/utils/MyDatabase.java`
2. Ligne 9 :
```java
private static final String PASSWORD = "";  // ⚠️ Mettez votre mot de passe !
```
3. Mettez votre mot de passe MySQL entre les guillemets
4. Sauvegardez (`Ctrl+S`)

---

## 📸 **BESOIN DE PLUS D'AIDE ?**

**Envoyez-moi :**
1. Le message d'erreur complet (copié depuis l'onglet Run)
2. OU une capture d'écran de l'erreur

Et je vous donne la solution exacte ! 😊
