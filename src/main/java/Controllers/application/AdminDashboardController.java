package Controllers.application;

import Services.application.ApplicationStatisticsService;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.Map;

public class AdminDashboardController {

    @FXML private HBox globalStatsContainer;
    @FXML private VBox offerStatsContainer;
    @FXML private Button btnRefresh;

    @FXML
    public void initialize() {
        if (btnRefresh != null) {
            btnRefresh.setOnAction(e -> {
                ScaleTransition spin = new ScaleTransition(Duration.millis(200), btnRefresh);
                spin.setFromX(0.9); spin.setFromY(0.9);
                spin.setToX(1.0);   spin.setToY(1.0);
                spin.play();
                loadStatistics();
            });
        }
        loadStatistics();
    }

    private void loadStatistics() {
        if (globalStatsContainer != null) globalStatsContainer.getChildren().clear();
        if (offerStatsContainer  != null) offerStatsContainer.getChildren().clear();

        ApplicationStatisticsService.OfferStatistics global =
                ApplicationStatisticsService.getGlobalStatistics();
        if (global != null) displayGlobalStatistics(global);

        Map<Long, ApplicationStatisticsService.OfferStatistics> offerStats =
                ApplicationStatisticsService.getAllOfferStatistics();

        if (offerStats == null || offerStats.isEmpty()) {
            Label noData = new Label("Aucune offre avec des candidatures trouvee");
            noData.setStyle("-fx-text-fill: #8FA3B8; -fx-font-size: 14px; -fx-padding: 30;");
            if (offerStatsContainer != null) offerStatsContainer.getChildren().add(noData);
        } else {
            int[] idx = {0};
            offerStats.forEach((id, stats) -> {
                VBox card = createOfferStatCard(stats);
                // staggered fade in
                card.setOpacity(0); card.setTranslateY(12);
                int delay = idx[0]++ * 60;
                Timeline tl = new Timeline(
                    new KeyFrame(Duration.millis(delay)),
                    new KeyFrame(Duration.millis(delay + 300),
                        new KeyValue(card.opacityProperty(), 1.0),
                        new KeyValue(card.translateYProperty(), 0))
                );
                tl.play();
                offerStatsContainer.getChildren().add(card);
            });
        }
    }

    // ── Global KPI row + pie chart side by side ───────────────────────────────

    private void displayGlobalStatistics(ApplicationStatisticsService.OfferStatistics stats) {
        if (globalStatsContainer == null) return;

        // Left: KPI cards in a wrapping flow
        FlowPane kpiPane = new FlowPane(12, 12);
        kpiPane.setPrefWrapLength(580);
        kpiPane.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(kpiPane, Priority.ALWAYS);

        int[] targets = {
            stats.totalApplications(), stats.submitted(), stats.shortlisted(),
            stats.rejected(), stats.interview(), stats.hired()
        };
        String[] labels = {"Total", "Soumises", "Preselectionnees", "Rejetees", "Entretiens", "Embauches"};
        String[] icons  = {"📋", "📝", "⭐", "✕", "🎤", "✓"};
        String[] colors = {"#5BA3F5", "#6C757D", "#F39C12", "#E74C3C", "#9B59B6", "#2ECC71"};

        for (int i = 0; i < labels.length; i++) {
            VBox card = createAnimatedKpiCard(labels[i], targets[i], colors[i], icons[i], i * 55);
            kpiPane.getChildren().add(card);
        }

        // Acceptance rate card — full width
        double pct = stats.getAcceptancePercentage();
        VBox rateCard = createRateCard(pct);

        // Right: Pie chart
        VBox pieBox = buildGlobalPieChart(stats);
        pieBox.setPrefWidth(260);
        pieBox.setMinWidth(220);

        globalStatsContainer.setSpacing(20);
        globalStatsContainer.setAlignment(Pos.TOP_LEFT);

        VBox leftCol = new VBox(12, kpiPane, rateCard);
        HBox.setHgrow(leftCol, Priority.ALWAYS);

        globalStatsContainer.getChildren().addAll(leftCol, pieBox);
    }

