package Controllers;

import Models.JobOffer;
import Models.JobOfferWarning;
import Models.OfferSkill;
import Models.ContractType;
import Models.Status;
import Models.SkillLevel;
import Services.JobOfferService;
import Services.JobOfferWarningService;
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
            warningSection.getChildren().add(alertHeader);

            // Liste des avertissements
            for (JobOfferWarning warning : warnings) {
                VBox warningCard = new VBox(8);
                warningCard.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8;");

                Label reasonLabel = new Label("📋 Raison: " + warning.getReason());
                reasonLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");

                Label messageLabel = new Label("💬 Message de l'admin:");
                messageLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #495057; -fx-font-size: 12px;");

                Label messageContent = new Label(warning.getMessage());
                messageContent.setWrapText(true);
                messageContent.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px; -fx-padding: 5 0 5 15;");

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
     * Marque les avertissements comme résolus après correction par le recruteur
     */
    private void handleMarkWarningsResolved(JobOffer job, List<JobOfferWarning> warnings) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Confirmer la correction");
        confirmation.setContentText("Confirmez-vous avoir corrigé le(s) problème(s) signalé(s) ?\n\n" +
                                   "L'administrateur sera informé de votre correction.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                for (JobOfferWarning warning : warnings) {
                    warningService.resolveWarning(warning.getId());
                }

                showAlert("Succès", "Les avertissements ont été marqués comme résolus.", Alert.AlertType.INFORMATION);

                // Recharger les données
                loadJobOffers();

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la résolution: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
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

        // Title field with help and error labels
        Label titleLabel = new Label("Job Title *");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formTitleField = new TextField();
        formTitleField.setPromptText("Job Title");
        formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label titleHelpLabel = new Label("ℹ️ 3-100 characters. Letters, numbers, spaces, and basic punctuation only.");
        titleHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        titleErrorLabel = new Label();
        titleErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        titleErrorLabel.setVisible(false);
        titleErrorLabel.setManaged(false);

        // Description field with help and error labels
        Label descLabel = new Label("Job Description *");
        descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formDescription = new TextArea();
        formDescription.setPromptText("Job Description");
        formDescription.setPrefRowCount(6);
        formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label descHelpLabel = new Label("ℹ️ 20-2000 characters. Provide detailed job requirements and responsibilities.");
        descHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        descriptionErrorLabel = new Label();
        descriptionErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        descriptionErrorLabel.setVisible(false);
        descriptionErrorLabel.setManaged(false);

        // Location field with help and error labels
        Label locationLabel = new Label("Location *");
        locationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formLocation = new TextField();
        formLocation.setPromptText("Location");
        formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label locationHelpLabel = new Label("ℹ️ 2-100 characters. City, country, or 'Remote'.");
        locationHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        locationErrorLabel = new Label();
        locationErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        locationErrorLabel.setVisible(false);
        locationErrorLabel.setManaged(false);

        // Contract Type
        Label contractLabel = new Label("Contract Type *");
        contractLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formContractType = new ComboBox<>();
        formContractType.getItems().addAll(ContractType.values());
        formContractType.setPromptText("Select Contract Type");
        formContractType.setStyle("-fx-font-size: 14px;");

        formStatus = new ComboBox<>();
        formStatus.getItems().addAll(Status.values());
        formStatus.setValue(Status.OPEN);
        formStatus.setStyle("-fx-font-size: 14px;");

        formDeadline = new DatePicker();
        formDeadline.setPromptText("Deadline (optional)");
        formDeadline.setStyle("-fx-font-size: 14px;");

        Label deadlineHelpLabel = new Label("ℹ️ Must be a future date. Leave empty if no specific deadline.");
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

        Label skillsLabel = new Label("Required Skills *");
        skillsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        Label skillsHelpLabel = new Label("ℹ️ Add at least one skill. Skill name: 2-50 chars. Only letters, numbers, spaces, -, +, #, . allowed.");
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
                titleLabel, formTitleField, titleHelpLabel, titleErrorLabel,
                descLabel, formDescription, descHelpLabel, descriptionErrorLabel,
                locationLabel, formLocation, locationHelpLabel, locationErrorLabel,
                contractLabel, formContractType,
                new Label("Status *"), formStatus,
                new Label("Deadline (Optional)"), formDeadline, deadlineHelpLabel, deadlineErrorLabel,
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

