package Controllers;

import Services.ApplicationService;
import Services.JobOfferService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class JobOffersController {

    @FXML private VBox mainContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
    @FXML private Button btnSearch;
    @FXML private Button btnClearSearch;

    private VBox jobListContainer;
    private ScrollPane detailScrollPane;
    private VBox detailContainer;
    private JobOfferService.JobOfferRow selectedJob;

    // Form elements
    private TextField formTitle;
    private TextArea formDescription;
    private TextField formLocation;
    private ComboBox<String> formContractType;
    private DatePicker formDeadline;

    @FXML
    public void initialize() {
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

        // Top Search Bar (same as Applications)
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

        // Main content area (split view like Applications)
        HBox contentArea = new HBox(20);
        contentArea.getStyleClass().add("split-view-container");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // LEFT: Job list (30-35% width) - same as Applications candidate list
        VBox leftSide = createJobListPanel();
        leftSide.setPrefWidth(400);
        leftSide.setMinWidth(350);
        leftSide.setMaxWidth(450);

        // RIGHT: Details (65-70% width) - same as Applications detail view
        VBox rightSide = createDetailPanel();
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        contentArea.getChildren().addAll(leftSide, rightSide);
        mainContainer.getChildren().add(contentArea);
    }

    private VBox createJobListPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        // Title only (search is at top now)
        Label title = new Label("Job Offers");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        // Scrollable list
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

        // Top bar with title and create button
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Job Details");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        topBar.getChildren().add(title);

        // Create button for recruiters only
        if (UserContext.getRole() == UserContext.Role.RECRUITER) {
            Button btnCreate = new Button("➕ Create Job Offer");
            btnCreate.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                              "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
            btnCreate.setOnAction(e -> showCreateForm());
            topBar.getChildren().add(btnCreate);
        }

        ScrollPane detailScrollPane = new ScrollPane();
        detailScrollPane.setFitToWidth(true);
        detailScrollPane.setFitToHeight(true);
        detailScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(detailScrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        detailScrollPane.setContent(detailContainer);

        panel.getChildren().addAll(topBar, detailScrollPane);
        return panel;
    }

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        List<JobOfferService.JobOfferRow> jobs = JobOfferService.getAll();

        if (jobs.isEmpty()) {
            Label empty = new Label("No job offers found");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
            jobListContainer.getChildren().add(empty);
            return;
        }

        boolean first = true;
        for (JobOfferService.JobOfferRow job : jobs) {
            VBox card = createJobCard(job);
            jobListContainer.getChildren().add(card);
            if (first) {
                selectJob(job, card);
                first = false;
            }
        }
    }

    private VBox createJobCard(JobOfferService.JobOfferRow job) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(job.title());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(job.contractType());
        badge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px;");

        header.getChildren().addAll(title, badge);

        Label location = new Label("📍 " + (job.location() != null ? job.location() : "Not specified"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px;");

        card.getChildren().addAll(header, location);

        card.setOnMouseClicked(e -> selectJob(job, card));

        return card;
    }

    private void selectJob(JobOfferService.JobOfferRow job, VBox card) {
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox vbox) {
                vbox.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 18; -fx-border-color: #e9ecef; -fx-border-width: 0 0 0 4; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });

        // Selected card: white background with blue left border (like Applications)
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 18; " +
                     "-fx-border-color: #5BA3F5; -fx-border-width: 0 0 0 4; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian, rgba(91,163,245,0.2), 10, 0, 0, 2); -fx-cursor: hand;");

        selectedJob = job;
        displayJobDetails(job);
    }

    private void displayJobDetails(JobOfferService.JobOfferRow job) {
        detailContainer.getChildren().clear();

        VBox headerCard = new VBox(15);
        headerCard.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 25;");

        Label title = new Label(job.title());
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label contractType = new Label("💼 " + job.contractType());
        contractType.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        Label location = new Label("📍 " + (job.location() != null ? job.location() : "Not specified"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        metaRow.getChildren().addAll(contractType, location);

        if (job.deadline() != null) {
            Label deadline = new Label("⏰ Deadline: " + job.deadline().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
            deadline.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 14px; -fx-font-weight: 700;");
            metaRow.getChildren().add(deadline);
        }

        headerCard.getChildren().addAll(title, metaRow);
        detailContainer.getChildren().add(headerCard);

        if (job.description() != null && !job.description().isBlank()) {
            VBox descSection = new VBox(12);
            descSection.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 25;");

            Label descTitle = new Label("Job Description");
            descTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

            Label descText = new Label(job.description());
            descText.setWrapText(true);
            descText.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px; -fx-line-spacing: 3;");

            descSection.getChildren().addAll(descTitle, descText);
            detailContainer.getChildren().add(descSection);
        }

        if (job.createdAt() != null) {
            Label posted = new Label("Posted on: " + job.createdAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
            posted.setStyle("-fx-text-fill: #8e9ba8; -fx-font-size: 12px; -fx-padding: 15 0;");
            detailContainer.getChildren().add(posted);
        }

        if (UserContext.getRole() != UserContext.Role.RECRUITER) {
            Button btnApply = new Button("Apply Now");
            btnApply.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 16px; -fx-padding: 14 40; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(91,163,245,0.3), 10, 0, 0, 2);");
            btnApply.setOnAction(e -> showApplicationForm(job));

            btnApply.setOnMouseEntered(e -> btnApply.setStyle("-fx-background-color: #4A90E2; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 16px; -fx-padding: 14 40; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(91,163,245,0.5), 15, 0, 0, 3);"));
            btnApply.setOnMouseExited(e -> btnApply.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 16px; -fx-padding: 14 40; -fx-background-radius: 10; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(91,163,245,0.3), 10, 0, 0, 2);"));

            HBox buttonBox = new HBox(btnApply);
            buttonBox.setAlignment(Pos.CENTER);
            buttonBox.setStyle("-fx-padding: 25 0;");
            detailContainer.getChildren().add(buttonBox);
        }
    }

    private void showCreateForm() {
        detailContainer.getChildren().clear();

        Button btnBack = new Button("← Back");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #5BA3F5; -fx-font-size: 14px; -fx-cursor: hand;");
        btnBack.setOnAction(e -> {
            if (selectedJob != null) displayJobDetails(selectedJob);
        });

        Label formTitleLabel = new Label("Create Job Offer");
        formTitleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 20 0;");

        VBox formContainer = new VBox(20);
        formContainer.setStyle("-fx-padding: 20;");

        formTitle = new TextField();
        formTitle.setPromptText("Job Title");

        formDescription = new TextArea();
        formDescription.setPromptText("Description");
        formDescription.setPrefRowCount(6);

        formLocation = new TextField();
        formLocation.setPromptText("Location");

        formContractType = new ComboBox<>();
        formContractType.getItems().addAll("CDI", "CDD", "INTERNSHIP", "FREELANCE", "PART_TIME", "FULL_TIME");
        formContractType.setPromptText("Contract Type");

        formDeadline = new DatePicker();
        formDeadline.setPromptText("Deadline");

        Button btnSubmit = new Button("Create");
        btnSubmit.getStyleClass().addAll("btn-success", "action-button");
        btnSubmit.setOnAction(e -> handleCreateJobOffer());

        formContainer.getChildren().addAll(
                new Label("Title *"), formTitle,
                new Label("Description *"), formDescription,
                new Label("Location *"), formLocation,
                new Label("Contract Type *"), formContractType,
                new Label("Deadline"), formDeadline,
                btnSubmit
        );

        detailContainer.getChildren().addAll(btnBack, formTitleLabel, formContainer);
    }

    private void handleCreateJobOffer() {
        if (formTitle.getText().trim().isEmpty() || formDescription.getText().trim().isEmpty() ||
                formLocation.getText().trim().isEmpty() || formContractType.getValue() == null) {
            showAlert("Error", "Please fill all required fields", Alert.AlertType.WARNING);
            return;
        }

        try {
            LocalDateTime deadline = formDeadline.getValue() != null ? formDeadline.getValue().atTime(23, 59) : null;

            JobOfferService.JobOfferRow newJob = new JobOfferService.JobOfferRow(
                    null, UserContext.getRecruiterId(),
                    formTitle.getText().trim(), formDescription.getText().trim(),
                    formLocation.getText().trim(), formContractType.getValue(),
                    LocalDateTime.now(), deadline, "OPEN"
            );

            JobOfferService.addJobOffer(newJob);
            showAlert("Success", "Job offer created!", Alert.AlertType.INFORMATION);
            loadJobOffers();
        } catch (Exception e) {
            showAlert("Error", e.getMessage(), Alert.AlertType.ERROR);
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

        List<JobOfferService.JobOfferRow> results = switch (criteria) {
            case "Location" -> JobOfferService.searchByLocation(keyword);
            case "Contract Type" -> JobOfferService.searchByContractType(keyword);
            default -> JobOfferService.searchByTitle(keyword);
        };

        jobListContainer.getChildren().clear();
        for (JobOfferService.JobOfferRow job : results) {
            jobListContainer.getChildren().add(createJobCard(job));
        }
    }

    @FXML
    private void handleClearSearch() {
        if (txtSearch != null) txtSearch.clear();
        loadJobOffers();
    }

    @FXML
    private void handleClear() {
        handleClearSearch(); // Alias for FXML compatibility
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showApplicationForm(JobOfferService.JobOfferRow job) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Apply for Job");
        dialog.setHeaderText("Apply for: " + job.title());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Phone field
        Label phoneLabel = new Label("Phone Number *");
        phoneLabel.setStyle("-fx-font-weight: bold;");
        TextField phoneField = new TextField();
        phoneField.setPromptText("Enter your phone number");
        phoneField.setPrefWidth(400);

        // Cover Letter field
        Label letterLabel = new Label("Cover Letter *");
        letterLabel.setStyle("-fx-font-weight: bold;");
        TextArea letterArea = new TextArea();
        letterArea.setPromptText("Tell us why you're interested in this position...");
        letterArea.setPrefRowCount(8);
        letterArea.setWrapText(true);

        // CV Path (optional)
        Label cvLabel = new Label("CV Path (optional)");
        cvLabel.setStyle("-fx-font-weight: bold;");
        TextField cvField = new TextField();
        cvField.setPromptText("/path/to/your/cv.pdf");
        cvField.setPrefWidth(400);

        content.getChildren().addAll(
            phoneLabel, phoneField,
            letterLabel, letterArea,
            cvLabel, cvField
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                submitApplication(job, phoneField.getText(), letterArea.getText(), cvField.getText());
            }
        });
    }

    private void submitApplication(JobOfferService.JobOfferRow job, String phone, String coverLetter, String cvPath) {
        if (phone == null || phone.trim().isEmpty()) {
            showAlert("Validation Error", "Phone number is required", Alert.AlertType.ERROR);
            return;
        }

        if (coverLetter == null || coverLetter.trim().isEmpty()) {
            showAlert("Validation Error", "Cover letter is required", Alert.AlertType.ERROR);
            return;
        }

        try {
            Long candidateId = UserContext.getCandidateId();
            if (candidateId == null) {
                showAlert("Error", "Candidate ID not found. Please login again.", Alert.AlertType.ERROR);
                return;
            }

            ApplicationService.create(job.id(), candidateId, phone, coverLetter, cvPath != null ? cvPath : "");
            showAlert("Success", "Application submitted successfully!", Alert.AlertType.INFORMATION);

        } catch (Exception e) {
            showAlert("Error", "Failed to submit application: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }
}
