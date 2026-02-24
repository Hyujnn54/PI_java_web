package Controllers;

import Models.JobOffer;
import Models.JobOfferWarning;
import Models.WarningCorrection;
import Models.OfferSkill;
import Models.ContractType;
import Models.Status;
import Models.SkillLevel;
import Services.JobOfferService;
import Services.JobOfferWarningService;
import Services.WarningCorrectionService;
import Services.OfferSkillService;
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

    // Filtres
    private ComboBox<String> cbFilterType;
    private ComboBox<String> cbFilterLocation;
    private ContractType selectedContractType = null;
    private String selectedLocation = null;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;
    private JobOfferWarningService warningService;
    private WarningCorrectionService correctionService;

    // Form elements
    private TextField formTitleField;
    private TextArea formDescription;
    private TextField formLocation;
    private ComboBox<ContractType> formContractType;
    private DatePicker formDeadline;
    private ComboBox<Status> formStatus;

    // Error labels for each field
    private Label titleErrorLabel;
    private Label descriptionErrorLabel;
    private Label locationErrorLabel;
    private Label deadlineErrorLabel;
    private Label skillsErrorLabel;

    // Skills management
    private VBox skillsContainer;
    private List<SkillRow> skillRows;
    private boolean isEditMode = false;
    private JobOffer editingJob = null;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        offerSkillService = new OfferSkillService();
        warningService = new JobOfferWarningService();
        correctionService = new WarningCorrectionService();
        skillRows = new ArrayList<>();
        buildUI();
        loadJobOffers();
        checkForWarnings(); // Vérifier les avertissements au démarrage
    }

    /**
     * Vérifie s'il y a des avertissements en attente pour ce recruteur
     */
    private void checkForWarnings() {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            int warningCount = warningService.countPendingWarningsForRecruiter(recruiterId);

            if (warningCount > 0) {
                showWarningNotification(warningCount);
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification avertissements: " + e.getMessage());
        }
    }

    /**
     * Affiche une notification d'avertissement
     */
    private void showWarningNotification(int count) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("⚠️ Attention");
        alert.setHeaderText("Vous avez " + count + " offre(s) signalée(s)");
        alert.setContentText("Un administrateur a signalé un problème avec certaines de vos offres.\n\n" +
                            "Veuillez consulter les offres marquées en jaune et les corriger ou les supprimer.");
        alert.show();
    }

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 20;");

        // === EN-TÊTE RECRUTEUR ===
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label recruiterBadge = new Label("👤 ESPACE RECRUTEUR");
        recruiterBadge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 8 16; " +
                               "-fx-background-radius: 8; -fx-font-weight: 700; -fx-font-size: 14px;");

        Label pageTitle = new Label("Gestion de mes offres");
        pageTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        headerBox.getChildren().addAll(recruiterBadge, pageTitle);
        mainContainer.getChildren().add(headerBox);
        mainContainer.getChildren().add(new Region() {{ setPrefHeight(15); }});

        // === BARRE DE RECHERCHE ET FILTRES ===
        VBox searchFilterBox = createSearchFilterBox();
        mainContainer.getChildren().add(searchFilterBox);
        mainContainer.getChildren().add(new Region() {{ setPrefHeight(15); }});

        // === CONTENU PRINCIPAL (2 colonnes) ===
        HBox contentArea = new HBox(20);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        // LEFT: Liste des offres
        VBox leftSide = createJobListPanel();
        leftSide.setPrefWidth(400);
        leftSide.setMinWidth(350);
        leftSide.setMaxWidth(450);

        // RIGHT: Détails
        VBox rightSide = createDetailPanel();
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        contentArea.getChildren().addAll(leftSide, rightSide);
        mainContainer.getChildren().add(contentArea);
    }

    private VBox createSearchFilterBox() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");

        // Recherche
        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setStyle("-fx-padding: 18 20 12 20;");

        Label searchIcon = new Label("🔍");
        searchIcon.setStyle("-fx-font-size: 18px;");

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

        searchRow.getChildren().addAll(searchIcon, txtSearch, btnSearchAction);

        // Séparateur
        Separator separator = new Separator();

        // Filtres
        HBox filterRow = new HBox(12);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setStyle("-fx-padding: 12 20 18 20;");

        Label filterLabel = new Label("Filtres:");
        filterLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        cbFilterType = new ComboBox<>();
        cbFilterType.setPromptText("Type");
        cbFilterType.getItems().add("Tous");
        for (ContractType type : ContractType.values()) {
            cbFilterType.getItems().add(formatContractType(type));
        }
        cbFilterType.setStyle("-fx-pref-width: 130; -fx-pref-height: 34;");
        cbFilterType.setOnAction(e -> applyFilters());

        cbFilterLocation = new ComboBox<>();
        cbFilterLocation.setPromptText("Lieu");
        cbFilterLocation.getItems().add("Tous");
        try {
            List<String> locations = jobOfferService.getAllLocations();
            cbFilterLocation.getItems().addAll(locations);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cbFilterLocation.setStyle("-fx-pref-width: 130; -fx-pref-height: 34;");
        cbFilterLocation.setOnAction(e -> applyFilters());

        Button btnReset = new Button("✕ Réinitialiser");
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
        String typeValue = cbFilterType.getValue();
        String locationValue = cbFilterLocation.getValue();

        selectedContractType = (typeValue == null || typeValue.equals("Tous")) ? null : getContractTypeFromLabel(typeValue);
        selectedLocation = (locationValue == null || locationValue.equals("Tous")) ? null : locationValue;

        loadFilteredJobOffers();
    }

    private void resetFilters() {
        selectedContractType = null;
        selectedLocation = null;
        if (cbFilterType != null) cbFilterType.setValue(null);
        if (cbFilterLocation != null) cbFilterLocation.setValue(null);
        if (txtSearch != null) txtSearch.clear();
        loadJobOffers();
    }

    private void loadFilteredJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            List<JobOffer> jobs = jobOfferService.filterJobOffers(selectedLocation, selectedContractType, null);

            String keyword = txtSearch != null ? txtSearch.getText().trim().toLowerCase() : "";
            if (!keyword.isEmpty()) {
                jobs = jobs.stream()
                    .filter(job -> job.getTitle().toLowerCase().contains(keyword) ||
                                  (job.getDescription() != null && job.getDescription().toLowerCase().contains(keyword)) ||
                                  (job.getLocation() != null && job.getLocation().toLowerCase().contains(keyword)))
                    .toList();
            }

            updateResultCount(jobs.size());

            if (jobs.isEmpty()) {
                jobListContainer.getChildren().add(createEmptyState());
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
            showAlert("Erreur", "Impossible de charger: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateResultCount(int count) {
        Label resultCount = (Label) mainContainer.lookup("#resultCount");
        if (resultCount != null) {
            resultCount.setText(count == 0 ? "Aucun résultat" : count + " offre(s)");
        }
    }

    private VBox createEmptyState() {
        VBox emptyBox = new VBox(10);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 30;");
        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 40px;");
        Label text = new Label("Aucune offre trouvée");
        text.setStyle("-fx-font-size: 14px; -fx-text-fill: #6c757d;");
        emptyBox.getChildren().addAll(icon, text);
        return emptyBox;
    }

    private String formatContractType(ContractType type) {
        return switch (type) {
            case CDI -> "CDI";
            case CDD -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE -> "Freelance";
            case PART_TIME -> "Temps Partiel";
            case FULL_TIME -> "Temps Plein";
        };
    }

    private ContractType getContractTypeFromLabel(String label) {
        return switch (label) {
            case "CDI" -> ContractType.CDI;
            case "CDD" -> ContractType.CDD;
            case "Stage" -> ContractType.INTERNSHIP;
            case "Freelance" -> ContractType.FREELANCE;
            case "Temps Partiel" -> ContractType.PART_TIME;
            case "Temps Plein" -> ContractType.FULL_TIME;
            default -> null;
        };
    }

    private VBox createJobListPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 18; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        Label title = new Label("📋 Mes offres d'emploi");
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

    private VBox createDetailPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📄 Détails");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnCreate = new Button("➕ Nouvelle offre");
        btnCreate.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-font-size: 13px; -fx-padding: 10 18; -fx-background-radius: 8; -fx-cursor: hand;");
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
            // Load ALL job offers (recruiter can see all but edit only their own)
            List<JobOffer> jobs = jobOfferService.getAllJobOffers();

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

        // Style différent si l'offre est signalée
        String bgColor = job.isFlagged() ? "#fff3cd" : "#f8f9fa";
        String borderColor = job.isFlagged() ? "#ffc107" : "#dee2e6";
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-padding: 15; " +
                     "-fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: " +
                     (job.isFlagged() ? "2" : "1") + "; -fx-cursor: hand;");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Label badge = new Label(formatContractType(job.getContractType()));
        badge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-size: 11px;");

        header.getChildren().addAll(title, badge);

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non spécifié"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px;");

        // Badge de statut avec gestion du statut FLAGGED
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        String statusColor;
        String statusText;
        if (job.isFlagged() || job.getStatus() == Status.FLAGGED) {
            statusColor = "#ffc107";
            statusText = "⚠️ Signalé";
        } else if (job.getStatus() == Status.OPEN) {
            statusColor = "#28a745";
            statusText = "Ouvert";
        } else {
            statusColor = "#dc3545";
            statusText = "Fermé";
        }

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: " +
                            (job.isFlagged() ? "#212529" : "white") + "; -fx-padding: 2 6; " +
                            "-fx-background-radius: 4; -fx-font-size: 10px;");
        statusRow.getChildren().add(statusLabel);

        // Message d'alerte si signalé
        if (job.isFlagged()) {
            Label alertLabel = new Label("Action requise");
            alertLabel.setStyle("-fx-text-fill: #856404; -fx-font-size: 10px; -fx-font-weight: bold;");
            statusRow.getChildren().add(alertLabel);
        }

        card.getChildren().addAll(header, location, statusRow);
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

    /**
     * Affiche les avertissements pour le recruteur avec actions possibles
     */
    private void displayWarningsForRecruiter(JobOffer job) {
        try {
            List<JobOfferWarning> warnings = warningService.getPendingWarningsByJobOfferId(job.getId());
            if (warnings.isEmpty()) return;

            // Marquer les avertissements comme vus
            for (JobOfferWarning warning : warnings) {
                if (warning.getStatus() == JobOfferWarning.WarningStatus.SENT) {
                    warningService.markAsSeen(warning.getId());
                }
            }

            VBox warningSection = new VBox(15);
            warningSection.setStyle("-fx-background-color: #f8d7da; -fx-background-radius: 10; -fx-padding: 20; " +
                                   "-fx-border-color: #f5c6cb; -fx-border-radius: 10; -fx-border-width: 2;");

            // En-tête d'alerte
            HBox alertHeader = new HBox(10);
            alertHeader.setAlignment(Pos.CENTER_LEFT);

            Label alertIcon = new Label("🚨");
            alertIcon.setStyle("-fx-font-size: 24px;");

            VBox alertTextBox = new VBox(3);
            Label alertTitle = new Label("Action requise - Offre signalée");
            alertTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #721c24;");

            Label alertSubtitle = new Label("Un administrateur a signalé un problème avec cette offre. Veuillez corriger ou supprimer l'offre.");
            alertSubtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #721c24;");
            alertSubtitle.setWrapText(true);

            alertTextBox.getChildren().addAll(alertTitle, alertSubtitle);
            alertHeader.getChildren().addAll(alertIcon, alertTextBox);
            HBox.setHgrow(alertTextBox, Priority.ALWAYS);
            warningSection.getChildren().add(alertHeader);

            // Liste des avertissements
            for (JobOfferWarning warning : warnings) {
                VBox warningCard = new VBox(10);
                warningCard.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8;");
                warningCard.setMaxWidth(Double.MAX_VALUE);

                Label reasonLabel = new Label("📋 Raison: " + warning.getReason());
                reasonLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");
                reasonLabel.setWrapText(true);

                Label messageLabel = new Label("💬 Message de l'admin:");
                messageLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #495057; -fx-font-size: 12px;");

                // Zone de texte pour le message complet
                TextArea messageContent = new TextArea(warning.getMessage());
                messageContent.setWrapText(true);
                messageContent.setEditable(false);
                messageContent.setPrefRowCount(4);
                messageContent.setStyle("-fx-control-inner-background: #f8f9fa; -fx-text-fill: #495057; " +
                                       "-fx-font-size: 13px; -fx-border-color: #dee2e6; -fx-border-radius: 5; " +
                                       "-fx-background-radius: 5;");

                Label dateLabel = new Label("📅 Signalé le " + warning.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
                dateLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

                warningCard.getChildren().addAll(reasonLabel, messageLabel, messageContent, dateLabel);
                warningSection.getChildren().add(warningCard);
            }

            // Boutons d'action
            HBox actionButtons = new HBox(15);
            actionButtons.setAlignment(Pos.CENTER);
            actionButtons.setStyle("-fx-padding: 10 0 0 0;");

            Button btnEdit = new Button("✏️ Modifier l'offre");
            btnEdit.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: 600; " +
                            "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnEdit.setOnAction(e -> showEditForm(job));

            Button btnDelete = new Button("🗑️ Supprimer l'offre");
            btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                              "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> handleDeleteJobOffer(job));

            Button btnMarkResolved = new Button("✓ J'ai corrigé le problème");
            btnMarkResolved.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                                    "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnMarkResolved.setOnAction(e -> handleMarkWarningsResolved(job, warnings));

            actionButtons.getChildren().addAll(btnEdit, btnDelete, btnMarkResolved);
            warningSection.getChildren().add(actionButtons);

            detailContainer.getChildren().add(warningSection);

        } catch (SQLException e) {
            System.err.println("Erreur chargement des avertissements: " + e.getMessage());
        }
    }

    /**
     * Ouvre le dialogue pour soumettre une correction à l'admin
     */
    private void handleMarkWarningsResolved(JobOffer job, List<JobOfferWarning> warnings) {
        // Créer un dialogue pour soumettre la correction
        Dialog<WarningCorrection> dialog = new Dialog<>();
        dialog.setTitle("Soumettre une correction");
        dialog.setHeaderText("Soumettre votre correction pour validation");

        ButtonType submitButtonType = new ButtonType("Soumettre", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        // Contenu du dialogue
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);

        // Info sur l'offre
        Label offerLabel = new Label("📋 Offre: " + job.getTitle());
        offerLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");

        // Explication
        Label infoLabel = new Label("⚠️ Votre correction sera envoyée à l'administrateur pour validation. " +
                                   "Une fois approuvée, votre offre sera republiée.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #856404; -fx-background-color: #fff3cd; -fx-padding: 10; " +
                          "-fx-background-radius: 5;");

        // Note de correction avec bouton de génération
        HBox noteLabelBox = new HBox(10);
        noteLabelBox.setAlignment(Pos.CENTER_LEFT);

        Label noteLabel = new Label("📝 Description des corrections:");
        noteLabel.setStyle("-fx-font-weight: 600;");

        Button btnGenerateNote = new Button("🤖 Générer automatiquement");
        btnGenerateNote.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-size: 11px; " +
                                "-fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

        noteLabelBox.getChildren().addAll(noteLabel, btnGenerateNote);

        TextArea correctionNote = new TextArea();
        correctionNote.setPromptText("Cliquez sur 'Générer automatiquement' pour créer une description des changements...");
        correctionNote.setPrefRowCount(5);
        correctionNote.setWrapText(true);

        // Label de chargement
        Label loadingLabel = new Label("");
        loadingLabel.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");

        // Récupérer la raison du signalement
        String warningReason = !warnings.isEmpty() ? warnings.get(0).getReason() : "Non spécifié";
        String warningMessage = !warnings.isEmpty() ? warnings.get(0).getMessage() : "";

        // Action du bouton de génération
        btnGenerateNote.setOnAction(e -> {
            loadingLabel.setText("⏳ Génération en cours...");
            btnGenerateNote.setDisable(true);

            // Exécuter dans un thread séparé
            new Thread(() -> {
                try {
                    String generatedNote = generateCorrectionNote(
                        warningReason,
                        warningMessage,
                        job.getTitle(),
                        job.getDescription()
                    );

                    // Mettre à jour l'UI dans le thread JavaFX
                    javafx.application.Platform.runLater(() -> {
                        if (generatedNote != null && !generatedNote.isEmpty()) {
                            correctionNote.setText(generatedNote);
                            loadingLabel.setText("✅ Description générée avec succès");
                        } else {
                            loadingLabel.setText("⚠️ Impossible de générer, veuillez écrire manuellement");
                        }
                        btnGenerateNote.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        loadingLabel.setText("❌ Erreur: " + ex.getMessage());
                        btnGenerateNote.setDisable(false);
                    });
                }
            }).start();
        });

        // Résumé des modifications
        VBox changesBox = new VBox(10);
        changesBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 8;");

        Label changesTitle = new Label("📊 Résumé de l'offre actuelle:");
        changesTitle.setStyle("-fx-font-weight: 600;");

        Label currentTitle = new Label("Titre: " + job.getTitle());
        currentTitle.setStyle("-fx-text-fill: #495057;");

        String descPreview = job.getDescription() != null
            ? (job.getDescription().length() > 100 ? job.getDescription().substring(0, 100) + "..." : job.getDescription())
            : "Aucune description";
        Label currentDesc = new Label("Description: " + descPreview);
        currentDesc.setStyle("-fx-text-fill: #495057;");
        currentDesc.setWrapText(true);

        changesBox.getChildren().addAll(changesTitle, currentTitle, currentDesc);

        content.getChildren().addAll(offerLabel, infoLabel, noteLabelBox, correctionNote, loadingLabel, changesBox);
        dialog.getDialogPane().setContent(content);

        // Validation
        Button submitButton = (Button) dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);

        correctionNote.textProperty().addListener((obs, oldVal, newVal) -> {
            submitButton.setDisable(newVal.trim().length() < 10);
        });

        // Résultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                WarningCorrection correction = new WarningCorrection();
                correction.setJobOfferId(job.getId());
                correction.setRecruiterId(UserContext.getRecruiterId());
                correction.setCorrectionNote(correctionNote.getText().trim());
                correction.setNewTitle(job.getTitle());
                correction.setNewDescription(job.getDescription());
                // Le warningId sera défini pour le premier warning
                if (!warnings.isEmpty()) {
                    correction.setWarningId(warnings.get(0).getId());
                }
                return correction;
            }
            return null;
        });

        Optional<WarningCorrection> result = dialog.showAndWait();
        result.ifPresent(correction -> {
            try {
                // Soumettre la correction pour chaque warning
                for (JobOfferWarning warning : warnings) {
                    WarningCorrection corr = new WarningCorrection();
                    corr.setWarningId(warning.getId());
                    corr.setJobOfferId(job.getId());
                    corr.setRecruiterId(UserContext.getRecruiterId());
                    corr.setCorrectionNote(correction.getCorrectionNote());
                    corr.setNewTitle(job.getTitle());
                    corr.setNewDescription(job.getDescription());

                    correctionService.submitCorrection(corr);
                }

                showAlert("Succès",
                    "Votre correction a été soumise à l'administrateur.\n\n" +
                    "Vous serez notifié une fois qu'elle sera validée et votre offre sera republiée.",
                    Alert.AlertType.INFORMATION);

                // Recharger les données
                loadJobOffers();

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la soumission: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        // Afficher les avertissements en premier si l'offre est signalée
        if (job.isFlagged()) {
            displayWarningsForRecruiter(job);
        }

        VBox headerCard = new VBox(15);
        headerCard.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 25;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label contractType = new Label("💼 " + formatContractType(job.getContractType()));
        contractType.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non spécifié"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        String statusColor;
        String statusText;
        if (job.isFlagged() || job.getStatus() == Status.FLAGGED) {
            statusColor = "#ffc107";
            statusText = "⚠️ Signalé";
        } else if (job.getStatus() == Status.OPEN) {
            statusColor = "#28a745";
            statusText = "Ouvert";
        } else {
            statusColor = "#dc3545";
            statusText = "Fermé";
        }

        Label status = new Label("📊 " + statusText);
        status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 14px; -fx-font-weight: 700;");

        metaRow.getChildren().addAll(contractType, location, status);

        if (job.getDeadline() != null) {
            Label deadline = new Label("⏰ Date limite: " + job.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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

        Button btnToggleStatus = new Button(job.getStatus() == Status.OPEN ? "🔒 Close" : "🔓 Open");
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
        // Check if the current user owns this job offer
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission Denied", "You can only edit job offers that you created.", Alert.AlertType.WARNING);
            return;
        }

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

        // Title field with AI Suggest button
        Label titleLabel = new Label("Titre du poste *");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        formTitleField = new TextField();
        formTitleField.setPromptText("Ex: Développeur Java Senior, Chef de projet IT...");
        formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        HBox.setHgrow(formTitleField, Priority.ALWAYS);

        Button btnAISuggest = new Button("🤖 AI Suggest");
        btnAISuggest.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: 600; " +
                             "-fx-padding: 10 15; -fx-background-radius: 6; -fx-cursor: hand;");

        Label aiStatusLabel = new Label("");
        aiStatusLabel.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");

        titleRow.getChildren().addAll(formTitleField, btnAISuggest);

        Label titleHelpLabel = new Label("ℹ️ Entrez le titre puis cliquez 'AI Suggest' pour remplir automatiquement le formulaire");
        titleHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        titleErrorLabel = new Label();
        titleErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        titleErrorLabel.setVisible(false);
        titleErrorLabel.setManaged(false);

        // Description field with help and error labels
        Label descLabel = new Label("Description du poste *");
        descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formDescription = new TextArea();
        formDescription.setPromptText("Description détaillée du poste...");
        formDescription.setPrefRowCount(6);
        formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label descHelpLabel = new Label("ℹ️ 20-2000 caractères. Décrivez les responsabilités et exigences.");
        descHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        descriptionErrorLabel = new Label();
        descriptionErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        descriptionErrorLabel.setVisible(false);
        descriptionErrorLabel.setManaged(false);

        // Location field with help and error labels
        Label locationLabel = new Label("Localisation *");
        locationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formLocation = new TextField();
        formLocation.setPromptText("Localisation (ex: Paris, France ou Remote)");
        formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label locationHelpLabel = new Label("ℹ️ 2-100 caractères. Ville, pays ou 'Remote'.");
        locationHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        locationErrorLabel = new Label();
        locationErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        locationErrorLabel.setVisible(false);
        locationErrorLabel.setManaged(false);

        // Contract Type
        Label contractLabel = new Label("Type de contrat *");
        contractLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formContractType = new ComboBox<>();
        formContractType.getItems().addAll(ContractType.values());
        formContractType.setPromptText("Sélectionner le type de contrat");
        formContractType.setStyle("-fx-font-size: 14px;");

        formStatus = new ComboBox<>();
        formStatus.getItems().addAll(Status.values());
        formStatus.setValue(Status.OPEN);
        formStatus.setStyle("-fx-font-size: 14px;");

        formDeadline = new DatePicker();
        formDeadline.setPromptText("Date limite (optionnel)");
        formDeadline.setStyle("-fx-font-size: 14px;");

        Label deadlineHelpLabel = new Label("ℹ️ Doit être une date future. Laissez vide si pas de date limite.");
        deadlineHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        deadlineErrorLabel = new Label();
        deadlineErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        deadlineErrorLabel.setVisible(false);
        deadlineErrorLabel.setManaged(false);

        // Add real-time validation listeners
        addValidationListeners();

        // Skills section
        VBox skillsSection = new VBox(10);
        skillsSection.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 8;");

        Label skillsLabel = new Label("Compétences requises *");
        skillsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        Label skillsHelpLabel = new Label("ℹ️ Ajoutez au moins une compétence. 2-50 caractères.");
        skillsHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 5 0 10 0;");

        skillsErrorLabel = new Label();
        skillsErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5 0 0 0;");
        skillsErrorLabel.setVisible(false);
        skillsErrorLabel.setManaged(false);

        skillsContainer = new VBox(10);
        skillRows = new ArrayList<>();

        Button btnAddSkill = new Button("+ Add Skill");
        btnAddSkill.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 8 15; -fx-background-radius: 6; -fx-cursor: hand;");
        btnAddSkill.setOnAction(e -> addSkillRow(null));

        skillsSection.getChildren().addAll(skillsLabel, skillsHelpLabel, skillsErrorLabel, skillsContainer, btnAddSkill);

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

        // Action du bouton AI Suggest
        btnAISuggest.setOnAction(e -> {
            String jobTitle = formTitleField.getText().trim();
            if (jobTitle.length() < 3) {
                showAlert("Attention", "Veuillez entrer un titre de poste valide (minimum 3 caractères)", Alert.AlertType.WARNING);
                return;
            }

            aiStatusLabel.setText("⏳ Génération en cours...");
            btnAISuggest.setDisable(true);

            // Exécuter dans un thread séparé
            new Thread(() -> {
                try {
                    String suggestions = generateJobSuggestions(jobTitle);

                    javafx.application.Platform.runLater(() -> {
                        if (suggestions != null) {
                            parseAndFillForm(suggestions, jobTitle);
                            aiStatusLabel.setText("✅ Formulaire rempli avec succès!");
                        } else {
                            aiStatusLabel.setText("⚠️ Impossible de générer les suggestions");
                        }
                        btnAISuggest.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        aiStatusLabel.setText("❌ Erreur: " + ex.getMessage());
                        btnAISuggest.setDisable(false);
                    });
                }
            }).start();
        });

        Button btnSubmit = new Button(isEditMode ? "Mettre à jour l'offre" : "Créer l'offre");
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
                titleLabel, titleRow, titleHelpLabel, aiStatusLabel, titleErrorLabel,
                descLabel, formDescription, descHelpLabel, descriptionErrorLabel,
                locationLabel, formLocation, locationHelpLabel, locationErrorLabel,
                contractLabel, formContractType,
                new Label("Statut *"), formStatus,
                new Label("Date limite (Optionnel)"), formDeadline, deadlineHelpLabel, deadlineErrorLabel,
                skillsSection,
                btnSubmit
        );

        detailContainer.getChildren().addAll(btnBack, formTitleLabel, formContainer);
    }

    private void addSkillRow(OfferSkill existingSkill) {
        HBox skillRow = new HBox(10);
        skillRow.setAlignment(Pos.CENTER_LEFT);
        skillRow.setStyle("-fx-padding: 5;");

        VBox skillNameContainer = new VBox(3);
        HBox.setHgrow(skillNameContainer, Priority.ALWAYS);

        TextField skillName = new TextField();
        skillName.setPromptText("Skill name (e.g., Java, JavaScript)");
        skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px;");

        Label skillErrorLabel = new Label();
        skillErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-weight: bold;");
        skillErrorLabel.setVisible(false);
        skillErrorLabel.setManaged(false);

        // Validation for skill name
        skillName.textProperty().addListener((obs, oldVal, newVal) -> {
            skillErrorLabel.setVisible(false);
            skillErrorLabel.setManaged(false);

            if (newVal.length() > 50) {
                skillName.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 2) {
                skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                skillErrorLabel.setText("⚠️ Min 2 characters");
                skillErrorLabel.setVisible(true);
                skillErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty() && !newVal.matches("^[a-zA-Z0-9\\s\\-+#.]+$")) {
                skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                skillErrorLabel.setText("❌ Only letters, numbers, -, +, #, . allowed");
                skillErrorLabel.setVisible(true);
                skillErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty()) {
                skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
            }
        });

        skillNameContainer.getChildren().addAll(skillName, skillErrorLabel);

        ComboBox<SkillLevel> skillLevel = new ComboBox<>();
        skillLevel.getItems().addAll(SkillLevel.values());
        skillLevel.setPromptText("Level");
        skillLevel.setPrefWidth(150);
        skillLevel.setStyle("-fx-font-size: 13px;");

        if (existingSkill != null) {
            skillName.setText(existingSkill.getSkillName());
            skillLevel.setValue(existingSkill.getLevelRequired());
        } else {
            skillLevel.setValue(SkillLevel.INTERMEDIATE);
        }

        Button btnRemove = new Button("✕");
        btnRemove.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 6 10; -fx-background-radius: 6; -fx-cursor: hand;");
        btnRemove.setOnAction(e -> {
            skillsContainer.getChildren().remove(skillRow);
            skillRows.removeIf(row -> row.nameField == skillName);
        });

        skillRow.getChildren().addAll(skillNameContainer, skillLevel, btnRemove);
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
        // Check if the current user owns this job offer
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission Denied", "You can only delete job offers that you created.", Alert.AlertType.WARNING);
            return;
        }

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
        // Check if the current user owns this job offer
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission Denied", "You can only change the status of job offers that you created.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Status newStatus = job.getStatus() == Status.OPEN
                ? Status.CLOSED
                : Status.OPEN;

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
        String title = formTitleField.getText().trim();
        String description = formDescription.getText().trim();
        String location = formLocation.getText().trim();

        // Validate Title
        if (title.isEmpty()) {
            showAlert("Validation Error", "Job title is required", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }
        if (title.length() < 3) {
            showAlert("Validation Error", "Job title must be at least 3 characters long", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }
        if (title.length() > 100) {
            showAlert("Validation Error", "Job title must not exceed 100 characters", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }
        if (!title.matches("^[a-zA-Z0-9\\s\\-\\/&,.]+$")) {
            showAlert("Validation Error", "Job title contains invalid characters.\nOnly letters, numbers, spaces, and basic punctuation are allowed.", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }

        // Validate Description
        if (description.isEmpty()) {
            showAlert("Validation Error", "Job description is required", Alert.AlertType.WARNING);
            formDescription.requestFocus();
            return false;
        }
        if (description.length() < 20) {
            showAlert("Validation Error", "Job description must be at least 20 characters long", Alert.AlertType.WARNING);
            formDescription.requestFocus();
            return false;
        }
        if (description.length() > 2000) {
            showAlert("Validation Error", "Job description must not exceed 2000 characters", Alert.AlertType.WARNING);
            formDescription.requestFocus();
            return false;
        }

        // Validate Location
        if (location.isEmpty()) {
            showAlert("Validation Error", "Location is required", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }
        if (location.length() < 2) {
            showAlert("Validation Error", "Location must be at least 2 characters long", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }
        if (location.length() > 100) {
            showAlert("Validation Error", "Location must not exceed 100 characters", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }
        if (!location.matches("^[a-zA-Z0-9\\s\\-,./]+$")) {
            showAlert("Validation Error", "Location contains invalid characters.\nOnly letters, numbers, spaces, and basic punctuation are allowed.", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }

        // Validate Contract Type
        if (formContractType.getValue() == null) {
            showAlert("Validation Error", "Please select a contract type", Alert.AlertType.WARNING);
            formContractType.requestFocus();
            return false;
        }

        // Validate Status
        if (formStatus.getValue() == null) {
            showAlert("Validation Error", "Please select a status", Alert.AlertType.WARNING);
            formStatus.requestFocus();
            return false;
        }

        // Validate Deadline (if provided)
        if (formDeadline.getValue() != null) {
            if (formDeadline.getValue().isBefore(java.time.LocalDate.now())) {
                showAlert("Validation Error", "Deadline cannot be in the past", Alert.AlertType.WARNING);
                formDeadline.requestFocus();
                return false;
            }
        }

        // Validate Skills
        boolean hasValidSkill = false;
        for (SkillRow row : skillRows) {
            String skillName = row.nameField.getText().trim();
            if (!skillName.isEmpty()) {
                if (skillName.length() < 2) {
                    showAlert("Validation Error", "Skill name must be at least 2 characters long", Alert.AlertType.WARNING);
                    row.nameField.requestFocus();
                    return false;
                }
                if (skillName.length() > 50) {
                    showAlert("Validation Error", "Skill name must not exceed 50 characters", Alert.AlertType.WARNING);
                    row.nameField.requestFocus();
                    return false;
                }
                if (!skillName.matches("^[a-zA-Z0-9\\s\\-+#.]+$")) {
                    showAlert("Validation Error", "Skill name '" + skillName + "' contains invalid characters.\nOnly letters, numbers, spaces, and -, +, #, . are allowed.", Alert.AlertType.WARNING);
                    row.nameField.requestFocus();
                    return false;
                }
                if (row.levelCombo.getValue() == null) {
                    showAlert("Validation Error", "Please select a level for skill: " + skillName, Alert.AlertType.WARNING);
                    row.levelCombo.requestFocus();
                    return false;
                }
                hasValidSkill = true;
            }
        }

        if (!hasValidSkill) {
            showAlert("Validation Error", "Please add at least one skill for this job offer", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private void addValidationListeners() {
        // Title validation - max 100 chars
        formTitleField.textProperty().addListener((obs, oldVal, newVal) -> {
            titleErrorLabel.setVisible(false);
            titleErrorLabel.setManaged(false);

            if (newVal.length() > 100) {
                formTitleField.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 3) {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                titleErrorLabel.setText("⚠️ Title must be at least 3 characters");
                titleErrorLabel.setVisible(true);
                titleErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty() && !newVal.matches("^[a-zA-Z0-9\\s\\-/&,.]+$")) {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                titleErrorLabel.setText("❌ Invalid characters. Use only letters, numbers, spaces, and -, /, &, , .");
                titleErrorLabel.setVisible(true);
                titleErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty()) {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
            }
        });

        // Description validation - max 2000 chars
        formDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionErrorLabel.setVisible(false);
            descriptionErrorLabel.setManaged(false);

            if (newVal.length() > 2000) {
                formDescription.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 20) {
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                descriptionErrorLabel.setText("⚠️ Description must be at least 20 characters (currently " + newVal.length() + ")");
                descriptionErrorLabel.setVisible(true);
                descriptionErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty()) {
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
            }
        });

        // Location validation - max 100 chars
        formLocation.textProperty().addListener((obs, oldVal, newVal) -> {
            locationErrorLabel.setVisible(false);
            locationErrorLabel.setManaged(false);

            if (newVal.length() > 100) {
                formLocation.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 2) {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                locationErrorLabel.setText("⚠️ Location must be at least 2 characters");
                locationErrorLabel.setVisible(true);
                locationErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty() && !newVal.matches("^[a-zA-Z0-9\\s\\-,./]+$")) {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                locationErrorLabel.setText("❌ Invalid characters. Use only letters, numbers, spaces, and -, , . /");
                locationErrorLabel.setVisible(true);
                locationErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty()) {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
            }
        });

        // Deadline validation - not in the past
        formDeadline.valueProperty().addListener((obs, oldVal, newVal) -> {
            deadlineErrorLabel.setVisible(false);
            deadlineErrorLabel.setManaged(false);

            if (newVal != null && newVal.isBefore(java.time.LocalDate.now())) {
                formDeadline.setStyle("-fx-font-size: 14px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                deadlineErrorLabel.setText("❌ Deadline cannot be in the past");
                deadlineErrorLabel.setVisible(true);
                deadlineErrorLabel.setManaged(true);
            } else if (newVal != null) {
                formDeadline.setStyle("-fx-font-size: 14px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                formDeadline.setStyle("-fx-font-size: 14px;");
            }
        });
    }

    private List<OfferSkill> getSkillsFromForm(Long offerId) {
        List<OfferSkill> skills = new ArrayList<>();
        for (SkillRow row : skillRows) {
            String skillName = row.nameField.getText().trim();
            SkillLevel level = row.levelCombo.getValue();

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

    /**
     * Génère automatiquement une note de correction via l'API Grok
     */
    private String generateCorrectionNote(String warningReason, String warningMessage, String jobTitle, String jobDescription) {
        try {
            String prompt = String.format(
                "Tu es un recruteur qui a reçu un signalement sur son offre d'emploi. " +
                "Génère une courte note de correction (3-4 phrases) en français expliquant les modifications apportées pour résoudre le problème signalé. " +
                "Sois professionnel et concis.\n\n" +
                "Raison du signalement: %s\n" +
                "Message de l'admin: %s\n" +
                "Titre de l'offre: %s\n" +
                "Description de l'offre: %s\n\n" +
                "Génère uniquement la note de correction, sans introduction.",
                warningReason,
                warningMessage != null && warningMessage.length() > 200 ? warningMessage.substring(0, 200) + "..." : warningMessage,
                jobTitle != null ? jobTitle : "Non spécifié",
                jobDescription != null && jobDescription.length() > 200 ? jobDescription.substring(0, 200) + "..." : (jobDescription != null ? jobDescription : "Non spécifiée")
            );

            return callGrokAPI(prompt);
        } catch (Exception e) {
            System.err.println("Erreur génération note de correction: " + e.getMessage());
            return getDefaultCorrectionNote(warningReason);
        }
    }

    /**
     * Appelle l'API Grok pour les avertissements
     */
    private String callGrokAPI(String prompt) throws Exception {
        String apiKey = "xai-BvO5mSs05cHXwQRM1qa8Z7lojgfAMS0I6Kc9Y1R5lQYSyHWO6eDq62ZZ0QsajWkyyyB6f41ZD4HmWOCU";
        String apiUrl = "https://api.x.ai/v1/chat/completions";

        java.net.URL url = new java.net.URL(apiUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);

        String jsonBody = String.format(
            "{\"model\": \"grok-beta\", \"messages\": [{\"role\": \"user\", \"content\": \"%s\"}], \"max_tokens\": 500, \"temperature\": 0.7}",
            escapeJson(prompt)
        );

        try (java.io.OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            return extractGrokContent(response.toString());
        } else {
            throw new Exception("Erreur API Grok: " + responseCode);
        }
    }

    /**
     * Extrait le contenu de la réponse Grok
     */
    private String extractGrokContent(String jsonResponse) {
        try {
            int contentStart = jsonResponse.indexOf("\"content\":");
            if (contentStart == -1) return null;

            contentStart = jsonResponse.indexOf("\"", contentStart + 10) + 1;
            int contentEnd = jsonResponse.indexOf("\"", contentStart);

            while (contentEnd > 0 && jsonResponse.charAt(contentEnd - 1) == '\\') {
                contentEnd = jsonResponse.indexOf("\"", contentEnd + 1);
            }

            if (contentStart > 0 && contentEnd > contentStart) {
                String content = jsonResponse.substring(contentStart, contentEnd);
                content = content.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                return content.trim();
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing réponse Grok: " + e.getMessage());
        }
        return null;
    }

    /**
     * Appelle l'API Gemini pour l'auto-remplissage du formulaire
     */
    private String callGeminiAPI(String prompt) throws Exception {
        String apiKey = "AIzaSyA40pYJkW9p7QYQerVUv_rmS4pNFo1T46o";
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;

        java.net.URL url = new java.net.URL(apiUrl);
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(60000);

        // Construire le JSON de la requête pour Gemini
        String jsonBody = String.format(
            "{\"contents\": [{\"parts\": [{\"text\": \"%s\"}]}], \"generationConfig\": {\"maxOutputTokens\": 1000, \"temperature\": 0.7}}",
            escapeJson(prompt)
        );

        System.out.println("Appel API Gemini...");

        try (java.io.OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        System.out.println("Response code: " + responseCode);

        if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }
            String result = extractGeminiContent(response.toString());
            System.out.println("Réponse API: " + (result != null ? result.substring(0, Math.min(100, result.length())) + "..." : "null"));
            return result;
        } else {
            // Lire le message d'erreur
            StringBuilder errorResponse = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getErrorStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
            } catch (Exception e) {
                // Ignorer si pas d'error stream
            }
            System.err.println("Erreur API Gemini: " + responseCode + " - " + errorResponse);
            throw new Exception("Erreur API: " + responseCode);
        }
    }

    /**
     * Extrait le contenu de la réponse Gemini
     */
    private String extractGeminiContent(String jsonResponse) {
        try {
            // Format Gemini: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
            int textStart = jsonResponse.indexOf("\"text\":");
            if (textStart == -1) return null;

            textStart = jsonResponse.indexOf("\"", textStart + 7) + 1;
            int textEnd = textStart;
            int braceCount = 0;
            boolean inString = true;

            for (int i = textStart; i < jsonResponse.length(); i++) {
                char c = jsonResponse.charAt(i);
                if (c == '\\' && i + 1 < jsonResponse.length()) {
                    i++; // Skip escaped character
                    continue;
                }
                if (c == '"' && inString) {
                    textEnd = i;
                    break;
                }
            }

            if (textEnd > textStart) {
                String content = jsonResponse.substring(textStart, textEnd);
                content = content.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\")
                                .replace("\\t", "\t");
                return content.trim();
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing réponse Gemini: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extrait le contenu du message de la réponse JSON
     */
    private String extractContentFromResponse(String jsonResponse) {
        try {
            int contentStart = jsonResponse.indexOf("\"content\":");
            if (contentStart == -1) return null;

            contentStart = jsonResponse.indexOf("\"", contentStart + 10) + 1;
            int contentEnd = jsonResponse.indexOf("\"", contentStart);

            while (contentEnd > 0 && jsonResponse.charAt(contentEnd - 1) == '\\') {
                contentEnd = jsonResponse.indexOf("\"", contentEnd + 1);
            }

            if (contentStart > 0 && contentEnd > contentStart) {
                String content = jsonResponse.substring(contentStart, contentEnd);
                content = content.replace("\\n", "\n")
                                .replace("\\\"", "\"")
                                .replace("\\\\", "\\");
                return content.trim();
            }
        } catch (Exception e) {
            System.err.println("Erreur parsing réponse: " + e.getMessage());
        }
        return null;
    }

    /**
     * Échappe les caractères spéciaux pour JSON
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    /**
     * Note de correction par défaut si l'API échoue
     */
    private String getDefaultCorrectionNote(String reason) {
        return switch (reason) {
            case "Contenu inapproprié" ->
                "J'ai revu et corrigé le contenu de l'offre pour supprimer tout élément inapproprié. " +
                "Le texte a été reformulé de manière professionnelle et conforme aux normes de la plateforme.";
            case "Information trompeuse" ->
                "J'ai vérifié et corrigé les informations de l'offre pour garantir leur exactitude. " +
                "Les détails du poste, du salaire et des conditions ont été mis à jour.";
            case "Discrimination" ->
                "J'ai modifié l'offre pour supprimer tout critère discriminatoire. " +
                "L'offre est maintenant conforme aux lois sur l'égalité des chances.";
            case "Information incomplète" ->
                "J'ai complété l'offre avec toutes les informations nécessaires: " +
                "description du poste, qualifications requises, conditions de travail et avantages.";
            case "Offre en double" ->
                "J'ai supprimé le doublon et conservé uniquement cette version mise à jour de l'offre.";
            case "Offre expirée non mise à jour" ->
                "J'ai mis à jour la date limite de candidature et vérifié que le poste est toujours disponible.";
            case "Spam" ->
                "J'ai reformulé l'offre de manière professionnelle et pertinente. " +
                "Le contenu est maintenant approprié pour la plateforme.";
            default ->
                "J'ai effectué les corrections nécessaires suite au signalement. " +
                "L'offre a été revue et mise à jour pour répondre aux exigences de la plateforme.";
        };
    }

    /**
     * Génère des suggestions pour remplir le formulaire basé sur le titre du poste (utilise Gemini)
     */
    private String generateJobSuggestions(String jobTitle) {
        try {
            String prompt = String.format(
                "Tu es un expert RH. Génère les informations pour une offre d'emploi basée sur le titre: '%s'.\n\n" +
                "Réponds UNIQUEMENT dans ce format exact (sans autre texte):\n" +
                "DESCRIPTION: [description détaillée du poste en 4-5 phrases, responsabilités et qualifications]\n" +
                "SKILLS: [skill1, skill2, skill3, skill4, skill5]\n\n" +
                "Exemple pour 'Développeur Java':\n" +
                "DESCRIPTION: Nous recherchons un développeur Java passionné pour rejoindre notre équipe technique. Vous serez responsable du développement d'applications backend robustes et scalables. Vous participerez à la conception et à l'implémentation de nouvelles fonctionnalités. Une expérience avec les frameworks Spring est appréciée.\n" +
                "SKILLS: Java, Spring Boot, SQL, Git, REST API",
                jobTitle
            );

            String result = callGeminiAPI(prompt);
            if (result != null && !result.isEmpty()) {
                return result;
            }
            // Si l'API retourne null, utiliser le fallback
            return getDefaultJobSuggestions(jobTitle);
        } catch (Exception e) {
            System.err.println("Erreur génération suggestions: " + e.getMessage());
            e.printStackTrace();
            // Retourner des suggestions par défaut en cas d'erreur
            return getDefaultJobSuggestions(jobTitle);
        }
    }

    /**
     * Génère des suggestions par défaut basées sur le titre
     */
    private String getDefaultJobSuggestions(String jobTitle) {
        String titleLower = jobTitle.toLowerCase();

        // Suggestions par défaut basées sur des mots-clés
        if (titleLower.contains("développeur") || titleLower.contains("developer") || titleLower.contains("dev")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " passionné(e) pour rejoindre notre équipe technique. Vous serez responsable du développement et de la maintenance d'applications. Vous travaillerez en collaboration avec l'équipe pour concevoir des solutions innovantes. Maîtrise des bonnes pratiques de développement requise.\n" +
                   "SKILLS: Programmation, Git, Base de données, API REST, Méthodologie Agile";
        } else if (titleLower.contains("chef") || titleLower.contains("manager") || titleLower.contains("responsable")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " expérimenté(e) pour piloter nos projets stratégiques. Vous serez en charge de la coordination des équipes et du suivi des objectifs. Vous assurerez la communication avec les parties prenantes. Leadership et vision stratégique requis.\n" +
                   "SKILLS: Management, Gestion de projet, Communication, Leadership, Planification";
        } else if (titleLower.contains("commercial") || titleLower.contains("vente") || titleLower.contains("sales")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " dynamique pour développer notre portefeuille clients. Vous serez responsable de la prospection et de la fidélisation. Vous atteindrez les objectifs de vente fixés. Excellent sens du relationnel requis.\n" +
                   "SKILLS: Négociation, Prospection, CRM, Communication, Relation client";
        } else if (titleLower.contains("mécanicien") || titleLower.contains("mecanicien") || titleLower.contains("technicien")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " qualifié(e) pour assurer l'entretien et la réparation des équipements. Vous diagnostiquerez les pannes et effectuerez les interventions nécessaires. Vous veillerez au respect des normes de sécurité. Une expérience en maintenance industrielle est un plus.\n" +
                   "SKILLS: Diagnostic, Réparation, Maintenance préventive, Lecture de plans, Sécurité";
        } else if (titleLower.contains("comptable") || titleLower.contains("finance") || titleLower.contains("accounting")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " rigoureux(se) pour gérer la comptabilité de l'entreprise. Vous serez en charge de la tenue des comptes et des déclarations fiscales. Vous participerez aux clôtures mensuelles et annuelles. Maîtrise des outils comptables requise.\n" +
                   "SKILLS: Comptabilité, Excel, Fiscalité, SAP, Analyse financière";
        } else if (titleLower.contains("rh") || titleLower.contains("ressources humaines") || titleLower.contains("hr")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " pour renforcer notre équipe RH. Vous gérerez le recrutement et l'administration du personnel. Vous contribuerez au développement de la marque employeur. Connaissance du droit du travail appréciée.\n" +
                   "SKILLS: Recrutement, Droit du travail, SIRH, Communication, Gestion administrative";
        } else if (titleLower.contains("design") || titleLower.contains("graphi") || titleLower.contains("ux") || titleLower.contains("ui")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " créatif(ve) pour concevoir des interfaces utilisateur attractives. Vous créerez des maquettes et prototypes. Vous collaborerez avec les équipes techniques pour implémenter vos designs. Portfolio requis.\n" +
                   "SKILLS: Figma, Adobe Creative Suite, UX Design, Prototypage, Design System";
        } else if (titleLower.contains("data") || titleLower.contains("analyst") || titleLower.contains("bi")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " pour analyser nos données et fournir des insights stratégiques. Vous créerez des dashboards et rapports. Vous contribuerez à la prise de décision basée sur les données. Esprit analytique requis.\n" +
                   "SKILLS: SQL, Python, Power BI, Excel, Statistiques";
        } else {
            // Suggestion générique
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " motivé(e) pour rejoindre notre équipe. Vous contribuerez au développement de nos activités et participerez aux projets stratégiques de l'entreprise. Vous travaillerez dans un environnement dynamique et collaboratif. Bonne capacité d'adaptation requise.\n" +
                   "SKILLS: Communication, Travail en équipe, Organisation, Adaptabilité, Rigueur";
        }
    }

    /**
     * Parse la réponse AI et remplit le formulaire (uniquement description et compétences)
     */
    private void parseAndFillForm(String suggestions, String jobTitle) {
        if (suggestions == null || suggestions.isEmpty()) return;

        try {
            // Parser la description
            String description = extractField(suggestions, "DESCRIPTION:");
            if (description != null && !description.isEmpty()) {
                formDescription.setText(description);
            }

            // Parser les compétences
            String skillsStr = extractField(suggestions, "SKILLS:");
            if (skillsStr != null && !skillsStr.isEmpty()) {
                // Effacer les compétences existantes
                skillsContainer.getChildren().clear();
                skillRows.clear();

                // Ajouter les nouvelles compétences
                String[] skills = skillsStr.split(",");
                for (String skill : skills) {
                    String trimmedSkill = skill.trim();
                    if (!trimmedSkill.isEmpty() && trimmedSkill.length() >= 2) {
                        OfferSkill offerSkill = new OfferSkill(null, trimmedSkill, SkillLevel.INTERMEDIATE);
                        addSkillRow(offerSkill);
                    }
                }

                // Ajouter une ligne vide si aucune compétence
                if (skillRows.isEmpty()) {
                    addSkillRow(null);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur parsing suggestions: " + e.getMessage());
        }
    }

    /**
     * Extrait un champ de la réponse AI
     */
    private String extractField(String text, String fieldName) {
        int startIndex = text.indexOf(fieldName);
        if (startIndex == -1) return null;

        startIndex += fieldName.length();
        int endIndex = text.indexOf("\n", startIndex);
        if (endIndex == -1) endIndex = text.length();

        return text.substring(startIndex, endIndex).trim();
    }

    /**
     * Convertit une chaîne en ContractType
     */
    private ContractType parseContractType(String contract) {
        String upper = contract.toUpperCase().trim();
        return switch (upper) {
            case "CDI" -> ContractType.CDI;
            case "CDD" -> ContractType.CDD;
            case "INTERNSHIP", "STAGE" -> ContractType.INTERNSHIP;
            case "FREELANCE" -> ContractType.FREELANCE;
            case "PART_TIME", "TEMPS PARTIEL" -> ContractType.PART_TIME;
            case "FULL_TIME", "TEMPS PLEIN" -> ContractType.FULL_TIME;
            default -> ContractType.CDI;
        };
    }

    // Helper class to store skill row components
    private static class SkillRow {
        TextField nameField;
        ComboBox<SkillLevel> levelCombo;

        SkillRow(TextField nameField, ComboBox<SkillLevel> levelCombo) {
            this.nameField = nameField;
            this.levelCombo = levelCombo;
        }
    }
}

