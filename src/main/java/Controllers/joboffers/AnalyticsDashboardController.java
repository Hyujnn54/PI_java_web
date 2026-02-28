package Controllers.joboffers;

import Models.joboffers.ContractType;
import Services.joboffers.AnalyticsService;
import Services.joboffers.AnalyticsService.DashboardStats;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Contrôleur pour le tableau de bord Analytics Avancé
 * Affiche des graphiques et statistiques détaillées pour Admin et Recruteur
 */
public class AnalyticsDashboardController {

    @FXML
    private VBox mainContainer;

    private AnalyticsService analyticsService;
    private boolean isAdmin;
    private Long recruiterId;

    @FXML
    public void initialize() {
        analyticsService = new AnalyticsService();
        isAdmin = UserContext.isAdmin();
        recruiterId = UserContext.getRecruiterId();
        buildDashboard();
    }

    private void buildDashboard() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom, #f8f9fa, #e9ecef); -fx-padding: 25;");
        mainContainer.setSpacing(25);

        // === EN-TÊTE AVEC CONTEXTE ===
        VBox headerSection = createHeaderSection();
        mainContainer.getChildren().add(headerSection);

        // ScrollPane pour le contenu
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox content = new VBox(25);
        content.setStyle("-fx-padding: 0 10 20 0;");

        try {
            // 1. Section KPIs principaux
            VBox kpiSection = createKPISection();
            content.getChildren().add(kpiSection);

            // 2. Section Performance
            VBox performanceSection = createPerformanceSection();
            content.getChildren().add(performanceSection);

            // 3. Section Analyses détaillées
            VBox analysisSection = createAnalysisSection();
            content.getChildren().add(analysisSection);

            // 4. Section Tendances
            VBox trendsSection = createTrendsSection();
            content.getChildren().add(trendsSection);

        } catch (SQLException e) {
            Label errorLabel = new Label("❌ Erreur de chargement des statistiques: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px;");
            content.getChildren().add(errorLabel);
        }

