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
import Services.NominatimMapService;
import Services.NominatimMapService.GeoLocation;
import Services.FuzzySearchService;
import Services.NotificationService;
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
import java.util.Timer;
import java.util.TimerTask;

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
    private NominatimMapService mapService;

    // Form elements
    private TextField formTitleField;
    private TextArea formDescription;
    private TextField formLocation;
    private ComboBox<ContractType> formContractType;
    private DatePicker formDeadline;
    private ComboBox<Status> formStatus;

    // G├⌐olocalisation
    private Double selectedLatitude = null;
    private Double selectedLongitude = null;
    private ListView<GeoLocation> locationSuggestions;
    private Timer autocompleteTimer;

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
        mapService = new NominatimMapService();
        skillRows = new ArrayList<>();
        buildUI();
        loadJobOffers();
        checkForWarnings(); // V├⌐rifier les avertissements au d├⌐marrage
    }

    /**
     * V├⌐rifie s'il y a des avertissements en attente pour ce recruteur
     */
    private void checkForWarnings() {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            int warningCount = warningService.countPendingWarningsForRecruiter(recruiterId);

            if (warningCount > 0) {
                showWarningNotification(warningCount);
            }
        } catch (SQLException e) {
            System.err.println("Erreur v├⌐rification avertissements: " + e.getMessage());
        }
    }

    /**
     * Affiche une notification d'avertissement
     */
    private void showWarningNotification(int count) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("ΓÜá∩╕Å Attention");
        alert.setHeaderText("Vous avez " + count + " offre(s) signal├⌐e(s)");
        alert.setContentText("Un administrateur a signal├⌐ un probl├¿me avec certaines de vos offres.\n\n" +
                            "Veuillez consulter les offres marqu├⌐es en jaune et les corriger ou les supprimer.");
        alert.show();
    }

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 20;");

        // === EN-T├èTE RECRUTEUR ===
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label recruiterBadge = new Label("≡ƒæñ ESPACE RECRUTEUR");
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

        // RIGHT: D├⌐tails
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

        Label searchIcon = new Label("≡ƒöì");
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

        // S├⌐parateur
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

        Button btnReset = new Button("Γ£ò R├⌐initialiser");
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
            resultCount.setText(count == 0 ? "Aucun r├⌐sultat" : count + " offre(s)");
        }
    }

    private VBox createEmptyState() {
        VBox emptyBox = new VBox(10);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 30;");
        Label icon = new Label("≡ƒô¡");
        icon.setStyle("-fx-font-size: 40px;");
        Label text = new Label("Aucune offre trouv├⌐e");
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

        Label title = new Label("≡ƒôï Mes offres d'emploi");
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

        Label title = new Label("≡ƒôä D├⌐tails");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button btnCreate = new Button("Γ₧ò Nouvelle offre");
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
            showAlert("Erreur", "Impossible de charger les offres : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(10);

        // Style diff├⌐rent si l'offre est signal├⌐e
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

        Label location = new Label("≡ƒôì " + (job.getLocation() != null ? job.getLocation() : "Non sp├⌐cifi├⌐"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px;");

        // Badge de statut avec gestion du statut FLAGGED
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        String statusColor;
        String statusText;
        if (job.isFlagged() || job.getStatus() == Status.FLAGGED) {
            statusColor = "#ffc107";
            statusText = "ΓÜá∩╕Å Signal├⌐";
        } else if (job.getStatus() == Status.OPEN) {
            statusColor = "#28a745";
            statusText = "Ouvert";
        } else {
            statusColor = "#dc3545";
            statusText = "Ferm├⌐";
        }

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle("-fx-background-color: " + statusColor + "; -fx-text-fill: " +
                            (job.isFlagged() ? "#212529" : "white") + "; -fx-padding: 2 6; " +
                            "-fx-background-radius: 4; -fx-font-size: 10px;");
        statusRow.getChildren().add(statusLabel);

        // Message d'alerte si signal├⌐
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

            // En-t├¬te d'alerte
            HBox alertHeader = new HBox(10);
            alertHeader.setAlignment(Pos.CENTER_LEFT);

            Label alertIcon = new Label("≡ƒÜ¿");
            alertIcon.setStyle("-fx-font-size: 24px;");

            VBox alertTextBox = new VBox(3);
            Label alertTitle = new Label("Action requise - Offre signal├⌐e");
            alertTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #721c24;");

            Label alertSubtitle = new Label("Un administrateur a signal├⌐ un probl├¿me avec cette offre. Veuillez corriger ou supprimer l'offre.");
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

                Label reasonLabel = new Label("≡ƒôï Raison: " + warning.getReason());
                reasonLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #2c3e50;");
                reasonLabel.setWrapText(true);

                Label messageLabel = new Label("≡ƒÆ¼ Message de l'admin:");
                messageLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #495057; -fx-font-size: 12px;");

                // Zone de texte pour le message complet
                TextArea messageContent = new TextArea(warning.getMessage());
                messageContent.setWrapText(true);
                messageContent.setEditable(false);
                messageContent.setPrefRowCount(4);
                messageContent.setStyle("-fx-control-inner-background: #f8f9fa; -fx-text-fill: #495057; " +
                                       "-fx-font-size: 13px; -fx-border-color: #dee2e6; -fx-border-radius: 5; " +
                                       "-fx-background-radius: 5;");

                Label dateLabel = new Label("≡ƒôà Signal├⌐ le " + warning.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy ├á HH:mm")));
                dateLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

                warningCard.getChildren().addAll(reasonLabel, messageLabel, messageContent, dateLabel);
                warningSection.getChildren().add(warningCard);
            }

            // Boutons d'action
            HBox actionButtons = new HBox(15);
            actionButtons.setAlignment(Pos.CENTER);
            actionButtons.setStyle("-fx-padding: 10 0 0 0;");

            Button btnEdit = new Button("Γ£Å∩╕Å Modifier l'offre");
            btnEdit.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: 600; " +
                            "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnEdit.setOnAction(e -> showEditForm(job));

            Button btnDelete = new Button("≡ƒùæ∩╕Å Supprimer l'offre");
            btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                              "-fx-padding: 10 20; -fx-background-radius: 6; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> handleDeleteJobOffer(job));

            Button btnMarkResolved = new Button("Γ£ô J'ai corrig├⌐ le probl├¿me");
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
     * Ouvre le dialogue pour soumettre une correction ├á l'admin
     */
    private void handleMarkWarningsResolved(JobOffer job, List<JobOfferWarning> warnings) {
        // Cr├⌐er un dialogue pour soumettre la correction
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
        Label offerLabel = new Label("≡ƒôï Offre: " + job.getTitle());
        offerLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");

        // Explication
        Label infoLabel = new Label("ΓÜá∩╕Å Votre correction sera envoy├⌐e ├á l'administrateur pour validation. " +
                                   "Une fois approuv├⌐e, votre offre sera republi├⌐e.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-text-fill: #856404; -fx-background-color: #fff3cd; -fx-padding: 10; " +
                          "-fx-background-radius: 5;");

        // Note de correction avec bouton de g├⌐n├⌐ration
        HBox noteLabelBox = new HBox(10);
        noteLabelBox.setAlignment(Pos.CENTER_LEFT);

        Label noteLabel = new Label("≡ƒô¥ Description des corrections:");
        noteLabel.setStyle("-fx-font-weight: 600;");

        Button btnGenerateNote = new Button("≡ƒñû G├⌐n├⌐rer automatiquement");
        btnGenerateNote.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-size: 11px; " +
                                "-fx-padding: 5 10; -fx-background-radius: 5; -fx-cursor: hand;");

        noteLabelBox.getChildren().addAll(noteLabel, btnGenerateNote);

        TextArea correctionNote = new TextArea();
        correctionNote.setPromptText("Cliquez sur 'G├⌐n├⌐rer automatiquement' pour cr├⌐er une description des changements...");
        correctionNote.setPrefRowCount(5);
        correctionNote.setWrapText(true);

        // Label de chargement
        Label loadingLabel = new Label("");
        loadingLabel.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");

        // R├⌐cup├⌐rer la raison du signalement
        String warningReason = !warnings.isEmpty() ? warnings.get(0).getReason() : "Non sp├⌐cifi├⌐";
        String warningMessage = !warnings.isEmpty() ? warnings.get(0).getMessage() : "";

        // Action du bouton de g├⌐n├⌐ration
        btnGenerateNote.setOnAction(e -> {
            loadingLabel.setText("ΓÅ│ G├⌐n├⌐ration en cours...");
            btnGenerateNote.setDisable(true);

            // Ex├⌐cuter dans un thread s├⌐par├⌐
            new Thread(() -> {
                try {
                    String generatedNote = generateCorrectionNote(
                        warningReason,
                        warningMessage,
                        job.getTitle(),
                        job.getDescription()
                    );

                    // Mettre ├á jour l'UI dans le thread JavaFX
                    javafx.application.Platform.runLater(() -> {
                        if (generatedNote != null && !generatedNote.isEmpty()) {
                            correctionNote.setText(generatedNote);
                            loadingLabel.setText("Γ£à Description g├⌐n├⌐r├⌐e avec succ├¿s");
                        } else {
                            loadingLabel.setText("ΓÜá∩╕Å Impossible de g├⌐n├⌐rer, veuillez ├⌐crire manuellement");
                        }
                        btnGenerateNote.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        loadingLabel.setText("Γ¥î Erreur: " + ex.getMessage());
                        btnGenerateNote.setDisable(false);
                    });
                }
            }).start();
        });

        // R├⌐sum├⌐ des modifications
        VBox changesBox = new VBox(10);
        changesBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 15; -fx-background-radius: 8;");

        Label changesTitle = new Label("≡ƒôè R├⌐sum├⌐ de l'offre actuelle:");
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

        // R├⌐sultat
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                WarningCorrection correction = new WarningCorrection();
                correction.setJobOfferId(job.getId());
                correction.setRecruiterId(UserContext.getRecruiterId());
                correction.setCorrectionNote(correctionNote.getText().trim());
                correction.setNewTitle(job.getTitle());
                correction.setNewDescription(job.getDescription());
                // Le warningId sera d├⌐fini pour le premier warning
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

                showAlert("Succ├¿s",
                    "Votre correction a ├⌐t├⌐ soumise ├á l'administrateur.\n\n" +
                    "Vous serez notifi├⌐ une fois qu'elle sera valid├⌐e et votre offre sera republi├⌐e.",
                    Alert.AlertType.INFORMATION);

                // Recharger les donn├⌐es
                loadJobOffers();

            } catch (SQLException e) {
                showAlert("Erreur", "Erreur lors de la soumission: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        // Afficher les avertissements en premier si l'offre est signal├⌐e
        if (job.isFlagged()) {
            displayWarningsForRecruiter(job);
        }

        VBox headerCard = new VBox(15);
        headerCard.setStyle("-fx-background-color: #F8F9FA; -fx-background-radius: 10; -fx-padding: 25;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label contractType = new Label("≡ƒÆ╝ " + formatContractType(job.getContractType()));
        contractType.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        Label location = new Label("≡ƒôì " + (job.getLocation() != null ? job.getLocation() : "Non sp├⌐cifi├⌐"));
        location.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 14px; -fx-font-weight: 600;");

        String statusColor;
        String statusText;
        if (job.isFlagged() || job.getStatus() == Status.FLAGGED) {
            statusColor = "#ffc107";
            statusText = "ΓÜá∩╕Å Signal├⌐";
        } else if (job.getStatus() == Status.OPEN) {
            statusColor = "#28a745";
            statusText = "Ouvert";
        } else {
            statusColor = "#dc3545";
            statusText = "Ferm├⌐";
        }

        Label status = new Label("≡ƒôè " + statusText);
        status.setStyle("-fx-text-fill: " + statusColor + "; -fx-font-size: 14px; -fx-font-weight: 700;");

        metaRow.getChildren().addAll(contractType, location, status);

        if (job.getDeadline() != null) {
            Label deadline = new Label("ΓÅ░ Date limite: " + job.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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
            System.err.println("Erreur lors du chargement des comp├⌐tences : " + e.getMessage());
        }

        // Date de publication
        if (job.getCreatedAt() != null) {
            Label posted = new Label("Publi├⌐ le: " + job.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            posted.setStyle("-fx-text-fill: #8e9ba8; -fx-font-size: 12px; -fx-padding: 15 0;");
            detailContainer.getChildren().add(posted);
        }

        // Section Localisation avec bouton carte (toujours visible si location existe)
        if (job.getLocation() != null && !job.getLocation().trim().isEmpty()) {
            VBox locationSection = new VBox(10);
            locationSection.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 10; -fx-padding: 15;");

            HBox locationInfo = new HBox(15);
            locationInfo.setAlignment(Pos.CENTER_LEFT);

            Label locationIcon = new Label("≡ƒôì");
            locationIcon.setStyle("-fx-font-size: 20px;");

            Label locationText = new Label(job.getLocation());
            locationText.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2e7d32;");
            HBox.setHgrow(locationText, Priority.ALWAYS);

            Button btnMap = new Button("≡ƒù║∩╕Å Voir sur la carte");
            btnMap.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-font-size: 13px; -fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
            btnMap.setOnAction(e -> showMapDialog(job));

            locationInfo.getChildren().addAll(locationIcon, locationText, btnMap);
            locationSection.getChildren().add(locationInfo);

            // Afficher les coordonn├⌐es si disponibles
            if (job.hasCoordinates()) {
                Label coordsLabel = new Label(String.format("Coordonn├⌐es: %.4f, %.4f", job.getLatitude(), job.getLongitude()));
                coordsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
                locationSection.getChildren().add(coordsLabel);
            }

            detailContainer.getChildren().add(locationSection);
        }

        // Action buttons
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setStyle("-fx-padding: 25 0;");

        Button btnEdit = new Button("Γ£Å∩╕Å Modifier");
        btnEdit.setStyle("-fx-background-color: #ffc107; -fx-text-fill: white; -fx-font-weight: 600; " +
                        "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnEdit.setOnAction(e -> showEditForm(job));

        Button btnDelete = new Button("≡ƒùæ∩╕Å Supprimer");
        btnDelete.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnDelete.setOnAction(e -> handleDeleteJobOffer(job));

        Button btnToggleStatus = new Button(job.getStatus() == Status.OPEN ? "≡ƒöÆ Fermer" : "≡ƒöô Ouvrir");
        btnToggleStatus.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: 600; " +
                                "-fx-font-size: 14px; -fx-padding: 10 20; -fx-background-radius: 8; -fx-cursor: hand;");
        btnToggleStatus.setOnAction(e -> handleToggleStatus(job));

        actionButtons.getChildren().addAll(btnEdit, btnDelete, btnToggleStatus);
        detailContainer.getChildren().add(actionButtons);
    }

    /**
     * Affiche une fen├¬tre modale avec la carte Leaflet (WebView) de l'entreprise
     * G├⌐ocode automatiquement si les coordonn├⌐es ne sont pas disponibles
     */
    private void showMapDialog(JobOffer job) {
        double lat, lon;

        if (job.hasCoordinates()) {
            // Utiliser les coordonn├⌐es existantes
            lat = job.getLatitude();
            lon = job.getLongitude();
            MapViewController.showMap(lat, lon, job.getLocation(), job.getTitle());
        } else if (job.getLocation() != null && !job.getLocation().trim().isEmpty()) {
            // G├⌐ocoder la localisation
            showAlert("Information", "G├⌐ocodage de la localisation en cours...", Alert.AlertType.INFORMATION);

            new Thread(() -> {
                NominatimMapService.GeoLocation geoResult = mapService.geocode(job.getLocation());

                javafx.application.Platform.runLater(() -> {
                    if (geoResult != null) {
                        // Mettre ├á jour les coordonn├⌐es dans la base de donn├⌐es
                        try {
                            job.setLatitude(geoResult.getLatitude());
                            job.setLongitude(geoResult.getLongitude());
                            jobOfferService.updateJobOffer(job);

                            // Afficher la carte
                            MapViewController.showMap(
                                geoResult.getLatitude(),
                                geoResult.getLongitude(),
                                job.getLocation(),
                                job.getTitle()
                            );
                        } catch (SQLException e) {
                            System.err.println("Erreur mise ├á jour coordonn├⌐es: " + e.getMessage());
                            // Afficher quand m├¬me la carte
                            MapViewController.showMap(
                                geoResult.getLatitude(),
                                geoResult.getLongitude(),
                                job.getLocation(),
                                job.getTitle()
                            );
                        }
                    } else {
                        showAlert("Erreur",
                            "Impossible de trouver les coordonn├⌐es pour: " + job.getLocation() +
                            "\n\nVeuillez modifier l'offre et s├⌐lectionner une ville dans les suggestions.",
                            Alert.AlertType.WARNING);
                    }
                });
            }).start();
        } else {
            showAlert("Erreur", "Aucune localisation d├⌐finie pour cette offre.", Alert.AlertType.WARNING);
        }
    }

    private void showCreateForm() {
        isEditMode = false;
        editingJob = null;
        showJobForm("Cr├⌐er une offre");
    }

    private void showEditForm(JobOffer job) {
        // Check if the current user owns this job offer
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refus├⌐e", "Vous ne pouvez modifier que vos propres offres.", Alert.AlertType.WARNING);
            return;
        }

        isEditMode = true;
        editingJob = job;
        showJobForm("Modifier l'offre");
    }

    private void showJobForm(String formTitle) {
        detailContainer.getChildren().clear();

        Button btnBack = new Button("ΓåÉ Retour");
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
        formTitleField.setPromptText("Ex: D├⌐veloppeur Java Senior, Chef de projet IT...");
        formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        HBox.setHgrow(formTitleField, Priority.ALWAYS);

        Button btnAISuggest = new Button("≡ƒñû AI Suggest");
        btnAISuggest.setStyle("-fx-background-color: #6f42c1; -fx-text-fill: white; -fx-font-weight: 600; " +
                             "-fx-padding: 10 15; -fx-background-radius: 6; -fx-cursor: hand;");

        Label aiStatusLabel = new Label("");
        aiStatusLabel.setStyle("-fx-text-fill: #6f42c1; -fx-font-size: 11px;");

        titleRow.getChildren().addAll(formTitleField, btnAISuggest);

        Label titleHelpLabel = new Label("Γä╣∩╕Å Entrez le titre puis cliquez 'AI Suggest' pour remplir automatiquement le formulaire");
        titleHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        titleErrorLabel = new Label();
        titleErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        titleErrorLabel.setVisible(false);
        titleErrorLabel.setManaged(false);

        // Description field with help and error labels
        Label descLabel = new Label("Description du poste *");
        descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        formDescription = new TextArea();
        formDescription.setPromptText("Description d├⌐taill├⌐e du poste...");
        formDescription.setPrefRowCount(6);
        formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        Label descHelpLabel = new Label("Γä╣∩╕Å 20-2000 caract├¿res. D├⌐crivez les responsabilit├⌐s et exigences.");
        descHelpLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-padding: 2 0 0 5;");

        descriptionErrorLabel = new Label();
        descriptionErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 2 0 0 5;");
        descriptionErrorLabel.setVisible(false);
        descriptionErrorLabel.setManaged(false);

        // Location field with autocomplete
        Label locationLabel = new Label("Localisation *");
        locationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        // Container pour le champ location avec autocomplete
        VBox locationContainer = new VBox(0);

        formLocation = new TextField();
        formLocation.setPromptText("Tapez pour rechercher une ville...");
        formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");

        // Liste de suggestions
        locationSuggestions = new ListView<>();
        locationSuggestions.setPrefHeight(150);
        locationSuggestions.setVisible(false);
        locationSuggestions.setManaged(false);
        locationSuggestions.setStyle("-fx-background-color: white; -fx-border-color: #5BA3F5; -fx-border-radius: 0 0 5 5;");

        // R├⌐initialiser les coordonn├⌐es
        selectedLatitude = null;
        selectedLongitude = null;

        // Autocomplete avec d├⌐lai
        formLocation.textProperty().addListener((obs, oldVal, newVal) -> {
            if (autocompleteTimer != null) {
                autocompleteTimer.cancel();
            }

            // R├⌐initialiser les coordonn├⌐es si l'utilisateur modifie le texte
            if (!newVal.equals(oldVal)) {
                selectedLatitude = null;
                selectedLongitude = null;
            }

            if (newVal.length() >= 2 && !newVal.equalsIgnoreCase("remote")) {
                autocompleteTimer = new Timer();
                autocompleteTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        javafx.application.Platform.runLater(() -> {
                            List<GeoLocation> suggestions = mapService.searchLocations(newVal);
                            if (!suggestions.isEmpty()) {
                                locationSuggestions.getItems().clear();
                                locationSuggestions.getItems().addAll(suggestions);
                                locationSuggestions.setVisible(true);
                                locationSuggestions.setManaged(true);
                            } else {
                                locationSuggestions.setVisible(false);
                                locationSuggestions.setManaged(false);
                            }
                        });
                    }
                }, 300);
            } else {
                locationSuggestions.setVisible(false);
                locationSuggestions.setManaged(false);
            }
        });

        // Label pour afficher les coordonn├⌐es s├⌐lectionn├⌐es (d├⌐clar├⌐ ici pour ├¬tre accessible dans les listeners)
        Label coordsLabel = new Label("");
        coordsLabel.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px; -fx-font-weight: 600;");
        coordsLabel.setVisible(false);

        // S├⌐lection d'une suggestion
        locationSuggestions.setOnMouseClicked(e -> {
            GeoLocation selected = locationSuggestions.getSelectionModel().getSelectedItem();
            if (selected != null) {
                formLocation.setText(selected.getFullLocation());
                selectedLatitude = selected.getLatitude();
                selectedLongitude = selected.getLongitude();
                locationSuggestions.setVisible(false);
                locationSuggestions.setManaged(false);
                coordsLabel.setText("≡ƒôì Coordonn├⌐es: " + String.format("%.4f, %.4f", selectedLatitude, selectedLongitude));
                coordsLabel.setVisible(true);
            }
        });

        locationContainer.getChildren().addAll(formLocation, locationSuggestions);

        // Bouton pour choisir sur la carte
        Button btnPickOnMap = new Button("≡ƒù║∩╕Å Choisir sur la carte");
        btnPickOnMap.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-weight: 600; " +
                             "-fx-padding: 10 15; -fx-background-radius: 6; -fx-cursor: hand;");

        // Action du bouton pour ouvrir le s├⌐lecteur de carte
        btnPickOnMap.setOnAction(e -> {
            LocationPickerController.pickLocation((lat, lon, address) -> {
                // Remplir le champ de localisation avec l'adresse
                formLocation.setText(address);
                selectedLatitude = lat;
                selectedLongitude = lon;
                coordsLabel.setText("≡ƒôì Coordonn├⌐es: " + String.format("%.4f, %.4f", lat, lon));
                coordsLabel.setVisible(true);

                // Fermer la liste de suggestions si ouverte
                locationSuggestions.setVisible(false);
                locationSuggestions.setManaged(false);
            });
        });

        // Ligne avec le champ et le bouton carte
        HBox locationRow = new HBox(10);
        locationRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(locationContainer, Priority.ALWAYS);
        locationRow.getChildren().addAll(locationContainer, btnPickOnMap);

        Label locationHelpLabel = new Label("Γä╣∩╕Å Tapez une ville ou cliquez sur 'Choisir sur la carte' pour s├⌐lectionner la localisation");
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
        formContractType.setPromptText("S├⌐lectionner le type de contrat");;
        formContractType.setStyle("-fx-font-size: 14px;");

        formStatus = new ComboBox<>();
        formStatus.getItems().addAll(Status.values());
        formStatus.setValue(Status.OPEN);
        formStatus.setStyle("-fx-font-size: 14px;");

        formDeadline = new DatePicker();
        formDeadline.setPromptText("Date limite (optionnel)");
        formDeadline.setStyle("-fx-font-size: 14px;");

        Label deadlineHelpLabel = new Label("Γä╣∩╕Å Doit ├¬tre une date future. Laissez vide si pas de date limite.");
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

        Label skillsLabel = new Label("Comp├⌐tences requises *");
        skillsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

        Label skillsHelpLabel = new Label("Γä╣∩╕Å Ajoutez au moins une comp├⌐tence. 2-50 caract├¿res.");
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
            selectedLatitude = editingJob.getLatitude();
            selectedLongitude = editingJob.getLongitude();
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
                showAlert("Erreur", "Impossible de charger les comp├⌐tences : " + e.getMessage(), Alert.AlertType.ERROR);
            }
        } else {
            // Add one empty skill row for new offers
            addSkillRow(null);
        }

        // Action du bouton AI Suggest
        btnAISuggest.setOnAction(e -> {
            String jobTitle = formTitleField.getText().trim();
            if (jobTitle.length() < 3) {
                showAlert("Attention", "Veuillez entrer un titre de poste valide (minimum 3 caract├¿res)", Alert.AlertType.WARNING);
                return;
            }

            aiStatusLabel.setText("ΓÅ│ G├⌐n├⌐ration en cours...");
            btnAISuggest.setDisable(true);

            // Ex├⌐cuter dans un thread s├⌐par├⌐
            new Thread(() -> {
                try {
                    String suggestions = generateJobSuggestions(jobTitle);

                    javafx.application.Platform.runLater(() -> {
                        if (suggestions != null) {
                            parseAndFillForm(suggestions, jobTitle);
                            aiStatusLabel.setText("Γ£à Formulaire rempli avec succ├¿s!");
                        } else {
                            aiStatusLabel.setText("ΓÜá∩╕Å Impossible de g├⌐n├⌐rer les suggestions");
                        }
                        btnAISuggest.setDisable(false);
                    });
                } catch (Exception ex) {
                    javafx.application.Platform.runLater(() -> {
                        aiStatusLabel.setText("Γ¥î Erreur: " + ex.getMessage());
                        btnAISuggest.setDisable(false);
                    });
                }
            }).start();
        });

        Button btnSubmit = new Button(isEditMode ? "Mettre ├á jour l'offre" : "Cr├⌐er l'offre");
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
                locationLabel, locationRow, coordsLabel, locationHelpLabel, locationErrorLabel,
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
        skillName.setPromptText("Nom de la comp├⌐tence (ex: Java, JavaScript)");
        skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px;");

        Label skillErrorLabel = new Label();
        skillErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-weight: bold;");
        skillErrorLabel.setVisible(false);
        skillErrorLabel.setManaged(false);

        // Validation des comp├⌐tences
        skillName.textProperty().addListener((obs, oldVal, newVal) -> {
            skillErrorLabel.setVisible(false);
            skillErrorLabel.setManaged(false);

            if (newVal.length() > 50) {
                skillName.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 2) {
                skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                skillErrorLabel.setText("ΓÜá∩╕Å Minimum 2 caract├¿res");
                skillErrorLabel.setVisible(true);
                skillErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty() && !newVal.matches("^[\\p{L}\\p{N}\\s\\-+#.]+$")) {
                skillName.setStyle("-fx-padding: 8; -fx-font-size: 13px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                skillErrorLabel.setText("Γ¥î Seuls lettres, chiffres, -, +, #, . sont autoris├⌐s");
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

        Button btnRemove = new Button("Γ£ò");
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
            newJob.setLatitude(selectedLatitude);
            newJob.setLongitude(selectedLongitude);
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

            showAlert("Succ├¿s", "Offre d'emploi cr├⌐├⌐e avec succ├¿s!", Alert.AlertType.INFORMATION);
            loadJobOffers();

            // Select the newly created job
            selectedJob = savedJob;
            displayJobDetails(savedJob);

        } catch (SQLException e) {
            showAlert("Erreur", "├ëchec de la cr├⌐ation: " + e.getMessage(), Alert.AlertType.ERROR);
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
            editingJob.setLatitude(selectedLatitude);
            editingJob.setLongitude(selectedLongitude);
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

                showAlert("Succ├¿s", "Offre mise ├á jour avec succ├¿s!", Alert.AlertType.INFORMATION);
                loadJobOffers();
                selectedJob = editingJob;
                displayJobDetails(editingJob);
            } else {
                showAlert("Erreur", "├ëchec de la mise ├á jour", Alert.AlertType.ERROR);
            }

        } catch (SQLException e) {
            showAlert("Erreur", "├ëchec de la mise ├á jour: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void handleDeleteJobOffer(JobOffer job) {
        // Check if the current user owns this job offer
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refus├⌐e", "Vous ne pouvez supprimer que les offres que vous avez cr├⌐├⌐es.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Delete");
        confirmation.setHeaderText("Supprimer l'offre d'emploi");
        confirmation.setContentText("├ètes-vous s├╗r de vouloir supprimer cette offre d'emploi ? Cette action est irr├⌐versible.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean deleted = jobOfferService.deleteJobOffer(job.getId());
                if (deleted) {
                    showAlert("Succ├¿s", "Offre d'emploi supprim├⌐e avec succ├¿s !", Alert.AlertType.INFORMATION);
                    selectedJob = null;
                    detailContainer.getChildren().clear();
                    Label noSelection = new Label("S├⌐lectionnez une offre pour voir les d├⌐tails");
                    noSelection.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 16px;");
                    detailContainer.getChildren().add(noSelection);
                    loadJobOffers();
                } else {
                    showAlert("Erreur", "Impossible de supprimer l'offre", Alert.AlertType.ERROR);
                }
            } catch (SQLException e) {
                showAlert("Erreur", "Impossible de supprimer l'offre : " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void handleToggleStatus(JobOffer job) {
        // Check if the current user owns this job offer
        if (!job.getRecruiterId().equals(UserContext.getRecruiterId())) {
            showAlert("Permission refus├⌐e", "Vous ne pouvez changer le statut que des offres que vous avez cr├⌐├⌐es.", Alert.AlertType.WARNING);
            return;
        }

        try {
            Status newStatus = job.getStatus() == Status.OPEN
                ? Status.CLOSED
                : Status.OPEN;

            boolean updated = jobOfferService.updateJobOfferStatus(job.getId(), newStatus);
            if (updated) {
                job.setStatus(newStatus);
                String statusLabel = newStatus == Status.OPEN ? "Ouverte" : "Ferm├⌐e";
                showAlert("Succ├¿s", "Le statut de l'offre a ├⌐t├⌐ mis ├á jour : " + statusLabel, Alert.AlertType.INFORMATION);
                loadJobOffers();
                displayJobDetails(job);
            } else {
                showAlert("Erreur", "Impossible de mettre ├á jour le statut", Alert.AlertType.ERROR);
            }
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de mettre ├á jour le statut : " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private boolean validateForm() {
        String title = formTitleField.getText().trim();
        String description = formDescription.getText().trim();
        String location = formLocation.getText().trim();

        // Validate Title
        if (title.isEmpty()) {
            showAlert("Erreur de validation", "Le titre du poste est requis", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }
        if (title.length() < 3) {
            showAlert("Erreur de validation", "Le titre doit contenir au moins 3 caract├¿res", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }
        if (title.length() > 100) {
            showAlert("Erreur de validation", "Le titre ne doit pas d├⌐passer 100 caract├¿res", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }
        if (!title.matches("^[\\p{L}\\p{N}\\s\\-\\/&,.]+$")) {
            showAlert("Erreur de validation", "Le titre contient des caract├¿res non autoris├⌐s.\nSeuls les lettres, chiffres, espaces et ponctuations de base sont accept├⌐s.", Alert.AlertType.WARNING);
            formTitleField.requestFocus();
            return false;
        }

        // Validation de la description
        if (description.isEmpty()) {
            showAlert("Erreur de validation", "La description du poste est requise", Alert.AlertType.WARNING);
            formDescription.requestFocus();
            return false;
        }
        if (description.length() < 20) {
            showAlert("Erreur de validation", "La description doit contenir au moins 20 caract├¿res", Alert.AlertType.WARNING);
            formDescription.requestFocus();
            return false;
        }
        if (description.length() > 2000) {
            showAlert("Erreur de validation", "La description ne doit pas d├⌐passer 2000 caract├¿res", Alert.AlertType.WARNING);
            formDescription.requestFocus();
            return false;
        }

        // Validation de la localisation
        if (location.isEmpty()) {
            showAlert("Erreur de validation", "La localisation est requise", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }
        if (location.length() < 2) {
            showAlert("Erreur de validation", "La localisation doit contenir au moins 2 caract├¿res", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }
        if (location.length() > 100) {
            showAlert("Erreur de validation", "La localisation ne doit pas d├⌐passer 100 caract├¿res", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }
        if (!location.matches("^[\\p{L}\\p{N}\\s\\-,./]+$")) {
            showAlert("Erreur de validation", "La localisation contient des caract├¿res non autoris├⌐s.\nSeuls les lettres, chiffres, espaces et ponctuations de base sont accept├⌐s.", Alert.AlertType.WARNING);
            formLocation.requestFocus();
            return false;
        }

        // Validation du type de contrat
        if (formContractType.getValue() == null) {
            showAlert("Erreur de validation", "Veuillez s├⌐lectionner un type de contrat", Alert.AlertType.WARNING);
            formContractType.requestFocus();
            return false;
        }

        // Validation du statut
        if (formStatus.getValue() == null) {
            showAlert("Erreur de validation", "Veuillez s├⌐lectionner un statut", Alert.AlertType.WARNING);
            formStatus.requestFocus();
            return false;
        }

        // Validation de la date limite (si fournie)
        if (formDeadline.getValue() != null) {
            if (formDeadline.getValue().isBefore(java.time.LocalDate.now())) {
                showAlert("Erreur de validation", "La date limite ne peut pas ├¬tre dans le pass├⌐", Alert.AlertType.WARNING);
                formDeadline.requestFocus();
                return false;
            }
        }

        // Validation des comp├⌐tences
        boolean hasValidSkill = false;
        for (SkillRow row : skillRows) {
            String skillName = row.nameField.getText().trim();
            if (!skillName.isEmpty()) {
                if (skillName.length() < 2) {
                    showAlert("Erreur de validation", "Le nom de la comp├⌐tence doit contenir au moins 2 caract├¿res", Alert.AlertType.WARNING);
                    row.nameField.requestFocus();
                    return false;
                }
                if (skillName.length() > 50) {
                    showAlert("Erreur de validation", "Le nom de la comp├⌐tence ne doit pas d├⌐passer 50 caract├¿res", Alert.AlertType.WARNING);
                    row.nameField.requestFocus();
                    return false;
                }
                if (!skillName.matches("^[\\p{L}\\p{N}\\s\\-+#.]+$")) {
                    showAlert("Erreur de validation", "La comp├⌐tence '" + skillName + "' contient des caract├¿res non autoris├⌐s.\nSeuls les lettres, chiffres, espaces et -, +, #, . sont accept├⌐s.", Alert.AlertType.WARNING);
                    row.nameField.requestFocus();
                    return false;
                }
                if (row.levelCombo.getValue() == null) {
                    showAlert("Erreur de validation", "Veuillez s├⌐lectionner un niveau pour la comp├⌐tence : " + skillName, Alert.AlertType.WARNING);
                    row.levelCombo.requestFocus();
                    return false;
                }
                hasValidSkill = true;
            }
        }

        if (!hasValidSkill) {
            showAlert("Erreur de validation", "Veuillez ajouter au moins une comp├⌐tence pour cette offre", Alert.AlertType.WARNING);
            return false;
        }

        return true;
    }

    private void addValidationListeners() {
        // Validation du titre - max 100 caract├¿res
        formTitleField.textProperty().addListener((obs, oldVal, newVal) -> {
            titleErrorLabel.setVisible(false);
            titleErrorLabel.setManaged(false);

            if (newVal.length() > 100) {
                formTitleField.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 3) {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                titleErrorLabel.setText("ΓÜá∩╕Å Le titre doit contenir au moins 3 caract├¿res");
                titleErrorLabel.setVisible(true);
                titleErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty() && !newVal.matches("^[\\p{L}\\p{N}\\s\\-/&,.]+$")) {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                titleErrorLabel.setText("Γ¥î Caract├¿res non autoris├⌐s. Utilisez uniquement lettres, chiffres, espaces et -, /, &, , .");
                titleErrorLabel.setVisible(true);
                titleErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty()) {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                formTitleField.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
            }
        });

        // Validation de la description - max 2000 caract├¿res
        formDescription.textProperty().addListener((obs, oldVal, newVal) -> {
            descriptionErrorLabel.setVisible(false);
            descriptionErrorLabel.setManaged(false);

            if (newVal.length() > 2000) {
                formDescription.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 20) {
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                descriptionErrorLabel.setText("ΓÜá∩╕Å La description doit contenir au moins 20 caract├¿res (actuellement " + newVal.length() + ")");
                descriptionErrorLabel.setVisible(true);
                descriptionErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty()) {
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                formDescription.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
            }
        });

        // Validation de la localisation - max 100 caract├¿res
        formLocation.textProperty().addListener((obs, oldVal, newVal) -> {
            locationErrorLabel.setVisible(false);
            locationErrorLabel.setManaged(false);

            if (newVal.length() > 100) {
                formLocation.setText(oldVal);
            }
            if (!newVal.isEmpty() && newVal.length() < 2) {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #ffc107; -fx-border-width: 2;");
                locationErrorLabel.setText("ΓÜá∩╕Å La localisation doit contenir au moins 2 caract├¿res");
                locationErrorLabel.setVisible(true);
                locationErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty() && !newVal.matches("^[\\p{L}\\p{N}\\s\\-,./]+$")) {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                locationErrorLabel.setText("Γ¥î Caract├¿res non autoris├⌐s. Utilisez uniquement lettres, chiffres, espaces et -, , . /");
                locationErrorLabel.setVisible(true);
                locationErrorLabel.setManaged(true);
            } else if (!newVal.isEmpty()) {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px; -fx-border-color: #28a745; -fx-border-width: 2;");
            } else {
                formLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
            }
        });

        // Validation de la date limite - pas dans le pass├⌐
        formDeadline.valueProperty().addListener((obs, oldVal, newVal) -> {
            deadlineErrorLabel.setVisible(false);
            deadlineErrorLabel.setManaged(false);

            if (newVal != null && newVal.isBefore(java.time.LocalDate.now())) {
                formDeadline.setStyle("-fx-font-size: 14px; -fx-border-color: #dc3545; -fx-border-width: 2;");
                deadlineErrorLabel.setText("Γ¥î La date limite ne peut pas ├¬tre dans le pass├⌐");
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
        String criteria = cbSearchCriteria != null ? cbSearchCriteria.getValue() : "Titre";

        // Notification de recherche
        NotificationService.showInfo("≡ƒöì Recherche", "Recherche en cours pour : \"" + keyword + "\"");

        try {
            List<JobOffer> results;
            FuzzySearchService fuzzySearch = FuzzySearchService.getInstance();
            final double FUZZY_THRESHOLD = 0.6;

            // R├⌐cup├⌐rer toutes les offres du recruteur
            List<JobOffer> allOffers = jobOfferService.getJobOffersByRecruiter(UserContext.getRecruiterId());

            // Filtrer avec recherche floue
            results = allOffers.stream()
                .filter(job -> {
                    String fieldToSearch = switch (criteria) {
                        case "Localisation" -> job.getLocation();
                        case "Type de contrat" -> job.getContractType() != null ? job.getContractType().toString() : "";
                        default -> job.getTitle();
                    };

                    if (fieldToSearch == null) return false;

                    // Recherche exacte d'abord
                    if (fieldToSearch.toLowerCase().contains(keyword.toLowerCase())) {
                        return true;
                    }

                    // Recherche floue ensuite
                    return fuzzySearch.calculateBestScore(fieldToSearch, keyword) >= FUZZY_THRESHOLD;
                })
                .sorted((j1, j2) -> {
                    String f1 = j1.getTitle() != null ? j1.getTitle() : "";
                    String f2 = j2.getTitle() != null ? j2.getTitle() : "";
                    double s1 = fuzzySearch.calculateBestScore(f1, keyword);
                    double s2 = fuzzySearch.calculateBestScore(f2, keyword);
                    return Double.compare(s2, s1);
                })
                .toList();

            jobListContainer.getChildren().clear();
            if (results.isEmpty()) {
                Label empty = new Label("≡ƒöì Aucun r├⌐sultat trouv├⌐ pour \"" + keyword + "\"");
                empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 20;");
                jobListContainer.getChildren().add(empty);

                // Suggestions
                List<String> suggestions = fuzzySearch.getSuggestions(keyword,
                    allOffers.stream().map(JobOffer::getTitle).filter(t -> t != null).toList(), 3);
                if (!suggestions.isEmpty()) {
                    VBox suggBox = new VBox(8);
                    suggBox.setStyle("-fx-padding: 10;");
                    Label suggLabel = new Label("≡ƒÆí Suggestions :");
                    suggLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");
                    suggBox.getChildren().add(suggLabel);

                    for (String sugg : suggestions) {
                        Button suggBtn = new Button(sugg);
                        suggBtn.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; " +
                                        "-fx-padding: 5 12; -fx-background-radius: 15; -fx-cursor: hand;");
                        suggBtn.setOnAction(e -> {
                            txtSearch.setText(sugg);
                            handleSearch();
                        });
                        suggBox.getChildren().add(suggBtn);
                    }
                    jobListContainer.getChildren().add(suggBox);
                }
            } else {
                for (JobOffer job : results) {
                    jobListContainer.getChildren().add(createJobCard(job));
                }
                NotificationService.showSuccess("Recherche termin├⌐e", results.size() + " r├⌐sultat(s) trouv├⌐(s)");
            }
        } catch (SQLException e) {
            showAlert("Erreur", "├ëchec de la recherche : " + e.getMessage(), Alert.AlertType.ERROR);
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
     * G├⌐n├¿re automatiquement une note de correction via l'API Grok
     */
    private String generateCorrectionNote(String warningReason, String warningMessage, String jobTitle, String jobDescription) {
        try {
            String prompt = String.format(
                "Tu es un recruteur qui a re├ºu un signalement sur son offre d'emploi. " +
                "G├⌐n├¿re une courte note de correction (3-4 phrases) en fran├ºais expliquant les modifications apport├⌐es pour r├⌐soudre le probl├¿me signal├⌐. " +
                "Sois professionnel et concis.\n\n" +
                "Raison du signalement: %s\n" +
                "Message de l'admin: %s\n" +
                "Titre de l'offre: %s\n" +
                "Description de l'offre: %s\n\n" +
                "G├⌐n├¿re uniquement la note de correction, sans introduction.",
                warningReason,
                warningMessage != null && warningMessage.length() > 200 ? warningMessage.substring(0, 200) + "..." : warningMessage,
                jobTitle != null ? jobTitle : "Non sp├⌐cifi├⌐",
                jobDescription != null && jobDescription.length() > 200 ? jobDescription.substring(0, 200) + "..." : (jobDescription != null ? jobDescription : "Non sp├⌐cifi├⌐e")
            );

            return callGrokAPI(prompt);
        } catch (Exception e) {
            System.err.println("Erreur g├⌐n├⌐ration note de correction: " + e.getMessage());
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
     * Extrait le contenu de la r├⌐ponse Grok
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
            System.err.println("Erreur parsing r├⌐ponse Grok: " + e.getMessage());
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

        // Construire le JSON de la requ├¬te pour Gemini
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
            System.out.println("R├⌐ponse API: " + (result != null ? result.substring(0, Math.min(100, result.length())) + "..." : "null"));
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
     * Extrait le contenu de la r├⌐ponse Gemini
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
            System.err.println("Erreur parsing r├⌐ponse Gemini: " + e.getMessage());
        }
        return null;
    }

    /**
     * Extrait le contenu du message de la r├⌐ponse JSON
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
            System.err.println("Erreur parsing r├⌐ponse: " + e.getMessage());
        }
        return null;
    }

    /**
     * ├ëchappe les caract├¿res sp├⌐ciaux pour JSON
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
     * Note de correction par d├⌐faut si l'API ├⌐choue
     */
    private String getDefaultCorrectionNote(String reason) {
        return switch (reason) {
            case "Contenu inappropri├⌐" ->
                "J'ai revu et corrig├⌐ le contenu de l'offre pour supprimer tout ├⌐l├⌐ment inappropri├⌐. " +
                "Le texte a ├⌐t├⌐ reformul├⌐ de mani├¿re professionnelle et conforme aux normes de la plateforme.";
            case "Information trompeuse" ->
                "J'ai v├⌐rifi├⌐ et corrig├⌐ les informations de l'offre pour garantir leur exactitude. " +
                "Les d├⌐tails du poste, du salaire et des conditions ont ├⌐t├⌐ mis ├á jour.";
            case "Discrimination" ->
                "J'ai modifi├⌐ l'offre pour supprimer tout crit├¿re discriminatoire. " +
                "L'offre est maintenant conforme aux lois sur l'├⌐galit├⌐ des chances.";
            case "Information incompl├¿te" ->
                "J'ai compl├⌐t├⌐ l'offre avec toutes les informations n├⌐cessaires: " +
                "description du poste, qualifications requises, conditions de travail et avantages.";
            case "Offre en double" ->
                "J'ai supprim├⌐ le doublon et conserv├⌐ uniquement cette version mise ├á jour de l'offre.";
            case "Offre expir├⌐e non mise ├á jour" ->
                "J'ai mis ├á jour la date limite de candidature et v├⌐rifi├⌐ que le poste est toujours disponible.";
            case "Spam" ->
                "J'ai reformul├⌐ l'offre de mani├¿re professionnelle et pertinente. " +
                "Le contenu est maintenant appropri├⌐ pour la plateforme.";
            default ->
                "J'ai effectu├⌐ les corrections n├⌐cessaires suite au signalement. " +
                "L'offre a ├⌐t├⌐ revue et mise ├á jour pour r├⌐pondre aux exigences de la plateforme.";
        };
    }

    /**
     * G├⌐n├¿re des suggestions pour remplir le formulaire bas├⌐ sur le titre du poste (utilise Gemini)
     */
    private String generateJobSuggestions(String jobTitle) {
        try {
            String prompt = String.format(
                "Tu es un expert RH. G├⌐n├¿re les informations pour une offre d'emploi bas├⌐e sur le titre: '%s'.\n\n" +
                "R├⌐ponds UNIQUEMENT dans ce format exact (sans autre texte):\n" +
                "DESCRIPTION: [description d├⌐taill├⌐e du poste en 4-5 phrases, responsabilit├⌐s et qualifications]\n" +
                "SKILLS: [skill1, skill2, skill3, skill4, skill5]\n\n" +
                "Exemple pour 'D├⌐veloppeur Java':\n" +
                "DESCRIPTION: Nous recherchons un d├⌐veloppeur Java passionn├⌐ pour rejoindre notre ├⌐quipe technique. Vous serez responsable du d├⌐veloppement d'applications backend robustes et scalables. Vous participerez ├á la conception et ├á l'impl├⌐mentation de nouvelles fonctionnalit├⌐s. Une exp├⌐rience avec les frameworks Spring est appr├⌐ci├⌐e.\n" +
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
            System.err.println("Erreur g├⌐n├⌐ration suggestions: " + e.getMessage());
            e.printStackTrace();
            // Retourner des suggestions par d├⌐faut en cas d'erreur
            return getDefaultJobSuggestions(jobTitle);
        }
    }

    /**
     * G├⌐n├¿re des suggestions par d├⌐faut bas├⌐es sur le titre
     */
    private String getDefaultJobSuggestions(String jobTitle) {
        String titleLower = jobTitle.toLowerCase();

        // Suggestions par d├⌐faut bas├⌐es sur des mots-cl├⌐s
        if (titleLower.contains("d├⌐veloppeur") || titleLower.contains("developer") || titleLower.contains("dev")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " passionn├⌐(e) pour rejoindre notre ├⌐quipe technique. Vous serez responsable du d├⌐veloppement et de la maintenance d'applications. Vous travaillerez en collaboration avec l'├⌐quipe pour concevoir des solutions innovantes. Ma├«trise des bonnes pratiques de d├⌐veloppement requise.\n" +
                   "SKILLS: Programmation, Git, Base de donn├⌐es, API REST, M├⌐thodologie Agile";
        } else if (titleLower.contains("chef") || titleLower.contains("manager") || titleLower.contains("responsable")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " exp├⌐riment├⌐(e) pour piloter nos projets strat├⌐giques. Vous serez en charge de la coordination des ├⌐quipes et du suivi des objectifs. Vous assurerez la communication avec les parties prenantes. Leadership et vision strat├⌐gique requis.\n" +
                   "SKILLS: Management, Gestion de projet, Communication, Leadership, Planification";
        } else if (titleLower.contains("commercial") || titleLower.contains("vente") || titleLower.contains("sales")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " dynamique pour d├⌐velopper notre portefeuille clients. Vous serez responsable de la prospection et de la fid├⌐lisation. Vous atteindrez les objectifs de vente fix├⌐s. Excellent sens du relationnel requis.\n" +
                   "SKILLS: N├⌐gociation, Prospection, CRM, Communication, Relation client";
        } else if (titleLower.contains("m├⌐canicien") || titleLower.contains("mecanicien") || titleLower.contains("technicien")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " qualifi├⌐(e) pour assurer l'entretien et la r├⌐paration des ├⌐quipements. Vous diagnostiquerez les pannes et effectuerez les interventions n├⌐cessaires. Vous veillerez au respect des normes de s├⌐curit├⌐. Une exp├⌐rience en maintenance industrielle est un plus.\n" +
                   "SKILLS: Diagnostic, R├⌐paration, Maintenance pr├⌐ventive, Lecture de plans, S├⌐curit├⌐";
        } else if (titleLower.contains("comptable") || titleLower.contains("finance") || titleLower.contains("accounting")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " rigoureux(se) pour g├⌐rer la comptabilit├⌐ de l'entreprise. Vous serez en charge de la tenue des comptes et des d├⌐clarations fiscales. Vous participerez aux cl├┤tures mensuelles et annuelles. Ma├«trise des outils comptables requise.\n" +
                   "SKILLS: Comptabilit├⌐, Excel, Fiscalit├⌐, SAP, Analyse financi├¿re";
        } else if (titleLower.contains("rh") || titleLower.contains("ressources humaines") || titleLower.contains("hr")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " pour renforcer notre ├⌐quipe RH. Vous g├⌐rerez le recrutement et l'administration du personnel. Vous contribuerez au d├⌐veloppement de la marque employeur. Connaissance du droit du travail appr├⌐ci├⌐e.\n" +
                   "SKILLS: Recrutement, Droit du travail, SIRH, Communication, Gestion administrative";
        } else if (titleLower.contains("design") || titleLower.contains("graphi") || titleLower.contains("ux") || titleLower.contains("ui")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " cr├⌐atif(ve) pour concevoir des interfaces utilisateur attractives. Vous cr├⌐erez des maquettes et prototypes. Vous collaborerez avec les ├⌐quipes techniques pour impl├⌐menter vos designs. Portfolio requis.\n" +
                   "SKILLS: Figma, Adobe Creative Suite, UX Design, Prototypage, Design System";
        } else if (titleLower.contains("data") || titleLower.contains("analyst") || titleLower.contains("bi")) {
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " pour analyser nos donn├⌐es et fournir des insights strat├⌐giques. Vous cr├⌐erez des dashboards et rapports. Vous contribuerez ├á la prise de d├⌐cision bas├⌐e sur les donn├⌐es. Esprit analytique requis.\n" +
                   "SKILLS: SQL, Python, Power BI, Excel, Statistiques";
        } else {
            // Suggestion g├⌐n├⌐rique
            return "DESCRIPTION: Nous recherchons un(e) " + jobTitle + " motiv├⌐(e) pour rejoindre notre ├⌐quipe. Vous contribuerez au d├⌐veloppement de nos activit├⌐s et participerez aux projets strat├⌐giques de l'entreprise. Vous travaillerez dans un environnement dynamique et collaboratif. Bonne capacit├⌐ d'adaptation requise.\n" +
                   "SKILLS: Communication, Travail en ├⌐quipe, Organisation, Adaptabilit├⌐, Rigueur";
        }
    }

    /**
     * Parse la r├⌐ponse AI et remplit le formulaire (uniquement description et comp├⌐tences)
     */
    private void parseAndFillForm(String suggestions, String jobTitle) {
        if (suggestions == null || suggestions.isEmpty()) return;

        try {
            // Parser la description
            String description = extractField(suggestions, "DESCRIPTION:");
            if (description != null && !description.isEmpty()) {
                formDescription.setText(description);
            }

            // Parser les comp├⌐tences
            String skillsStr = extractField(suggestions, "SKILLS:");
            if (skillsStr != null && !skillsStr.isEmpty()) {
                // Effacer les comp├⌐tences existantes
                skillsContainer.getChildren().clear();
                skillRows.clear();

                // Ajouter les nouvelles comp├⌐tences
                String[] skills = skillsStr.split(",");
                for (String skill : skills) {
                    String trimmedSkill = skill.trim();
                    if (!trimmedSkill.isEmpty() && trimmedSkill.length() >= 2) {
                        OfferSkill offerSkill = new OfferSkill(null, trimmedSkill, SkillLevel.INTERMEDIATE);
                        addSkillRow(offerSkill);
                    }
                }

                // Ajouter une ligne vide si aucune comp├⌐tence
                if (skillRows.isEmpty()) {
                    addSkillRow(null);
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur parsing suggestions: " + e.getMessage());
        }
    }

    /**
     * Extrait un champ de la r├⌐ponse AI
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
     * Convertit une cha├«ne en ContractType
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

