package Controllers;

import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Job Offers Controller
 * - Recruiter: Can create and manage job offers (uses JobOffers.fxml)
 * - Candidate: Can browse and apply to jobs (uses JobOffersBrowse.fxml or dynamic layout)
 */
public class JobOffersController {

    @FXML private HBox mainContent;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
    @FXML private Label lblTitle;
    @FXML private Label lblSubtitle;

    // Form fields for recruiter
    @FXML private TextField txtJobTitle;
    @FXML private ComboBox<String> cbDepartment;
    @FXML private ComboBox<String> cbEmploymentType;
    @FXML private TextField txtLocation;
    @FXML private ComboBox<String> cbWorkMode;
    @FXML private TextField txtSalaryMin;
    @FXML private TextField txtSalaryMax;
    @FXML private TextArea txtDescription;
    @FXML private TextArea txtRequirements;
    @FXML private TextArea txtBenefits;
    @FXML private ComboBox<String> cbExperienceLevel;
    @FXML private DatePicker dpDeadline;
    @FXML private TextField txtContactEmail;

    private boolean isRecruiter = true;
    private JobOffer selectedJob;

    public void setUserRole(String role) {
        isRecruiter = "RECRUITER".equalsIgnoreCase(role);
        setupUI();
    }

    @FXML
    public void initialize() {
        setupComboBoxes();
        setupUI();
    }

    private void setupComboBoxes() {
        // Search criteria
        if (cbSearchCriteria != null) {
            cbSearchCriteria.getItems().addAll("Title", "Department", "Location", "Type");
            cbSearchCriteria.setValue("Title");
        }

        // Form combo boxes
        if (cbDepartment != null) {
            cbDepartment.getItems().addAll("Technology", "Design", "Marketing", "Sales", "HR", "Finance");
        }

        if (cbEmploymentType != null) {
            cbEmploymentType.getItems().addAll("Full-time", "Part-time", "Contract", "Internship");
        }

        if (cbWorkMode != null) {
            cbWorkMode.getItems().addAll("On-site", "Remote", "Hybrid");
        }

        if (cbExperienceLevel != null) {
            cbExperienceLevel.getItems().addAll("Entry Level", "Mid Level", "Senior Level", "Lead/Principal");
        }
    }

    private void setupUI() {
        if (mainContent == null) return;

        mainContent.getChildren().clear();

        if (isRecruiter) {
            setupRecruiterView();
        } else {
            setupCandidateView();
        }
    }

    private void setupRecruiterView() {
        // The recruiter view is already defined in JobOffers.fxml
        // Just load the job list on the left
        VBox leftSide = createJobListView(true);
        mainContent.getChildren().add(leftSide);
    }

    private void setupCandidateView() {
        // LEFT SIDE: Job List (40%)
        VBox leftSide = createJobListView(false);
        leftSide.setPrefWidth(450);

        // RIGHT SIDE: Job Details for candidates (60%)
        VBox rightSide = createJobDetailView();
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        mainContent.getChildren().addAll(leftSide, rightSide);
    }

    private VBox createJobListView(boolean isRecruiter) {
        VBox container = new VBox(15);
        container.getStyleClass().add("job-offers-list-container");

        Label title = new Label(isRecruiter ? "Active Job Offers" : "Available Positions");
        title.getStyleClass().add("section-title");

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox jobsList = new VBox(15);
        jobsList.setStyle("-fx-padding: 5;");

        // Sample job offers
        List<JobOffer> jobs = getSampleJobs();

        boolean first = true;
        for (JobOffer job : jobs) {
            VBox card = createJobCard(job, first);
            jobsList.getChildren().add(card);

            if (first) {
                selectedJob = job;
                first = false;
            }
        }

        scroll.setContent(jobsList);
        container.getChildren().addAll(title, scroll);

        return container;
    }

