package Controllers.joboffers;

import Models.joboffers.JobOffer;
import Models.joboffers.JobOfferWarning;
import Models.joboffers.WarningCorrection;
import Models.joboffers.OfferSkill;
import Models.joboffers.ContractType;
import Models.joboffers.Status;
import Models.joboffers.SkillLevel;
import Services.joboffers.JobOfferService;
import Services.joboffers.JobOfferWarningService;
import Services.joboffers.WarningCorrectionService;
import Services.joboffers.OfferSkillService;
import Services.joboffers.FuzzySearchService;
import Services.joboffers.NotificationService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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

    private ComboBox<String> cbFilterType;
    private ComboBox<String> cbFilterLocation;
    private ContractType selectedContractType = null;
    private String selectedLocation = null;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;
    private JobOfferWarningService warningService;
    private WarningCorrectionService correctionService;

    // Form fields
    private TextField formTitleField;
    private TextArea formDescription;
    private TextField formLocation;
    private ComboBox<ContractType> formContractType;
    private DatePicker formDeadline;
    private ComboBox<Status> formStatus;

    // Inline error labels
    private Label titleErrorLabel;
    private Label descriptionErrorLabel;
    private Label locationErrorLabel;
    private Label deadlineErrorLabel;
    private Label skillsErrorLabel;

    // Skills
    private VBox skillsContainer;
    private List<SkillRow> skillRows;
    private boolean isEditMode = false;
    private JobOffer editingJob = null;

    // =========================================================================
    // Init
    // =========================================================================

    @FXML
    public void initialize() {
        jobOfferService  = new JobOfferService();
        offerSkillService = new OfferSkillService();
        warningService   = new JobOfferWarningService();
        correctionService = new WarningCorrectionService();
        skillRows = new ArrayList<>();
        buildUI();
        loadJobOffers();
        checkForWarnings();
    }

    // =========================================================================
    // Warning check on startup
    // =========================================================================

    private void checkForWarnings() {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            int count = warningService.countPendingWarningsForRecruiter(recruiterId);
            if (count > 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Attention");
                alert.setHeaderText("Vous avez " + count + " offre(s) signalee(s)");
                alert.setContentText("Un administrateur a signale un probleme avec certaines de vos offres.\n" +
                        "Veuillez consulter les offres marquees en jaune et les corriger ou supprimer.");
                alert.show();
            }
        } catch (SQLException e) {
            System.err.println("Erreur verification avertissements: " + e.getMessage());
        }
    }

    // =========================================================================
    // Build UI
    // =========================================================================

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 20;");

        // Header
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label recruiterBadge = new Label("ESPACE RECRUTEUR");
        recruiterBadge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 8 16; " +
                "-fx-background-radius: 8; -fx-font-weight: 700; -fx-font-size: 14px;");

        Label pageTitle = new Label("Gestion de mes offres");
        pageTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        headerBox.getChildren().addAll(recruiterBadge, pageTitle);
        mainContainer.getChildren().add(headerBox);

        Region gap1 = new Region(); gap1.setPrefHeight(15);
        mainContainer.getChildren().add(gap1);
        mainContainer.getChildren().add(createSearchFilterBox());

        Region gap2 = new Region(); gap2.setPrefHeight(15);
        mainContainer.getChildren().add(gap2);

        HBox contentArea = new HBox(20);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        VBox leftSide = createJobListPanel();
        leftSide.setPrefWidth(400);
        leftSide.setMinWidth(350);
        leftSide.setMaxWidth(450);

        VBox rightSide = createDetailPanel();
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        contentArea.getChildren().addAll(leftSide, rightSide);
        mainContainer.getChildren().add(contentArea);
    }

    // =========================================================================
    // Search & filter box
    // =========================================================================

    private VBox createSearchFilterBox() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");

        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setStyle("-fx-padding: 18 20 12 20;");

        txtSearch = new TextField();
        txtSearch.setPromptText("Rechercher dans mes offres...");
        txtSearch.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 10 15; " +
                "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-font-size: 13px; -fx-pref-height: 38;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setOnAction(e -> handleSearch());

        Button btnSearchAction = new Button("Rechercher");
        btnSearchAction.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-size: 13px; " +
                "-fx-font-weight: 600; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSearchAction.setOnAction(e -> handleSearch());

        searchRow.getChildren().addAll(txtSearch, btnSearchAction);

        Separator separator = new Separator();

        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setStyle("-fx-padding: 12 20 18 20;");

        Label filterLabel = new Label("Filtres:");
        filterLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        cbFilterType = new ComboBox<>();
        cbFilterType.setPromptText("Type");
        cbFilterType.getItems().add("Tous");
        for (ContractType t : ContractType.values()) cbFilterType.getItems().add(formatContractType(t));
        cbFilterType.setStyle("-fx-pref-width: 130; -fx-pref-height: 34;");
        cbFilterType.setOnAction(e -> applyFilters());

        cbFilterLocation = new ComboBox<>();
        cbFilterLocation.setPromptText("Lieu");
        cbFilterLocation.getItems().add("Tous");
        try {
            cbFilterLocation.getItems().addAll(jobOfferService.getAllLocations());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cbFilterLocation.setStyle("-fx-pref-width: 130; -fx-pref-height: 34;");
        cbFilterLocation.setOnAction(e -> applyFilters());

        Button btnReset = new Button("Reinitialiser");
        btnReset.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #6c757d; -fx-font-size: 12px; " +
                "-fx-padding: 8 14; -fx-background-radius: 6; -fx-cursor: hand; " +
                "-fx-border-color: #dee2e6; -fx-border-radius: 6;");
        btnReset.setOnAction(e -> resetFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label resultCount = new Label("");
        resultCount.setId("resultCount");
        resultCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        filterRow.getChildren().addAll(filterLabel, cbFilterType, cbFilterLocation, btnReset, spacer, resultCount);
        container.getChildren().addAll(searchRow, separator, filterRow);
        return container;
    }

    private void applyFilters() {
        String tv = cbFilterType.getValue();
        String lv = cbFilterLocation.getValue();
        selectedContractType = (tv == null || tv.equals("Tous")) ? null : getContractTypeFromLabel(tv);
        selectedLocation     = (lv == null || lv.equals("Tous")) ? null : lv;
        loadFilteredJobOffers();
    }

    private void resetFilters() {
        selectedContractType = null;
        selectedLocation     = null;
        if (cbFilterType     != null) cbFilterType.setValue(null);
        if (cbFilterLocation != null) cbFilterLocation.setValue(null);
        if (txtSearch        != null) txtSearch.clear();
        loadJobOffers();
    }

    private void loadFilteredJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();
        try {
            List<JobOffer> jobs = jobOfferService.filterJobOffers(selectedLocation, selectedContractType, null);
            String kw = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";
            if (!kw.isEmpty()) {
                jobs = jobs.stream()
                        .filter(j -> (j.getTitle()       != null && j.getTitle().toLowerCase().contains(kw)) ||
                                     (j.getDescription() != null && j.getDescription().toLowerCase().contains(kw)) ||
                                     (j.getLocation()    != null && j.getLocation().toLowerCase().contains(kw)))
                        .toList();
            }
            updateResultCount(jobs.size());
            if (jobs.isEmpty()) { jobListContainer.getChildren().add(createEmptyState()); return; }
            boolean first = true;
            for (JobOffer job : jobs) {
                VBox card = createJobCard(job);
                jobListContainer.getChildren().add(card);
                if (first) { selectJob(job, card); first = false; }
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateResultCount(int count) {
        if (mainContainer == null) return;
        Label lbl = (Label) mainContainer.lookup("#resultCount");
        if (lbl != null) lbl.setText(count == 0 ? "Aucun resultat" : count + " offre(s)");
    }

    private VBox createEmptyState() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        box.setStyle("-fx-padding: 30;");
        Label icon = new Label("📋");
        icon.setStyle("-fx-font-size: 36px;");
        Label text = new Label("Aucune offre trouvee");
        text.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
        box.getChildren().addAll(icon, text);
        return box;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private String formatContractType(ContractType type) {
        return switch (type) {
            case CDI       -> "CDI";
            case CDD       -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE  -> "Freelance";
            case PART_TIME  -> "Temps Partiel";
            case FULL_TIME  -> "Temps Plein";
        };
    }

    private ContractType getContractTypeFromLabel(String label) {
        return switch (label) {
            case "CDI"          -> ContractType.CDI;
            case "CDD"          -> ContractType.CDD;
            case "Stage"        -> ContractType.INTERNSHIP;
            case "Freelance"    -> ContractType.FREELANCE;
            case "Temps Partiel" -> ContractType.PART_TIME;
            case "Temps Plein"  -> ContractType.FULL_TIME;
            default -> null;
        };
    }

    private String formatSkillLevel(SkillLevel level) {
        return switch (level) {
            case BEGINNER     -> "Debutant";
            case INTERMEDIATE -> "Intermediaire";
            case ADVANCED     -> "Avance";
        };
    }

    // =========================================================================
    // List panel
    // =========================================================================

    private VBox createJobListPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 18; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        Label title = new Label("Mes offres d'emploi");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        jobListContainer = new VBox(8);
        jobListContainer.setStyle("-fx-padding: 5 5 5 0;");
        scroll.setContent(jobListContainer);

        panel.getChildren().addAll(title, scroll);
        return panel;
    }

    // =========================================================================
    // Detail panel
    // =========================================================================

    private VBox createDetailPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Details");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnCreate = new Button("+ Nouvelle offre");
        btnCreate.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 18; -fx-background-radius: 8; -fx-cursor: hand;");
        btnCreate.setOnAction(e -> showCreateForm());

        topBar.getChildren().addAll(title, btnCreate);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

        // Placeholder
        Label placeholder = new Label("Selectionnez une offre pour voir les details");
        placeholder.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-padding: 40;");
        detailContainer.getChildren().add(placeholder);

        panel.getChildren().addAll(topBar, scrollPane);
        return panel;
    }

    // =========================================================================
    // Load all offers
    // =========================================================================

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();
        try {
            List<JobOffer> jobs = jobOfferService.getAllJobOffers();
            if (jobs.isEmpty()) { jobListContainer.getChildren().add(createEmptyState()); return; }
            boolean first = true;
            for (JobOffer job : jobs) {
                VBox card = createJobCard(job);
                jobListContainer.getChildren().add(card);
                if (first) { selectJob(job, card); first = false; }
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les offres : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =========================================================================
    // Job card
    // =========================================================================

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(8);
        boolean flagged = job.isFlagged() || job.getStatus() == Status.FLAGGED;
        String bgColor     = flagged ? "#fff3cd" : "#f8f9fa";
        String borderColor = flagged ? "#ffc107" : "#dee2e6";
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-padding: 14; " +
                "-fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: " +
                (flagged ? "2" : "1") + "; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(job.getTitle());
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label badge = new Label(formatContractType(job.getContractType()));
        badge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 3 8; " +
                "-fx-background-radius: 4; -fx-font-size: 11px;");
        header.getChildren().addAll(titleLbl, badge);

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non specifie"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        // Status row
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        String statusColor, statusText;
        if (flagged) {
            statusColor = "#ffc107"; statusText = "⚠ Signale";
        } else if (job.getStatus() == Status.OPEN) {
            statusColor = "#28a745"; statusText = "Ouvert";
        } else {
            statusColor = "#dc3545"; statusText = "Ferme";
        }
        Label statusLbl = new Label(statusText);
        statusLbl.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: " +
                (flagged ? "#212529" : "white") + "; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-size: 10px;");
        statusRow.getChildren().add(statusLbl);
        if (flagged) {
            Label actionReq = new Label("Action requise");
            actionReq.setStyle("-fx-text-fill: #856404; -fx-font-size: 10px; -fx-font-weight: bold;");
            statusRow.getChildren().add(actionReq);
        }

        card.getChildren().addAll(header, location, statusRow);
        card.setOnMouseClicked(e -> selectJob(job, card));
        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        // Reset all cards
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 8; -fx-padding: 14; " +
                        "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 14; " +
                "-fx-border-color: #5BA3F5; -fx-border-width: 2; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(91,163,245,0.25), 10, 0, 0, 2); -fx-cursor: hand;");
        selectedJob = job;
        displayJobDetails(job);
    }

    // =========================================================================
    // Display job details (read-only view)
    // =========================================================================

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        // Show warning section first if flagged
        if (job.isFlagged() || job.getStatus() == Status.FLAGGED) {
            displayWarningsForRecruiter(job);
        }

        // ---- Header card ----
        VBox headerCard = new VBox(12);
        headerCard.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 22;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        FlowPane metaFlow = new FlowPane(12, 8);
        metaFlow.setAlignment(Pos.CENTER_LEFT);

        Label contractTypeLbl = new Label("💼 " + formatContractType(job.getContractType()));
        contractTypeLbl.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; -fx-padding: 5 12; " +
                "-fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: 600;");

        Label locationLbl = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non specifie"));
        locationLbl.setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #7b1fa2; -fx-padding: 5 12; " +
                "-fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: 600;");

        boolean flagged = job.isFlagged() || job.getStatus() == Status.FLAGGED;
        String statusColor = flagged ? "#ffc107" : job.getStatus() == Status.OPEN ? "#28a745" : "#dc3545";
        String statusText  = flagged ? "⚠ Signale" : job.getStatus() == Status.OPEN ? "Ouvert" : "Ferme";
        Label statusLbl = new Label("● " + statusText);
        statusLbl.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 13px; -fx-font-weight: 700;");

        metaFlow.getChildren().addAll(contractTypeLbl, locationLbl, statusLbl);

        if (job.getDeadline() != null) {
            Label deadline = new Label("⏰ Date limite: " + job.getDeadline()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            deadline.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 5 12; " +
                    "-fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: 600;");
            metaFlow.getChildren().add(deadline);
        }

        headerCard.getChildren().addAll(title, metaFlow);
        detailContainer.getChildren().add(headerCard);

        // ---- Description ----
        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            VBox descSection = new VBox(10);
            descSection.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 20;");
            Label descTitle = new Label("Description du poste");
            descTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
            Label descText = new Label(job.getDescription());
            descText.setWrapText(true);
            descText.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px; -fx-line-spacing: 3;");
            descSection.getChildren().addAll(descTitle, descText);
            detailContainer.getChildren().add(descSection);
        }

        // ---- Skills ----
        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(job.getId());
            if (!skills.isEmpty()) {
                VBox skillsSection = new VBox(10);
                skillsSection.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 20;");
                Label skillsTitle = new Label("Competences requises");
                skillsTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
                FlowPane skillsFlow = new FlowPane(8, 8);
                for (OfferSkill skill : skills) {
                    VBox skillBox = new VBox(3);
                    skillBox.setStyle("-fx-background-color: white; -fx-padding: 8 14; -fx-background-radius: 8; " +
                            "-fx-border-color: #dee2e6; -fx-border-radius: 8;");
                    Label sName = new Label(skill.getSkillName());
                    sName.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
                    Label sLevel = new Label(formatSkillLevel(skill.getLevelRequired()));
                    sLevel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                    skillBox.getChildren().addAll(sName, sLevel);
                    skillsFlow.getChildren().add(skillBox);
                }
                skillsSection.getChildren().addAll(skillsTitle, skillsFlow);
                detailContainer.getChildren().add(skillsSection);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement competences : " + e.getMessage());
        }

        // ---- Location info (no map) ----
        if (job.getLocation() != null && !job.getLocation().trim().isEmpty()) {
            VBox locationSection = new VBox(8);
            locationSection.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 10; -fx-padding: 14;");
            Label locationLabel = new Label("📍 " + job.getLocation());
            locationLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2e7d32;");
            locationSection.getChildren().add(locationLabel);
            detailContainer.getChildren().add(locationSection);
        }

        // ---- Published date ----
        if (job.getCreatedAt() != null) {
            Label posted = new Label("Publie le: " + job.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            posted.setStyle("-fx-text-fill: #8e9ba8; -fx-font-size: 12px; -fx-padding: 5 0;");
            detailContainer.getChildren().add(posted);
        }

        // ---- Action buttons ----
        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setStyle("-fx-padding: 20 0 10 0;");

        Button btnEdit = new Button("✏ Modifier");
        btnEdit.setStyle("-fx-background-color: #ffc107; -fx-text-fill: #212529; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> showEditForm(job));

        Button btnDelete = new Button("🗑 Supprimer");
        btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> handleDeleteJobOffer(job));

        String toggleText = (job.getStatus() == Status.OPEN) ? "Fermer l'offre" : "Ouvrir l'offre";
        Button btnToggle = new Button(toggleText);
        btnToggle.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnToggle.setOnAction(e -> handleToggleStatus(job));

        actionButtons.getChildren().addAll(btnEdit, btnDelete, btnToggle);
        detailContainer.getChildren().add(actionButtons);
    }

    // =========================================================================
    // Warnings section
    // =========================================================================

    private void displayWarningsForRecruiter(JobOffer job) {
        try {
            List<JobOfferWarning> warnings = warningService.getPendingWarningsByJobOfferId(job.getId());
            if (warnings.isEmpty()) return;

            for (JobOfferWarning w : warnings) {
                if (w.getStatus() == JobOfferWarning.WarningStatus.SENT) warningService.markAsSeen(w.getId());
            }

            VBox section = new VBox(14);
            section.setStyle("-fx-background-color: #f8d7da; -fx-background-radius: 10; -fx-padding: 20; " +
                    "-fx-border-color: #f5c6cb; -fx-border-radius: 10; -fx-border-width: 2;");

            Label alertTitle = new Label("⚠  Action requise - Offre signalee");
            alertTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #721c24;");
            Label alertSub = new Label("Un administrateur a signale un probleme. Corrigez ou supprimez l'offre.");
            alertSub.setStyle("-fx-font-size: 12px; -fx-text-fill: #721c24;");
            alertSub.setWrapText(true);
            section.getChildren().addAll(alertTitle, alertSub);

            for (JobOfferWarning w : warnings) {
                VBox card = new VBox(8);
                card.setStyle("-fx-background-color: white; -fx-padding: 14; -fx-background-radius: 8;");

                Label reason = new Label("Raison : " + w.getReason());
                reason.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");
                reason.setWrapText(true);

                Label msgLbl = new Label("Message de l'admin :");
                msgLbl.setStyle("-fx-font-weight: 600; -fx-text-fill: #495057; -fx-font-size: 12px;");

                TextArea msgArea = new TextArea(w.getMessage());
                msgArea.setWrapText(true);
                msgArea.setEditable(false);
                msgArea.setPrefRowCount(3);
                msgArea.setStyle("-fx-control-inner-background: #f8f9fa; -fx-font-size: 13px;");

                Label dateLbl = new Label("Signale le " + w.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                dateLbl.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

                card.getChildren().addAll(reason, msgLbl, msgArea, dateLbl);
                section.getChildren().add(card);
            }

            HBox btns = new HBox(12);
            btns.setAlignment(Pos.CENTER);
            btns.setStyle("-fx-padding: 8 0 0 0;");

            Button bEdit = new Button("✏ Modifier l'offre");
            bEdit.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: 600; " +
                    "-fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
            bEdit.setOnAction(e -> showEditForm(job));

            Button bDel = new Button("🗑 Supprimer l'offre");
            bDel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                    "-fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
            bDel.setOnAction(e -> handleDeleteJobOffer(job));

            Button bResolve = new Button("✔ J'ai corrige le probleme");
            bResolve.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                    "-fx-padding: 9 18; -fx-background-radius: 6; -fx-cursor: hand;");
            bResolve.setOnAction(e -> handleMarkWarningsResolved(job, warnings));

            btns.getChildren().addAll(bEdit, bDel, bResolve);
            section.getChildren().add(btns);
            detailContainer.getChildren().add(section);

        } catch (SQLException e) {
            System.err.println("Erreur chargement avertissements: " + e.getMessage());
        }
    }

    private void handleMarkWarningsResolved(JobOffer job, List<JobOfferWarning> warnings) {
        Dialog<WarningCorrection> dialog = new Dialog<>();
        dialog.setTitle("Soumettre une correction");
        dialog.setHeaderText("Soumettre votre correction pour validation");

        ButtonType submitBT = new ButtonType("Soumettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitBT, ButtonType.CANCEL);

        VBox content = new VBox(14);
        content.setPadding(new Insets(20));
        content.setPrefWidth(560);

        Label offerLbl = new Label("Offre : " + job.getTitle());
        offerLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");

        Label infoLbl = new Label("Votre correction sera envoyee a l'administrateur pour validation.");
        infoLbl.setWrapText(true);
        infoLbl.setStyle("-fx-text-fill: #856404; -fx-background-color: #fff3cd; " +
                "-fx-padding: 10; -fx-background-radius: 5;");

        HBox noteLabelRow = new HBox(10);
        noteLabelRow.setAlignment(Pos.CENTER_LEFT);
        Label noteLbl = new Label("Description des corrections :");
        noteLbl.setStyle("-fx-font-weight: 600;");

        Button btnGen = new Button("Generer automatiquement");
        btnGen.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-size: 11px; " +
                "-fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
        noteLabelRow.getChildren().addAll(noteLbl, btnGen);

        TextArea corrNote = new TextArea();
        corrNote.setPromptText("Decrivez les corrections apportees...");
        corrNote.setPrefRowCount(5);
        corrNote.setWrapText(true);

        Label loadingLbl = new Label("");
        loadingLbl.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");

        String wReason  = !warnings.isEmpty() ? warnings.get(0).getReason()  : "Non specifie";
        String wMessage = !warnings.isEmpty() ? warnings.get(0).getMessage() : "";

        btnGen.setOnAction(e -> {
            loadingLbl.setText("Generation en cours...");
            btnGen.setDisable(true);
            new Thread(() -> {
                String generated = generateCorrectionNote(wReason, wMessage, job.getTitle(), job.getDescription());
                javafx.application.Platform.runLater(() -> {
                    corrNote.setText(generated != null ? generated : "");
                    loadingLbl.setText(generated != null ? "Description generee." : "Echec - ecrivez manuellement.");
                    btnGen.setDisable(false);
                });
            }).start();
        });

        content.getChildren().addAll(offerLbl, infoLbl, noteLabelRow, corrNote, loadingLbl);
        dialog.getDialogPane().setContent(content);

        Button submitBtn = (Button) dialog.getDialogPane().lookupButton(submitBT);
        submitBtn.setDisable(true);
        corrNote.textProperty().addListener((obs, o, n) -> submitBtn.setDisable(n.trim().length() < 10));

        dialog.setResultConverter(btn -> {
            if (btn != submitBT) return null;
            WarningCorrection c = new WarningCorrection();
            c.setJobOfferId(job.getId());
            c.setRecruiterId(UserContext.getRecruiterId());
            c.setCorrectionNote(corrNote.getText().trim());
            c.setNewTitle(job.getTitle());
            c.setNewDescription(job.getDescription());
            if (!warnings.isEmpty()) c.setWarningId(warnings.get(0).getId());
            return c;
        });

        dialog.showAndWait().ifPresent(correction -> {
            try {
                for (JobOfferWarning w : warnings) {
                    WarningCorrection c = new WarningCorrection();
                    c.setWarningId(w.getId());
                    c.setJobOfferId(job.getId());
                    c.setRecruiterId(UserContext.getRecruiterId());
                    c.setCorrectionNote(correction.getCorrectionNote());
                    c.setNewTitle(job.getTitle());
                    c.setNewDescription(job.getDescription());
                    correctionService.submitCorrection(c);
                }
                showAlert("Succes", "Correction soumise. Vous serez notifie une fois validee.",
                        Alert.AlertType.INFORMATION);
                loadJobOffers();
            } catch (SQLException ex) {
                showAlert("Erreur", "Erreur soumission: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    // =========================================================================
    // Create / Edit form  (NO map, NO autocomplete, plain text location)
    // =========================================================================

    private void showCreateForm() {
        isEditMode = false;
        editingJob = null;
        showJobForm("Creer une offre");
    }

    private void showEditForm(JobOffer job) {
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refusee", "Vous ne pouvez modifier que vos propres offres.", Alert.AlertType.WARNING);
            return;
        }
        isEditMode = true;
        editingJob = job;
        showJobForm("Modifier l'offre");
    }

    private void showJobForm(String formTitle) {
        detailContainer.getChildren().clear();
        skillRows = new ArrayList<>();

        Button btnBack = new Button("← Retour");
        btnBack.setStyle("-fx-background-color: transparent; -fx-text-fill: #5BA3F5; " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-padding: 0 0 8 0;");
        btnBack.setOnAction(e -> { if (selectedJob != null) displayJobDetails(selectedJob); });

        Label formTitleLbl = new Label(formTitle);
        formTitleLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 0 10 0;");

        VBox form = new VBox(14);

        // --- Title ---
        Label titleLbl = new Label("Titre du poste *");
        titleLbl.setStyle("-fx-font-weight: bold;");
        formTitleField = new TextField();
        formTitleField.setPromptText("Ex: Developpeur Java Senior...");
        formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        titleErrorLabel = new Label();
        titleErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        titleErrorLabel.setVisible(false); titleErrorLabel.setManaged(false);

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(formTitleField, Priority.ALWAYS);
        Button btnAI = new Button("AI Suggest");
        btnAI.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: 600; " +
                "-fx-padding: 10 14; -fx-background-radius: 6; -fx-cursor: hand;");
        Label aiStatus = new Label("");
        aiStatus.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");
        titleRow.getChildren().addAll(formTitleField, btnAI);

        // --- Description ---
        Label descLbl = new Label("Description du poste *");
        descLbl.setStyle("-fx-font-weight: bold;");
        formDescription = new TextArea();
        formDescription.setPromptText("Description detaillee du poste...");
        formDescription.setPrefRowCount(6);
        formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        descriptionErrorLabel = new Label();
        descriptionErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        descriptionErrorLabel.setVisible(false); descriptionErrorLabel.setManaged(false);

        // --- Location (plain text, no map) ---
        Label locLbl = new Label("Localisation *");
        locLbl.setStyle("-fx-font-weight: bold;");
        formLocation = new TextField();
        formLocation.setPromptText("Ex: Tunis, Paris, Remote...");
        formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        locationErrorLabel = new Label();
        locationErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        locationErrorLabel.setVisible(false); locationErrorLabel.setManaged(false);

        // --- Contract Type ---
        Label contractLbl = new Label("Type de contrat *");
        contractLbl.setStyle("-fx-font-weight: bold;");
        formContractType = new ComboBox<>();
        formContractType.getItems().addAll(ContractType.values());
        formContractType.setPromptText("Selectionner...");
        formContractType.setMaxWidth(Double.MAX_VALUE);
        formContractType.setStyle("-fx-font-size: 14px;");

        // --- Status ---
        Label statusLbl = new Label("Statut *");
        statusLbl.setStyle("-fx-font-weight: bold;");
        formStatus = new ComboBox<>();
        formStatus.getItems().addAll(Status.values());
        formStatus.setValue(Status.OPEN);
        formStatus.setMaxWidth(Double.MAX_VALUE);
        formStatus.setStyle("-fx-font-size: 14px;");

        // --- Deadline ---
        Label deadlineLbl = new Label("Date limite (Optionnel)");
        deadlineLbl.setStyle("-fx-font-weight: bold;");
        formDeadline = new DatePicker();
        formDeadline.setPromptText("Selectionner une date...");
        formDeadline.setMaxWidth(Double.MAX_VALUE);
        formDeadline.setStyle("-fx-font-size: 14px;");
        deadlineErrorLabel = new Label();
        deadlineErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        deadlineErrorLabel.setVisible(false); deadlineErrorLabel.setManaged(false);

        // --- Skills ---
        VBox skillsSection = new VBox(10);
        skillsSection.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 8;");
        Label skillsLbl = new Label("Competences requises *");
        skillsLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        skillsErrorLabel = new Label();
        skillsErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        skillsErrorLabel.setVisible(false); skillsErrorLabel.setManaged(false);
        skillsContainer = new VBox(8);
        Button btnAddSkill = new Button("+ Ajouter une competence");
        btnAddSkill.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; " +
                "-fx-padding: 8 15; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAddSkill.setOnAction(e -> addSkillRow(null));
        skillsSection.getChildren().addAll(skillsLbl, skillsErrorLabel, skillsContainer, btnAddSkill);

        // Populate if editing
        if (isEditMode && editingJob != null) {
            formTitleField.setText(editingJob.getTitle() != null ? editingJob.getTitle() : "");
            formDescription.setText(editingJob.getDescription() != null ? editingJob.getDescription() : "");
            formLocation.setText(editingJob.getLocation() != null ? editingJob.getLocation() : "");
            formContractType.setValue(editingJob.getContractType());
            formStatus.setValue(editingJob.getStatus() != null ? editingJob.getStatus() : Status.OPEN);
            if (editingJob.getDeadline() != null) formDeadline.setValue(editingJob.getDeadline().toLocalDate());
            try {
                for (OfferSkill sk : offerSkillService.getSkillsByOfferId(editingJob.getId())) addSkillRow(sk);
            } catch (SQLException ex) {
                showAlert("Erreur", "Impossible de charger les competences : " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            addSkillRow(null);
        }

        // AI Suggest handler
        btnAI.setOnAction(e -> {
            String t = formTitleField.getText().trim();
            if (t.length() < 3) { showAlert("Attention", "Entrez un titre valide (min 3 car.)", Alert.AlertType.WARNING); return; }
            aiStatus.setText("Generation en cours...");
            btnAI.setDisable(true);
            new Thread(() -> {
                String sugg = generateJobSuggestions(t);
                javafx.application.Platform.runLater(() -> {
                    if (sugg != null) { parseAndFillForm(sugg); aiStatus.setText("Formulaire rempli!"); }
                    else aiStatus.setText("Echec de la generation.");
                    btnAI.setDisable(false);
                });
            }).start();
        });

        addValidationListeners();

        Button btnSubmit = new Button(isEditMode ? "Mettre a jour l'offre" : "Creer l'offre");
        btnSubmit.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: 700; " +
                "-fx-font-size: 15px; -fx-padding: 12 30; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSubmit.setOnAction(e -> { if (isEditMode) handleUpdateJobOffer(); else handleCreateJobOffer(); });

        form.getChildren().addAll(
                titleLbl, titleRow, aiStatus, titleErrorLabel,
                descLbl, formDescription, descriptionErrorLabel,
                locLbl, formLocation, locationErrorLabel,
                contractLbl, formContractType,
                statusLbl, formStatus,
                deadlineLbl, formDeadline, deadlineErrorLabel,
                skillsSection, btnSubmit
        );

        ScrollPane scroll = new ScrollPane(form);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        detailContainer.getChildren().addAll(btnBack, formTitleLbl, scroll);
    }

    // =========================================================================
    // Skill rows
    // =========================================================================

    private void addSkillRow(OfferSkill existing) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField();
        nameField.setPromptText("Nom de la competence (ex: Java, Python)");
        nameField.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        ComboBox<SkillLevel> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll(SkillLevel.values());
        levelCombo.setValue(SkillLevel.INTERMEDIATE);
        levelCombo.setStyle("-fx-font-size: 13px;");
        levelCombo.setPrefWidth(150);

        if (existing != null) {
            nameField.setText(existing.getSkillName());
            levelCombo.setValue(existing.getLevelRequired());
        }

        Button btnRemove = new Button("✕");
        btnRemove.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; " +
                "-fx-padding: 6 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRemove.setOnAction(e -> {
            skillsContainer.getChildren().remove(row);
            skillRows.removeIf(r -> r.nameField == nameField);
        });

        row.getChildren().addAll(nameField, levelCombo, btnRemove);
        skillsContainer.getChildren().add(row);
        skillRows.add(new SkillRow(nameField, levelCombo));
    }

    // =========================================================================
    // CRUD handlers
    // =========================================================================

    private void handleCreateJobOffer() {
        if (!validateForm()) return;
        try {
            JobOffer newJob = new JobOffer();
            newJob.setRecruiterId(UserContext.getRecruiterId());
            newJob.setTitle(formTitleField.getText().trim());
            newJob.setDescription(formDescription.getText().trim());
            newJob.setLocation(formLocation.getText().trim());
            newJob.setContractType(formContractType.getValue());
            newJob.setStatus(formStatus.getValue());
            newJob.setCreatedAt(LocalDateTime.now());
            if (formDeadline.getValue() != null) newJob.setDeadline(formDeadline.getValue().atTime(23, 59));

            JobOffer saved = jobOfferService.createJobOffer(newJob);
            List<OfferSkill> skills = getSkillsFromForm(saved.getId());
            if (!skills.isEmpty()) offerSkillService.createOfferSkills(skills);

            showAlert("Succes", "Offre creee avec succes!", Alert.AlertType.INFORMATION);
            loadJobOffers();
            selectedJob = saved;
            displayJobDetails(saved);
        } catch (SQLException e) {
            showAlert("Erreur", "Echec creation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleUpdateJobOffer() {
        if (!validateForm() || editingJob == null) return;
        try {
            editingJob.setTitle(formTitleField.getText().trim());
            editingJob.setDescription(formDescription.getText().trim());
            editingJob.setLocation(formLocation.getText().trim());
            editingJob.setContractType(formContractType.getValue());
            editingJob.setStatus(formStatus.getValue());
            editingJob.setDeadline(formDeadline.getValue() != null
                    ? formDeadline.getValue().atTime(23, 59) : null);

            if (jobOfferService.updateJobOffer(editingJob)) {
                offerSkillService.replaceOfferSkills(editingJob.getId(), getSkillsFromForm(editingJob.getId()));
                showAlert("Succes", "Offre mise a jour!", Alert.AlertType.INFORMATION);
                loadJobOffers();
                selectedJob = editingJob;
                displayJobDetails(editingJob);
            } else {
                showAlert("Erreur", "Echec de la mise a jour.", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Echec: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void handleDeleteJobOffer(JobOffer job) {
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refusee", "Vous ne pouvez supprimer que vos propres offres.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText(null);
        confirm.setContentText("Etes-vous sur de vouloir supprimer \"" + job.getTitle() + "\" ? Cette action est irreversible.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    if (jobOfferService.deleteJobOffer(job.getId())) {
                        showAlert("Succes", "Offre supprimee.", Alert.AlertType.INFORMATION);
                        selectedJob = null;
                        detailContainer.getChildren().clear();
                        Label ph = new Label("Selectionnez une offre pour voir les details");
                        ph.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-padding: 40;");
                        detailContainer.getChildren().add(ph);
                        loadJobOffers();
                    } else {
                        showAlert("Erreur", "Impossible de supprimer.", Alert.AlertType.ERROR);
                    }
                } catch (SQLException e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void handleToggleStatus(JobOffer job) {
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refusee", "Vous ne pouvez changer le statut que de vos propres offres.", Alert.AlertType.WARNING);
            return;
        }
        try {
            Status newStatus = (job.getStatus() == Status.OPEN) ? Status.CLOSED : Status.OPEN;
            if (jobOfferService.updateJobOfferStatus(job.getId(), newStatus)) {
                job.setStatus(newStatus);
                showAlert("Succes", "Statut mis a jour : " + (newStatus == Status.OPEN ? "Ouvert" : "Ferme"),
                        Alert.AlertType.INFORMATION);
                loadJobOffers();
                displayJobDetails(job);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de changer le statut: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // =========================================================================
    // Validation
    // =========================================================================

    private boolean validateForm() {
        String title = formTitleField.getText().trim();
        String desc  = formDescription.getText().trim();
        String loc   = formLocation.getText().trim();

        if (title.length() < 3) {
            showAlert("Validation", "Le titre doit contenir au moins 3 caracteres.", Alert.AlertType.WARNING);
            formTitleField.requestFocus(); return false;
        }
        if (title.length() > 100) {
            showAlert("Validation", "Le titre ne doit pas depasser 100 caracteres.", Alert.AlertType.WARNING);
            formTitleField.requestFocus(); return false;
        }
        if (desc.length() < 20) {
            showAlert("Validation", "La description doit contenir au moins 20 caracteres.", Alert.AlertType.WARNING);
            formDescription.requestFocus(); return false;
        }
        if (desc.length() > 2000) {
            showAlert("Validation", "La description ne doit pas depasser 2000 caracteres.", Alert.AlertType.WARNING);
            formDescription.requestFocus(); return false;
        }
        if (loc.length() < 2) {
            showAlert("Validation", "La localisation doit contenir au moins 2 caracteres.", Alert.AlertType.WARNING);
            formLocation.requestFocus(); return false;
        }
        if (formContractType.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner un type de contrat.", Alert.AlertType.WARNING); return false;
        }
        if (formStatus.getValue() == null) {
            showAlert("Validation", "Veuillez selectionner un statut.", Alert.AlertType.WARNING); return false;
        }
        if (formDeadline.getValue() != null && formDeadline.getValue().isBefore(java.time.LocalDate.now())) {
            showAlert("Validation", "La date limite ne peut pas etre dans le passe.", Alert.AlertType.WARNING); return false;
        }
        boolean hasSkill = false;
        for (SkillRow r : skillRows) {
            String sn = r.nameField.getText().trim();
            if (!sn.isEmpty()) {
                if (sn.length() < 2) {
                    showAlert("Validation", "Chaque competence doit avoir au moins 2 caracteres.", Alert.AlertType.WARNING); return false;
                }
                if (r.levelCombo.getValue() == null) {
                    showAlert("Validation", "Selectionnez un niveau pour : " + sn, Alert.AlertType.WARNING); return false;
                }
                hasSkill = true;
            }
        }
        if (!hasSkill) {
            showAlert("Validation", "Ajoutez au moins une competence.", Alert.AlertType.WARNING); return false;
        }
        return true;
    }

    private void addValidationListeners() {
        formTitleField.textProperty().addListener((obs, o, n) ->
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-width: 2; -fx-border-color: " +
                        (n.length() >= 3 ? "#28a745" : n.isEmpty() ? "transparent" : "#ffc107") + ";"));
        formDescription.textProperty().addListener((obs, o, n) ->
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-width: 2; -fx-border-color: " +
                        (n.length() >= 20 ? "#28a745" : n.isEmpty() ? "transparent" : "#ffc107") + ";"));
        formLocation.textProperty().addListener((obs, o, n) ->
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-width: 2; -fx-border-color: " +
                        (n.length() >= 2 ? "#28a745" : n.isEmpty() ? "transparent" : "#ffc107") + ";"));
        formDeadline.valueProperty().addListener((obs, o, n) -> {
            boolean past = n != null && n.isBefore(java.time.LocalDate.now());
            formDeadline.setStyle("-fx-font-size: 14px;" + (past ? "-fx-border-color: #dc3545; -fx-border-width: 2;" : ""));
        });
    }

    private List<OfferSkill> getSkillsFromForm(Long offerId) {
        List<OfferSkill> list = new ArrayList<>();
        for (SkillRow r : skillRows) {
            String sn = r.nameField.getText().trim();
            if (!sn.isEmpty() && r.levelCombo.getValue() != null)
                list.add(new OfferSkill(offerId, sn, r.levelCombo.getValue()));
        }
        return list;
    }

    // =========================================================================
    // Search handler
    // =========================================================================

    @FXML
    private void handleSearch() {
        if (txtSearch == null || txtSearch.getText().trim().isEmpty()) { loadJobOffers(); return; }
        String keyword = txtSearch.getText().trim();
        NotificationService.showInfo("Recherche", "Recherche pour : \"" + keyword + "\"");
        try {
            FuzzySearchService fuzzy = FuzzySearchService.getInstance();
            final double THRESHOLD = 0.6;
            List<JobOffer> all = jobOfferService.getJobOffersByRecruiterId(UserContext.getRecruiterId());
            List<JobOffer> results = all.stream()
                    .filter(j -> {
                        String t = j.getTitle() != null ? j.getTitle() : "";
                        return t.toLowerCase().contains(keyword.toLowerCase()) ||
                               fuzzy.calculateBestScore(t, keyword) >= THRESHOLD;
                    })
                    .sorted((a, b) -> Double.compare(
                            fuzzy.calculateBestScore(b.getTitle() != null ? b.getTitle() : "", keyword),
                            fuzzy.calculateBestScore(a.getTitle() != null ? a.getTitle() : "", keyword)))
                    .toList();

            jobListContainer.getChildren().clear();
            if (results.isEmpty()) {
                Label empty = new Label("Aucun resultat pour \"" + keyword + "\"");
                empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
                jobListContainer.getChildren().add(empty);
            } else {
                for (JobOffer j : results) jobListContainer.getChildren().add(createJobCard(j));
                NotificationService.showSuccess("Recherche", results.size() + " resultat(s)");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Echec recherche: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleClearSearch() {
        if (txtSearch != null) txtSearch.clear();
        loadJobOffers();
    }

    // =========================================================================
    // AI helpers
    // =========================================================================

    private String generateJobSuggestions(String jobTitle) {
        try {
            String prompt = "Tu es expert RH. Genere les infos pour: '" + jobTitle + "'.\n" +
                    "Format EXACT:\nDESCRIPTION: [4-5 phrases]\nSKILLS: [skill1, skill2, skill3, skill4, skill5]";
            String r = callGeminiAPI(prompt);
            return r != null ? r : getDefaultJobSuggestions(jobTitle);
        } catch (Exception e) { return getDefaultJobSuggestions(jobTitle); }
    }

    private String generateCorrectionNote(String reason, String message, String jobTitle) {
        try {
            String prompt = "Recruteur: correction (3-4 phrases FR) pour:\nRaison: " + reason +
                    "\nMessage: " + message + "\nOffre: " + jobTitle;
            String r = callGrokAPI(prompt);
            return r != null ? r : getDefaultCorrectionNote(reason);
        } catch (Exception e) { return getDefaultCorrectionNote(reason); }
    }

    private String callGeminiAPI(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyA40pYJkW9p7QYQerVUv_rmS4pNFo1T46o";
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod("POST"); c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(60000);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapeJson(prompt) + "\"}]}]," +
                "\"generationConfig\":{\"maxOutputTokens\":1000,\"temperature\":0.7}}";
        c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (c.getResponseCode() != 200) throw new Exception("Gemini HTTP " + c.getResponseCode());
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        return extractGeminiContent(sb.toString());
    }

    private String callGrokAPI(String prompt) throws Exception {
        String url = "https://api.x.ai/v1/chat/completions";
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod("POST"); c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Authorization", "Bearer xai-BvO5mSs05cHXwQRM1qa8Z7lojgfAMS0I6Kc9Y1R5lQYSyHWO6eDq62ZZ0QsajWkyyyB6f41ZD4HmWOCU");
        c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(60000);
        String body = "{\"model\":\"grok-beta\",\"messages\":[{\"role\":\"user\",\"content\":\"" +
                escapeJson(prompt) + "\"}],\"max_tokens\":500,\"temperature\":0.7}";
        c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (c.getResponseCode() != 200) throw new Exception("Grok HTTP " + c.getResponseCode());
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        return extractGrokContent(sb.toString());
    }

    private String extractGeminiContent(String json) {
        try {
            int s = json.indexOf("\"text\":"); if (s < 0) return null;
            s = json.indexOf("\"", s + 7) + 1; int e = s;
            for (int i = s; i < json.length(); i++) {
                char ch = json.charAt(i);
                if (ch == '\\' && i + 1 < json.length()) { i++; continue; }
                if (ch == '"') { e = i; break; }
            }
            return e > s ? json.substring(s, e).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim() : null;
        } catch (Exception ex) { return null; }
    }

    private String extractGrokContent(String json) {
        try {
            int s = json.indexOf("\"content\":"); if (s < 0) return null;
            s = json.indexOf("\"", s + 10) + 1;
            int e = json.indexOf("\"", s);
            while (e > 0 && json.charAt(e - 1) == '\\') e = json.indexOf("\"", e + 1);
            return (s > 0 && e > s) ? json.substring(s, e).replace("\\n", "\n").replace("\\\"", "\"").trim() : null;
        } catch (Exception ex) { return null; }
    }

    private String escapeJson(String t) {
        if (t == null) return "";
        return t.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private void parseAndFillForm(String suggestions) {
        try {
            String d = extractField(suggestions, "DESCRIPTION:");
            if (d != null) formDescription.setText(d);
            String sk = extractField(suggestions, "SKILLS:");
            if (sk != null) {
                skillsContainer.getChildren().clear(); skillRows.clear();
                for (String s : sk.split(",")) {
                    String n = s.trim();
                    if (n.length() >= 2) addSkillRow(new OfferSkill(null, n, SkillLevel.INTERMEDIATE));
                }
                if (skillRows.isEmpty()) addSkillRow(null);
            }
        } catch (Exception e) { System.err.println("parseAndFillForm error: " + e.getMessage()); }
    }

    private String extractField(String text, String fieldName) {
        int s = text.indexOf(fieldName); if (s < 0) return null;
        s += fieldName.length();
        int e = text.indexOf("\n", s);
        return text.substring(s, e < 0 ? text.length() : e).trim();
    }

    private String getDefaultJobSuggestions(String jobTitle) {
        return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " motive(e) pour rejoindre notre equipe. " +
               "Vous contribuerez au developpement et aux projets strategiques de l'entreprise.\n" +
               "SKILLS: Communication, Travail en equipe, Organisation, Adaptabilite, Rigueur";
    }

    private String getDefaultCorrectionNote(String reason) {
        return "Nous avons procede aux corrections necessaires suite au signalement (" + reason + "). " +
               "L'offre a ete revue et mise a jour pour repondre aux exigences de la plateforme.";
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(message);
        a.showAndWait();
    }

    // =========================================================================
    // Inner class
    // =========================================================================

    private static class SkillRow {
        TextField nameField;
        ComboBox<SkillLevel> levelCombo;
        SkillRow(TextField n, ComboBox<SkillLevel> l) { this.nameField = n; this.levelCombo = l; }
    }
}



