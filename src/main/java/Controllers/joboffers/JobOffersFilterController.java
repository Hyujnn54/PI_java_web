package Controllers.joboffers;

import Models.joboffers.ContractType;
import Models.joboffers.JobOffer;
import Models.joboffers.OfferSkill;
import Models.joboffers.Status;
import Services.joboffers.JobOfferService;
import Services.joboffers.OfferSkillService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller pour le filtrage des offres d'emploi (vue Candidat)
 */
public class JobOffersFilterController {

    @FXML private VBox mainContainer;
    @FXML private TextField txtLocationFilter;
    @FXML private ComboBox<String> cbContractTypeFilter;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private Button btnApplyFilter;
    @FXML private Button btnClearFilter;
    @FXML private Label lblResultsCount;

    private VBox jobListContainer;
    private VBox detailContainer;
    private JobOffer selectedJob;
    private VBox selectedCard;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        offerSkillService = new OfferSkillService();

        if (mainContainer != null) {
            setupUI();
        }
    }

    private void setupUI() {
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #f5f5f5;");
        mainContainer.setPadding(new Insets(20));
        mainContainer.setSpacing(20);

        // Header
        Label headerLabel = new Label("🔍 Rechercher des Offres d'Emploi");
        headerLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Filter Section
        VBox filterSection = createFilterSection();

        // Results count label
        lblResultsCount = new Label("");
        lblResultsCount.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d; -fx-padding: 10 0 0 0;");

        // Main content (list + details)
        HBox contentBox = new HBox(20);
        contentBox.setFillHeight(true);
        HBox.setHgrow(contentBox, Priority.ALWAYS);

        // Job list container
        jobListContainer = new VBox(10);
        jobListContainer.setPrefWidth(350);
        jobListContainer.setMinWidth(350);
        jobListContainer.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10;");

        ScrollPane listScroll = new ScrollPane(jobListContainer);
        listScroll.setFitToWidth(true);
        listScroll.setPrefWidth(370);
        listScroll.setStyle("-fx-background: white; -fx-background-color: white;");

        // Detail container
        detailContainer = new VBox(15);
        detailContainer.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10;");
        HBox.setHgrow(detailContainer, Priority.ALWAYS);

        Label selectLabel = new Label("Sélectionnez une offre pour voir les détails");
        selectLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #95a5a6;");
        detailContainer.getChildren().add(selectLabel);
        detailContainer.setAlignment(Pos.CENTER);

        ScrollPane detailScroll = new ScrollPane(detailContainer);
        detailScroll.setFitToWidth(true);
        detailScroll.setStyle("-fx-background: white; -fx-background-color: white;");
        HBox.setHgrow(detailScroll, Priority.ALWAYS);

        contentBox.getChildren().addAll(listScroll, detailScroll);
        VBox.setVgrow(contentBox, Priority.ALWAYS);

        mainContainer.getChildren().addAll(headerLabel, filterSection, lblResultsCount, contentBox);

        // Load all jobs initially
        loadJobOffers(null, null, null);
    }

    private VBox createFilterSection() {
        VBox filterBox = new VBox(10);
        filterBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10;");

        Label filterTitle = new Label("Filtres de Recherche");
        filterTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        HBox filtersRow = new HBox(15);
        filtersRow.setAlignment(Pos.CENTER_LEFT);

        // Location filter
        VBox locationBox = new VBox(5);
        Label locationLabel = new Label("📍 Ville");
        locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        txtLocationFilter = new TextField();
        txtLocationFilter.setPromptText("Ex: Paris, Lyon...");
        txtLocationFilter.setPrefWidth(180);
        txtLocationFilter.setStyle("-fx-background-radius: 5;");
        locationBox.getChildren().addAll(locationLabel, txtLocationFilter);

        // Contract type filter
        VBox contractBox = new VBox(5);
        Label contractLabel = new Label("📋 Type de Contrat");
        contractLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        cbContractTypeFilter = new ComboBox<>();
        cbContractTypeFilter.setPromptText("Tous les types");
        cbContractTypeFilter.setPrefWidth(180);
        cbContractTypeFilter.getItems().add("Tous");
        for (ContractType type : ContractType.values()) {
            cbContractTypeFilter.getItems().add(type.name());
        }
        contractBox.getChildren().addAll(contractLabel, cbContractTypeFilter);

        // Status filter
        VBox statusBox = new VBox(5);
        Label statusLabel = new Label("📊 Statut");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
        cbStatusFilter = new ComboBox<>();
        cbStatusFilter.setPromptText("Tous les statuts");
        cbStatusFilter.setPrefWidth(180);
        cbStatusFilter.getItems().add("Tous");
        for (Status status : Status.values()) {
            cbStatusFilter.getItems().add(status.name());
        }
        statusBox.getChildren().addAll(statusLabel, cbStatusFilter);

        // Buttons
        VBox buttonBox = new VBox(5);
        Label buttonLabel = new Label(" ");
        buttonLabel.setStyle("-fx-font-size: 12px;");

        HBox buttonsHBox = new HBox(10);
        btnApplyFilter = new Button("🔍 Filtrer");
        btnApplyFilter.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        btnApplyFilter.setOnAction(e -> applyFilters());

        btnClearFilter = new Button("✖ Effacer");
        btnClearFilter.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        btnClearFilter.setOnAction(e -> clearFilters());

        buttonsHBox.getChildren().addAll(btnApplyFilter, btnClearFilter);
        buttonBox.getChildren().addAll(buttonLabel, buttonsHBox);

        filtersRow.getChildren().addAll(locationBox, contractBox, statusBox, buttonBox);
        filterBox.getChildren().addAll(filterTitle, filtersRow);

        return filterBox;
    }

    @FXML
    private void applyFilters() {
        String location = txtLocationFilter.getText();
        if (location != null && location.trim().isEmpty()) {
            location = null;
        }

        ContractType contractType = null;
        String contractValue = cbContractTypeFilter.getValue();
        if (contractValue != null && !contractValue.equals("Tous") && !contractValue.isEmpty()) {
            try {
                contractType = ContractType.valueOf(contractValue);
            } catch (IllegalArgumentException e) {
                // Ignore invalid contract type
            }
        }

        Status status = null;
        String statusValue = cbStatusFilter.getValue();
        if (statusValue != null && !statusValue.equals("Tous") && !statusValue.isEmpty()) {
            try {
                status = Status.valueOf(statusValue);
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        loadJobOffers(location, contractType, status);
    }

    @FXML
    private void clearFilters() {
        txtLocationFilter.clear();
        cbContractTypeFilter.setValue(null);
        cbStatusFilter.setValue(null);
        loadJobOffers(null, null, null);
    }

    private void loadJobOffers(String location, ContractType contractType, Status status) {
        jobListContainer.getChildren().clear();
        selectedJob = null;
        selectedCard = null;

        try {
            List<JobOffer> jobs;

            // Check if any filter is applied
            if ((location == null || location.trim().isEmpty()) && contractType == null && status == null) {
                jobs = jobOfferService.getAllJobOffers();
            } else {
                jobs = jobOfferService.filterJobOffers(location, contractType, status);
            }

            if (jobs.isEmpty()) {
                Label noResults = new Label("Aucune offre trouvée");
                noResults.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
                jobListContainer.getChildren().add(noResults);
                updateResultsCount(0, location, contractType, status);

                // Clear details
                detailContainer.getChildren().clear();
                Label selectLabel = new Label("Aucune offre à afficher");
                selectLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #95a5a6;");
                detailContainer.getChildren().add(selectLabel);
                detailContainer.setAlignment(Pos.CENTER);
            } else {
                updateResultsCount(jobs.size(), location, contractType, status);
                for (JobOffer job : jobs) {
                    VBox card = createJobCard(job);
                    jobListContainer.getChildren().add(card);
                }

                // Auto-select first job
                if (!jobs.isEmpty()) {
                    JobOffer firstJob = jobs.get(0);
                    VBox firstCard = (VBox) jobListContainer.getChildren().get(0);
                    selectJob(firstJob, firstCard);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les offres: " + e.getMessage());
        }
    }

    private void updateResultsCount(int count, String location, ContractType contractType, Status status) {
        StringBuilder sb = new StringBuilder();
        sb.append(count).append(" offre(s) trouvée(s)");

        if (location != null || contractType != null || status != null) {
            sb.append(" avec filtres: ");
            boolean hasFilter = false;
            if (location != null && !location.trim().isEmpty()) {
                sb.append("Ville=").append(location);
                hasFilter = true;
            }
            if (contractType != null) {
                if (hasFilter) sb.append(", ");
                sb.append("Type=").append(contractType.name());
                hasFilter = true;
            }
            if (status != null) {
                if (hasFilter) sb.append(", ");
                sb.append("Statut=").append(status.name());
            }
        }

        lblResultsCount.setText(sb.toString());
    }

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; " +
                "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-cursor: hand;");

        // Title
        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        // Status badge
        String statusColor = job.getStatus() == Status.OPEN ? "#28a745" : "#dc3545";
        String statusText = job.getStatus() == Status.OPEN ? "Ouvert" : "Fermé";
        Label statusBadge = new Label(statusText);
        statusBadge.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; " +
                "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 11px;");

        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.getChildren().addAll(title, statusBadge);
        HBox.setHgrow(title, Priority.ALWAYS);

        // Location and contract type
        Label locationLabel = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "N/A"));
        locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        Label contractLabel = new Label("📋 " + (job.getContractType() != null ? job.getContractType().name() : "N/A"));
        contractLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");

        card.getChildren().addAll(headerBox, locationLabel, contractLabel);

        // Mouse hover effects
        card.setOnMouseEntered(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 8; " +
                        "-fx-border-color: #2196f3; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; " +
                        "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-cursor: hand;");
            }
        });
        card.setOnMouseClicked(e -> selectJob(job, card));

        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        // Reset previous selection
        if (selectedCard != null) {
            selectedCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; " +
                    "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-cursor: hand;");
        }

        // Set new selection
        selectedJob = job;
        selectedCard = card;
        card.setStyle("-fx-background-color: #bbdefb; -fx-background-radius: 8; " +
                "-fx-border-color: #2196f3; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");

        displayJobDetails(job);
    }

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();
        detailContainer.setAlignment(Pos.TOP_LEFT);

        // Title
        Label titleLabel = new Label(job.getTitle());
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        titleLabel.setWrapText(true);

        // Status badge
        String statusColor = job.getStatus() == Status.OPEN ? "#28a745" : "#dc3545";
        String statusText = job.getStatus() == Status.OPEN ? "✓ Ouvert" : "✕ Fermé";
        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; " +
                "-fx-padding: 5 15; -fx-background-radius: 15; -fx-font-size: 13px; -fx-font-weight: bold;");

        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.getChildren().addAll(titleLabel, statusLabel);

        // Info Grid
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(20);
        infoGrid.setVgap(10);
        infoGrid.setPadding(new Insets(15, 0, 15, 0));

        addInfoRow(infoGrid, 0, "📍 Ville", job.getLocation() != null ? job.getLocation() : "Non spécifiée");
        addInfoRow(infoGrid, 1, "📋 Type de Contrat", job.getContractType() != null ? job.getContractType().name() : "Non spécifié");
        addInfoRow(infoGrid, 2, "📊 Statut", job.getStatus() != null ? job.getStatus().name() : "Non spécifié");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        addInfoRow(infoGrid, 3, "📅 Date de création", job.getCreatedAt() != null ? job.getCreatedAt().format(formatter) : "N/A");
        addInfoRow(infoGrid, 4, "⏰ Date limite", job.getDeadline() != null ? job.getDeadline().format(formatter) : "Non définie");

        // Description Section
        Label descTitle = new Label("Description du poste");
        descTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        Label descContent = new Label(job.getDescription() != null && !job.getDescription().isEmpty()
                ? job.getDescription()
                : "Aucune description fournie.");
        descContent.setWrapText(true);
        descContent.setStyle("-fx-font-size: 14px; -fx-text-fill: #555555; -fx-line-spacing: 3;");

        VBox descBox = new VBox(8);
        descBox.setPadding(new Insets(10));
        descBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
        descBox.getChildren().addAll(descTitle, descContent);

        // Skills Section
        VBox skillsSection = createSkillsSection(job);

        // Apply Button (for candidates)
        Button applyBtn = new Button("📩 Postuler à cette offre");
        applyBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 8; -fx-cursor: hand;");
        applyBtn.setOnAction(e -> showAlert(Alert.AlertType.INFORMATION, "Candidature",
                "Votre candidature a été enregistrée pour le poste: " + job.getTitle()));

        // Disable apply button if job is closed
        if (job.getStatus() == Status.CLOSED) {
            applyBtn.setDisable(true);
            applyBtn.setText("❌ Offre fermée");
            applyBtn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-font-size: 14px; " +
                    "-fx-font-weight: bold; -fx-padding: 12 30; -fx-background-radius: 8;");
        }

        HBox buttonBox = new HBox();
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        buttonBox.getChildren().add(applyBtn);

        detailContainer.getChildren().addAll(titleBox, infoGrid, descBox, skillsSection, buttonBox);
    }

    private void addInfoRow(GridPane grid, int row, String label, String value) {
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        grid.add(labelNode, 0, row);
        grid.add(valueNode, 1, row);
    }

    private VBox createSkillsSection(JobOffer job) {
        VBox skillsBox = new VBox(10);
        skillsBox.setPadding(new Insets(10));
        skillsBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");

        Label skillsTitle = new Label("🎯 Compétences Requises");
        skillsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34495e;");

        FlowPane skillsFlow = new FlowPane();
        skillsFlow.setHgap(8);
        skillsFlow.setVgap(8);

        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(job.getId());

            if (skills.isEmpty()) {
                Label noSkills = new Label("Aucune compétence spécifiée");
                noSkills.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                skillsFlow.getChildren().add(noSkills);
            } else {
                for (OfferSkill skill : skills) {
                    Label skillBadge = new Label(skill.getSkillName() +
                            (skill.getLevelRequired() != null ? " (" + skill.getLevelRequired().name() + ")" : ""));
                    skillBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                            "-fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 12px;");
                    skillsFlow.getChildren().add(skillBadge);
                }
            }
        } catch (SQLException e) {
            Label errorLabel = new Label("Erreur lors du chargement des compétences");
            errorLabel.setStyle("-fx-text-fill: #e74c3c;");
            skillsFlow.getChildren().add(errorLabel);
        }

        skillsBox.getChildren().addAll(skillsTitle, skillsFlow);
        return skillsBox;
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

