# Guide d'Intégration des Statistiques avec Gestion des Rôles

## Vue d'ensemble

Le module de statistiques affiche des graphiques et des indicateurs différents selon le rôle de l'utilisateur :
- **ADMIN** : Statistiques globales de toutes les offres
- **RECRUITER** : Statistiques personnelles de ses propres offres
- **CANDIDATE** : Pas d'accès aux statistiques

## Architecture

### Services (JobOfferService.java)

#### Méthodes pour ADMIN
```java
// Statistiques globales
public Map<ContractType, Integer> statsGlobal() throws SQLException
public int getTotalOffresGlobal() throws SQLException
public int getExpiredOffresGlobal() throws SQLException
```

#### Méthodes pour RECRUITER
```java
// Statistiques par recruteur
public Map<ContractType, Integer> statsByRecruiter(Long recruiterId) throws SQLException
public int getTotalOffresByRecruiter(Long recruiterId) throws SQLException
public int getExpiredOffresByRecruiter(Long recruiterId) throws SQLException
```

### Contrôleur (StatisticsController.java)

Le contrôleur charge automatiquement les statistiques appropriées selon le rôle :

```java
@FXML
public void initialize() {
    jobOfferService = new JobOfferService();
    loadStatistics();  // Charge selon le rôle actuel
}

private void loadStatistics() {
    UserContext.Role currentRole = UserContext.getRole();
    Long userId = UserContext.getUserId();

    if (currentRole == UserContext.Role.CANDIDATE) {
        // Cacher les statistiques
        hideStatistics();
        return;
    }

    if (currentRole == UserContext.Role.ADMIN) {
        loadAdminStatistics();
    } else if (currentRole == UserContext.Role.RECRUITER) {
        loadRecruiterStatistics(userId);
    }
}
```

## Interface FXML (Statistics.fxml)

### Labels affichés
```xml
<Label fx:id="roleInfoLabel" text="Statistiques - Recruiter" />
<Label fx:id="totalOffersLabel" text="Total des offres : 0" />
<Label fx:id="expiredOffersLabel" text="Offres expirées : 0" />
```

### PieChart
```xml
<PieChart fx:id="contractTypePieChart"
          title="Répartition des offres par type de contrat"
          legendVisible="true"
          labelsVisible="true" />
```

## Utilisation

### Pour tester avec différents rôles

Dans MainFX.java, vous pouvez changer le rôle au démarrage :

```java
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Tooltip;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import Models.ContractType;
import Services.JobOfferService;
import java.sql.SQLException;
import java.util.Map;
```

### 2. Déclarer le PieChart dans votre contrôleur

```java
@FXML
private PieChart contractTypePieChart;

private JobOfferService jobOfferService;
```

### 3. Initialiser dans initialize()

```java
@FXML
public void initialize() {
    jobOfferService = new JobOfferService();
    loadPieChart();
}
```

### 4. Méthode loadPieChart() complète

```java
private void loadPieChart() {
    try {
        // Récupérer les statistiques
        Map<ContractType, Integer> stats = jobOfferService.statsOffresParContractType();
        
        // Calculer le total
        int total = stats.values().stream().mapToInt(Integer::intValue).sum();
        
        // Si aucune donnée
        if (total == 0) {
            PieChart.Data emptyData = new PieChart.Data("Aucune offre", 1);
            contractTypePieChart.setData(FXCollections.observableArrayList(emptyData));
            return;
        }
        
        // Créer les données du PieChart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        
        for (Map.Entry<ContractType, Integer> entry : stats.entrySet()) {
            ContractType type = entry.getKey();
            int count = entry.getValue();
            
            if (count > 0) {
                String label = String.format("%s (%d)", formatContractType(type), count);
                PieChart.Data slice = new PieChart.Data(label, count);
                pieChartData.add(slice);
            }
        }
        
        // Appliquer les données
        contractTypePieChart.setData(pieChartData);
        contractTypePieChart.setTitle("Répartition par type de contrat");
        contractTypePieChart.setLegendVisible(true);
        contractTypePieChart.setLabelsVisible(true);
        
        // Ajouter les tooltips avec pourcentages
        for (PieChart.Data data : pieChartData) {
            double percentage = (data.getPieValue() / total) * 100;
            
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    String tooltipText = String.format("%s\n%.0f offre(s)\n%.1f%%", 
                                                      data.getName(), 
                                                      data.getPieValue(), 
                                                      percentage);
                    
                    Tooltip tooltip = new Tooltip(tooltipText);
                    tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: rgba(0,0,0,0.85); " +
                                   "-fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 8px;");
                    Tooltip.install(newNode, tooltip);
                    
                    // Effet hover
                    newNode.setOnMouseEntered(e -> {
                        newNode.setScaleX(1.1);
                        newNode.setScaleY(1.1);
                    });
                    
                    newNode.setOnMouseExited(e -> {
                        newNode.setScaleX(1.0);
                        newNode.setScaleY(1.0);
                    });
                }
            });
        }
        
        // Animation fade in
        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), contractTypePieChart);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
        
    } catch (SQLException e) {
        System.err.println("Erreur chargement PieChart : " + e.getMessage());
    }
}
```

### 5. Méthode helper pour formater les noms

```java
private String formatContractType(ContractType type) {
    switch (type) {
        case CDI: return "CDI";
        case CDD: return "CDD";
        case INTERNSHIP: return "Stage";
        case FREELANCE: return "Freelance";
        case PART_TIME: return "Temps Partiel";
        case FULL_TIME: return "Temps Plein";
        default: return type.name();
    }
}
```

### 6. Dans votre FXML

```xml
<PieChart fx:id="contractTypePieChart" 
          title="Répartition des offres par type de contrat"
          legendVisible="true"
          labelsVisible="true"
          animated="true"
          minHeight="400"
          prefHeight="500"/>
```

## Utilisation

Pour utiliser le PieChart, il vous suffit de :

1. **Copier le code ci-dessus** dans votre contrôleur
2. **Ajouter le PieChart dans votre FXML** avec l'attribut `fx:id="contractTypePieChart"`
3. **Compiler et lancer** l'application

Le PieChart se chargera automatiquement avec les données de votre base !