    private VBox createJobCard(JobOffer job, boolean selected) {
        VBox card = new VBox(12);
        card.getStyleClass().add("job-offer-card");
        if (selected) {
            card.getStyleClass().add("job-offer-selected");
        }
        card.setUserData(job);

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(6);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        Label title = new Label(job.title);
        title.getStyleClass().add("job-title");

        Label meta = new Label(job.department + " • " + job.type);
        meta.getStyleClass().add("job-meta");

        titleBox.getChildren().addAll(title, meta);

        Label statusBadge = new Label(job.status);
        statusBadge.getStyleClass().addAll("job-status-badge",
            job.status.equals("ACTIVE") ? "job-status-active" : "job-status-closed");

        header.getChildren().addAll(titleBox, statusBadge);

        // Info
        HBox info = new HBox(15);
        Label location = new Label("📍 " + job.location);
        location.getStyleClass().add("job-info");
        Label salary = new Label("💰 " + job.salaryRange);
        salary.getStyleClass().add("job-info");
        info.getChildren().addAll(location, salary);

        // Stats
        HBox stats = new HBox(10);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label applicants = new Label(job.applicants + " Applicants");
        applicants.getStyleClass().add("applicant-count");
        Label sep = new Label("•");
        sep.getStyleClass().add("job-separator");
        Label posted = new Label("Posted: " + job.postedDate);
        posted.getStyleClass().add("job-date");
        stats.getChildren().addAll(applicants, sep, posted);

        card.getChildren().addAll(header, info, stats);

        // Click handler
        card.setOnMouseClicked(e -> selectJob(job, card));

        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        // Remove selection from all cards
        VBox parent = (VBox) card.getParent();
        parent.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.getStyleClass().remove("job-offer-selected");
            }
        });

        // Add selection to clicked card
        card.getStyleClass().add("job-offer-selected");
        selectedJob = job;

        // Update detail view for candidates
        if (!isRecruiter && mainContent.getChildren().size() > 1) {
            VBox detailView = (VBox) mainContent.getChildren().get(1);
            updateJobDetailView(detailView, job);
        }
    }

    private VBox createJobDetailView() {
        VBox container = new VBox(20);
        container.getStyleClass().add("job-form-container");

        Label title = new Label("Job Details");
        title.getStyleClass().add("section-title");

        ScrollPane scroll = new ScrollPane();
        scroll.getStyleClass().add("transparent-scroll");
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 10;");

        if (selectedJob != null) {
            updateJobDetailView(content, selectedJob);
        }

        scroll.setContent(content);
        container.getChildren().addAll(title, scroll);

        return container;
    }

    private void updateJobDetailView(VBox container, JobOffer job) {
        container.getChildren().clear();

        // Job Header
        VBox headerCard = new VBox(15);
        headerCard.getStyleClass().add("detail-header-card");

        Label jobTitle = new Label(job.title);
        jobTitle.getStyleClass().add("detail-candidate-name");

        Label department = new Label(job.department + " • " + job.type);
        department.getStyleClass().add("detail-candidate-position");

        Separator sep = new Separator();
        sep.getStyleClass().add("detail-separator");

        HBox infoRow = new HBox(30);

        VBox locBox = new VBox(5);
        Label locLabel = new Label("📍 Location");
        locLabel.getStyleClass().add("detail-label");
        Label locValue = new Label(job.location);
        locValue.getStyleClass().add("detail-value");
        locBox.getChildren().addAll(locLabel, locValue);

        VBox salBox = new VBox(5);
        Label salLabel = new Label("💰 Salary");
        salLabel.getStyleClass().add("detail-label");
        Label salValue = new Label(job.salaryRange);
        salValue.getStyleClass().add("detail-value");
        salBox.getChildren().addAll(salLabel, salValue);

        VBox typeBox = new VBox(5);
        Label typeLabel = new Label("💼 Type");
        typeLabel.getStyleClass().add("detail-label");
        Label typeValue = new Label(job.type);
        typeValue.getStyleClass().add("detail-value");
        typeBox.getChildren().addAll(typeLabel, typeValue);

        infoRow.getChildren().addAll(locBox, salBox, typeBox);

        headerCard.getChildren().addAll(jobTitle, department, sep, infoRow);
        container.getChildren().add(headerCard);

        // Description Section
        VBox descSection = new VBox(12);
        descSection.getStyleClass().add("detail-section-card");

        Label descTitle = new Label("Job Description");
        descTitle.getStyleClass().add("detail-section-title");

        Label descText = new Label(job.description);
        descText.setWrapText(true);
        descText.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");

        descSection.getChildren().addAll(descTitle, descText);
        container.getChildren().add(descSection);

        // Requirements Section
        VBox reqSection = new VBox(12);
        reqSection.getStyleClass().add("detail-section-card");

        Label reqTitle = new Label("Requirements");
        reqTitle.getStyleClass().add("detail-section-title");

        VBox reqList = new VBox(8);
        String[] requirements = job.requirements.split("\n");
        for (String req : requirements) {
            if (!req.trim().isEmpty()) {
                Label reqItem = new Label("• " + req.trim());
                reqItem.setWrapText(true);
                reqItem.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px;");
                reqList.getChildren().add(reqItem);
            }
        }

        reqSection.getChildren().addAll(reqTitle, reqList);
        container.getChildren().add(reqSection);

        // Benefits Section
        VBox benSection = new VBox(12);
        benSection.getStyleClass().add("detail-section-card");

        Label benTitle = new Label("Benefits");
        benTitle.getStyleClass().add("detail-section-title");

        Label benText = new Label(job.benefits);
        benText.setWrapText(true);
        benText.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");

        benSection.getChildren().addAll(benTitle, benText);
        container.getChildren().add(benSection);

        // Apply Button
        HBox actionButtons = new HBox(15);
        actionButtons.getStyleClass().add("action-buttons-container");
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        Button btnApply = new Button("📝 Apply for this Position");
        btnApply.getStyleClass().addAll("btn-primary", "action-button");
        btnApply.setStyle("-fx-font-size: 16px; -fx-padding: 14 28;");
        btnApply.setOnAction(e -> handleApply(job));

        Button btnSave = new Button("💾 Save for Later");
        btnSave.getStyleClass().addAll("btn-secondary", "action-button");
        btnSave.setStyle("-fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnShare = new Button("🔗 Share");
        btnShare.getStyleClass().addAll("btn-secondary", "action-button");
        btnShare.setStyle("-fx-font-size: 14px;");

        actionButtons.getChildren().addAll(btnApply, btnSave, spacer, btnShare);
        container.getChildren().add(actionButtons);
    }

    @FXML
    private void handleSearch() {
        // Implement search functionality
        System.out.println("Search: " + txtSearch.getText());
    }

    @FXML
    private void handleClear() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
    }

    private void handleApply(JobOffer job) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Apply for Position");
        confirm.setHeaderText("Apply for " + job.title + "?");
        confirm.setContentText("Your application will be submitted to the recruiter.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Success", "Application submitted successfully!", Alert.AlertType.INFORMATION);
            }
        });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private List<JobOffer> getSampleJobs() {
        List<JobOffer> jobs = new ArrayList<>();

        jobs.add(new JobOffer(
            "Senior Frontend Developer",
            "Technology",
            "Full-time",
            "New York, NY",
            "$120k - $150k",
            "ACTIVE",
            12,
            "1/15/2026",
            "We are seeking an experienced Frontend Developer to join our dynamic team...",
            "5+ years of React experience\nStrong TypeScript skills\nExperience with modern build tools",
            "Health insurance, 401k, Remote work options, Professional development"
        ));

        jobs.add(new JobOffer(
            "Product Designer",
            "Design",
            "Full-time",
            "San Francisco, CA",
            "$100k - $130k",
            "ACTIVE",
            8,
            "1/18/2026",
            "Join our design team to create beautiful and intuitive user experiences...",
            "3+ years of product design experience\nProficiency in Figma\nStrong portfolio",
            "Competitive salary, Stock options, Flexible hours"
        ));

        jobs.add(new JobOffer(
            "DevOps Engineer",
            "Technology",
            "Full-time",
            "Remote",
            "$110k - $140k",
            "CLOSED",
            24,
            "1/10/2026",
            "Help us build and maintain our cloud infrastructure...",
            "Experience with AWS/Azure\nKubernetes knowledge\nCI/CD expertise",
            "Remote work, Unlimited PTO, Learning budget"
        ));

        return jobs;
    }

    // Inner class for job data
    private static class JobOffer {
        String title, department, type, location, salaryRange, status, postedDate;
        String description, requirements, benefits;
        int applicants;

        JobOffer(String title, String department, String type, String location,
                 String salaryRange, String status, int applicants, String postedDate,
                 String description, String requirements, String benefits) {
            this.title = title;
            this.department = department;
            this.type = type;
            this.location = location;
            this.salaryRange = salaryRange;
            this.status = status;
            this.applicants = applicants;
            this.postedDate = postedDate;
            this.description = description;
            this.requirements = requirements;
            this.benefits = benefits;
        }
    }
}