    private VBox createAnimatedKpiCard(String title, int target, String color, String icon, int delayMs) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(140);
        card.setMinWidth(120);
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-padding: 18 14;" +
            "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.10),12,0,0,3);" +
            "-fx-border-color: #F0F4FA; -fx-border-width:1; -fx-border-radius:14;" +
            "-fx-cursor: hand;");

        // Colored accent top bar
        Rectangle accent = new Rectangle(140, 4);
        accent.setFill(Color.web(color));
        accent.setArcWidth(8); accent.setArcHeight(8);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 22px;");

        Label valueLbl = new Label("0");
        valueLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 30px; -fx-text-fill: " + color + ";");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8FA3B8; -fx-font-weight: 600;");
        titleLbl.setWrapText(true);
        titleLbl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(accent, iconLbl, valueLbl, titleLbl);

        // Tooltip
        Tooltip tip = new Tooltip(title + ": " + target);
        tip.setStyle("-fx-font-size:12px; -fx-background-color:#1E2D3D; -fx-text-fill:white;" +
                "-fx-background-radius:8; -fx-padding:7 12;");
        tip.setShowDelay(Duration.millis(150));
        Tooltip.install(card, tip);

        // Hover scale
        String base = card.getStyle();
        card.setOnMouseEntered(e -> {
            card.setStyle(base +
                "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.22),18,0,0,5);" +
                "-fx-border-color: " + color + "; -fx-border-width:2;");
            ScaleTransition st = new ScaleTransition(Duration.millis(130), card);
            st.setToX(1.05); st.setToY(1.05); st.play();
        });
        card.setOnMouseExited(e -> {
            card.setStyle(base);
            ScaleTransition st = new ScaleTransition(Duration.millis(130), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        // Animate counter after delay
        Timeline tl = new Timeline();
        int steps = Math.min(target, 30);
        if (steps == 0) { valueLbl.setText("0"); }
        else {
            for (int s = 0; s <= steps; s++) {
                final int displayed = (int) Math.round(target * ((double) s / steps));
                final double ms = delayMs + 500.0 * s / steps;
                tl.getKeyFrames().add(new KeyFrame(Duration.millis(ms),
                    ev -> valueLbl.setText(String.valueOf(displayed))));
            }
        }
        tl.play();

        return card;
    }

    private VBox createRateCard(double pct) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 16 18;" +
                "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.10),12,0,0,3);" +
                "-fx-border-color: #F0F4FA; -fx-border-width:1; -fx-border-radius:14;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label("Taux d'acceptation global");
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label pctLbl = new Label(pct + "%");
        pctLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2ECC71;");
        header.getChildren().addAll(lbl, sp, pctLbl);

        // Animated progress bar
        StackPane barBg = new StackPane();
        barBg.setPrefHeight(10);
        barBg.setStyle("-fx-background-color:#E4EBF5; -fx-background-radius:5;");

        Region fill = new Region();
        fill.setStyle("-fx-background-color: linear-gradient(to right,#2ECC71,#27AE60);" +
                "-fx-background-radius: 5;");
        fill.setPrefHeight(10);
        fill.setPrefWidth(0);
        fill.setMaxWidth(Region.USE_PREF_SIZE);
        barBg.getChildren().add(fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        // Animate bar after scene attach
        barBg.sceneProperty().addListener((obs, o, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    Timeline tl = new Timeline(
                        new KeyFrame(Duration.millis(200),
                            new KeyValue(fill.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(900),
                            new KeyValue(fill.prefWidthProperty(),
                                barBg.getWidth() * Math.min(pct, 100) / 100.0))
                    );
                    tl.setOnFinished(e ->
                        fill.prefWidthProperty().bind(
                            barBg.widthProperty().multiply(Math.min(pct, 100) / 100.0)));
                    tl.play();
                });
            }
        });

        card.getChildren().addAll(header, barBg);
        return card;
    }

    private VBox buildGlobalPieChart(ApplicationStatisticsService.OfferStatistics stats) {
        VBox box = new VBox(10);
        box.setAlignment(Pos.TOP_CENTER);
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 16;" +
                "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.10),12,0,0,3);" +
                "-fx-border-color: #F0F4FA; -fx-border-width:1; -fx-border-radius:14;");

        Label title = new Label("Repartition des statuts");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        PieChart pie = new PieChart();
        pie.setLegendVisible(false);
        pie.setLabelsVisible(false);
        pie.setStartAngle(90);
        pie.setPrefSize(200, 200);
        pie.setMinSize(160, 160);

        int[][] data = {
            {stats.submitted(),   0xFF6C757D},
            {stats.shortlisted(), 0xFFF39C12},
            {stats.rejected(),    0xFFE74C3C},
            {stats.interview(),   0xFF9B59B6},
            {stats.hired(),       0xFF2ECC71}
        };
        String[] pieLabels = {"Soumises","Preselectionnees","Rejetees","Entretiens","Embauches"};
        String[] pieColors = {"#6C757D","#F39C12","#E74C3C","#9B59B6","#2ECC71"};

        int total = stats.totalApplications();
        for (int i = 0; i < data.length; i++) {
            if (data[i][0] > 0) {
                pie.getData().add(new PieChart.Data(pieLabels[i], data[i][0]));
            }
        }

        // Animate slices on scene attach
        pie.sceneProperty().addListener((obs, o, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    int ci = 0;
                    for (PieChart.Data d : pie.getData()) {
                        javafx.scene.Node node = d.getNode();
                        if (node == null) continue;
                        node.setScaleX(0); node.setScaleY(0); node.setOpacity(0);
                        String col = pieColors[ci % pieColors.length];
                        node.setStyle("-fx-pie-color: " + col + ";");
                        Timeline tl = new Timeline(
                            new KeyFrame(Duration.millis(ci * 80),
                                new KeyValue(node.scaleXProperty(), 0),
                                new KeyValue(node.scaleYProperty(), 0),
                                new KeyValue(node.opacityProperty(), 0)),
                            new KeyFrame(Duration.millis(ci * 80 + 450),
                                new KeyValue(node.scaleXProperty(), 1),
                                new KeyValue(node.scaleYProperty(), 1),
                                new KeyValue(node.opacityProperty(), 1))
                        );
                        tl.play();
                        double pct = total > 0 ? d.getPieValue() * 100.0 / total : 0;
                        Tooltip tip = new Tooltip(d.getName() + "\n" + (int)d.getPieValue()
                                + " (" + String.format("%.1f", pct) + "%)");
                        tip.setStyle("-fx-font-size:12px; -fx-background-color:#1E2D3D;" +
                                "-fx-text-fill:white; -fx-background-radius:8; -fx-padding:7 12;");
                        tip.setShowDelay(Duration.millis(80));
                        Tooltip.install(node, tip);
                        node.setOnMouseEntered(e -> {
                            ScaleTransition st = new ScaleTransition(Duration.millis(130), node);
                            st.setToX(1.10); st.setToY(1.10); st.play();
                        });
                        node.setOnMouseExited(e -> {
                            ScaleTransition st = new ScaleTransition(Duration.millis(130), node);
                            st.setToX(1.0); st.setToY(1.0); st.play();
                        });
                        ci++;
                    }
                });
            }
        });

        // Mini legend
        VBox legend = new VBox(5);
        legend.setPadding(new Insets(4, 0, 0, 0));
        for (int i = 0; i < pieLabels.length; i++) {
            int val = data[i][0];
            if (val == 0) continue;
            HBox row = new HBox(7);
            row.setAlignment(Pos.CENTER_LEFT);
            Region dot = new Region();
            dot.setMinSize(10, 10); dot.setMaxSize(10, 10);
            dot.setStyle("-fx-background-color:" + pieColors[i] + "; -fx-background-radius:5;");
            Label lbl = new Label(pieLabels[i] + ": " + val);
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #5A7080;");
            row.getChildren().addAll(dot, lbl);
            legend.getChildren().add(row);
        }

        box.getChildren().addAll(title, pie, legend);
        return box;
    }

    // ── Per-offer stat card ────────────────────────────────────────────────────

    private VBox createOfferStatCard(ApplicationStatisticsService.OfferStatistics stats) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                "-fx-border-color: #E8EEF8; -fx-border-width: 1; -fx-border-radius: 14;" +
                "-fx-padding: 18;" +
                "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.09),10,0,0,2);");

        // Hover
        String base = card.getStyle();
        card.setOnMouseEntered(e -> card.setStyle(base +
                "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.22),18,0,0,5);" +
                "-fx-border-color: #AACEF5;"));
        card.setOnMouseExited(e -> card.setStyle(base));

        // Title row
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label offerTitle = new Label(stats.offerTitle());
        offerTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(offerTitle, Priority.ALWAYS);

        Label totalBadge = new Label(stats.totalApplications() + " candidature(s)");
        totalBadge.setStyle("-fx-background-color: #DCEEFB; -fx-text-fill: #1565C0;" +
                "-fx-font-weight: 700; -fx-font-size: 11px;" +
                "-fx-padding: 4 13; -fx-background-radius: 20;");
        titleRow.getChildren().addAll(offerTitle, totalBadge);

        // Status pills
        HBox pillRow = new HBox(8);
        pillRow.setAlignment(Pos.CENTER_LEFT);
        pillRow.setStyle("-fx-padding: 2 0;");
        pillRow.getChildren().addAll(
            makePill("Soumises",         stats.submitted(),   "#6C757D"),
            makePill("Preselectionnees", stats.shortlisted(), "#F39C12"),
            makePill("Rejetees",         stats.rejected(),    "#E74C3C"),
            makePill("Entretiens",       stats.interview(),   "#9B59B6"),
            makePill("Embauches",        stats.hired(),       "#2ECC71")
        );

        // Progress bar
        double pct = stats.getAcceptancePercentage();
        VBox rateBox = new VBox(6);

        HBox rateHeader = new HBox(8);
        rateHeader.setAlignment(Pos.CENTER_LEFT);
        Label rateLabel = new Label("Taux d'acceptation");
        rateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #8FA3B8; -fx-font-weight: 600;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label ratePct = new Label(pct + "%");
        ratePct.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2ECC71;");
        rateHeader.getChildren().addAll(rateLabel, spacer, ratePct);

        StackPane barBg = new StackPane();
        barBg.setPrefHeight(8);
        barBg.setStyle("-fx-background-color:#E4EBF5; -fx-background-radius:4;");

        Region fill = new Region();
        fill.setStyle("-fx-background-color: linear-gradient(to right,#2ECC71,#27AE60); -fx-background-radius:4;");
        fill.setPrefHeight(8); fill.setPrefWidth(0);
        fill.setMaxWidth(Region.USE_PREF_SIZE);
        barBg.getChildren().add(fill);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        barBg.sceneProperty().addListener((obs, o, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> {
                    Timeline tl = new Timeline(
                        new KeyFrame(Duration.millis(150), new KeyValue(fill.prefWidthProperty(), 0)),
                        new KeyFrame(Duration.millis(750),
                            new KeyValue(fill.prefWidthProperty(),
                                barBg.getWidth() * Math.min(pct, 100) / 100.0))
                    );
                    tl.setOnFinished(e ->
                        fill.prefWidthProperty().bind(
                            barBg.widthProperty().multiply(Math.min(pct, 100) / 100.0)));
                    tl.play();
                });
            }
        });

        rateBox.getChildren().addAll(rateHeader, barBg);
        card.getChildren().addAll(titleRow, pillRow, rateBox);
        return card;
    }

    private HBox makePill(String label, int value, String color) {
        HBox pill = new HBox(5);
        pill.setAlignment(Pos.CENTER);
        pill.setStyle("-fx-background-color: " + color + "1A; -fx-background-radius: 10;" +
                "-fx-padding: 6 12; -fx-cursor: hand;" +
                "-fx-border-color: " + color + "40; -fx-border-width:1; -fx-border-radius:10;");

        Label val = new Label(String.valueOf(value));
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: " + color + "; -fx-font-weight: 600;");

        pill.getChildren().addAll(val, lbl);

        // Tooltip
        Tooltip tip = new Tooltip(label + ": " + value);
        tip.setStyle("-fx-font-size:12px; -fx-background-color:#1E2D3D; -fx-text-fill:white;" +
                "-fx-background-radius:8; -fx-padding:6 10;");
        Tooltip.install(pill, tip);

        // Hover
        String base = pill.getStyle();
        pill.setOnMouseEntered(e -> {
            pill.setStyle("-fx-background-color: " + color + "33; -fx-background-radius: 10;" +
                    "-fx-padding: 6 12; -fx-cursor: hand;" +
                    "-fx-border-color: " + color + "; -fx-border-width:1; -fx-border-radius:10;");
            ScaleTransition st = new ScaleTransition(Duration.millis(120), pill);
            st.setToX(1.06); st.setToY(1.06); st.play();
        });
        pill.setOnMouseExited(e -> {
            pill.setStyle(base);
            ScaleTransition st = new ScaleTransition(Duration.millis(120), pill);
            st.setToX(1.0); st.setToY(1.0); st.play();
        });

        return pill;
    }
}
