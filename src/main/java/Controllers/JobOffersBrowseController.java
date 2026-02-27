package Controllers;

import Models.JobOffer;
import Models.OfferSkill;
import Models.ContractType;
import Models.Status;
import Models.MatchingResult;
import Models.SkillLevel;
import Services.JobOfferService;
import Services.OfferSkillService;
import Services.NominatimMapService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur pour la vue Candidat - Consultation des offres d'emploi
 */
public class JobOffersBrowseController {

    @FXML private VBox mainContainer;
    @FXML private TextField txtSearch;

    private VBox jobListContainer;
    private VBox detailContainer;
    private JobOffer selectedJob;
    private ComboBox<String> cbFilterType;
    private ComboBox<String> cbFilterLocation;

    private JobOfferService jobOfferService;
    private OfferSkillService offerSkillService;
    private MatchingWidgetController matchingWidget;

    private Double candidateLatitude = null;
    private Double candidateLongitude = null;
    private ContractType selectedContractType = null;
    private String selectedLocation = null;

    @FXML
    public void initialize() {
        jobOfferService = new JobOfferService();
        offerSkillService = new OfferSkillService();
        matchingWidget = new MatchingWidgetController();
        matchingWidget.setOnProfileUpdated(() -> {
            if (selectedJob != null) {
                displayJobDetails(selectedJob);
            }
            loadJobOffers();
        });
        buildUI();
        loadJobOffers();
    }

    private void buildUI() {
        if (mainContainer == null) return;
        mainContainer.getChildren().clear();
        mainContainer.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 20;");

        Label pageTitle = new Label("🔍 Rechercher une offre d'emploi");
        pageTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #2c3e50; -fx-padding: 0 0 15 0;");
        mainContainer.getChildren().add(pageTitle);

        VBox searchFilterBox = createSearchFilterBox();
        mainContainer.getChildren().add(searchFilterBox);
        mainContainer.getChildren().add(new Region() {{ setPrefHeight(20); }});

        HBox contentArea = new HBox(20);
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        VBox leftSide = createJobListPanel();
        leftSide.setPrefWidth(420);
        leftSide.setMinWidth(380);
        leftSide.setMaxWidth(480);

        VBox rightSide = createDetailPanel();
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        contentArea.getChildren().addAll(leftSide, rightSide);
        mainContainer.getChildren().add(contentArea);
    }

    private VBox createSearchFilterBox() {
        VBox container = new VBox(0);
        container.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 3);");

        HBox searchRow = new HBox(12);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setStyle("-fx-padding: 20 20 15 20;");

