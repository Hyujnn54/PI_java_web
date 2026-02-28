package Controllers.joboffers;

import Models.joboffers.JobOffer;
import Models.joboffers.JobOfferWarning;
import Models.joboffers.WarningCorrection;
import Models.joboffers.OfferSkill;
import Models.joboffers.ContractType;
import Models.joboffers.Status;
import Services.joboffers.JobOfferService;
import Services.joboffers.JobOfferWarningService;
import Services.joboffers.WarningCorrectionService;
import Services.joboffers.OfferSkillService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour la vue Admin - Gestion des offres d'emploi
 * Système de signalement au lieu de suppression directe
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
    private ComboBox<String> cbFilterType;
    private ComboBox<String> cbFilterLocation;
    private ComboBox<String> cbFilterStatus;
    private Label notificationBadge;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;
    private JobOfferWarningService warningService;
    private WarningCorrectionService correctionService;

    // Filtres actifs
    private ContractType selectedContractType = null;
    private Status selectedStatus = null;
    private String selectedLocation = null;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        offerSkillService = new OfferSkillService();
        warningService = new JobOfferWarningService();
        correctionService = new WarningCorrectionService();
        buildUI();
        loadJobOffers();
    }

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 20;");

        // === EN-TÊTE ADMIN ===
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label adminBadge = new Label("⚙️ ADMINISTRATION");
        adminBadge.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 8 16; " +
                           "-fx-background-radius: 8; -fx-font-weight: 700; -fx-font-size: 14px;");

        Label pageTitle = new Label("Gestion des offres d'emploi");
        pageTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Bouton notifications pour les corrections en attente
        Button btnCorrections = new Button("📬 Corrections en attente");
        btnCorrections.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: 600; " +
                               "-fx-padding: 8 16; -fx-background-radius: 8; -fx-cursor: hand;");
        btnCorrections.setOnAction(e -> showPendingCorrectionsDialog());

        // Badge de notification
        notificationBadge = new Label("0");
        notificationBadge.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 11px; " +
                                  "-fx-font-weight: 700; -fx-padding: 2 6; -fx-background-radius: 10;");
        notificationBadge.setVisible(false);

        StackPane notificationPane = new StackPane();
        notificationPane.getChildren().addAll(btnCorrections, notificationBadge);
        StackPane.setAlignment(notificationBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(notificationBadge, new Insets(-5, -5, 0, 0));

        headerBox.getChildren().addAll(adminBadge, pageTitle, spacer, notificationPane);
        mainContainer.getChildren().add(headerBox);
        mainContainer.getChildren().add(new Region() {{ setPrefHeight(15); }});

        // Mettre à jour le compteur de notifications
        updateNotificationBadge();

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
        txtSearch.setPromptText("Rechercher dans toutes les offres...");
        txtSearch.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 10 15; " +
                          "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-font-size: 13px; -fx-pref-height: 38;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setOnAction(e -> handleSearch());

        Button btnSearchAction = new Button("Rechercher");
        btnSearchAction.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-size: 13px; " +
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

        // Type de contrat
        cbFilterType = new ComboBox<>();
        cbFilterType.setPromptText("Type");
        cbFilterType.getItems().add("Tous");
        for (ContractType type : ContractType.values()) {
            cbFilterType.getItems().add(formatContractType(type));
        }
        cbFilterType.setStyle("-fx-pref-width: 130; -fx-pref-height: 34;");
        cbFilterType.setOnAction(e -> applyFilters());

        // Localisation
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

        // Statut
        cbFilterStatus = new ComboBox<>();
        cbFilterStatus.setPromptText("Statut");
        cbFilterStatus.getItems().addAll("Tous", "Ouvert", "Fermé");
        cbFilterStatus.setStyle("-fx-pref-width: 110; -fx-pref-height: 34;");
        cbFilterStatus.setOnAction(e -> applyFilters());

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

        filterRow.getChildren().addAll(filterLabel, cbFilterType, cbFilterLocation, cbFilterStatus, btnReset, spacer, resultCount);

        container.getChildren().addAll(searchRow, separator, filterRow);
        return container;
    }

    private void applyFilters() {
        String typeValue = cbFilterType.getValue();
        String locationValue = cbFilterLocation.getValue();
        String statusValue = cbFilterStatus.getValue();

        selectedContractType = (typeValue == null || typeValue.equals("Tous")) ? null : getContractTypeFromLabel(typeValue);
        selectedLocation = (locationValue == null || locationValue.equals("Tous")) ? null : locationValue;

        if (statusValue == null || statusValue.equals("Tous")) {
            selectedStatus = null;
        } else {
            selectedStatus = statusValue.equals("Ouvert") ? Status.OPEN : Status.CLOSED;
        }

        loadFilteredJobOffers();
    }

    private void resetFilters() {
        selectedContractType = null;
        selectedLocation = null;
        selectedStatus = null;
        if (cbFilterType != null) cbFilterType.setValue(null);
        if (cbFilterLocation != null) cbFilterLocation.setValue(null);
        if (cbFilterStatus != null) cbFilterStatus.setValue(null);
        if (txtSearch != null) txtSearch.clear();
        loadJobOffers();
    }

    private void loadFilteredJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            List<JobOffer> jobs = jobOfferService.filterJobOffers(selectedLocation, selectedContractType, selectedStatus);

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
            showAlert("Erreur", "Impossible de charger les offres: " + e.getMessage(), Alert.AlertType.ERROR);
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

        Label title = new Label("📋 Toutes les offres");
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

        Label title = new Label("📄 Détails");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(15);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

        VBox selectMessage = new VBox(8);
        selectMessage.setAlignment(Pos.CENTER);
        selectMessage.setStyle("-fx-padding: 40;");
        Label icon = new Label("👈");
        icon.setStyle("-fx-font-size: 32px;");
        Label text = new Label("Sélectionnez une offre");
        text.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px;");
        selectMessage.getChildren().addAll(icon, text);
        detailContainer.getChildren().add(selectMessage);

        panel.getChildren().addAll(title, scrollPane);
        return panel;
    }

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            List<JobOffer> jobs = jobOfferService.getAllJobOffers();
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
            showAlert("Erreur", "Impossible de charger les offres: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(6);
        String bgColor = job.isFlagged() ? "#fff3cd" : "#f8f9fa";
        String borderStyle = job.isFlagged() ? "-fx-border-color: #ffc107; -fx-border-width: 1; -fx-border-radius: 8;" : "";
        card.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand; " + borderStyle);

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(formatContractType(job.getContractType()));
        typeBadge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 2 8; " +
                          "-fx-background-radius: 10; -fx-font-size: 10px;");

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

        Label statusBadge = new Label(statusText);
        statusBadge.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: " +
                            (job.isFlagged() ? "#212529" : "white") + "; -fx-padding: 2 8; " +
                            "-fx-background-radius: 10; -fx-font-size: 10px;");

        badges.getChildren().addAll(typeBadge, statusBadge);

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "N/A"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        card.getChildren().addAll(title, badges, location);

        final String normalBgColor = bgColor;
        final String normalBorderStyle = borderStyle;

        card.setOnMouseEntered(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: " + (job.isFlagged() ? "#ffecb5" : "#e9ecef") + "; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand; " + normalBorderStyle);
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: " + normalBgColor + "; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand; " + normalBorderStyle);
            }
        });

        card.setOnMouseClicked(e -> selectJob(job, card));
        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 12; -fx-cursor: hand;");
            }
        });

        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 12; " +
                     "-fx-border-color: #dc3545; -fx-border-width: 2; -fx-border-radius: 8; -fx-cursor: hand;");

        selectedJob = job;
        displayJobDetails(job);
    }

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        // En-tête
        VBox headerCard = new VBox(10);
        headerCard.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 18;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        FlowPane metaFlow = new FlowPane(8, 6);
        metaFlow.getChildren().addAll(
            createMetaBadge("💼 " + formatContractType(job.getContractType()), "#e3f2fd", "#1976d2"),
            createMetaBadge("📍 " + (job.getLocation() != null ? job.getLocation() : "N/A"), "#f3e5f5", "#7b1fa2"),
            createMetaBadge("📊 " + (job.getStatus() == Status.OPEN ? "Ouvert" : "Fermé"),
                           job.getStatus() == Status.OPEN ? "#e8f5e9" : "#ffebee",
                           job.getStatus() == Status.OPEN ? "#2e7d32" : "#c62828"),
            createMetaBadge("👤 ID: " + job.getRecruiterId(), "#fff3e0", "#e65100")
        );

        if (job.getDeadline() != null) {
            metaFlow.getChildren().add(createMetaBadge("⏰ " + job.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), "#ffebee", "#c62828"));
        }

        headerCard.getChildren().addAll(title, metaFlow);
        detailContainer.getChildren().add(headerCard);

        // Description
        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            VBox descSection = new VBox(8);
            descSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15;");
            Label descTitle = new Label("📝 Description");
            descTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
            Label descText = new Label(job.getDescription());
            descText.setWrapText(true);
            descText.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px;");
            descSection.getChildren().addAll(descTitle, descText);
            detailContainer.getChildren().add(descSection);
        }

        // Compétences
        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(job.getId());
            if (!skills.isEmpty()) {
                VBox skillsSection = new VBox(8);
                skillsSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 15;");
                Label skillsTitle = new Label("🎯 Compétences");
                skillsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
                FlowPane skillsFlow = new FlowPane(6, 6);
                for (OfferSkill skill : skills) {
                    Label skillTag = new Label(skill.getSkillName());
                    skillTag.setStyle("-fx-background-color: white; -fx-padding: 5 10; -fx-background-radius: 6; " +
                                     "-fx-border-color: #dee2e6; -fx-border-radius: 6; -fx-font-size: 11px;");
                    skillsFlow.getChildren().add(skillTag);
                }
                skillsSection.getChildren().addAll(skillsTitle, skillsFlow);
                detailContainer.getChildren().add(skillsSection);
            }
        } catch (SQLException e) {
            System.err.println("Erreur: " + e.getMessage());
        }

        // Afficher les avertissements existants si l'offre est signalée
        if (job.isFlagged()) {
            displayExistingWarnings(job);
        }

        // Boutons d'action Admin
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setStyle("-fx-padding: 20 0;");

        // Bouton Signaler (principal)
        Button btnFlag = new Button("⚠️ Signaler cette offre");
        btnFlag.setStyle("-fx-background-color: #ffc107; -fx-text-fill: #212529; -fx-font-weight: 600; " +
                        "-fx-font-size: 13px; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;");
        btnFlag.setOnAction(e -> showFlagDialog(job));

        // Si déjà signalée, option de retirer le signalement
        if (job.isFlagged()) {
            Button btnUnflag = new Button("✓ Retirer le signalement");
            btnUnflag.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                              "-fx-font-size: 13px; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand;");
            btnUnflag.setOnAction(e -> handleUnflagOffer(job));
            actionBox.getChildren().addAll(btnFlag, btnUnflag);
        } else {
            actionBox.getChildren().add(btnFlag);
        }

        detailContainer.getChildren().add(actionBox);

        // Info Admin
        String infoText = job.isFlagged()
            ? "🚨 Cette offre est actuellement signalée - Le recruteur a été notifié"
            : "💡 Mode Admin - Signalez une offre pour notifier le recruteur d'un problème";
        String infoBgColor = job.isFlagged() ? "#f8d7da" : "#fff3cd";
        String infoTextColor = job.isFlagged() ? "#721c24" : "#856404";

        Label adminNote = new Label(infoText);
        adminNote.setStyle("-fx-text-fill: " + infoTextColor + "; -fx-font-size: 11px; -fx-background-color: " + infoBgColor + "; " +
                          "-fx-padding: 10 15; -fx-background-radius: 6;");
        adminNote.setWrapText(true);
        detailContainer.getChildren().add(adminNote);
    }

    /**
     * Affiche les avertissements existants pour une offre
     */
    private void displayExistingWarnings(JobOffer job) {
        try {
            List<JobOfferWarning> warnings = warningService.getWarningsByJobOfferId(job.getId());
            if (!warnings.isEmpty()) {
                VBox warningsSection = new VBox(10);
                warningsSection.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 8; -fx-padding: 15;");

                Label warningsTitle = new Label("⚠️ Signalements (" + warnings.size() + ")");
                warningsTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #856404;");
                warningsSection.getChildren().add(warningsTitle);

                for (JobOfferWarning warning : warnings) {
                    VBox warningCard = new VBox(8);
                    warningCard.setStyle("-fx-background-color: white; -fx-padding: 12; -fx-background-radius: 6; " +
                                        "-fx-border-color: #ffc107; -fx-border-radius: 6;");
                    warningCard.setMaxWidth(Double.MAX_VALUE);

                    HBox headerRow = new HBox(10);
                    headerRow.setAlignment(Pos.CENTER_LEFT);

                    Label reasonLabel = new Label("📋 " + warning.getReason());
                    reasonLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #856404;");

                    Label statusLabel = new Label(warning.getStatusLabel());
                    String statusColor = switch (warning.getStatus()) {
                        case SENT -> "#17a2b8";
                        case SEEN -> "#ffc107";
                        case RESOLVED -> "#28a745";
                        case DISMISSED -> "#6c757d";
                    };
                    statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: white; " +
                                        "-fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10px;");

                    headerRow.getChildren().addAll(reasonLabel, statusLabel);

                    // Zone de texte pour le message complet
                    TextArea messageArea = new TextArea(warning.getMessage());
                    messageArea.setWrapText(true);
                    messageArea.setEditable(false);
                    messageArea.setPrefRowCount(3);
                    messageArea.setStyle("-fx-control-inner-background: #f8f9fa; -fx-text-fill: #495057; " +
                                        "-fx-font-size: 12px; -fx-border-color: transparent;");

                    Label dateLabel = new Label("Signalé le " + warning.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    dateLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 10px;");

                    warningCard.getChildren().addAll(headerRow, messageArea, dateLabel);
                    warningsSection.getChildren().add(warningCard);
                }

                detailContainer.getChildren().add(warningsSection);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement des avertissements: " + e.getMessage());
        }
    }

    /**
     * Affiche le dialogue pour signaler une offre
     */
    private void showFlagDialog(JobOffer job) {
        Dialog<JobOfferWarning> dialog = new Dialog<>();
        dialog.setTitle("Signaler une offre");
        dialog.setHeaderText("Signaler: " + job.getTitle());

        // Boutons
        ButtonType flagButtonType = new ButtonType("Signaler", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(flagButtonType, ButtonType.CANCEL);

        // Contenu
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        // Raison du signalement
        Label reasonLabel = new Label("Raison du signalement:");
        reasonLabel.setStyle("-fx-font-weight: 600;");

        ComboBox<String> reasonCombo = new ComboBox<>();
        reasonCombo.getItems().addAll(
            "Contenu inapproprié",
            "Information trompeuse",
            "Discrimination",
            "Information incomplète",
            "Offre en double",
            "Offre expirée non mise à jour",
            "Spam",
            "Autre"
        );
        reasonCombo.setPromptText("Sélectionnez une raison...");
        reasonCombo.setMaxWidth(Double.MAX_VALUE);

        // Message détaillé avec bouton de génération
        HBox messageLabelBox = new HBox(10);
        messageLabelBox.setAlignment(Pos.CENTER_LEFT);

        Label messageLabel = new Label("Message pour le recruteur:");
        messageLabel.setStyle("-fx-font-weight: 600;");

        Button btnGenerate = new Button("🤖 Générer avec IA");
        btnGenerate.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-size: 11px; " +
                           "-fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");
        btnGenerate.setDisable(true);

        messageLabelBox.getChildren().addAll(messageLabel, btnGenerate);

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Expliquez le problème au recruteur...\nOu cliquez sur 'Générer avec IA' pour un message automatique.");
        messageArea.setPrefRowCount(5);
        messageArea.setWrapText(true);

        // Label de chargement
        Label loadingLabel = new Label("");
        loadingLabel.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");

        // Action du bouton de génération
        btnGenerate.setOnAction(e -> {
            String selectedReason = reasonCombo.getValue();
            if (selectedReason != null) {
                loadingLabel.setText("⏳ Génération en cours...");
                btnGenerate.setDisable(true);

                new Thread(() -> {
                    try {
                        String generatedMessage = generateWarningMessageWithAI(
                            selectedReason,
                            job.getTitle(),
                            job.getDescription()
                        );

                        javafx.application.Platform.runLater(() -> {
                            if (generatedMessage != null && !generatedMessage.isEmpty()) {
                                messageArea.setText(generatedMessage);
                                loadingLabel.setText("✅ Message généré avec succès");
                            } else {
                                messageArea.setText(getDefaultWarningMessage(selectedReason, job.getTitle()));
                                loadingLabel.setText("ℹ️ Message par défaut utilisé");
                            }
                            btnGenerate.setDisable(false);
                        });
                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() -> {
                            messageArea.setText(getDefaultWarningMessage(selectedReason, job.getTitle()));
                            loadingLabel.setText("⚠️ IA indisponible - message par défaut utilisé");
                            btnGenerate.setDisable(false);
                        });
                    }
                }).start();
            }
        });

        // Activer le bouton de génération quand une raison est sélectionnée
        reasonCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            btnGenerate.setDisable(newVal == null);
            loadingLabel.setText("");
        });

        // Info
        Label infoLabel = new Label("💡 Le recruteur sera notifié et devra corriger ou supprimer son offre.");
        infoLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
        infoLabel.setWrapText(true);

        content.getChildren().addAll(reasonLabel, reasonCombo, messageLabelBox, messageArea, loadingLabel, infoLabel);
        dialog.getDialogPane().setContent(content);

        // Validation
        Button flagButton = (Button) dialog.getDialogPane().lookupButton(flagButtonType);
        flagButton.setDisable(true);

        reasonCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            flagButton.setDisable(newVal == null || messageArea.getText().trim().isEmpty());
        });
        messageArea.textProperty().addListener((obs, oldVal, newVal) -> {
            flagButton.setDisable(reasonCombo.getValue() == null || newVal.trim().isEmpty());
        });

        // Résultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == flagButtonType) {
                JobOfferWarning warning = new JobOfferWarning(
                    job.getId(),
                    job.getRecruiterId(),
                    UserContext.getAdminId() != null ? UserContext.getAdminId() : 1L,
                    reasonCombo.getValue(),
                    messageArea.getText().trim()
                );
                return warning;
            }
            return null;
        });

        Optional<JobOfferWarning> result = dialog.showAndWait();
        result.ifPresent(warning -> {
            try {
                warningService.createWarning(warning);
                showAlert("Succès", "L'offre a été signalée. Le recruteur sera notifié.", Alert.AlertType.INFORMATION);
                loadJobOffers();
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors du signalement: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    /**
     * Retire le signalement d'une offre
     */
    private void handleUnflagOffer(JobOffer job) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Retirer le signalement ?");
        confirmation.setContentText("Tous les avertissements en attente seront annulés.\n\n" +
                                   "Offre: " + job.getTitle());

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // Marquer tous les avertissements comme annulés
                List<JobOfferWarning> warnings = warningService.getPendingWarningsByJobOfferId(job.getId());
                for (JobOfferWarning warning : warnings) {
                    warningService.dismissWarning(warning.getId());
                }

                showAlert("Succès", "Le signalement a été retiré.", Alert.AlertType.INFORMATION);
                loadJobOffers();
            } catch (SQLException e) {
                showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Met à jour le badge de notification
     */
    private void updateNotificationBadge() {
        try {
            int count = correctionService.countPendingCorrections();
            if (notificationBadge != null) {
                notificationBadge.setText(String.valueOf(count));
                notificationBadge.setVisible(count > 0);
            }
        } catch (SQLException e) {
            System.err.println("Erreur comptage corrections: " + e.getMessage());
        }
    }

    /**
     * Affiche le dialogue des corrections en attente
     */
    private void showPendingCorrectionsDialog() {
        try {
            List<WarningCorrection> corrections = correctionService.getPendingCorrections();

            if (corrections.isEmpty()) {
                showAlert("Information", "Aucune correction en attente de validation.", Alert.AlertType.INFORMATION);
                return;
            }

            // Créer un dialogue pour afficher les corrections
            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("Corrections en attente");
            dialog.setHeaderText("📬 " + corrections.size() + " correction(s) en attente de validation");
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(700);
            dialog.getDialogPane().setPrefHeight(500);

            VBox content = new VBox(15);
            content.setPadding(new Insets(10));

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background: transparent;");

            VBox correctionsList = new VBox(15);
            correctionsList.setPadding(new Insets(5));

            for (WarningCorrection correction : corrections) {
                VBox card = createCorrectionCard(correction, dialog);
                correctionsList.getChildren().add(card);
            }

            scrollPane.setContent(correctionsList);
            content.getChildren().add(scrollPane);
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            dialog.getDialogPane().setContent(content);
            dialog.showAndWait();

        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement des corrections: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Crée une carte pour afficher une correction
     */
    private VBox createCorrectionCard(WarningCorrection correction, Dialog<Void> parentDialog) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 10; " +
                     "-fx-border-color: #17a2b8; -fx-border-radius: 10; -fx-border-width: 2;");

        // En-tête
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("📝");
        icon.setStyle("-fx-font-size: 20px;");

        VBox headerText = new VBox(2);
        Label titleLabel = new Label("Correction pour l'offre #" + correction.getJobOfferId());
        titleLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Label dateLabel = new Label("Soumise le " + correction.getSubmittedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");

        headerText.getChildren().addAll(titleLabel, dateLabel);
        header.getChildren().addAll(icon, headerText);

        // Note du recruteur
        VBox noteBox = new VBox(5);
        noteBox.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 10; -fx-background-radius: 5;");

        Label noteTitle = new Label("💬 Note du recruteur:");
        noteTitle.setStyle("-fx-font-weight: 600; -fx-text-fill: #1976d2;");

        TextArea noteContent = new TextArea(correction.getCorrectionNote());
        noteContent.setEditable(false);
        noteContent.setWrapText(true);
        noteContent.setPrefRowCount(3);
        noteContent.setStyle("-fx-control-inner-background: transparent;");

        noteBox.getChildren().addAll(noteTitle, noteContent);

        // Détails de l'offre corrigée
        VBox offerDetails = new VBox(5);
        offerDetails.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 5;");

        Label offerTitle = new Label("📋 Offre corrigée:");
        offerTitle.setStyle("-fx-font-weight: 600; -fx-text-fill: #495057;");

        Label newTitleLabel = new Label("Titre: " + (correction.getNewTitle() != null ? correction.getNewTitle() : "N/A"));
        newTitleLabel.setStyle("-fx-text-fill: #2c3e50;");

        String descPreview = correction.getNewDescription() != null
            ? (correction.getNewDescription().length() > 150
                ? correction.getNewDescription().substring(0, 150) + "..."
                : correction.getNewDescription())
            : "Aucune description";
        Label newDescLabel = new Label("Description: " + descPreview);
        newDescLabel.setWrapText(true);
        newDescLabel.setStyle("-fx-text-fill: #495057;");

        offerDetails.getChildren().addAll(offerTitle, newTitleLabel, newDescLabel);

        // Boutons d'action
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnApprove = new Button("✅ Approuver et republier");
        btnApprove.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnApprove.setOnAction(e -> handleApproveCorrection(correction, parentDialog));

        Button btnReject = new Button("❌ Rejeter");
        btnReject.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnReject.setOnAction(e -> handleRejectCorrection(correction, parentDialog));

        Button btnViewOffer = new Button("👁️ Voir l'offre");
        btnViewOffer.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: 600; " +
                            "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
        btnViewOffer.setOnAction(e -> viewOfferFromCorrection(correction));

        actions.getChildren().addAll(btnViewOffer, btnReject, btnApprove);

        card.getChildren().addAll(header, noteBox, offerDetails, actions);
        return card;
    }

    /**
     * Approuver une correction
     */
    private void handleApproveCorrection(WarningCorrection correction, Dialog<Void> parentDialog) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmer l'approbation");
        confirmation.setHeaderText("Approuver cette correction ?");
        confirmation.setContentText("L'offre sera republiée automatiquement.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                correctionService.approveCorrection(correction.getId(), "Correction approuvée");
                showAlert("Succès", "La correction a été approuvée.\nL'offre a été republiée.", Alert.AlertType.INFORMATION);

                parentDialog.close();
                updateNotificationBadge();
                loadJobOffers();

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de l'approbation: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    /**
     * Rejeter une correction
     */
    private void handleRejectCorrection(WarningCorrection correction, Dialog<Void> parentDialog) {
        TextInputDialog noteDialog = new TextInputDialog();
        noteDialog.setTitle("Rejeter la correction");
        noteDialog.setHeaderText("Expliquez pourquoi la correction est rejetée");
        noteDialog.setContentText("Raison du rejet:");

        Optional<String> result = noteDialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            try {
                correctionService.rejectCorrection(correction.getId(), result.get());
                showAlert("Information", "La correction a été rejetée.\nLe recruteur devra soumettre une nouvelle correction.", Alert.AlertType.INFORMATION);

                parentDialog.close();
                updateNotificationBadge();
                loadJobOffers();

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors du rejet: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Attention", "Veuillez indiquer une raison pour le rejet.", Alert.AlertType.WARNING);
        }
    }

    /**
     * Voir l'offre associée à une correction
     */
    private void viewOfferFromCorrection(WarningCorrection correction) {
        try {
            JobOffer job = jobOfferService.getJobOfferById(correction.getJobOfferId());
            if (job != null) {
                displayJobDetails(job);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Erreur lors du chargement de l'offre: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private Label createMetaBadge(String text, String bgColor, String textColor) {
        Label badge = new Label(text);
        badge.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; " +
                      "-fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 600;");
        return badge;
    }

    @FXML
    private void handleSearch() {
        loadFilteredJobOffers();
    }

    @FXML
    private void handleClearSearch() {
        resetFilters();
    }

    /**
     * Generates a warning message using the Groq AI API
     */
    private String generateWarningMessageWithAI(String reason, String jobTitle, String jobDescription) {
        try {
            String GROQ_API_KEY = "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";
            String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";
            String[] MODELS     = {"llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"};

            String descPreview = (jobDescription != null && jobDescription.length() > 300)
                    ? jobDescription.substring(0, 300) + "..." : jobDescription;

            String prompt = String.format(
                "Tu es un administrateur RH professionnel. Rédige un message d'avertissement formel et constructif " +
                "(150-250 mots, en français) adressé au recruteur concernant son offre d'emploi.\n\n" +
                "Raison du signalement: %s\n" +
                "Titre de l'offre: %s\n" +
                "Description: %s\n\n" +
                "Le message doit:\n" +
                "1. Expliquer clairement le problème détecté\n" +
                "2. Mentionner les points spécifiques à corriger\n" +
                "3. Indiquer les conséquences si non corrigé\n" +
                "4. Rester professionnel et constructif\n\n" +
                "Génère UNIQUEMENT le message, sans introduction ni titre.",
                reason, jobTitle, descPreview != null ? descPreview : "Non fournie"
            );

            for (String model : MODELS) {
                try {
                    java.net.URL url = new java.net.URL(GROQ_URL);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(30000);

                    String body = "{\"model\":\"" + model + "\",\"messages\":[" +
                            "{\"role\":\"system\",\"content\":\"Tu es un assistant administrateur RH professionnel.\"}," +
                            "{\"role\":\"user\",\"content\":\"" + escapeJson(prompt) + "\"}]," +
                            "\"max_tokens\":400,\"temperature\":0.7}";

                    conn.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                    if (conn.getResponseCode() == 200) {
                        StringBuilder sb = new StringBuilder();
                        try (java.io.BufferedReader br = new java.io.BufferedReader(
                                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                            String line; while ((line = br.readLine()) != null) sb.append(line);
                        }
                        String json = sb.toString();
                        int s = json.indexOf("\"content\":"); if (s < 0) continue;
                        s = json.indexOf("\"", s + 10) + 1;
                        int e2 = json.indexOf("\"", s);
                        while (e2 > 0 && json.charAt(e2 - 1) == '\\') e2 = json.indexOf("\"", e2 + 1);
                        if (s > 0 && e2 > s) {
                            String content = json.substring(s, e2)
                                    .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                            if (!content.isBlank()) return content;
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Groq model " + model + " failed: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("generateWarningMessageWithAI error: " + e.getMessage());
        }
        return null;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                   .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /**
     * Returns a professional default warning message when AI is unavailable
     */
    private String getDefaultWarningMessage(String reason, String jobTitle) {
        String base = "Madame, Monsieur,\n\n" +
            "Nous vous informons que votre offre d'emploi intitulée \"" + jobTitle + "\" a été signalée par notre équipe d'administration pour la raison suivante : ";
        String end = "\n\nNous vous demandons de prendre les mesures correctives nécessaires dans les meilleurs délais. " +
            "Sans correction de votre part, cette offre pourra être retirée de la plateforme.\n\n" +
            "Cordialement,\nL'équipe d'administration";

        return switch (reason) {
            case "Contenu inapproprié" -> base +
                "contenu inapproprié détecté dans l'offre.\n\nPoints à corriger :\n" +
                "- Supprimez tout contenu offensant, discriminatoire ou contraire à nos conditions d'utilisation\n" +
                "- Reformulez les sections problématiques de manière professionnelle\n" +
                "- Assurez-vous que le contenu respecte la législation en vigueur" + end;
            case "Information trompeuse" -> base +
                "informations trompeuses ou inexactes détectées.\n\nPoints à corriger :\n" +
                "- Vérifiez et corrigez les informations sur le salaire, les avantages ou les conditions\n" +
                "- Assurez-vous que le titre du poste correspond aux missions décrites\n" +
                "- Supprimez toute promesse irréaliste ou non vérifiable" + end;
            case "Discrimination" -> base +
                "critères discriminatoires détectés.\n\nPoints à corriger :\n" +
                "- Supprimez toute mention d'âge, sexe, origine, religion ou état civil non pertinente\n" +
                "- Reformulez les exigences pour qu'elles se basent uniquement sur les compétences professionnelles\n" +
                "- Assurez-vous de la conformité avec la législation anti-discrimination" + end;
            case "Information incomplète" -> base +
                "informations essentielles manquantes.\n\nPoints à corriger :\n" +
                "- Complétez la description des missions et responsabilités\n" +
                "- Précisez les qualifications et compétences requises\n" +
                "- Ajoutez les informations sur le lieu de travail et le type de contrat" + end;
            case "Offre en double" -> base +
                "doublon détecté avec une ou plusieurs offres existantes.\n\nPoints à corriger :\n" +
                "- Supprimez les offres en double\n" +
                "- Consolidez les informations dans une seule offre mise à jour\n" +
                "- Différenciez clairement cette offre si les postes sont distincts" + end;
            case "Offre expirée non mise à jour" -> base +
                "offre expirée non mise à jour.\n\nPoints à corriger :\n" +
                "- Mettez à jour la date limite de candidature\n" +
                "- Confirmez que le poste est toujours disponible\n" +
                "- Fermez l'offre si le poste a été pourvu" + end;
            case "Spam" -> base +
                "contenu de type spam ou publicité non autorisée.\n\nPoints à corriger :\n" +
                "- Reformulez l'offre pour qu'elle corresponde à un vrai poste à pourvoir\n" +
                "- Supprimez tout contenu publicitaire ou promotionnel non lié au poste\n" +
                "- Respectez les règles de publication de notre plateforme" + end;
            default -> base +
                reason + ".\n\nNous vous prions de revoir votre offre et d'apporter les corrections nécessaires " +
                "pour qu'elle soit conforme aux règles de notre plateforme." + end;
        };
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

