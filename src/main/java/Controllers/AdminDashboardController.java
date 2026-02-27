package Controllers;

import Services.ApplicationStatisticsService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

import java.util.Map;

public class AdminDashboardController {

    @FXML private HBox globalStatsContainer;
    @FXML private VBox offerStatsContainer;
    @FXML private Button btnRefresh;

    @FXML
    public void initialize() {
        if (btnRefresh != null) btnRefresh.setOnAction(e -> loadStatistics());
        loadStatistics();
    }

    private void loadStatistics() {
        if (globalStatsContainer != null) globalStatsContainer.getChildren().clear();
        if (offerStatsContainer  != null) offerStatsContainer.getChildren().clear();

        // Global stats
        ApplicationStatisticsService.OfferStatistics global =
                ApplicationStatisticsService.getGlobalStatistics();
        if (global != null) displayGlobalStatistics(global);

        // Per-offer stats
        Map<Long, ApplicationStatisticsService.OfferStatistics> offerStats =
                ApplicationStatisticsService.getAllOfferStatistics();

        if (offerStats == null || offerStats.isEmpty()) {
            Label noData = new Label("Aucune offre avec des candidatures trouvée");
            noData.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 14px; -fx-padding: 20;");
            if (offerStatsContainer != null) offerStatsContainer.getChildren().add(noData);
        } else {
            offerStats.forEach((id, stats) ->
                    offerStatsContainer.getChildren().add(createOfferStatCard(stats)));
        }
    }

    private void displayGlobalStatistics(ApplicationStatisticsService.OfferStatistics stats) {
        if (globalStatsContainer == null) return;
        globalStatsContainer.getChildren().addAll(
            createStatCard("Total",           String.valueOf(stats.totalApplications()), "#5BA3F5", "📋"),
            createStatCard("Soumises",        String.valueOf(stats.submitted()),          "#6c757d", "📝"),
            createStatCard("Présélectionnés", String.valueOf(stats.shortlisted()),        "#f0ad4e", "⭐"),
            createStatCard("Rejetées",        String.valueOf(stats.rejected()),           "#dc3545", "✕"),
            createStatCard("Entretiens",      String.valueOf(stats.interview()),          "#9b59b6", "🎤"),
            createStatCard("Embauchés",       String.valueOf(stats.hired()),              "#28a745", "✓"),
            createStatCard("Taux d'acceptation", stats.getAcceptancePercentage() + "%",  "#17a2b8", "📈")
        );
    }

    private VBox createStatCard(String title, String value, String color, String icon) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: " + color + "; -fx-border-width: 0 0 3 0; "
                + "-fx-padding: 18 20; "
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),10,0,0,2);");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(145);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 20px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 28px; -fx-text-fill: " + color + ";");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-weight: 600;");
        titleLabel.setWrapText(true);
        titleLabel.setAlignment(Pos.CENTER);

        card.getChildren().addAll(iconLabel, valueLabel, titleLabel);
        return card;
    }

    private VBox createOfferStatCard(ApplicationStatisticsService.OfferStatistics stats) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                + "-fx-padding: 18; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");

        // Offer title + total badge
        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label offerTitle = new Label(stats.offerTitle());
        offerTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 15px; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(offerTitle, Priority.ALWAYS);

        Label totalBadge = new Label(stats.totalApplications() + " candidature(s)");
        totalBadge.setStyle("-fx-background-color: #EBF3FE; -fx-text-fill: #5BA3F5; "
                + "-fx-font-weight: 600; -fx-font-size: 12px; "
                + "-fx-padding: 4 12; -fx-background-radius: 20;");

        titleRow.getChildren().addAll(offerTitle, totalBadge);

        // Status pill row
        HBox pillRow = new HBox(10);
        pillRow.setAlignment(Pos.CENTER_LEFT);
        pillRow.getChildren().addAll(
            makePill("Soumises",        stats.submitted(),  "#6c757d"),
            makePill("Présélectionnés", stats.shortlisted(), "#f0ad4e"),
            makePill("Rejetées",        stats.rejected(),   "#dc3545"),
            makePill("Entretiens",      stats.interview(),  "#9b59b6"),
            makePill("Embauchés",       stats.hired(),      "#28a745")
        );

        // Acceptance rate bar
        double pct = stats.getAcceptancePercentage();
        VBox rateBox = new VBox(4);
        HBox rateHeader = new HBox(8);
        Label rateLabel = new Label("Taux d'acceptation");
        rateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d; -fx-font-weight: 600;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label ratePct = new Label(pct + "%");
        ratePct.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #17a2b8;");
        rateHeader.getChildren().addAll(rateLabel, spacer, ratePct);

        // Progress bar background
        VBox barBg = new VBox();
        barBg.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 4; -fx-pref-height: 7;");
        barBg.setMaxWidth(Double.MAX_VALUE);
        VBox barFill = new VBox();
        double width = Math.max(0, Math.min(pct, 100));
        barFill.setStyle("-fx-background-color: #17a2b8; -fx-background-radius: 4; -fx-pref-height: 7;");
        barFill.prefWidthProperty().bind(barBg.widthProperty().multiply(width / 100.0));
        barBg.getChildren().add(barFill);

        rateBox.getChildren().addAll(rateHeader, barBg);

        card.getChildren().addAll(titleRow, pillRow, rateBox);
        return card;
    }

    private HBox makePill(String label, int value, String color) {
        HBox pill = new HBox(5);
        pill.setAlignment(Pos.CENTER);
        pill.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 8; -fx-padding: 6 12;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: " + color + "; -fx-font-weight: 600;");
        Label val = new Label(String.valueOf(value));
        val.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");
        pill.getChildren().addAll(val, lbl);
        return pill;
    }
}
