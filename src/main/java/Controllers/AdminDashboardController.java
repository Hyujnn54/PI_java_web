package Controllers;

import Services.ApplicationStatisticsService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Map;

public class AdminDashboardController {

    @FXML private HBox globalStatsContainer;
    @FXML private VBox offerStatsContainer;
    @FXML private Button btnRefresh;

    @FXML
    public void initialize() {
        // Only admins should see this
        if (UserContext.getRole() != UserContext.Role.ADMIN) {
            Label accessDenied = new Label("Access Denied. Admin only.");
            accessDenied.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 16; -fx-font-weight: bold;");
            globalStatsContainer.getChildren().add(accessDenied);
            return;
        }

        btnRefresh.setOnAction(e -> loadStatistics());
        loadStatistics();
    }

    private void loadStatistics() {
        // Clear existing content
        globalStatsContainer.getChildren().clear();
        offerStatsContainer.getChildren().clear();

        // Load global statistics
        ApplicationStatisticsService.OfferStatistics globalStats = ApplicationStatisticsService.getGlobalStatistics();
        if (globalStats != null) {
            displayGlobalStatistics(globalStats);
        }

        // Load per-offer statistics
        Map<Long, ApplicationStatisticsService.OfferStatistics> offerStats =
            ApplicationStatisticsService.getAllOfferStatistics();

        if (offerStats.isEmpty()) {
            Label noData = new Label("No offers with applications found");
            noData.setStyle("-fx-text-fill: #999; -fx-font-size: 14;");
            offerStatsContainer.getChildren().add(noData);
        } else {
            offerStats.forEach((offerId, stats) -> {
                VBox offerCard = createOfferStatisticsCard(stats);
                offerStatsContainer.getChildren().add(offerCard);
            });
        }
    }

    private void displayGlobalStatistics(ApplicationStatisticsService.OfferStatistics stats) {
        // Total Applications Card
        VBox totalCard = createStatCard("Total Applications", String.valueOf(stats.totalApplications()), "#0066cc");
        globalStatsContainer.getChildren().add(totalCard);

        // Submitted Card
        VBox submittedCard = createStatCard("Submitted", String.valueOf(stats.submitted()), "#6c757d");
        globalStatsContainer.getChildren().add(submittedCard);

        // Shortlisted Card
        VBox shortlistedCard = createStatCard("Shortlisted", String.valueOf(stats.shortlisted()), "#ffc107");
        globalStatsContainer.getChildren().add(shortlistedCard);

        // Rejected Card
        VBox rejectedCard = createStatCard("Rejected", String.valueOf(stats.rejected()), "#dc3545");
        globalStatsContainer.getChildren().add(rejectedCard);

        // Interview Card
        VBox interviewCard = createStatCard("Interview", String.valueOf(stats.interview()), "#17a2b8");
        globalStatsContainer.getChildren().add(interviewCard);

        // Hired Card
        VBox hiredCard = createStatCard("Hired", String.valueOf(stats.hired()), "#28a745");
        globalStatsContainer.getChildren().add(hiredCard);

        // Acceptance Rate Card
        VBox acceptanceCard = createStatCard("Acceptance Rate",
            stats.getAcceptancePercentage() + "%", "#007bff");
        globalStatsContainer.getChildren().add(acceptanceCard);
    }

    private VBox createStatCard(String title, String value, String color) {
        VBox card = new VBox(10);
        card.setStyle("-fx-border-color: " + color + "; -fx-border-radius: 4; -fx-padding: 15; " +
                     "-fx-background-color: white; -fx-border-width: 2;");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(140);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #666;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 28; -fx-text-fill: " + color + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private VBox createOfferStatisticsCard(ApplicationStatisticsService.OfferStatistics stats) {
        VBox card = new VBox(12);
        card.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15; " +
                     "-fx-background-color: #f8f9fa; -fx-border-width: 1;");

        // Offer Title
        Label offerTitle = new Label(stats.offerTitle());
        offerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        // Total Applications
        HBox totalBox = createStatRow("Total Applications", String.valueOf(stats.totalApplications()));

        // Status breakdown in a 3x2 grid
        HBox statusGrid = new HBox(15);
        statusGrid.setStyle("-fx-padding: 10; -fx-border-top: 1px solid #ddd; -fx-border-bottom: 1px solid #ddd;");

        VBox submittedBox = createCompactStatBox("Submitted", stats.submitted(), "#6c757d");
        VBox shortlistedBox = createCompactStatBox("Shortlisted", stats.shortlisted(), "#ffc107");
        VBox rejectedBox = createCompactStatBox("Rejected", stats.rejected(), "#dc3545");
        VBox interviewBox = createCompactStatBox("Interview", stats.interview(), "#17a2b8");
        VBox hiredBox = createCompactStatBox("Hired", stats.hired(), "#28a745");

        statusGrid.getChildren().addAll(submittedBox, shortlistedBox, rejectedBox,
                                        interviewBox, hiredBox);

        // Acceptance Rate
        HBox acceptanceBox = createStatRow("Acceptance Rate", stats.getAcceptancePercentage() + "%");
        acceptanceBox.setStyle("-fx-padding: 10; -fx-border-top: 1px solid #ddd;");

        card.getChildren().addAll(offerTitle, totalBox, statusGrid, acceptanceBox);
        return card;
    }

    private HBox createStatRow(String label, String value) {
        HBox row = new HBox(15);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8;");

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #666;");
        labelText.setPrefWidth(120);

        Label valueText = new Label(value);
        valueText.setStyle("-fx-font-size: 14; -fx-text-fill: #333; -fx-font-weight: bold;");

        row.getChildren().addAll(labelText, valueText);
        return row;
    }

    private VBox createCompactStatBox(String label, int value, String color) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(100);

        Label labelText = new Label(label);
        labelText.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");

        Label valueText = new Label(String.valueOf(value));
        valueText.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        box.getChildren().addAll(labelText, valueText);
        return box;
    }
}

