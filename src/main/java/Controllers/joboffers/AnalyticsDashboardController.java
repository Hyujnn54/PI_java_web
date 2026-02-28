package Controllers.joboffers;

import Models.joboffers.ContractType;
import Services.joboffers.AnalyticsService;
import Services.joboffers.AnalyticsService.DashboardStats;
import Utils.UserContext;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class AnalyticsDashboardController {

    @FXML private VBox mainContainer;

    private AnalyticsService analyticsService;
    private boolean isAdmin;
    private Long recruiterId;

    @FXML
    public void initialize() {
        isAdmin     = UserContext.isAdmin();
        recruiterId = UserContext.getRecruiterId();
        buildDashboard();
    }

    // =========================================================================
    // Main builder
    // =========================================================================

    private void buildDashboard() {
        analyticsService = new AnalyticsService();
        if (mainContainer == null) return;

        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: linear-gradient(to bottom,#f8f9fa,#e9ecef); -fx-padding: 25;");
        mainContainer.setSpacing(25);

        VBox headerNode = createHeaderSection();
        animateFadeIn(headerNode, 0);
        mainContainer.getChildren().add(headerNode);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(25);
        content.setStyle("-fx-padding: 0 10 20 0;");

        try {
            VBox kpi = createKPISection();
            VBox perf = createPerformanceSection();
            VBox analysis = createAnalysisSection();
            VBox trends = createTrendsSection();

            animateFadeIn(kpi,      80);
            animateFadeIn(perf,     160);
            animateFadeIn(analysis, 240);
            animateFadeIn(trends,   320);

            content.getChildren().addAll(kpi, perf, analysis, trends);
        } catch (SQLException e) {
            Label err = new Label("Erreur chargement statistiques: " + e.getMessage());
            err.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px;");
            content.getChildren().add(err);
            e.printStackTrace();
        }

        scroll.setContent(content);
        mainContainer.getChildren().add(scroll);
    }

    /** Fade + slide-up entrance animation with delay (ms) */
    private void animateFadeIn(javafx.scene.Node node, double delayMs) {
        node.setOpacity(0);
        node.setTranslateY(18);
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(delayMs)),
            new KeyFrame(Duration.millis(delayMs + 400),
                new KeyValue(node.opacityProperty(), 1),
                new KeyValue(node.translateYProperty(), 0))
        );
        tl.play();
    }

    // =========================================================================
    // Header
    // =========================================================================

    private VBox createHeaderSection() {
        VBox header = new VBox(10);
        header.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.1),15,0,0,3);");

        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label emojiLbl = new Label(isAdmin ? "\uD83D\uDC51" : "\uD83D\uDCCA");
        emojiLbl.setStyle("-fx-font-size: 36px;");

        VBox titleBox = new VBox(3);
        Label title = new Label(isAdmin ? "Tableau de Bord Administrateur" : "Mes Statistiques de Recrutement");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        Label subtitle = new Label(isAdmin
                ? "Vue d'ensemble complete de la plateforme TalentBridge"
                : "Analysez les performances de vos offres d'emploi");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
        titleBox.getChildren().addAll(title, subtitle);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        VBox dateBox = new VBox(3);
        dateBox.setAlignment(Pos.CENTER_RIGHT);
        Label dateLbl = new Label("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057; -fx-font-weight: 600;");
        Label periodLbl = new Label("Periode: 12 derniers mois");
        periodLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
        dateBox.getChildren().addAll(dateLbl, periodLbl);

        titleRow.getChildren().addAll(emojiLbl, titleBox, dateBox);
        header.getChildren().add(titleRow);

        HBox badges = new HBox(10);
        badges.setAlignment(Pos.CENTER_LEFT);
        badges.setStyle("-fx-padding: 10 0 0 0;");

        Label roleLbl = new Label(isAdmin ? "Acces Administrateur" : "Recruteur: " + UserContext.getUserName());
        roleLbl.setStyle("-fx-background-color: " + (isAdmin ? "#dc3545" : "#5BA3F5") + "; " +
                "-fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px;");

        Label statusLbl = new Label("Donnees en temps reel");
        statusLbl.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; " +
                "-fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 11px;");

        badges.getChildren().addAll(roleLbl, statusLbl);
        header.getChildren().add(badges);
        return header;
    }

    // =========================================================================
    // KPI Section
    // =========================================================================

    private VBox createKPISection() throws SQLException {
        VBox section = new VBox(15);

        Label secTitle = new Label("Indicateurs Cles de Performance (KPIs)");
        secTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        section.getChildren().add(secTitle);

        DashboardStats stats;
        if (isAdmin) {
            stats = analyticsService.getGlobalStats();
        } else {
            if (recruiterId == null) {
                Label err = new Label("Impossible de charger les stats : identifiant recruteur introuvable.");
                err.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 13px;");
                section.getChildren().add(err);
                return section;
            }
            stats = analyticsService.getRecruiterStats(recruiterId);
        }

        HBox grid = new HBox(20);
        grid.setAlignment(Pos.CENTER);

        grid.getChildren().addAll(
            kpiCard("Total Offres",    String.valueOf(stats.getTotalOffers()),
                    isAdmin ? "Sur toute la plateforme" : "Creees par vous",
                    "#5BA3F5", pct(stats.getTotalOffers(), stats.getTotalOffers()) + "% actif"),
            kpiCard("Offres Actives",  String.valueOf(stats.getActiveOffers()),
                    "En cours de recrutement",
                    "#28a745", pct(stats.getActiveOffers(), stats.getTotalOffers()) + "% du total"),
            kpiCard("Offres Fermees",  String.valueOf(stats.getClosedOffers()),
                    "Recrutement termine",
                    "#6c757d", pct(stats.getClosedOffers(), stats.getTotalOffers()) + "% du total"),
            kpiCard("Signalements",    String.valueOf(stats.getFlaggedOffers()),
                    isAdmin ? "A moderer" : "A corriger",
                    stats.getFlaggedOffers() > 0 ? "#dc3545" : "#28a745",
                    stats.getFlaggedOffers() > 0 ? "Action requise" : "Aucun probleme")
        );

        if (isAdmin) {
            grid.getChildren().add(
                kpiCard("Recruteurs Actifs", String.valueOf(stats.getActiveRecruiters()),
                        "Utilisateurs de la plateforme", "#17a2b8", "Contributeurs")
            );
        }

        section.getChildren().add(grid);
        return section;
    }

    private VBox kpiCard(String title, String value, String desc, String color, String trend) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,2); " +
                "-fx-min-width: 160; -fx-pref-width: 185; -fx-cursor: hand;");

        // Colored top accent bar
        Rectangle accent = new Rectangle(185, 5);
        accent.setFill(Color.web(color));
        accent.setArcWidth(10); accent.setArcHeight(10);
        card.getChildren().add(accent);

        // Animated counter label
        Label valLbl = new Label("0");
        valLbl.setStyle("-fx-font-size: 36px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");

        // Try to parse numeric value and animate it
        try {
            int target = Integer.parseInt(value.trim());
            animateCounter(valLbl, 0, target, 800);
        } catch (NumberFormatException ex) {
            valLbl.setText(value);
        }

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
        descLbl.setWrapText(true);

        Separator sep = new Separator();

        Label trendLbl = new Label(trend);
        trendLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + "; -fx-font-weight: 600;");

        // Tooltip with extra info
        Tooltip tip = new Tooltip(title + "\n" + desc + "\n" + trend);
        tip.setStyle("-fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: white; " +
                "-fx-background-radius: 8; -fx-padding: 8 12;");
        tip.setShowDelay(Duration.millis(200));
        Tooltip.install(card, tip);

        card.getChildren().addAll(valLbl, titleLbl, descLbl, sep, trendLbl);

        // Hover: scale up + deeper shadow
        String baseStyle = "-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-min-width: 160; -fx-pref-width: 185; -fx-cursor: hand;";
        card.setOnMouseEntered(e -> {
            card.setStyle(baseStyle +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.22),22,0,0,6); " +
                    "-fx-border-color: " + color + "; -fx-border-radius: 15; -fx-border-width: 2;");
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.04); st.setToY(1.04); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle +
                    "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,2);");
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        // Click: show detail popup
        card.setOnMouseClicked(e -> showKpiDetail(title, value, desc, trend, color));

        return card;
    }

    /** Animates a label from 0 to target over durationMs */
    private void animateCounter(Label label, int from, int to, int durationMs) {
        if (to == 0) { label.setText("0"); return; }
        Timeline tl = new Timeline();
        int steps = Math.min(to, 40);
        for (int i = 0; i <= steps; i++) {
            final int displayed = from + (int) Math.round((to - from) * ((double) i / steps));
            final double ms = durationMs * ((double) i / steps);
            tl.getKeyFrames().add(new KeyFrame(Duration.millis(ms),
                ev -> label.setText(String.valueOf(displayed))));
        }
        tl.play();
    }

    /** Popup dialog with detailed KPI info */
    private void showKpiDetail(String title, String value, String desc, String trend, String color) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle(title);

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 16;");
        root.setPrefWidth(320);

        // Colored circle value display
        StackPane circle = new StackPane();
        circle.setMinSize(100, 100); circle.setMaxSize(100, 100);
        circle.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 50; " +
                "-fx-border-color: " + color + "; -fx-border-radius: 50; -fx-border-width: 3;");

        Label bigVal = new Label(value);
        bigVal.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");
        circle.getChildren().add(bigVal);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");
        descLbl.setWrapText(true);
        descLbl.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Separator sep = new Separator();

        Label trendLbl = new Label("Tendance: " + trend);
        trendLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: 600;");

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Fermer");
        closeBtn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 8 24; -fx-background-radius: 20; -fx-font-weight: 600; -fx-cursor: hand;");
        closeBtn.setOnAction(ev -> popup.close());

        root.getChildren().addAll(circle, titleLbl, descLbl, sep, trendLbl, closeBtn);

        // Fade in the popup
        root.setOpacity(0);
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(200),
                new KeyValue(root.opacityProperty(), 1)));

        popup.setScene(new javafx.scene.Scene(root));
        popup.show();
        tl.play();
    }

    // =========================================================================
    // Performance Section
    // =========================================================================

    private VBox createPerformanceSection() throws SQLException {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,2);");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label secTitle = new Label("Performance Globale");
        secTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        Label ctxLbl = new Label(isAdmin ? "(Toutes les offres)" : "(Vos offres uniquement)");
        ctxLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        titleRow.getChildren().addAll(secTitle, ctxLbl);
        section.getChildren().add(titleRow);

        HBox charts = new HBox(20);
        charts.setAlignment(Pos.TOP_CENTER);

        VBox pie = createContractTypePieChart();
        pie.setPrefWidth(400);
        VBox line = createMonthlyOffersChart();
        HBox.setHgrow(line, Priority.ALWAYS);

        charts.getChildren().addAll(pie, line);
        section.getChildren().add(charts);
        return section;
    }

    // =========================================================================
    // Analysis Section
    // =========================================================================

    private VBox createAnalysisSection() throws SQLException {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,2);");

        Label secTitle = new Label("Analyses Detaillees");
        secTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        section.getChildren().add(secTitle);

        Label desc = new Label(isAdmin
                ? "Identifiez les tendances du marche et les besoins en recrutement sur l'ensemble de la plateforme."
                : "Optimisez vos offres en comprenant quelles localisations et competences attirent le plus de candidats.");
        desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-padding: 0 0 10 0;");
        desc.setWrapText(true);
        section.getChildren().add(desc);

        HBox charts = new HBox(20);
        charts.setAlignment(Pos.TOP_CENTER);

        VBox locs = createTopLocationsChart();
        HBox.setHgrow(locs, Priority.ALWAYS);
        VBox skills = createTopSkillsChart();
        HBox.setHgrow(skills, Priority.ALWAYS);

        charts.getChildren().addAll(locs, skills);
        section.getChildren().add(charts);
        return section;
    }

    // =========================================================================
    // Trends Section
    // =========================================================================

    private VBox createTrendsSection() throws SQLException {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,2);");

        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label secTitle = new Label("Activite Recente");
        secTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        Label badge = new Label("7 derniers jours");
        badge.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; " +
                "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-size: 11px;");
        titleRow.getChildren().addAll(secTitle, badge);
        section.getChildren().add(titleRow);

        Map<String, Integer> weekData = analyticsService.getOffersThisWeek();
        int totalWeek = weekData.values().stream().mapToInt(Integer::intValue).sum();
        int maxDay    = weekData.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String bestDay = weekData.entrySet().stream()
                .filter(e -> e.getValue() == maxDay)
                .map(Map.Entry::getKey)
                .findFirst().orElse("N/A");

        HBox insights = new HBox(20);
        insights.setAlignment(Pos.CENTER_LEFT);
        insights.setStyle("-fx-padding: 10 0;");
        insights.getChildren().addAll(
            insightCard("Offres cette semaine", String.valueOf(totalWeek), "#5BA3F5"),
            insightCard("Jour le plus actif",   bestDay,                  "#28a745"),
            insightCard("Pic d'activite",        maxDay + " offres",       "#ffc107")
        );
        section.getChildren().add(insights);
        section.getChildren().add(createWeekActivityChart());
        return section;
    }

    private VBox insightCard(String label, String value, String color) {
        VBox card = new VBox(5);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 10; -fx-padding: 15; " +
                "-fx-border-color: " + color + "; -fx-border-radius: 10; -fx-border-width: 1;");
        card.setPrefWidth(200);

        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #495057;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    // =========================================================================
    // Charts
    // =========================================================================

    private VBox createContractTypePieChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("Repartition par Type de Contrat");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<ContractType, Integer> data = (isAdmin || recruiterId == null)
                ? analyticsService.getOffersByContractType()
                : analyticsService.getOffersByContractType(recruiterId);

        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        Label totalLbl = new Label("Total: " + total + " offres");
        totalLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #28a745; -fx-font-weight: 600; -fx-padding: 5 0;");
        container.getChildren().add(totalLbl);

        if (total == 0) {
            container.getChildren().add(emptyLabel("Aucune donnee disponible"));
            return container;
        }

        PieChart pie = new PieChart();
        pie.setLegendVisible(false);
        pie.setLabelsVisible(true);
        pie.setStartAngle(90);

        for (Map.Entry<ContractType, Integer> e : data.entrySet()) {
            if (e.getValue() > 0) {
                double pct = (e.getValue() * 100.0) / total;
                pie.getData().add(new PieChart.Data(
                        fmtContract(e.getKey()) + " (" + String.format("%.1f", pct) + "%)",
                        e.getValue()));
            }
        }
        pie.setPrefHeight(250);

        // Animate: fade + scale each slice in after added to scene
        pie.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                javafx.application.Platform.runLater(() -> {
                    for (PieChart.Data d : pie.getData()) {
                        javafx.scene.Node node = d.getNode();
                        if (node == null) continue;
                        node.setScaleX(0); node.setScaleY(0); node.setOpacity(0);
                        Timeline tl = new Timeline(
                            new KeyFrame(Duration.ZERO,
                                new KeyValue(node.scaleXProperty(), 0),
                                new KeyValue(node.scaleYProperty(), 0),
                                new KeyValue(node.opacityProperty(), 0)),
                            new KeyFrame(Duration.millis(550),
                                new KeyValue(node.scaleXProperty(), 1),
                                new KeyValue(node.scaleYProperty(), 1),
                                new KeyValue(node.opacityProperty(), 1))
                        );
                        tl.play();
                        // Tooltip on each slice
                        Tooltip sliceTip = new Tooltip(d.getName() + "\n" + (int) d.getPieValue() + " offres");
                        sliceTip.setStyle("-fx-font-size:12px; -fx-background-color:#1E2D3D; " +
                                "-fx-text-fill:white; -fx-background-radius:8; -fx-padding:7 12;");
                        sliceTip.setShowDelay(Duration.millis(80));
                        Tooltip.install(node, sliceTip);
                        // Hover pop-out
                        node.setOnMouseEntered(e -> {
                            ScaleTransition st = new ScaleTransition(Duration.millis(140), node);
                            st.setToX(1.10); st.setToY(1.10); st.play();
                        });
                        node.setOnMouseExited(e -> {
                            ScaleTransition st = new ScaleTransition(Duration.millis(140), node);
                            st.setToX(1.0); st.setToY(1.0); st.play();
                        });
                    }
                });
            }
        });

        container.getChildren().add(pie);

        // Legend
        VBox legend = new VBox(5);
        legend.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 10;");
        Label legTitle = new Label("Detail par type:");
        legTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        legend.getChildren().add(legTitle);

        String[] cols = {"#5BA3F5", "#28a745", "#ffc107", "#dc3545", "#17a2b8", "#6f42c1"};
        int ci = 0;
        for (Map.Entry<ContractType, Integer> e : data.entrySet()) {
            if (e.getValue() > 0) {
                double pct = (e.getValue() * 100.0) / total;
                HBox row = new HBox(8);
                row.setAlignment(Pos.CENTER_LEFT);

                Region dot = new Region();
                dot.setMinSize(12, 12); dot.setMaxSize(12, 12);
                dot.setStyle("-fx-background-color: " + cols[ci % cols.length] + "; -fx-background-radius: 6;");

                Label item = new Label(String.format("%s: %d offres (%.1f%%)",
                        fmtContract(e.getKey()), e.getValue(), pct));
                item.setStyle("-fx-font-size: 11px; -fx-text-fill: #495057;");

                row.getChildren().addAll(dot, item);
                legend.getChildren().add(row);
                ci++;
            }
        }
        container.getChildren().add(legend);
        return container;
    }

    private VBox createMonthlyOffersChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("Evolution des Offres (12 mois)");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<String, Integer> data = (isAdmin || recruiterId == null)
                ? analyticsService.getOffersByMonth()
                : analyticsService.getOffersByMonth(recruiterId);

        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        int max   = data.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        double avg = data.isEmpty() ? 0 : (double) total / data.size();

        HBox stats = new HBox(15);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setStyle("-fx-padding: 5 0;");

        Label tLbl = new Label("Total: " + total);
        tLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #5BA3F5; -fx-font-weight: 600;");
        Label aLbl = new Label("Moyenne: " + String.format("%.1f", avg) + "/mois");
        aLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745; -fx-font-weight: 600;");
        Label mLbl = new Label("Max: " + max);
        mLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffc107; -fx-font-weight: 600;");
        stats.getChildren().addAll(tLbl, aLbl, mLbl);
        container.getChildren().add(stats);

        if (total == 0) {
            container.getChildren().add(emptyLabel("Aucune donnee sur cette periode"));
            return container;
        }

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis   = new NumberAxis();
        yAxis.setLabel("Nombre d'offres");

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Map.Entry<String, Integer> e : data.entrySet())
            series.getData().add(new XYChart.Data<>(fmtMonth(e.getKey()), e.getValue()));

        chart.getData().add(series);
        chart.setPrefHeight(250);

        // Animate: fade in the entire chart
        chart.setOpacity(0);
        chart.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) {
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(300),
                        new KeyValue(chart.opacityProperty(), 1.0))
                );
                tl.play();
            }
        });

        container.getChildren().add(chart);

        FlowPane valuesPane = new FlowPane(8, 5);
        valuesPane.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 8;");
        for (Map.Entry<String, Integer> e : data.entrySet()) {
            if (e.getValue() > 0) {
                Label l = new Label(fmtMonth(e.getKey()) + ": " + e.getValue());
                l.setStyle("-fx-font-size: 10px; -fx-background-color: white; -fx-padding: 3 6; " +
                        "-fx-background-radius: 4; -fx-text-fill: #495057;");
                valuesPane.getChildren().add(l);
            }
        }
        if (!valuesPane.getChildren().isEmpty()) container.getChildren().add(valuesPane);
        return container;
    }

    private VBox createTopLocationsChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("Top 5 des Localisations");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<String, Integer> data = analyticsService.getTopLocations(5);
        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        Label explain = new Label("Passez la souris sur une ligne pour plus de details");
        explain.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        container.getChildren().add(explain);

        if (data.isEmpty() || total == 0) {
            container.getChildren().add(emptyLabel("Aucune donnee de localisation disponible"));
            return container;
        }

        VBox listBox = new VBox(8);
        listBox.setStyle("-fx-padding: 10 0;");

        int rank = 1;
        int maxVal = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int rowIndex = 0;

        for (Map.Entry<String, Integer> e : data.entrySet()) {
            final int count = e.getValue();
            final double totalPct = (count * 100.0) / total;
            final int r = rank;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 10, 6, 10));
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");

            String rankColor = rank == 1 ? "#ffc107" : rank == 2 ? "#6c757d" : rank == 3 ? "#cd7f32" : "#495057";
            Label rankLbl = new Label("#" + rank);
            rankLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + rankColor + "; -fx-min-width: 25;");

            String loc = e.getKey().length() > 20 ? e.getKey().substring(0, 17) + "..." : e.getKey();
            Label nameLbl = new Label(loc);
            nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-min-width: 120;");

            double pct = (count * 100.0) / maxVal;
            StackPane bar = progressBarDelayed(pct, "#5BA3F5", rowIndex * 80);
            bar.setPrefHeight(18);
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label valLbl = new Label(count + " (" + String.format("%.1f", totalPct) + "%)");
            valLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: #5BA3F5; -fx-min-width: 70;");

            row.getChildren().addAll(rankLbl, nameLbl, bar, valLbl);

            // Tooltip
            Tooltip tip = new Tooltip(
                "Rang #" + r + ": " + e.getKey() + "\n" +
                count + " offres publiees\n" +
                String.format("%.1f", totalPct) + "% du total top 5");
            tip.setStyle("-fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-padding: 8 12;");
            tip.setShowDelay(Duration.millis(100));
            Tooltip.install(row, tip);

            // Hover highlight
            row.setOnMouseEntered(ev -> row.setStyle(
                "-fx-background-color: #EBF4FF; -fx-background-radius: 8; -fx-cursor: hand;"));
            row.setOnMouseExited(ev -> row.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;"));

            listBox.getChildren().add(row);
            rank++;
            rowIndex++;
        }

        container.getChildren().add(listBox);
        Label totalLbl = new Label("Total top 5: " + total + " offres");
        totalLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745; -fx-font-weight: 600;");
        container.getChildren().add(totalLbl);
        return container;
    }

    private VBox createTopSkillsChart() throws SQLException {
        VBox container = new VBox(12);

        Label title = new Label("Top 10 Competences Demandees");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");
        container.getChildren().add(title);

        Map<String, Integer> data = analyticsService.getTopSkills(10);
        int total = data.values().stream().mapToInt(Integer::intValue).sum();

        Label explain = new Label("Cliquez sur une competence pour voir le detail");
        explain.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        container.getChildren().add(explain);

        if (data.isEmpty() || total == 0) {
            container.getChildren().add(emptyLabel("Aucune competence enregistree"));
            return container;
        }

        VBox listBox = new VBox(6);
        listBox.setStyle("-fx-padding: 10 0;");

        String[] cols = {"#5BA3F5","#28a745","#ffc107","#17a2b8","#6f42c1",
                         "#dc3545","#fd7e14","#20c997","#e83e8c","#6c757d"};
        int rank = 1;
        int maxVal = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);

        for (Map.Entry<String, Integer> e : data.entrySet()) {
            final String skillName = e.getKey();
            final int count = e.getValue();
            final double totalPct = (count * 100.0) / total;
            final String col = cols[(rank - 1) % cols.length];
            final int r = rank;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(5, 10, 5, 10));
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");

            // Staggered fade-in for each row
            row.setOpacity(0);
            Timeline rowFade = new Timeline(
                new KeyFrame(Duration.millis(rank * 60)),
                new KeyFrame(Duration.millis(rank * 60 + 300),
                    new KeyValue(row.opacityProperty(), 1))
            );
            rowFade.play();

            String medal = r == 1 ? "1." : r == 2 ? "2." : r == 3 ? "3." : r + ".";
            Label rankLbl = new Label(medal);
            rankLbl.setStyle("-fx-font-size: " + (r <= 3 ? "14px" : "11px") + "; -fx-min-width: 28; " +
                    "-fx-font-weight: " + (r <= 3 ? "700" : "400") + "; -fx-text-fill: " + col + ";");

            Label nameLbl = new Label(skillName);
            nameLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-min-width: 110; " +
                    "-fx-font-weight: " + (r <= 3 ? "600" : "400") + ";");

            double pct = (count * 100.0) / maxVal;
            StackPane bar = progressBarDelayed(pct, col, rank * 60);
            bar.setPrefHeight(16);
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label valLbl = new Label(count + " (" + String.format("%.1f", totalPct) + "%)");
            valLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + col + "; -fx-font-weight: 600;");

            row.getChildren().addAll(rankLbl, nameLbl, bar, valLbl);

            // Tooltip
            Tooltip tip = new Tooltip(
                "Competence: " + skillName + "\n" +
                "Rang: #" + r + "\n" +
                count + " offres la demandent\n" +
                String.format("%.1f", totalPct) + "% du total des demandes");
            tip.setStyle("-fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-padding: 8 12;");
            tip.setShowDelay(Duration.millis(100));
            Tooltip.install(row, tip);

            // Hover
            row.setOnMouseEntered(ev -> row.setStyle(
                "-fx-background-color: " + col + "18; -fx-background-radius: 8; -fx-cursor: hand;"));
            row.setOnMouseExited(ev -> row.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;"));

            // Click: bounce + popup
            row.setOnMouseClicked(ev -> {
                ScaleTransition bounce = new ScaleTransition(Duration.millis(120), row);
                bounce.setToX(1.03); bounce.setToY(1.03);
                bounce.setAutoReverse(true); bounce.setCycleCount(2);
                bounce.play();
                showSkillDetail(skillName, count, totalPct, r, col);
            });

            listBox.getChildren().add(row);
            rank++;
        }

        container.getChildren().add(listBox);
        Label totalLbl = new Label("Total des demandes: " + total);
        totalLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #28a745; -fx-font-weight: 600;");
        container.getChildren().add(totalLbl);
        return container;
    }

    private void showSkillDetail(String skill, int count, double pct, int rank, String color) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("Competence: " + skill);

        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: white; -fx-padding: 28; -fx-background-radius: 16;");
        root.setPrefWidth(300);

        Label rankBadge = new Label("#" + rank + " des competences");
        rankBadge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 4 14; -fx-background-radius: 20; -fx-font-size: 11px; -fx-font-weight: 700;");

        Label nameLbl = new Label(skill);
        nameLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        VBox statsBox = new VBox(8);
        statsBox.setStyle("-fx-background-color: " + color + "15; -fx-background-radius: 10; -fx-padding: 14;");
        statsBox.setAlignment(Pos.CENTER_LEFT);

        Label countLbl = new Label(count + " offres la demandent");
        countLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: " + color + ";");

        Label pctLbl = new Label(String.format("%.1f", pct) + "% du total des demandes");
        pctLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");

        String insight = rank == 1 ? "Competence la plus demandee sur la plateforme!" :
                         rank <= 3 ? "Dans le top 3 des competences les plus recherchees." :
                                     "Competence populaire chez les recruteurs.";
        Label insightLbl = new Label(insight);
        insightLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        insightLbl.setWrapText(true);

        statsBox.getChildren().addAll(countLbl, pctLbl, insightLbl);

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Fermer");
        closeBtn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-padding: 8 24; -fx-background-radius: 20; -fx-font-weight: 600; -fx-cursor: hand;");
        closeBtn.setOnAction(ev -> popup.close());

        root.getChildren().addAll(rankBadge, nameLbl, statsBox, closeBtn);

        root.setOpacity(0);
        Timeline tl = new Timeline(new KeyFrame(Duration.millis(200),
                new KeyValue(root.opacityProperty(), 1)));

        popup.setScene(new javafx.scene.Scene(root));
        popup.show();
        tl.play();
    }

    private VBox createWeekActivityChart() throws SQLException {
        VBox container = new VBox(12);

        Map<String, Integer> data = analyticsService.getOffersThisWeek();
        int total = data.values().stream().mapToInt(Integer::intValue).sum();
        int max   = data.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        String bestDay = data.entrySet().stream()
                .filter(e -> e.getValue() == max)
                .map(Map.Entry::getKey)
                .findFirst().orElse("N/A");

        Label explain = new Label("Nombre d'offres creees chaque jour de la semaine");
        explain.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        container.getChildren().add(explain);

        // Summary boxes
        HBox summaryRow = new HBox(20);
        summaryRow.setAlignment(Pos.CENTER_LEFT);
        summaryRow.setStyle("-fx-padding: 10 0;");

        summaryRow.getChildren().addAll(
            summaryBox(String.valueOf(total), "offres cette semaine", "#e3f2fd", "#1976d2"),
            summaryBox(String.format("%.1f", total / 7.0), "moyenne/jour", "#e8f5e9", "#388e3c"),
            summaryBox(bestDay, "jour le plus actif (" + max + ")", "#fff3e0", "#f57c00")
        );
        container.getChildren().add(summaryRow);

        // Day bars
        VBox daysBox = new VBox(6);
        daysBox.setStyle("-fx-padding: 10 0;");

        String[] fullNames  = {"Lundi","Mardi","Mercredi","Jeudi","Vendredi","Samedi","Dimanche"};
        String[] shortNames = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        int maxForBar = max > 0 ? max : 1;

        for (int i = 0; i < shortNames.length; i++) {
            int value = data.getOrDefault(shortNames[i], 0);
            boolean isBest = value == max && max > 0;
            String barCol = isBest ? "#28a745" : "#5BA3F5";
            final String dayFull = fullNames[i];
            final int dayVal = value;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 10, 4, 10));
            row.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;");

            Label dayLbl = new Label(fullNames[i]);
            dayLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (isBest ? "#28a745" : "#495057") +
                    "; -fx-min-width: 75; -fx-font-weight: " + (isBest ? "700" : "400") + ";");

            StackPane bar = progressBarDelayed((value * 100.0) / maxForBar, barCol, i * 70);
            bar.setPrefHeight(20);
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label valLbl = new Label(value + " offre" + (value != 1 ? "s" : ""));
            valLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + barCol + "; -fx-min-width: 65;");

            row.getChildren().addAll(dayLbl, bar, valLbl);
            if (isBest) {
                Label trophy = new Label("★");
                trophy.setStyle("-fx-font-size: 14px; -fx-text-fill: #ffc107;");
                row.getChildren().add(trophy);
            }

            // Tooltip
            double dayPct = max > 0 ? (value * 100.0) / total : 0;
            Tooltip tip = new Tooltip(
                dayFull + "\n" +
                dayVal + " offre" + (dayVal != 1 ? "s" : "") + " creee" + (dayVal != 1 ? "s" : "") + "\n" +
                (total > 0 ? String.format("%.1f", dayPct) + "% de l'activite de la semaine" : "Aucune activite") +
                (isBest ? "\nJour le plus actif de la semaine" : ""));
            tip.setStyle("-fx-font-size: 12px; -fx-background-color: #2c3e50; -fx-text-fill: white; " +
                    "-fx-background-radius: 8; -fx-padding: 8 12;");
            tip.setShowDelay(Duration.millis(100));
            Tooltip.install(row, tip);

            row.setOnMouseEntered(ev -> row.setStyle(
                "-fx-background-color: " + barCol + "18; -fx-background-radius: 8; -fx-cursor: hand;"));
            row.setOnMouseExited(ev -> row.setStyle(
                "-fx-background-color: transparent; -fx-background-radius: 8; -fx-cursor: hand;"));

            daysBox.getChildren().add(row);
        }
        container.getChildren().add(daysBox);
        return container;
    }

    // =========================================================================
    // Shared helpers
    // =========================================================================

    private StackPane progressBar(double pct, String color) {
        StackPane bar = new StackPane();
        bar.setPrefHeight(18);
        bar.setPrefWidth(160);

        Region bg = new Region();
        bg.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 4;");
        bg.prefWidthProperty().bind(bar.widthProperty());

        Region fill = new Region();
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        // Start at 0, animate to target
        fill.setPrefWidth(0);
        fill.setMaxWidth(Region.USE_PREF_SIZE);

        bar.getChildren().addAll(bg, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        // Animate fill width after bar is laid out
        bar.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.ZERO,
                        new KeyValue(fill.prefWidthProperty(), 0)),
                    new KeyFrame(Duration.millis(700),
                        new KeyValue(fill.prefWidthProperty(),
                            bar.getPrefWidth() * Math.min(pct, 100) / 100.0))
                );
                // Re-bind after animation using layout width
                tl.setOnFinished(e ->
                    fill.prefWidthProperty().bind(
                        bar.widthProperty().multiply(Math.min(pct, 100) / 100.0)));
                tl.play();
            }
        });

        return bar;
    }

    /** Animated progress bar — immediate (for items loaded after scene is ready) */
    private StackPane progressBarDelayed(double pct, String color, int delayMs) {
        StackPane bar = new StackPane();
        bar.setPrefHeight(18);
        bar.setPrefWidth(160);

        Region bg = new Region();
        bg.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 4;");
        bg.prefWidthProperty().bind(bar.widthProperty());

        Region fill = new Region();
        fill.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        fill.setPrefWidth(0);
        fill.setMaxWidth(Region.USE_PREF_SIZE);

        bar.getChildren().addAll(bg, fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(delayMs),
                new KeyValue(fill.prefWidthProperty(), 0)),
            new KeyFrame(Duration.millis(delayMs + 600),
                new KeyValue(fill.prefWidthProperty(),
                    bar.getPrefWidth() * Math.min(pct, 100) / 100.0))
        );
        tl.setOnFinished(e ->
            fill.prefWidthProperty().bind(
                bar.widthProperty().multiply(Math.min(pct, 100) / 100.0)));
        tl.play();

        return bar;
    }

    private VBox summaryBox(String value, String label, String bgColor, String textColor) {
        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-background-color: " + bgColor + "; -fx-padding: 10; -fx-background-radius: 8;");
        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: " + textColor + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + textColor + ";");
        box.getChildren().addAll(valLbl, lbl);
        return box;
    }

    private Label emptyLabel(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
        return l;
    }

    private String fmtContract(ContractType t) {
        return switch (t) {
            case CDI        -> "CDI";
            case CDD        -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE  -> "Freelance";
            case PART_TIME  -> "Temps Partiel";
            case FULL_TIME  -> "Temps Plein";
        };
    }

    private String fmtMonth(String yearMonth) {
        String[] parts = yearMonth.split("-");
        if (parts.length != 2) return yearMonth;
        String[] mois = {"Jan","Fev","Mar","Avr","Mai","Jun","Jul","Aou","Sep","Oct","Nov","Dec"};
        int m = Integer.parseInt(parts[1]);
        return mois[m - 1] + " " + parts[0].substring(2);
    }

    private String pct(int part, int total) {
        if (total == 0) return "0";
        return String.format("%.0f", (part * 100.0) / total);
    }

    public void refresh() {
        buildDashboard();
    }
}