        scrollPane.setContent(content);
        mainContainer.getChildren().add(scrollPane);
    }

    /**
     * Crée la section d'en-tête avec titre et contexte
     */
    private VBox createHeaderSection() {
        VBox header = new VBox(10);
        header.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 3);");

        // Ligne de titre
        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        String emoji = isAdmin ? "👑" : "📊";
        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 36px;");

        VBox titleBox = new VBox(3);
        Label title = new Label(isAdmin ? "Tableau de Bord Administrateur" : "Mes Statistiques de Recrutement");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        String subtitleText = isAdmin ?
            "Vue d'ensemble complète de la plateforme TalentBridge" :
            "Analysez les performances de vos offres d'emploi";
        Label subtitle = new Label(subtitleText);
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");

        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        // Date et période
        VBox dateBox = new VBox(3);
        dateBox.setAlignment(Pos.CENTER_RIGHT);

        Label dateLabel = new Label("📅 " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057; -fx-font-weight: 600;");

        Label periodLabel = new Label("Période: 12 derniers mois");
        periodLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

        dateBox.getChildren().addAll(dateLabel, periodLabel);

        titleRow.getChildren().addAll(emojiLabel, titleBox, dateBox);
        header.getChildren().add(titleRow);

        // Badges de contexte
        HBox badgesRow = new HBox(10);
        badgesRow.setAlignment(Pos.CENTER_LEFT);
        badgesRow.setStyle("-fx-padding: 10 0 0 0;");

        String roleBadge = isAdmin ? "🔐 Accès Administrateur" : "👤 " + UserContext.getUserName();
        Label roleLabel = new Label(roleBadge);
        roleLabel.setStyle("-fx-background-color: " + (isAdmin ? "#dc3545" : "#5BA3F5") + "; " +
                          "-fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px;");

        Label statusLabel = new Label("🟢 Données en temps réel");
        statusLabel.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; " +
                            "-fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px;");

        badgesRow.getChildren().addAll(roleLabel, statusLabel);
        header.getChildren().add(badgesRow);

        return header;
    }

    /**
     * Crée la section des KPIs principaux
     */
    private VBox createKPISection() throws SQLException {
        VBox section = new VBox(15);

        // Titre de section
        Label sectionTitle = new Label("📈 Indicateurs Clés de Performance (KPIs)");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        section.getChildren().add(sectionTitle);

        DashboardStats stats = isAdmin ?
            analyticsService.getGlobalStats() :
            analyticsService.getRecruiterStats(recruiterId);

        // Grille de KPIs
        HBox kpiGrid = new HBox(20);
        kpiGrid.setAlignment(Pos.CENTER);

        // KPI 1: Total Offres
        VBox totalCard = createDetailedKPICard(
            "📋", "Total des Offres",
            String.valueOf(stats.getTotalOffers()),
            isAdmin ? "Sur toute la plateforme" : "Créées par vous",
            "#5BA3F5",
            calculateTrend(stats.getTotalOffers(), 10)
        );

        // KPI 2: Offres Actives
        VBox activeCard = createDetailedKPICard(
            "✅", "Offres Actives",
            String.valueOf(stats.getActiveOffers()),
            "En cours de recrutement",
            "#28a745",
            calculatePercentage(stats.getActiveOffers(), stats.getTotalOffers()) + "% du total"
        );

        // KPI 3: Offres Fermées
        VBox closedCard = createDetailedKPICard(
            "🔒", "Offres Fermées",
            String.valueOf(stats.getClosedOffers()),
            "Recrutement terminé",
            "#6c757d",
            calculatePercentage(stats.getClosedOffers(), stats.getTotalOffers()) + "% du total"
        );

        // KPI 4: Offres Signalées
        VBox flaggedCard = createDetailedKPICard(
            "⚠️", "Signalements",
            String.valueOf(stats.getFlaggedOffers()),
            isAdmin ? "À modérer" : "À corriger",
            stats.getFlaggedOffers() > 0 ? "#dc3545" : "#28a745",
            stats.getFlaggedOffers() > 0 ? "Action requise" : "Aucun problème"
        );

        kpiGrid.getChildren().addAll(totalCard, activeCard, closedCard, flaggedCard);

        // KPI supplémentaire pour Admin
        if (isAdmin) {
            VBox recruitersCard = createDetailedKPICard(
                "👥", "Recruteurs Actifs",
                String.valueOf(stats.getActiveRecruiters()),
                "Utilisateurs de la plateforme",
                "#17a2b8",
                "Contributeurs"
            );
            kpiGrid.getChildren().add(recruitersCard);
        }

        section.getChildren().add(kpiGrid);
        return section;
    }

    /**
     * Crée une carte KPI détaillée
     */
    private VBox createDetailedKPICard(String emoji, String title, String value,
                                        String description, String color, String trend) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2); " +
                     "-fx-min-width: 160; -fx-pref-width: 180;");

        Label emojiLabel = new Label(emoji);
        emojiLabel.setStyle("-fx-font-size: 28px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
        descLabel.setWrapText(true);

        Separator sep = new Separator();
        sep.setStyle("-fx-padding: 5 0;");

        Label trendLabel = new Label(trend);
        trendLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + "; -fx-font-weight: 600;");

        card.getChildren().addAll(emojiLabel, valueLabel, titleLabel, descLabel, sep, trendLabel);

        // Effet hover
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 18, 0, 0, 4); " +
            "-fx-min-width: 160; -fx-pref-width: 180; -fx-cursor: hand; " +
            "-fx-border-color: " + color + "; -fx-border-radius: 15; -fx-border-width: 2;"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2); " +
            "-fx-min-width: 160; -fx-pref-width: 180;"));

        return card;
    }

    /**
     * Crée la section de performance avec graphiques
     */
    private VBox createPerformanceSection() throws SQLException {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");

        // Titre
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("📊 Performance Globale");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        Label contextLabel = new Label(isAdmin ? "(Toutes les offres)" : "(Vos offres uniquement)");
        contextLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

        titleRow.getChildren().addAll(sectionTitle, contextLabel);
        section.getChildren().add(titleRow);

        // Graphiques côte à côte
        HBox chartsRow = new HBox(20);
        chartsRow.setAlignment(Pos.TOP_CENTER);

        // Camembert
        VBox pieChartBox = createContractTypePieChart();
        pieChartBox.setPrefWidth(400);

        // Graphique linéaire
        VBox lineChartBox = createMonthlyOffersChart();
        HBox.setHgrow(lineChartBox, Priority.ALWAYS);

        chartsRow.getChildren().addAll(pieChartBox, lineChartBox);
        section.getChildren().add(chartsRow);

        return section;
    }

    /**
     * Crée la section d'analyses détaillées
     */
    private VBox createAnalysisSection() throws SQLException {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");

        // Titre
        Label sectionTitle = new Label("🔍 Analyses Détaillées");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        section.getChildren().add(sectionTitle);

        // Description selon le rôle
        String analysisDesc = isAdmin ?
            "Identifiez les tendances du marché et les besoins en recrutement sur l'ensemble de la plateforme." :
            "Optimisez vos offres en comprenant quelles localisations et compétences attirent le plus de candidats.";
        Label descLabel = new Label(analysisDesc);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-padding: 0 0 10 0;");
        descLabel.setWrapText(true);
        section.getChildren().add(descLabel);

        // Graphiques côte à côte
        HBox chartsRow = new HBox(20);
        chartsRow.setAlignment(Pos.TOP_CENTER);

        // Top localisations
        VBox locationsBox = createTopLocationsChart();
        HBox.setHgrow(locationsBox, Priority.ALWAYS);

        // Top compétences
        VBox skillsBox = createTopSkillsChart();
        HBox.setHgrow(skillsBox, Priority.ALWAYS);

        chartsRow.getChildren().addAll(locationsBox, skillsBox);
        section.getChildren().add(chartsRow);

        return section;
    }

    /**
     * Crée la section des tendances
     */
    private VBox createTrendsSection() throws SQLException {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 2);");

        // Titre avec contexte
        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label sectionTitle = new Label("📅 Activité Récente");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        Label periodBadge = new Label("7 derniers jours");
        periodBadge.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; " +
                            "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 11px;");

        titleRow.getChildren().addAll(sectionTitle, periodBadge);
        section.getChildren().add(titleRow);

        // Insights textuels
        HBox insightsRow = new HBox(20);
        insightsRow.setAlignment(Pos.CENTER_LEFT);
        insightsRow.setStyle("-fx-padding: 10 0;");

        Map<String, Integer> weekData = analyticsService.getOffersThisWeek();
        int totalWeek = weekData.values().stream().mapToInt(Integer::intValue).sum();
        int maxDay = weekData.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String bestDay = weekData.entrySet().stream()
            .filter(e -> e.getValue() == maxDay)
            .map(Map.Entry::getKey)
            .findFirst().orElse("N/A");

        VBox insight1 = createInsightCard("📝", "Offres cette semaine", String.valueOf(totalWeek), "#5BA3F5");
        VBox insight2 = createInsightCard("🏆", "Jour le plus actif", bestDay, "#28a745");
        VBox insight3 = createInsightCard("📈", "Pic d'activité", maxDay + " offres", "#ffc107");

        insightsRow.getChildren().addAll(insight1, insight2, insight3);
        section.getChildren().add(insightsRow);

        // Graphique d'activité
        VBox activityChart = createWeekActivityChart();
        section.getChildren().add(activityChart);

        return section;
    }

    /**
     * Crée une carte d'insight compact
     */
    private VBox createInsightCard(String emoji, String label, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 10; -fx-padding: 15; " +
                     "-fx-border-color: " + color + "; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setPrefWidth(200);

        Label emojiLabel = new Label(emoji + " " + label);
        emojiLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #495057;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(emojiLabel, valueLabel);
        return card;
    }

    /**
     * Crée le camembert des types de contrat avec valeurs et pourcentages
     */
    private VBox createContractTypePieChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("🥧 Répartition par Type de Contrat");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<ContractType, Integer> data = isAdmin ?
            analyticsService.getOffersByContractType() :
            analyticsService.getOffersByContractType(recruiterId);

        // Calculer le total
        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        // Afficher le total
        Label totalLabel = new Label("📊 Total: " + total + " offres");
        totalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #28a745; -fx-font-weight: 600; -fx-padding: 5 0;");
        container.getChildren().add(totalLabel);

        if (total == 0) {
            Label emptyLabel = new Label("Aucune donnée disponible");
            emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            container.getChildren().add(emptyLabel);
            return container;
        }

        PieChart pieChart = new PieChart();
        pieChart.setLegendVisible(false); // On crée notre propre légende
        pieChart.setLabelsVisible(true);
        pieChart.setStartAngle(90);

        for (Map.Entry<ContractType, Integer> entry : data.entrySet()) {
            if (entry.getValue() > 0) {
                double percentage = (entry.getValue() * 100.0) / total;
                String name = formatContractType(entry.getKey()) + " (" + String.format("%.1f", percentage) + "%)";
                PieChart.Data slice = new PieChart.Data(name, entry.getValue());
                pieChart.getData().add(slice);
            }
        }

        pieChart.setPrefHeight(250);
        container.getChildren().add(pieChart);

        // Légende détaillée avec valeurs
        VBox legendBox = new VBox(5);
        legendBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 10;");

        Label legendTitle = new Label("📋 Détail par type:");
        legendTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        legendBox.getChildren().add(legendTitle);

        String[] colors = {"#5BA3F5", "#28a745", "#ffc107", "#dc3545", "#17a2b8", "#6f42c1"};
        int colorIdx = 0;

        for (Map.Entry<ContractType, Integer> entry : data.entrySet()) {
            if (entry.getValue() > 0) {
                double percentage = (entry.getValue() * 100.0) / total;
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Region colorDot = new Region();
                colorDot.setMinSize(12, 12);
                colorDot.setMaxSize(12, 12);
                colorDot.setStyle("-fx-background-color: " + colors[colorIdx % colors.length] + "; -fx-background-radius: 6;");

                Label itemLabel = new Label(String.format("%s: %d offres (%.1f%%)",
                    formatContractType(entry.getKey()), entry.getValue(), percentage));
                itemLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #495057;");

                row.getChildren().addAll(colorDot, itemLabel);
                legendBox.getChildren().add(row);
                colorIdx++;
            }
        }

        container.getChildren().add(legendBox);
        return container;
    }

    /**
     * Crée le graphique des offres par mois avec valeurs numériques
     */
    private VBox createMonthlyOffersChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("📈 Évolution des Offres (12 mois)");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<String, Integer> data = isAdmin ?
            analyticsService.getOffersByMonth() :
            analyticsService.getOffersByMonth(recruiterId);

        // Calculer les statistiques
        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = data.values().stream().filter(v -> v > 0).mapToInt(Integer::intValue).min().orElse(0);
        double avg = data.isEmpty() ? 0 : (double) total / data.size();

        // Afficher les statistiques
        HBox statsBox = new HBox(15);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.setStyle("-fx-padding: 5 0;");

        Label totalLbl = new Label("📊 Total: " + total);
        totalLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #5BA3F5; -fx-font-weight: 600;");

        Label avgLbl = new Label("📈 Moyenne: " + String.format("%.1f", avg) + "/mois");
        avgLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745; -fx-font-weight: 600;");

        Label maxLbl = new Label("🔝 Max: " + max);
        maxLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffc107; -fx-font-weight: 600;");

        statsBox.getChildren().addAll(totalLbl, avgLbl, maxLbl);
        container.getChildren().add(statsBox);

        if (total == 0) {
            Label emptyLabel = new Label("Aucune donnée sur cette période");
            emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            container.getChildren().add(emptyLabel);
            return container;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Nombre d'offres");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setLegendVisible(false);
        lineChart.setCreateSymbols(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Offres");

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String monthLabel = formatMonthLabel(entry.getKey());
            series.getData().add(new XYChart.Data<>(monthLabel, entry.getValue()));
        }

        lineChart.getData().add(series);
        lineChart.setPrefHeight(250);
        container.getChildren().add(lineChart);

        // Tableau des valeurs
        FlowPane valuesPane = new FlowPane(8, 5);
        valuesPane.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 8;");

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            if (entry.getValue() > 0) {
                Label valueLbl = new Label(formatMonthLabel(entry.getKey()) + ": " + entry.getValue());
                valueLbl.setStyle("-fx-font-size: 10px; -fx-background-color: white; -fx-padding: 3 6; " +
                                 "-fx-background-radius: 4; -fx-text-fill: #495057;");
                valuesPane.getChildren().add(valueLbl);
            }
        }

        if (!valuesPane.getChildren().isEmpty()) {
            container.getChildren().add(valuesPane);
        }

        return container;
    }

    /**
     * Crée le graphique des top localisations avec valeurs
     */
    private VBox createTopLocationsChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("📍 Top 5 des Localisations");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<String, Integer> data = analyticsService.getTopLocations(5);

        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        // Explication
        Label explainLabel = new Label("🔍 Villes avec le plus d'offres publiées");
        explainLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        container.getChildren().add(explainLabel);

        if (data.isEmpty() || total == 0) {
            Label emptyLabel = new Label("Aucune donnée de localisation disponible");
            emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            container.getChildren().add(emptyLabel);
            return container;
        }

        // Liste détaillée avec barres de progression
        VBox listBox = new VBox(8);
        listBox.setStyle("-fx-padding: 10 0;");

        int rank = 1;
        int maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            // Rang
            Label rankLabel = new Label("#" + rank);
            rankLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " +
                              (rank == 1 ? "#ffc107" : rank == 2 ? "#6c757d" : rank == 3 ? "#cd7f32" : "#495057") +
                              "; -fx-min-width: 25;");

            // Nom de la ville
            String location = entry.getKey();
            if (location.length() > 20) location = location.substring(0, 17) + "...";
            Label nameLabel = new Label(location);
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-min-width: 120;");

            // Barre de progression
            double percentage = (entry.getValue() * 100.0) / maxValue;
            StackPane progressBar = new StackPane();
            progressBar.setPrefHeight(18);
            progressBar.setPrefWidth(150);

            Region bgBar = new Region();
            bgBar.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 4;");
            bgBar.prefWidthProperty().bind(progressBar.widthProperty());

            Region fillBar = new Region();
            fillBar.setStyle("-fx-background-color: #5BA3F5; -fx-background-radius: 4;");
            fillBar.prefWidthProperty().bind(progressBar.widthProperty().multiply(percentage / 100));
            fillBar.setMaxWidth(Region.USE_PREF_SIZE);

            progressBar.getChildren().addAll(bgBar, fillBar);
            StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);
            HBox.setHgrow(progressBar, Priority.ALWAYS);

            // Valeur
            double totalPercentage = (entry.getValue() * 100.0) / total;
            Label valueLabel = new Label(entry.getValue() + " (" + String.format("%.1f", totalPercentage) + "%)");
            valueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #5BA3F5; -fx-min-width: 70;");

            row.getChildren().addAll(rankLabel, nameLabel, progressBar, valueLabel);
            listBox.getChildren().add(row);
            rank++;
        }

        container.getChildren().add(listBox);

        // Total
        Label totalLabel = new Label("📊 Total top 5: " + total + " offres");
        totalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745; -fx-font-weight: 600;");
        container.getChildren().add(totalLabel);

        return container;
    }

    /**
     * Crée le graphique des top compétences avec valeurs
     */
    private VBox createTopSkillsChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("🎯 Top 10 Compétences Demandées");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<String, Integer> data = analyticsService.getTopSkills(10);

        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        // Explication
        Label explainLabel = new Label("🔍 Compétences les plus recherchées par les recruteurs");
        explainLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        container.getChildren().add(explainLabel);

        if (data.isEmpty() || total == 0) {
            Label emptyLabel = new Label("Aucune compétence enregistrée");
            emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            container.getChildren().add(emptyLabel);
            return container;
        }

        // Liste détaillée avec barres
        VBox listBox = new VBox(6);
        listBox.setStyle("-fx-padding: 10 0;");

        int rank = 1;
        int maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        String[] colors = {"#5BA3F5", "#28a745", "#ffc107", "#17a2b8", "#6f42c1", "#dc3545", "#fd7e14", "#20c997", "#e83e8c", "#6c757d"};

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            // Rang avec médaille pour top 3
            String rankIcon = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : "#" + rank;
            Label rankLabel = new Label(rankIcon);
            rankLabel.setStyle("-fx-font-size: " + (rank <= 3 ? "14px" : "11px") + "; -fx-min-width: 25;");

            // Nom de la compétence
            Label nameLabel = new Label(entry.getKey());
            nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-min-width: 100; -fx-font-weight: " + (rank <= 3 ? "600" : "400") + ";");

            // Barre de progression
            double percentage = (entry.getValue() * 100.0) / maxValue;
            StackPane progressBar = new StackPane();
            progressBar.setPrefHeight(16);
            progressBar.setPrefWidth(120);

            Region bgBar = new Region();
            bgBar.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 3;");
            bgBar.prefWidthProperty().bind(progressBar.widthProperty());

            Region fillBar = new Region();
            fillBar.setStyle("-fx-background-color: " + colors[(rank-1) % colors.length] + "; -fx-background-radius: 3;");
            fillBar.prefWidthProperty().bind(progressBar.widthProperty().multiply(percentage / 100));
            fillBar.setMaxWidth(Region.USE_PREF_SIZE);

            progressBar.getChildren().addAll(bgBar, fillBar);
            StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);
            HBox.setHgrow(progressBar, Priority.ALWAYS);

            // Valeur
            double totalPercentage = (entry.getValue() * 100.0) / total;
            Label valueLabel = new Label(entry.getValue() + " demandes (" + String.format("%.1f", totalPercentage) + "%)");
            valueLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + colors[(rank-1) % colors.length] + "; -fx-font-weight: 600;");

            row.getChildren().addAll(rankLabel, nameLabel, progressBar, valueLabel);
            listBox.getChildren().add(row);
            rank++;
        }

        container.getChildren().add(listBox);

        // Total
        Label totalLabel = new Label("📊 Total des demandes: " + total);
        totalLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745; -fx-font-weight: 600;");
        container.getChildren().add(totalLabel);

        return container;
    }

    /**
     * Crée le graphique d'activité de la semaine avec valeurs détaillées
     */
    private VBox createWeekActivityChart() throws SQLException {
        VBox container = new VBox(12);

        Map<String, Integer> data = analyticsService.getOffersThisWeek();

        // Calculer les statistiques
        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        int max = data.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String bestDay = data.entrySet().stream()
            .filter(e -> e.getValue() == max)
            .map(Map.Entry::getKey)
            .findFirst().orElse("N/A");

        // Explication
        Label explainLabel = new Label("🔍 Nombre d'offres créées chaque jour de la semaine");
        explainLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        container.getChildren().add(explainLabel);

        // Statistiques de la semaine
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER_LEFT);
        statsBox.setStyle("-fx-padding: 10 0;");

        VBox totalBox = new VBox(2);
        totalBox.setAlignment(Pos.CENTER);
        totalBox.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 10; -fx-background-radius: 8;");
        Label totalNum = new Label(String.valueOf(total));
        totalNum.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #1976d2;");
        Label totalLbl = new Label("offres cette semaine");
        totalLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #1976d2;");
        totalBox.getChildren().addAll(totalNum, totalLbl);

        VBox avgBox = new VBox(2);
        avgBox.setAlignment(Pos.CENTER);
        avgBox.setStyle("-fx-background-color: #e8f5e9; -fx-padding: 10; -fx-background-radius: 8;");
        double avg = total / 7.0;
        Label avgNum = new Label(String.format("%.1f", avg));
        avgNum.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #388e3c;");
        Label avgLbl = new Label("moyenne/jour");
        avgLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #388e3c;");
        avgBox.getChildren().addAll(avgNum, avgLbl);

        VBox bestBox = new VBox(2);
        bestBox.setAlignment(Pos.CENTER);
        bestBox.setStyle("-fx-background-color: #fff3e0; -fx-padding: 10; -fx-background-radius: 8;");
        Label bestNum = new Label(bestDay);
        bestNum.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #f57c00;");
        Label bestLbl = new Label("jour le plus actif (" + max + ")");
        bestLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #f57c00;");
        bestBox.getChildren().addAll(bestNum, bestLbl);

        statsBox.getChildren().addAll(totalBox, avgBox, bestBox);
        container.getChildren().add(statsBox);

        // Affichage jour par jour avec barres
        VBox daysBox = new VBox(6);
        daysBox.setStyle("-fx-padding: 10 0;");

        String[] joursFull = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
        String[] joursShort = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        int maxForBar = max > 0 ? max : 1;

        for (int i = 0; i < joursShort.length; i++) {
            String jour = joursShort[i];
            int value = data.getOrDefault(jour, 0);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);

            // Nom du jour
            Label dayLabel = new Label(joursFull[i]);
            dayLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057; -fx-min-width: 70;");

            // Barre de progression
            double percentage = (value * 100.0) / maxForBar;
            StackPane progressBar = new StackPane();
            progressBar.setPrefHeight(20);
            progressBar.setPrefWidth(200);

            Region bgBar = new Region();
            bgBar.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 4;");
            bgBar.prefWidthProperty().bind(progressBar.widthProperty());

            String barColor = value == max && max > 0 ? "#28a745" : "#5BA3F5";
            Region fillBar = new Region();
            fillBar.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 4;");
            fillBar.prefWidthProperty().bind(progressBar.widthProperty().multiply(percentage / 100));
            fillBar.setMaxWidth(Region.USE_PREF_SIZE);

            progressBar.getChildren().addAll(bgBar, fillBar);
            StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);
            HBox.setHgrow(progressBar, Priority.ALWAYS);

            // Valeur
            Label valueLabel = new Label(value + " offre" + (value > 1 ? "s" : ""));
            valueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + barColor + "; -fx-min-width: 60;");

            // Indicateur si meilleur jour
            if (value == max && max > 0) {
                Label starLabel = new Label("🏆");
                starLabel.setStyle("-fx-font-size: 14px;");
                row.getChildren().addAll(dayLabel, progressBar, valueLabel, starLabel);
            } else {
                row.getChildren().addAll(dayLabel, progressBar, valueLabel);
            }

            daysBox.getChildren().add(row);
        }

        container.getChildren().add(daysBox);

        return container;
    }

    // === MÉTHODES UTILITAIRES ===

    private String formatContractType(ContractType type) {
        return switch (type) {
            case CDI -> "CDI";
            case CDD -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE -> "Freelance";
            case PART_TIME -> "Temps Partiel";
            case FULL_TIME -> "Temps Plein";
        };
    }

    private String formatMonthLabel(String yearMonth) {
        String[] parts = yearMonth.split("-");
        if (parts.length != 2) return yearMonth;

        String[] mois = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};
        int monthNum = Integer.parseInt(parts[1]);
        String year = parts[0].substring(2);

        return mois[monthNum - 1] + " " + year;
    }

    private String calculateTrend(int current, int baseline) {
        if (baseline == 0) return "Nouveau";
        int diff = current - baseline;
        if (diff > 0) return "↑ +" + diff + " ce mois";
        if (diff < 0) return "↓ " + diff + " ce mois";
        return "→ Stable";
    }

    private String calculatePercentage(int part, int total) {
        if (total == 0) return "0";
        return String.format("%.0f", (part * 100.0) / total);
    }

    public void refresh() {
        buildDashboard();
    }
}









