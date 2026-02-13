package Controllers;

import Models.JobOffer;
import Models.OfferSkill;
import Services.JobOfferService;
import Services.OfferSkillService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JobOffersController {

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

    // Form elements
    private TextField formTitleField;
    private TextArea formDescription;
    private TextField formLocation;
    private ComboBox<JobOffer.ContractType> formContractType;
    private DatePicker formDeadline;
    private ComboBox<JobOffer.Status> formStatus;

    // Skills management
    private VBox skillsContainer;
    private List<SkillRow> skillRows;
    private boolean isEditMode = false;
    private JobOffer editingJob = null;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        offerSkillService = new OfferSkillService();
        skillRows = new ArrayList<>();
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

        Label title = new Label("Job Offers");
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

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Job Details");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnCreate = new Button("➕ Create Job Offer");
        btnCreate.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> showCreateForm());

        topBar.getChildren().addAll(title, btnCreate);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

        panel.getChildren().addAll(topBar, scrollPane);
        return panel;
    }

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            List<JobOffer> jobs = jobOfferService.getAllJobOffers();

            if (jobs.isEmpty()) {
                Label empty = new Label("No job offers found");
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

        Label statusLabel = new Label(job.getStatus().name());
        String statusColor = job.getStatus() == JobOffer.Status.OPEN ? "#28a745" : "#dc3545";
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");

        card.getChildren().addAll(header, location, statusLabel);
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

        String statusColor = job.getStatus() == JobOffer.Status.OPEN ? "#28a745" : "#dc3545";
        Label status = new Label("📊 " + job.getStatus().name());
        status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 14px; -fx-font-weight: 700;");

        metaRow.getChildren().addAll(contractType, location, status);

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

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setStyle("-fx-padding: 25 0;");

        Button btnEdit = new Button("✏️ Edit");
        btnEdit.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white; -fx-font-weight: 600; " +
                        "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> showEditForm(job));

        Button btnDelete = new Button("🗑️ Delete");
        btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> handleDeleteJobOffer(job));

        Button btnToggleStatus = new Button(job.getStatus() == JobOffer.Status.OPEN ? "🔒 Close" : "🔓 Open");
        btnToggleStatus.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: 600; " +
                                "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnToggleStatus.setOnAction(e -> handleToggleStatus(job));

        actionButtons.getChildren().addAll(btnEdit, btnDelete, btnToggleStatus);
        detailContainer.getChildren().add(actionButtons);
    }

    private void showCreateForm() {
        isEditMode = false;
        editingJob = null;
        showJobForm("Create Job Offer");
    }

    private void showEditForm(JobOffer job) {
        isEditMode = true;
        editingJob = job;
        showJobForm("Edit Job Offer");
    }

    private void showJobForm(String formTitle) {
        detailContainer.getChildren().clear();

        Button btnBack = new Button("← Back");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #5BA3F5; -fx-font-size: 14px; -fx-cursor: hand;");
        btnBack.setOnAction(e -> {
            if (selectedJob != null) displayJobDetails(selectedJob);
        });

        Label formTitleLabel = new Label(formTitle);
        formTitleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 20 0;");

        VBox formContainer = new VBox(15);
        formContainer.setStyle("-fx-padding: 20;");

        // Basic fields
        formTitleField = new TextField();
        formTitleField.setPromptText("Job Title");
        formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        formDescription = new TextArea();
        formDescription.setPromptText("Job Description");
        formDescription.setPrefRowCount(6);
        formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        formLocation = new TextField();
        formLocation.setPromptText("Location");
        formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        formContractType = new ComboBox<>();
        formContractType.getItems().addAll(JobOffer.ContractType.values());
        formContractType.setPromptText("Select Contract Type");
        formContractType.setStyle("-fx-font-size: 14px;");

        formStatus = new ComboBox<>();
        formStatus.getItems().addAll(JobOffer.Status.values());
        formStatus.setValue(JobOffer.Status.OPEN);
        formStatus.setStyle("-fx-font-size: 14px;");

        formDeadline = new DatePicker();
        formDeadline.setPromptText("Deadline (optional)");
        formDeadline.setStyle("-fx-font-size: 14px;");

        // Skills section
        VBox skillsSection = new VBox(10);
        skillsSection.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 8;");

        Label skillsLabel = new Label("Required Skills");
        skillsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        skillsContainer = new VBox(10);
        skillRows = new ArrayList<>();

        Button btnAddSkill = new Button("+ Add Skill");
        btnAddSkill.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAddSkill.setOnAction(e -> addSkillRow(null));

        skillsSection.getChildren().addAll(skillsLabel, skillsContainer, btnAddSkill);

        // If editing, populate form with existing data
        if (isEditMode && editingJob != null) {
            formTitleField.setText(editingJob.getTitle());
            formDescription.setText(editingJob.getDescription());
            formLocation.setText(editingJob.getLocation());
            formContractType.setValue(editingJob.getContractType());
            formStatus.setValue(editingJob.getStatus());
            if (editingJob.getDeadline() != null) {
                formDeadline.setValue(editingJob.getDeadline().toLocalDate());
            }

            // Load existing skills
            try {
                List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(editingJob.getId());
                for (OfferSkill skill : skills) {
                    addSkillRow(skill);
                }
            } catch (SQLException e) {
                showAlert("Error", "Failed to load skills: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            // Add one empty skill row for new offers
            addSkillRow(null);
        }

        Button btnSubmit = new Button(isEditMode ? "Update Job Offer" : "Create Job Offer");
        btnSubmit.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-font-size: 16px; -fx-padding: 12 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSubmit.setOnAction(e -> {
            if (isEditMode) {
                handleUpdateJobOffer();
            } else {
                handleCreateJobOffer();
            }
        });

        formContainer.getChildren().addAll(
                new Label("Title *"), formTitleField,
                new Label("Description *"), formDescription,
                new Label("Location *"), formLocation,
                new Label("Contract Type *"), formContractType,
                new Label("Status *"), formStatus,
                new Label("Deadline"), formDeadline,
                skillsSection,
                btnSubmit
        );

        detailContainer.getChildren().addAll(btnBack, formTitleLabel, formContainer);
    }

    private void addSkillRow(OfferSkill existingSkill) {
        HBox skillRow = new HBox(10);
        skillRow.setAlignment(Pos.CENTER_LEFT);
        skillRow.setStyle("-fx-padding: 5;");

        TextField skillName = new TextField();
        skillName.setPromptText("Skill name (e.g., Java, JavaScript)");
        skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
        HBox.setHgrow(skillName, Priority.ALWAYS);

        ComboBox<OfferSkill.SkillLevel> skillLevel = new ComboBox<>();
        skillLevel.getItems().addAll(OfferSkill.SkillLevel.values());
        skillLevel.setPromptText("Level");
        skillLevel.setPrefWidth(150);
        skillLevel.setStyle("-fx-font-size: 13px;");

        if (existingSkill != null) {
            skillName.setText(existingSkill.getSkillName());
            skillLevel.setValue(existingSkill.getLevelRequired());
        } else {
            skillLevel.setValue(OfferSkill.SkillLevel.INTERMEDIATE);
        }

        Button btnRemove = new Button("✕");
        btnRemove.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRemove.setOnAction(e -> {
            skillsContainer.getChildren().remove(skillRow);
            skillRows.removeIf(row -> row.nameField == skillName);
        });

        skillRow.getChildren().addAll(skillName, skillLevel, btnRemove);
        skillsContainer.getChildren().add(skillRow);
        skillRows.add(new SkillRow(skillName, skillLevel));
    }

    private void handleCreateJobOffer() {
        if (!validateForm()) {
            return;
        }

        try {
            // Create JobOffer
            JobOffer newJob = new JobOffer();
            newJob.setRecruiterId(UserContext.getRecruiterId());
            newJob.setTitle(formTitleField.getText().trim());
            newJob.setDescription(formDescription.getText().trim());
            newJob.setLocation(formLocation.getText().trim());
            newJob.setContractType(formContractType.getValue());
            newJob.setStatus(formStatus.getValue());
            newJob.setCreatedAt(LocalDateTime.now());

            if (formDeadline.getValue() != null) {
                newJob.setDeadline(formDeadline.getValue().atTime(23, 59));
            }

            // Save to database
            JobOffer savedJob = jobOfferService.createJobOffer(newJob);

            // Save skills
            List<OfferSkill> skills = getSkillsFromForm(savedJob.getId());
            if (!skills.isEmpty()) {
                offerSkillService.createOfferSkills(skills);
            }

            showAlert("Success", "Job offer created successfully!", Alert.AlertType.INFORMATION);
            loadJobOffers();

            // Select the newly created job
            selectedJob = savedJob;
            displayJobDetails(savedJob);

        } catch (SQLException e) {
            showAlert("Error", "Failed to create job offer: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void handleUpdateJobOffer() {
        if (!validateForm() || editingJob == null) {
            return;
        }

        try {
            // Update JobOffer
            editingJob.setTitle(formTitleField.getText().trim());
            editingJob.setDescription(formDescription.getText().trim());
            editingJob.setLocation(formLocation.getText().trim());
            editingJob.setContractType(formContractType.getValue());
            editingJob.setStatus(formStatus.getValue());

            if (formDeadline.getValue() != null) {
                editingJob.setDeadline(formDeadline.getValue().atTime(23, 59));
            } else {
                editingJob.setDeadline(null);
            }

            // Update in database
            boolean updated = jobOfferService.updateJobOffer(editingJob);

            if (updated) {
                // Update skills
                List<OfferSkill> newSkills = getSkillsFromForm(editingJob.getId());
                offerSkillService.replaceOfferSkills(editingJob.getId(), newSkills);

                showAlert("Success", "Job offer updated successfully!", Alert.AlertType.INFORMATION);
                loadJobOffers();
                selectedJob = editingJob;
                displayJobDetails(editingJob);
            } else {
                showAlert("Error", "Failed to update job offer", Alert.AlertType.ERROR);
            }

        } catch (SQLException e) {
            showAlert("Error", "Failed to update job offer: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void handleDeleteJobOffer(JobOffer job) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Delete Job Offer");
        confirmation.setContentText("Are you sure you want to delete this job offer? This action cannot be undone.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = jobOfferService.deleteJobOffer(job.getId());
                if (deleted) {
                    showAlert("Success", "Job offer deleted successfully!", Alert.AlertType.INFORMATION);
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

    private void handleToggleStatus(JobOffer job) {
        try {
            JobOffer.Status newStatus = job.getStatus() == JobOffer.Status.OPEN
                ? JobOffer.Status.CLOSED
                : JobOffer.Status.OPEN;

            boolean updated = jobOfferService.updateJobOfferStatus(job.getId(), newStatus);
            if (updated) {
                job.setStatus(newStatus);
                showAlert("Success", "Job offer status updated to " + newStatus, Alert.AlertType.INFORMATION);
                loadJobOffers();
                displayJobDetails(job);
            } else {
                showAlert("Error", "Failed to update status", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to update status: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        if (formTitleField.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter a job title", Alert.AlertType.WARNING);
            return false;
        }
        if (formDescription.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter a job description", Alert.AlertType.WARNING);
            return false;
        }
        if (formLocation.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter a location", Alert.AlertType.WARNING);
            return false;
        }
        if (formContractType.getValue() == null) {
            showAlert("Validation Error", "Please select a contract type", Alert.AlertType.WARNING);
            return false;
        }
        if (formStatus.getValue() == null) {
            showAlert("Validation Error", "Please select a status", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    private List<OfferSkill> getSkillsFromForm(Long offerId) {
        List<OfferSkill> skills = new ArrayList<>();
        for (SkillRow row : skillRows) {
            String skillName = row.nameField.getText().trim();
            OfferSkill.SkillLevel level = row.levelCombo.getValue();

            if (!skillName.isEmpty() && level != null) {
                skills.add(new OfferSkill(offerId, skillName, level));
            }
        }
        return skills;
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

    // Helper class to store skill row components
    private static class SkillRow {
        TextField nameField;
        ComboBox<OfferSkill.SkillLevel> levelCombo;

        SkillRow(TextField nameField, ComboBox<OfferSkill.SkillLevel> levelCombo) {
            this.nameField = nameField;
            this.levelCombo = levelCombo;
        }
    }
}