        txtSearch = new TextField();
        txtSearch.setPromptText("Rechercher par titre, description...");
        txtSearch.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 12 15; " +
                          "-fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-font-size: 14px; -fx-pref-height: 42;");
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setOnAction(e -> handleSearch());

        Button btnSearchAction = new Button("🔍 Rechercher");
        btnSearchAction.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-size: 14px; " +
                                "-fx-font-weight: 600; -fx-padding: 12 24; -fx-background-radius: 8; -fx-cursor: hand;");
        btnSearchAction.setOnAction(e -> handleSearch());

        searchRow.getChildren().addAll(txtSearch, btnSearchAction);

        Separator separator = new Separator();

        HBox filterRow = new HBox(15);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setStyle("-fx-padding: 15 20 20 20;");

        Label filterLabel = new Label("Filtrer par :");
        filterLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #495057;");

        cbFilterType = new ComboBox<>();
        cbFilterType.setPromptText("Type de contrat");
        cbFilterType.getItems().add("Tous les types");
        for (ContractType type : ContractType.values()) {
            cbFilterType.getItems().add(formatContractType(type));
        }
        cbFilterType.setStyle("-fx-pref-width: 160; -fx-pref-height: 38;");
        cbFilterType.setOnAction(e -> applyFilters());

        cbFilterLocation = new ComboBox<>();
        cbFilterLocation.setPromptText("Localisation");
        cbFilterLocation.getItems().add("Toutes les villes");
        try {
            List<String> locations = jobOfferService.getAllLocations();
            cbFilterLocation.getItems().addAll(locations);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        cbFilterLocation.setStyle("-fx-pref-width: 160; -fx-pref-height: 38;");
        cbFilterLocation.setOnAction(e -> applyFilters());

        Button btnReset = new Button("✕ Réinitialiser");
        btnReset.setStyle("-fx-background-color: #f8f9fa; -fx-text-fill: #6c757d; -fx-font-size: 13px; " +
                         "-fx-padding: 10 16; -fx-background-radius: 8; -fx-cursor: hand; " +
                         "-fx-border-color: #dee2e6; -fx-border-radius: 8;");
        btnReset.setOnAction(e -> resetFilters());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label resultCount = new Label("");
        resultCount.setId("resultCount");
        resultCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 13px;");

        filterRow.getChildren().addAll(filterLabel, cbFilterType, cbFilterLocation, btnReset, spacer, resultCount);
        container.getChildren().addAll(searchRow, separator, filterRow);
        return container;
    }

    private void applyFilters() {
        String typeValue = cbFilterType.getValue();
        String locationValue = cbFilterLocation.getValue();

        selectedContractType = (typeValue == null || typeValue.equals("Tous les types")) ? null : getContractTypeFromLabel(typeValue);
        selectedLocation = (locationValue == null || locationValue.equals("Toutes les villes")) ? null : locationValue;

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
            List<JobOffer> jobs = jobOfferService.filterJobOffers(selectedLocation, selectedContractType, Status.OPEN);

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
            showAlert("Erreur", "Impossible de charger les offres : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateResultCount(int count) {
        Label resultCount = (Label) mainContainer.lookup("#resultCount");
        if (resultCount != null) {
            if (count == 0) {
                resultCount.setText("Aucun résultat");
            } else if (count == 1) {
                resultCount.setText("1 offre trouvée");
            } else {
                resultCount.setText(count + " offres trouvées");
            }
        }
    }

    private VBox createEmptyState() {
        VBox emptyBox = new VBox(15);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setStyle("-fx-padding: 40;");

        Label emptyIcon = new Label("🔭");
        emptyIcon.setStyle("-fx-font-size: 48px;");

        Label emptyText = new Label("Aucune offre trouvée");
        emptyText.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #6c757d;");

        Label emptyHint = new Label("Essayez de modifier vos critères de recherche");
        emptyHint.setStyle("-fx-font-size: 13px; -fx-text-fill: #adb5bd;");

        emptyBox.getChildren().addAll(emptyIcon, emptyText, emptyHint);
        return emptyBox;
    }

    private String formatContractType(ContractType type) {
        return switch (type) {
            case CDI -> "CDI";
            case CDD -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE -> "Freelance";
            case PART_TIME -> "Temps partiel";
            case FULL_TIME -> "Temps plein";
        };
    }

    private ContractType getContractTypeFromLabel(String label) {
        return switch (label) {
            case "CDI" -> ContractType.CDI;
            case "CDD" -> ContractType.CDD;
            case "Stage" -> ContractType.INTERNSHIP;
            case "Freelance" -> ContractType.FREELANCE;
            case "Temps partiel" -> ContractType.PART_TIME;
            case "Temps plein" -> ContractType.FULL_TIME;
            default -> null;
        };
    }

    private VBox createJobListPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        Label title = new Label("📋 Offres disponibles");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        jobListContainer = new VBox(10);
        jobListContainer.setStyle("-fx-padding: 5 5 5 0;");
        scroll.setContent(jobListContainer);

        panel.getChildren().addAll(title, scroll);
        return panel;
    }

    private VBox createDetailPanel() {
        VBox panel = new VBox(20);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 25; " +
                      "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 15, 0, 0, 2);");

        Label title = new Label("📄 Détails de l'offre");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        detailContainer = new VBox(20);
        detailContainer.setStyle("-fx-padding: 10 5 10 0;");
        scrollPane.setContent(detailContainer);

        VBox selectMessage = new VBox(10);
        selectMessage.setAlignment(Pos.CENTER);
        selectMessage.setStyle("-fx-padding: 60;");

        Label icon = new Label("👈");
        icon.setStyle("-fx-font-size: 36px;");

        Label text = new Label("Sélectionnez une offre pour voir les détails");
        text.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 15px;");

        selectMessage.getChildren().addAll(icon, text);
        detailContainer.getChildren().add(selectMessage);

        panel.getChildren().addAll(title, scrollPane);
        return panel;
    }

    private void loadJobOffers() {
        if (jobListContainer == null) return;
        jobListContainer.getChildren().clear();

        try {
            List<JobOffer> jobs = jobOfferService.getJobOffersByStatus(Status.OPEN);
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
            showAlert("Erreur", "Impossible de charger les offres : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox createJobCard(JobOffer job) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15; " +
                     "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(formatContractType(job.getContractType()));
        typeBadge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 3 10; " +
                          "-fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: 600;");

        Label locationBadge = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non spécifié"));
        locationBadge.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

        badges.getChildren().addAll(typeBadge, locationBadge);

        if (job.hasCoordinates() && candidateLatitude != null && candidateLongitude != null) {
            double distance = NominatimMapService.calculateDistance(
                candidateLatitude, candidateLongitude, job.getLatitude(), job.getLongitude());
            int travelTime = NominatimMapService.estimateTravelTime(distance);

            Label distanceBadge = new Label("🚗 " + NominatimMapService.formatDistance(distance) +
                                           " (" + NominatimMapService.formatTravelTime(travelTime) + ")");
            distanceBadge.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 3 8; " +
                                  "-fx-background-radius: 10; -fx-font-size: 10px;");
            badges.getChildren().add(distanceBadge);
        }

        if (job.getCreatedAt() != null) {
            Label date = new Label("Publié le " + job.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            date.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 11px;");
            card.getChildren().addAll(title, badges, date);
        } else {
            card.getChildren().addAll(title, badges);
        }

        card.setOnMouseEntered(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 10; -fx-padding: 15; " +
                             "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });
        card.setOnMouseExited(e -> {
            if (selectedJob == null || !selectedJob.getId().equals(job.getId())) {
                card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15; " +
                             "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });

        card.setOnMouseClicked(e -> selectJob(job, card));
        return card;
    }

    private void selectJob(JobOffer job, VBox card) {
        jobListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 15; " +
                             "-fx-border-color: transparent; -fx-border-radius: 10; -fx-cursor: hand;");
            }
        });

        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15; " +
                     "-fx-border-color: #5BA3F5; -fx-border-width: 2; -fx-border-radius: 10; " +
                     "-fx-effect: dropshadow(gaussian, rgba(91,163,245,0.3), 8, 0, 0, 2); -fx-cursor: hand;");

        selectedJob = job;
        displayJobDetails(job);
    }

    private void displayJobDetails(JobOffer job) {
        detailContainer.getChildren().clear();

        VBox headerCard = new VBox(12);
        headerCard.setStyle("-fx-background-color: linear-gradient(to right, #f8f9fa, #fff); " +
                           "-fx-background-radius: 10; -fx-padding: 25;");

        Label title = new Label(job.getTitle());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        FlowPane metaFlow = new FlowPane(12, 8);
        metaFlow.setAlignment(Pos.CENTER_LEFT);

        Label contractType = new Label("💼 " + formatContractType(job.getContractType()));
        contractType.setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #1976d2; -fx-padding: 6 12; " +
                             "-fx-background-radius: 15; -fx-font-size: 13px; -fx-font-weight: 600;");

        Label location = new Label("📍 " + (job.getLocation() != null ? job.getLocation() : "Non spécifié"));
        location.setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #7b1fa2; -fx-padding: 6 12; " +
                         "-fx-background-radius: 15; -fx-font-size: 13px; -fx-font-weight: 600;");

        metaFlow.getChildren().addAll(contractType, location);

        if (job.getDeadline() != null) {
            Label deadline = new Label("⏰ Date limite : " + job.getDeadline().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            deadline.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-padding: 6 12; " +
                             "-fx-background-radius: 15; -fx-font-size: 13px; -fx-font-weight: 600;");
            metaFlow.getChildren().add(deadline);
        }

        headerCard.getChildren().addAll(title, metaFlow);
        detailContainer.getChildren().add(headerCard);

        if (matchingWidget != null) {
            MatchingResult matchResult = matchingWidget.calculateMatch(job);
            VBox matchingSection = matchingWidget.createMatchingScoreWidget(matchResult);
            detailContainer.getChildren().add(matchingSection);
        }

        if (job.getDescription() != null && !job.getDescription().isBlank()) {
            VBox descSection = new VBox(10);
            descSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 20;");

            Label descTitle = new Label("📝 Description du poste");
            descTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

            Label descText = new Label(job.getDescription());
            descText.setWrapText(true);
            descText.setStyle("-fx-text-fill: #495057; -fx-font-size: 14px; -fx-line-spacing: 4;");

            descSection.getChildren().addAll(descTitle, descText);
            detailContainer.getChildren().add(descSection);
        }

        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(job.getId());
            if (!skills.isEmpty()) {
                VBox skillsSection = new VBox(12);
                skillsSection.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 20;");

                Label skillsTitle = new Label("🎯 Compétences requises");
                skillsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

                FlowPane skillsFlow = new FlowPane(8, 8);
                for (OfferSkill skill : skills) {
                    Label skillTag = new Label(skill.getSkillName() + " - " + formatSkillLevel(skill.getLevelRequired()));
                    skillTag.setStyle("-fx-background-color: white; -fx-padding: 8 14; -fx-background-radius: 8; " +
                                     "-fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-font-size: 12px;");
                    skillsFlow.getChildren().add(skillTag);
                }

                skillsSection.getChildren().addAll(skillsTitle, skillsFlow);
                detailContainer.getChildren().add(skillsSection);
            }
        } catch (SQLException e) {
            System.err.println("Erreur chargement compétences : " + e.getMessage());
        }

        if (job.getLocation() != null && !job.getLocation().trim().isEmpty()) {
            VBox distanceSection = new VBox(10);
            distanceSection.setStyle("-fx-background-color: #e8f5e9; -fx-background-radius: 10; -fx-padding: 15;");

            HBox distanceInfo = new HBox(15);
            distanceInfo.setAlignment(Pos.CENTER_LEFT);

            if (job.hasCoordinates() && candidateLatitude != null && candidateLongitude != null) {
                double distance = NominatimMapService.calculateDistance(
                    candidateLatitude, candidateLongitude, job.getLatitude(), job.getLongitude());
                int travelTime = NominatimMapService.estimateTravelTime(distance);

                Label distanceLabel = new Label("📍 " + job.getLocation() + " - Distance : " +
                    NominatimMapService.formatDistance(distance) + " (" +
                    NominatimMapService.formatTravelTime(travelTime) + ")");
                distanceLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2e7d32;");
                distanceInfo.getChildren().add(distanceLabel);
            } else {
                Label locationLabel = new Label("📍 " + job.getLocation());
                locationLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2e7d32;");

                Button btnSetLocation = new Button("📌 Définir ma position");
                btnSetLocation.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; " +
                                       "-fx-padding: 6 12; -fx-background-radius: 6; -fx-cursor: hand;");
                btnSetLocation.setOnAction(e -> showSetLocationDialog());

                distanceInfo.getChildren().addAll(locationLabel, btnSetLocation);
            }

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnMap = new Button("🗺️ Voir sur la carte");
            btnMap.setStyle("-fx-background-color: #2196f3; -fx-text-fill: white; -fx-font-weight: 600; " +
                          "-fx-padding: 8 16; -fx-background-radius: 6; -fx-cursor: hand;");
            btnMap.setOnAction(e -> showMapDialog(job));

            distanceInfo.getChildren().addAll(spacer, btnMap);
            distanceSection.getChildren().add(distanceInfo);
            detailContainer.getChildren().add(distanceSection);
        }

        if (job.getCreatedAt() != null) {
            Label posted = new Label("📅 Publié le " + job.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH)));
            posted.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px; -fx-padding: 10 0;");
            detailContainer.getChildren().add(posted);
        }

        HBox actionBox = new HBox();
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setStyle("-fx-padding: 20 0;");

        Button btnApply = new Button("📝 Postuler à cette offre");
        btnApply.setStyle("-fx-background-color: linear-gradient(to right, #28a745, #20c997); " +
                         "-fx-text-fill: white; -fx-font-weight: 700; -fx-font-size: 15px; " +
                         "-fx-padding: 14 40; -fx-background-radius: 25; -fx-cursor: hand;");
        btnApply.setOnAction(e -> handleApply(job));

        actionBox.getChildren().add(btnApply);
        detailContainer.getChildren().add(actionBox);
    }

    private String formatSkillLevel(SkillLevel level) {
        return switch (level) {
            case BEGINNER -> "Débutant";
            case INTERMEDIATE -> "Intermédiaire";
            case ADVANCED -> "Avancé";
        };
    }

    private void showSetLocationDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Définir ma position");
        dialog.setHeaderText("Entrez votre ville");
        dialog.setContentText("Ville :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(city -> {
            NominatimMapService geoService = new NominatimMapService();
            NominatimMapService.GeoLocation location = geoService.geocode(city);
            if (location != null) {
                candidateLatitude = location.getLatitude();
                candidateLongitude = location.getLongitude();
                showAlert("Succès", "Position définie : " + location.getFullLocation(), Alert.AlertType.INFORMATION);
                loadJobOffers();
            } else {
                showAlert("Erreur", "Ville non trouvée. Veuillez réessayer.", Alert.AlertType.WARNING);
            }
        });
    }

    private void showMapDialog(JobOffer job) {
        if (job.hasCoordinates()) {
            if (candidateLatitude != null && candidateLongitude != null) {
                MapViewController.showMapWithDistance(
                    job.getLatitude(), job.getLongitude(),
                    job.getLocation(), job.getTitle(),
                    candidateLatitude, candidateLongitude);
            } else {
                MapViewController.showMap(
                    job.getLatitude(), job.getLongitude(),
                    job.getLocation(), job.getTitle());
            }
        } else if (job.getLocation() != null && !job.getLocation().trim().isEmpty()) {
            new Thread(() -> {
                NominatimMapService geoService = new NominatimMapService();
                NominatimMapService.GeoLocation geoResult = geoService.geocode(job.getLocation());

                javafx.application.Platform.runLater(() -> {
                    if (geoResult != null) {
                        job.setLatitude(geoResult.getLatitude());
                        job.setLongitude(geoResult.getLongitude());

                        if (candidateLatitude != null && candidateLongitude != null) {
                            MapViewController.showMapWithDistance(
                                geoResult.getLatitude(), geoResult.getLongitude(),
                                job.getLocation(), job.getTitle(),
                                candidateLatitude, candidateLongitude);
                        } else {
                            MapViewController.showMap(
                                geoResult.getLatitude(), geoResult.getLongitude(),
                                job.getLocation(), job.getTitle());
                        }
                    } else {
                        showAlert("Erreur", "Impossible de localiser cette adresse.", Alert.AlertType.WARNING);
                    }
                });
            }).start();
        } else {
            showAlert("Erreur", "Aucune localisation définie pour cette offre.", Alert.AlertType.WARNING);
        }
    }

    private void handleApply(JobOffer job) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Candidature");
        alert.setHeaderText("Postuler pour : " + job.getTitle());
        alert.setContentText("La fonctionnalité de candidature sera bientôt disponible.\n\n" +
                            "Poste : " + job.getTitle() + "\n" +
                            "Lieu : " + (job.getLocation() != null ? job.getLocation() : "Non spécifié"));
        alert.showAndWait();
    }

    @FXML
    private void handleSearch() {
        loadFilteredJobOffers();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}




