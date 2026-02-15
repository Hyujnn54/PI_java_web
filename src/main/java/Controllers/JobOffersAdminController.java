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
import java.util.Optional;

/**
 * Controller for Admin view - Can see all offers and delete any
 */
public class JobOffersAdminController {

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
            cbSearchCriteria.getItems().addAll("Title", "Location", "Contract Type", "Status");
            cbSearchCriteria.setValue("Title");
        }
    }

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 25;");

        // Top Search Bar with Admin badge
        HBox searchBar = new HBox(15);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setStyle("-fx-padding: 0 0 20 0;");

        Label adminBadge = new Label("⚙️ ADMIN PANEL");
        adminBadge.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 8 15; " +
                           "-fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 14px;");

        if (cbSearchCriteria != null) {
            cbSearchCriteria.setPromptText("Search by...");
            cbSearchCriteria.setStyle("-fx-pref-width: 150px;");
        }

        if (txtSearch != null) {
            txtSearch.setPromptText("Search all job offers...");
            txtSearch.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 10 15; " +
                              "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1;");
            HBox.setHgrow(txtSearch, Priority.ALWAYS);
        }

        if (btnSearch != null) {
            btnSearch.setText("🔍");
            btnSearch.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-size: 16px; " +
                              "-fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand;");
            btnSearch.setOnAction(e -> handleSearch());
        }

        if (btnClearSearch != null) {
            btnClearSearch.setText("✕");
            btnClearSearch.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #6c757d; -fx-font-size: 14px; " +
                                   "-fx-padding: 8 14; -fx-background-radius: 8; -fx-cursor: hand;");
            btnClearSearch.setOnAction(e -> handleClearSearch());
        }

        searchBar.getChildren().addAll(adminBadge, cbSearchCriteria, txtSearch, btnSearch, btnClearSearch);
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

        Label title = new Label("All Job Offers");
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

        Label title = new Label("Job Offer Details");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

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
            // Admin sees ALL job offers (both OPEN and CLOSED)
            List<JobOffer> jobs = jobOfferService.getAllJobOffers();

            if (jobs.isEmpty()) {
                Label empty = new Label("No job offers in database");
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
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15; " +
                     "-fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(job.getContractType().name());
        badge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 4 8; " +
                      "-fx-background-radius: 4; -fx-font-size: 11px;");

        header.getChildren().addAll(title, badge);

        HBox metaRow = new HBox(10);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Not specified"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px;");

        Label recruiterInfo = new Label("👤 Recruiter ID: " + job.getRecruiterId());
        recruiterInfo.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        metaRow.getChildren().addAll(location, recruiterInfo);

        Label statusLabel = new Label(job.getStatus().name());
        String statusColor = job.getStatus() == Status.OPEN ? "#28a745" : "#dc3545";
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; " +
                            "-fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");

        card.getChildren().addAll(header, metaRow, statusLabel);
        card.setOnMouseClicked(e -> selectJob(job, card));

        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 18; " +
                             "-fx-border-color: #e9ecef; -fx-border-width: 0 0 0 4; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });

        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18; " +
                     "-fx-border-color: #dc3545; -fx-border-width: 0 0 0 4; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian, rgba(220,53,69,0.3), 12, 0, 0, 2); -fx-cursor: hand;");

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

        String statusColor = job.getStatus() == Status.OPEN ? "#28a745" : "#dc3545";
        Label status = new Label("📊 " + job.getStatus().name());
        status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 14px; -fx-font-weight: 700;");

        Label recruiterInfo = new Label("👤 Recruiter ID: " + job.getRecruiterId());
        recruiterInfo.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        metaRow.getChildren().addAll(contractType, location, status, recruiterInfo);

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
                    skillBox.setStyle("-fx-background-color: white; -fx-padding: 10 15; -fx-background-radius: 8; " +
                                     "-fx-border-color: #dee2e6; -fx-border-radius: 8;");

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

        // Admin action buttons - DELETE only
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setStyle("-fx-padding: 25 0;");

        Button btnDelete = new Button("🗑️ DELETE JOB OFFER");
        btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-font-size: 14px; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> handleDeleteJobOffer(job));

        actionButtons.getChildren().add(btnDelete);
        detailContainer.getChildren().add(actionButtons);

        // Admin info box
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER);
        infoBox.setStyle("-fx-background-color: #fff3cd; -fx-padding: 15; -fx-background-radius: 8; " +
                        "-fx-border-color: #ffc107; -fx-border-radius: 8; -fx-border-width: 1;");

        Label infoLabel = new Label("⚙️ ADMIN MODE");
        infoLabel.setStyle("-fx-text-fill: #856404; -fx-font-size: 13px; -fx-font-weight: 700;");

        Label noteLabel = new Label("You have full access to delete any job offer in the system");
        noteLabel.setStyle("-fx-text-fill: #856404; -fx-font-size: 12px;");
        noteLabel.setWrapText(true);

        infoBox.getChildren().addAll(infoLabel, noteLabel);
        detailContainer.getChildren().add(infoBox);
    }

    private void handleDeleteJobOffer(JobOffer job) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("⚠️ Admin Delete Confirmation");
        confirmation.setHeaderText("Delete Job Offer");
        confirmation.setContentText("Are you sure you want to delete this job offer?\n\n" +
                                   "Title: " + job.getTitle() + "\n" +
                                   "Recruiter ID: " + job.getRecruiterId() + "\n\n" +
                                   "This action CANNOT be undone!");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = jobOfferService.deleteJobOffer(job.getId());
                if (deleted) {
                    showAlert("Success", "Job offer deleted successfully by Admin!", Alert.AlertType.INFORMATION);
                    selectedJob = null;
                    detailContainer.getChildren().clear();
                    Label noSelection = new Label("Select a job offer to view details");
                    noSelection.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 16px;");
                    detailContainer.getChildren().add(noSelection);
                    loadJobOffers();
                } else {
                    showAlert("Error", "Failed to delete job offer", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to delete job offer: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
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
            } else if ("Status".equals(criteria)) {
                results = jobOfferService.searchJobOffers(keyword, "status");
            } else {
                results = jobOfferService.searchJobOffers(keyword, "title");
            }

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

