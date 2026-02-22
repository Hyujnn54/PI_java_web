package Controllers;

import Models.ContractType;
import Services.JobOfferService;
import Utils.UserContext;
import javafx.animation.FadeTransition;
import javafx.animation.RotateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;

import java.sql.SQLException;
import java.util.Map;

/**
 * Contrôleur pour les statistiques des offres d'emploi
 * Gestion des rôles : ADMIN (statistiques globales), RECRUITER (statistiques personnelles), CANDIDATE (pas de statistiques)
 */
public class StatisticsController {

    @FXML
    private PieChart contractTypePieChart;

    @FXML
    private Label totalOffersLabel;

    @FXML
    private Label expiredOffersLabel;

    @FXML
    private Label roleInfoLabel;

    private JobOfferService jobOfferService;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        loadStatistics();
    }

    /**
     * Charge les statistiques en fonction du rôle de l'utilisateur
     */
    private void loadStatistics() {
        UserContext.Role currentRole = UserContext.getRole();
        Long userId = UserContext.getUserId();

        // Afficher les informations de rôle
        if (roleInfoLabel != null) {
            roleInfoLabel.setText("Statistiques - " + currentRole.getLabel());
            roleInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3498db;");
        }

        // Vérifier le rôle
        if (currentRole == UserContext.Role.CANDIDATE) {
            // Les candidats n'ont pas accès aux statistiques
            hideStatistics();
            showAlert("Accès refusé", "Les candidats n'ont pas accès aux statistiques.", Alert.AlertType.WARNING);
            return;
        }

        // Charger les statistiques selon le rôle
        if (currentRole == UserContext.Role.ADMIN) {
            loadAdminStatistics();
        } else if (currentRole == UserContext.Role.RECRUITER) {
            loadRecruiterStatistics(userId);
        }
    }

    /**
     * Charge les statistiques globales pour ADMIN
     */
    private void loadAdminStatistics() {
        try {
            // Statistiques par type de contrat
            Map<ContractType, Integer> stats = jobOfferService.statsGlobal();
            displayPieChart(stats, "Répartition globale des offres par type de contrat");

            // Nombre total d'offres
            int totalOffers = jobOfferService.getTotalOffresGlobal();
            if (totalOffersLabel != null) {
                totalOffersLabel.setText("Total des offres : " + totalOffers);
                totalOffersLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            }

            // Nombre d'offres expirées
            int expiredOffers = jobOfferService.getExpiredOffresGlobal();
            if (expiredOffersLabel != null) {
                expiredOffersLabel.setText("Offres expirées : " + expiredOffers);
                expiredOffersLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            }

            // Animation
            animatePieChart();

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les statistiques : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Charge les statistiques personnelles pour RECRUITER
     */
    private void loadRecruiterStatistics(Long recruiterId) {
        try {
            // Statistiques par type de contrat
            Map<ContractType, Integer> stats = jobOfferService.statsByRecruiter(recruiterId);
            displayPieChart(stats, "Répartition de vos offres par type de contrat");

            // Nombre total d'offres
            int totalOffers = jobOfferService.getTotalOffresByRecruiter(recruiterId);
            if (totalOffersLabel != null) {
                totalOffersLabel.setText("Vos offres : " + totalOffers);
                totalOffersLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            }

            // Nombre d'offres expirées
            int expiredOffers = jobOfferService.getExpiredOffresByRecruiter(recruiterId);
            if (expiredOffersLabel != null) {
                expiredOffersLabel.setText("Vos offres expirées : " + expiredOffers);
                expiredOffersLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            }

            // Animation
            animatePieChart();

        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger vos statistiques : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Affiche le PieChart avec les statistiques données
     */
    private void displayPieChart(Map<ContractType, Integer> stats, String title) {
        // Calculer le total pour les pourcentages
        int total = stats.values().stream().mapToInt(Integer::intValue).sum();

        // Si aucune donnée, afficher un message
        if (total == 0) {
            contractTypePieChart.setTitle(title + " (Aucune donnée)");
            contractTypePieChart.setData(FXCollections.observableArrayList());
            return;
        }

        // Créer les données pour le PieChart
        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();

        for (Map.Entry<ContractType, Integer> entry : stats.entrySet()) {
            ContractType type = entry.getKey();
            int count = entry.getValue();

            // Ignorer les types avec 0 offres pour un graphique plus propre
            if (count > 0) {
                // Créer le label avec le nom et le nombre
                String label = String.format("%s (%d)", formatContractType(type), count);
                PieChart.Data slice = new PieChart.Data(label, count);
                pieChartData.add(slice);
            }
        }

        // Ajouter les données au PieChart
        contractTypePieChart.setData(pieChartData);

        // Configurer l'apparence du PieChart
        contractTypePieChart.setTitle(title);
        contractTypePieChart.setLegendVisible(true);
        contractTypePieChart.setLabelsVisible(true);
        contractTypePieChart.setStartAngle(90);
        contractTypePieChart.setVisible(true);

        // Ajouter des tooltips et des effets visuels
        addTooltipsAndEffects(pieChartData, total);
    }

    /**
     * Cache les statistiques (pour les candidats)
     */
    private void hideStatistics() {
        if (contractTypePieChart != null) {
            contractTypePieChart.setVisible(false);
        }
        if (totalOffersLabel != null) {
            totalOffersLabel.setText("Accès refusé");
            totalOffersLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        }
        if (expiredOffersLabel != null) {
            expiredOffersLabel.setText("");
        }
    }

    /**
     * Ajoute des tooltips personnalisés et des effets visuels aux sections du PieChart
     */
    private void addTooltipsAndEffects(ObservableList<PieChart.Data> pieChartData, int total) {
        for (PieChart.Data data : pieChartData) {
            // Calculer le pourcentage
            double percentage = (data.getPieValue() / total) * 100;

            // Créer un tooltip avec des informations détaillées
            String tooltipText = String.format("%s\n%.0f offre(s)\n%.1f%%",
                                              data.getName(),
                                              data.getPieValue(),
                                              percentage);

            // Attendre que le nœud soit créé par JavaFX
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) {
                    // Installer le tooltip
                    Tooltip tooltip = new Tooltip(tooltipText);
                    tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: rgba(0,0,0,0.8); " +
                                   "-fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 8px;");
                    Tooltip.install(newNode, tooltip);

                    // Effet hover : agrandir légèrement la section
                    newNode.setOnMouseEntered(e -> {
                        newNode.setScaleX(1.1);
                        newNode.setScaleY(1.1);
                        newNode.setStyle("-fx-cursor: hand;");
                    });

                    newNode.setOnMouseExited(e -> {
                        newNode.setScaleX(1.0);
                        newNode.setScaleY(1.0);
                    });

                    // Effet clic : afficher les détails
                    newNode.setOnMouseClicked(e -> {
                        showContractTypeDetails(data.getName(), (int) data.getPieValue(), percentage);
                    });
                }
            });
        }
    }

    /**
     * Animation d'apparition du PieChart
     */
    private void animatePieChart() {
        if (contractTypePieChart == null || !contractTypePieChart.isVisible()) {
            return;
        }

        // Fade in
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(1000), contractTypePieChart);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);

        // Légère rotation
        RotateTransition rotateTransition = new RotateTransition(Duration.millis(1000), contractTypePieChart);
        rotateTransition.setByAngle(360);

        // Jouer les animations
        fadeTransition.play();
        // rotateTransition.play(); // Décommenter si vous voulez la rotation
    }


    /**
     * Formate le nom du type de contrat pour l'affichage
     */
    private String formatContractType(ContractType type) {
        switch (type) {
            case CDI:
                return "CDI";
            case CDD:
                return "CDD";
            case INTERNSHIP:
                return "Stage";
            case FREELANCE:
                return "Freelance";
            case PART_TIME:
                return "Temps Partiel";
            case FULL_TIME:
                return "Temps Plein";
            default:
                return type.name();
        }
    }

    /**
     * Affiche les détails d'un type de contrat
     */
    private void showContractTypeDetails(String name, int count, double percentage) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du type de contrat");
        alert.setHeaderText(name);
        alert.setContentText(String.format("Nombre d'offres : %d\nPourcentage : %.1f%%", count, percentage));
        alert.showAndWait();
    }

    /**
     * Rafraîchit les statistiques
     */
    @FXML
    private void handleRefresh() {
        loadStatistics();
    }

    /**
     * Affiche une alerte
     */
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

