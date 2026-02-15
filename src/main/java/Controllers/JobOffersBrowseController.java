package Controllers;

import Models.JobOffer;
import Models.OfferSkill;
import Models.Status;
import Services.JobOfferService;
import Services.OfferSkillService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for Job Offers Browse page (Candidate view - Read-only)
 */
public class JobOffersBrowseController {

    @FXML private VBox mainContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;

    private VBox jobListContainer;
    private VBox detailContainer;
    private JobOffer selectedJob;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        offerSkillService = new OfferSkillService();

        setupComboBoxes();
        buildUI();
        loadJobOffers();
    }

    private void setupComboBoxes() {
        if (cbSearchCriteria != null) {
            cbSearchCriteria.getItems().addAll("Title", "Location", "Contract Type");
            cbSearchCriteria.setValue("Title");
        }
    }

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 25;");

        // Top Search Bar
        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setStyle("-fx-padding: 0 0 20 0;");

        if (cbSearchCriteria != null) {
            cbSearchCriteria.setPromptText("Search by...");
            cbSearchCriteria.setStyle("-fx-pref-width: 150px;");
            cbSearchCriteria.setVisible(true);
            cbSearchCriteria.setManaged(true);
        }

        if (txtSearch != null) {
            txtSearch.setPromptText("Search job offers...");
            txtSearch.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10 15; -fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1;");
            HBox.setHgrow(txtSearch, Priority.ALWAYS);
            txtSearch.setVisible(true);
            txtSearch.setManaged(true);
        }

        if (btnSearch != null) {
            btnSearch.setText("🔍");
            btnSearch.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand;");
            btnSearch.setOnAction(e -> handleSearch());
            btnSearch.setVisible(true);
            btnSearch.setManaged(true);
        }

        if (btnClearSearch != null) {
            btnClearSearch.setText("✕");
            btnClearSearch.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand;");
            btnClearSearch.setOnAction(e -> handleClearSearch());
            btnClearSearch.setVisible(true);
            btnClearSearch.setManaged(true);
        }

        searchBar.getChildren().addAll(cbSearchCriteria, txtSearch, btnSearch, btnClearSearch);
        mainContainer.getChildren().add(searchBar);

        // Main content area
        HBox contentArea = new HBox(20);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // LEFT: Job list
        VBox leftSide = createJobListPanel();
        leftSide.setPrefWidth(400);
        leftSide.setMinWidth(350);
        leftSide.setMaxWidth(450);

        // RIGHT: Details
        VBox rightSide = createDetailPanel();
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        contentArea.getChildren().addAll(leftSide, rightSide);
        mainContainer.getChildren().add(contentArea);
    }

    private VBox createJobListPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        Label title = new Label("Available Job Offers");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        jobListContainer = new VBox(12);
        jobListContainer.setStyle("-fx-padding: 5 0;");
        scroll.setContent(jobListContainer);

        panel.getChildren().addAll(title, scroll);
        return panel;
    }

    private VBox createDetailPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        Label title = new Label("Job Details");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

        // Initial message
        Label selectMessage = new Label("Select a job offer to view details");
        selectMessage.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 16px; -fx-padding: 40;");
        selectMessage.setAlignment(Pos.CENTER);
        detailContainer.getChildren().add(selectMessage);

        panel.getChildren().addAll(title, scrollPane);
        return panel;
    }

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            // Only load OPEN job offers for candidates
            List<JobOffer> jobs = jobOfferService.getJobOffersByStatus(Status.OPEN);

            if (jobs.isEmpty()) {
                Label empty = new Label("No job offers available");
                empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
                jobListContainer.getChildren().add(empty);
                return;
            }

            boolean first = true;
            for (JobOffer job : jobs) {
                VBox card = createJobCard(job);
                jobListContainer.getChildren().add(card);
                if (first) {
                    selectJob(job, card);
                    first = false;
                }
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load job offers: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(job.getContractType().name());
        badge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px;");

        header.getChildren().addAll(title, badge);

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Not specified"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px;");

        card.getChildren().addAll(header, location);
        card.setOnMouseClicked(e -> selectJob(job, card));

        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 18; -fx-border-color: #e9ecef; -fx-border-width: 0 0 0 4; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });

        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18; " +
                     "-fx-border-color: #5BA3F5; -fx-border-width: 0 0 0 4; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian, rgba(91,163,245,0.2), 10, 0, 0, 2); -fx-cursor: hand;");

        selectedJob = job;
        displayJobDetails(job);
    }

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        VBox headerCard = new VBox(15);
        headerCard.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 25;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label contractType = new Label("💼 " + job.getContractType().name());
        contractType.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Not specified"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        metaRow.getChildren().addAll(contractType, location);

        if (job.getDeadline() != null) {
            Label deadline = new Label("⏰ Deadline: " + job.getDeadline().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            deadline.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px; -fx-font-weight: 700;");
            metaRow.getChildren().add(deadline);
        }

        headerCard.getChildren().addAll(title, metaRow);
        detailContainer.getChildren().add(headerCard);

        // Description section
        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            VBox descSection = new VBox(12);
            descSection.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 25;");

            Label descTitle = new Label("Job Description");
            descTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

            Label descText = new Label(job.getDescription());
            descText.setWrapText(true);
            descText.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px; -fx-line-spacing: 3;");

            descSection.getChildren().addAll(descTitle, descText);
            detailContainer.getChildren().add(descSection);
        }

        // Skills section
        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(job.getId());
            if (!skills.isEmpty()) {
                VBox skillsSection = new VBox(12);
                skillsSection.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 25;");

                Label skillsTitle = new Label("Required Skills");
                skillsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

                FlowPane skillsFlow = new FlowPane(10, 10);
                for (OfferSkill skill : skills) {
                    VBox skillBox = new VBox(5);
                    skillBox.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 8; -fx-border-color: #dee2e6; -fx-border-radius: 8;");

                    Label skillName = new Label(skill.getSkillName());
                    skillName.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");

                    Label skillLevel = new Label(skill.getLevelRequired().name());
                    skillLevel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

                    skillBox.getChildren().addAll(skillName, skillLevel);
                    skillsFlow.getChildren().add(skillBox);
                }

                skillsSection.getChildren().addAll(skillsTitle, skillsFlow);
                detailContainer.getChildren().add(skillsSection);
            }
        } catch (SQLException e) {
            System.err.println("Failed to load skills: " + e.getMessage());
        }

        // Posted date
        if (job.getCreatedAt() != null) {
            Label posted = new Label("Posted on: " + job.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            posted.setStyle("-fx-text-fill: #8e9ba8; -fx-font-size: 12px; -fx-padding: 15 0;");
            detailContainer.getChildren().add(posted);
        }

        // Apply button for candidates
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setStyle("-fx-padding: 25 0;");

        Button btnApply = new Button("📝 Apply for this Position");
        btnApply.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                         "-fx-font-size: 16px; -fx-padding: 14 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btnApply.setOnAction(e -> handleApply(job));

        actionButtons.getChildren().add(btnApply);
        detailContainer.getChildren().add(actionButtons);
    }

    private void handleApply(JobOffer job) {
        // Static button - just show confirmation
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Application");
        alert.setHeaderText("Apply for: " + job.getTitle());
        alert.setContentText("Application feature will be implemented soon.\n\nYou would apply for this position:\n" +
                            job.getTitle() + " at " + (job.getLocation() != null ? job.getLocation() : "Unknown location"));
        alert.showAndWait();
    }

    @FXML
    private void handleSearch() {
        if (txtSearch == null || txtSearch.getText().trim().isEmpty()) {
            loadJobOffers();
            return;
        }

        String keyword = txtSearch.getText().trim();
        String criteria = cbSearchCriteria != null ? cbSearchCriteria.getValue() : "Title";

        try {
            List<JobOffer> results;
            if ("Location".equals(criteria)) {
                results = jobOfferService.searchJobOffers(keyword, "location");
            } else if ("Contract Type".equals(criteria)) {
                results = jobOfferService.searchJobOffers(keyword, "contract_type");
            } else {
                results = jobOfferService.searchJobOffers(keyword, "title");
            }

            // Filter only OPEN jobs
            results.removeIf(job -> job.getStatus() != Status.OPEN);

            jobListContainer.getChildren().clear();
            if (results.isEmpty()) {
                Label empty = new Label("No results found");
                empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
                jobListContainer.getChildren().add(empty);
            } else {
                for (JobOffer job : results) {
                    jobListContainer.getChildren().add(createJobCard(job));
                }
            }
        } catch (SQLException e) {
            showAlert("Error", "Search failed: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearSearch() {
        if (txtSearch != null) txtSearch.clear();
        loadJobOffers();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

