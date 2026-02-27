package Controllers;

import Services.ApplicationService;
import Services.ApplicationStatusHistoryService;
import Services.FileService;
import Services.GrokAIService;
import Services.JobOfferService;
import Services.OllamaRankingService;
import Services.UserService;
import Utils.UserContext;
import Utils.ValidationUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ApplicationsController {

    @FXML private VBox mainContainer;
    @FXML private VBox candidateListContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
import Services.InterviewService;
import Services.EmailService;
import Services.MeetingService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modern Applications UI with CRUD functionality
 * - Recruiter: Full CRUD - view details, accept, reject, schedule interviews
 * - Candidate: Read-only view of their applications
 */
public class ApplicationsController {

    @FXML private VBox candidateListContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
    @FXML private ComboBox<String> cbApplicationStatus;
    @FXML private Label lblSubtitle;
    @FXML private Button btnSearch;
    @FXML private Button btnClear;

    private VBox detailContainer;
    private ApplicationService.ApplicationRow selectedApplication;
    private List<Long> selectedApplicationIds = new ArrayList<>();
    private VBox bulkActionPanel;
    private List<ApplicationService.ApplicationRow> currentApplications = new ArrayList<>();
    private final java.util.Map<Long, OllamaRankingService.RankResult> rankingCache = new java.util.HashMap<>();
    private boolean rankingActive = false;
    private Label rankingStatusLabel;
    private Label selectionTextLabel;

    @FXML
    public void initialize() {
        // mainContainer is injected from FXML - it's the right panel VBox
        if (mainContainer != null) {
            detailContainer = mainContainer;
        } else {
            detailContainer = new VBox(15);
        }

        // Initialize search UI
        initializeSearchUI();
        loadApplications();
    }

    private void initializeSearchUI() {
        UserContext.Role role = UserContext.getRole();

        // Set search criteria options based on role
        if (cbSearchCriteria != null) {
            if (role == UserContext.Role.RECRUITER || role == UserContext.Role.ADMIN) {
                cbSearchCriteria.getItems().addAll(
                    "Candidate Name",
                    "Candidate Email",
                    "Offer Title"
                );
            } else if (role == UserContext.Role.CANDIDATE) {
                cbSearchCriteria.getItems().addAll(
                    "Offer Title",
                    "Company Name",
                    "Status"
                );
            }
            // Ensure the prompt is visible even when ComboBox is non-editable by providing a custom button cell
            cbSearchCriteria.setPromptText("Search by...");
            cbSearchCriteria.setButtonCell(new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("Search by...");
                    } else {
                        setText(item);
                    }
                }
            });
            // Keep default list cell rendering for dropdown
            cbSearchCriteria.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(item);
                    }
                }
            });
        }

        // Setup search button
        if (btnSearch != null) {
            btnSearch.setOnAction(e -> performSearch());
        }

        // Setup clear button
        if (btnClear != null) {
            btnClear.setOnAction(e -> clearSearch());
        }

        // Allow search on Enter key
        if (txtSearch != null) {
            txtSearch.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ENTER) {
                    performSearch();
                }
            });
        }
    }

    private void performSearch() {
        String searchCriteria = cbSearchCriteria.getValue();
        String searchText = txtSearch.getText();

        if (searchCriteria == null || searchCriteria.isEmpty()) {
            showAlert("Warning", "Please select a search criteria", Alert.AlertType.WARNING);
            return;
        }

        if (searchText == null || searchText.trim().isEmpty()) {
            showAlert("Warning", "Please enter search text", Alert.AlertType.WARNING);
            return;
        }

        UserContext.Role role = UserContext.getRole();
        List<Long> offerIds = null;

        // Get offer IDs based on role
        if (role == UserContext.Role.RECRUITER) {
            Long recruiterId = UserContext.getRecruiterId();
            List<JobOfferService.JobOfferRow> recruiterOffers = JobOfferService.getByRecruiterId(recruiterId);
            offerIds = recruiterOffers.stream()
                .map(JobOfferService.JobOfferRow::id)
                .toList();
        }
        // For candidates and admins, pass null to search all offers

        // Perform search
        List<ApplicationService.ApplicationRow> searchResults = ApplicationService.searchApplications(
            offerIds,
            searchCriteria,
            searchText
        );

        // Filter by role if needed
        if (role == UserContext.Role.CANDIDATE) {
            Long candidateId = UserContext.getCandidateId();
            searchResults = searchResults.stream()
                .filter(app -> app.candidateId().equals(candidateId))
                .toList();
        }

        displaySearchResults(searchResults);
    }

    private void clearSearch() {
        if (cbSearchCriteria != null) {
            cbSearchCriteria.getSelectionModel().clearSelection();
            cbSearchCriteria.setValue(null);
            cbSearchCriteria.setPromptText("Search by...");
            // Force button cell text to show prompt for non-editable ComboBox
            try {
                if (cbSearchCriteria.getButtonCell() != null) {
                    cbSearchCriteria.getButtonCell().setText("Search by...");
                }
            } catch (Exception ignored) {
            }

            // If editable, clear its editor too
            try {
                if (cbSearchCriteria.isEditable() && cbSearchCriteria.getEditor() != null) {
                    cbSearchCriteria.getEditor().clear();
                }
            } catch (Exception ignored) {
            }
        }

        if (txtSearch != null) {
            txtSearch.clear();
        }

        loadApplications();
    }

    private void displaySearchResults(List<ApplicationService.ApplicationRow> results) {
        currentApplications = results;
        boolean showBulkPanel = UserContext.getRole() == UserContext.Role.RECRUITER;
        renderApplications(results, showBulkPanel);
    // Detail view elements (dynamically populated)
    private ApplicationService.ApplicationRow selectedApplication;
    private VBox detailContainer;

    @FXML
    public void initialize() {
        setupComboBoxes();
        loadApplications();
    }

    private void setupComboBoxes() {
        if (cbSearchCriteria != null) {
            cbSearchCriteria.getItems().addAll("Nom", "Email", "Poste", "Statut", "Date");
            cbSearchCriteria.setValue("Nom");
        }

        if (cbApplicationStatus != null) {
            cbApplicationStatus.getItems().addAll("Nouveau", "En révision", "Présélectionné",
                                                  "Entretien planifié", "Accepté", "Rejeté");
        }
    }

    private void loadApplications() {
        if (candidateListContainer == null) return;
        candidateListContainer.getChildren().clear();
        selectedApplicationIds.clear();

        List<ApplicationService.ApplicationRow> applications = ApplicationService.getAll();
        UserContext.Role role = UserContext.getRole();

        // Filter by role
        if (role == UserContext.Role.CANDIDATE) {
            Long candidateId = UserContext.getCandidateId();
            if (candidateId != null) {
                applications = applications.stream()
                    .filter(app -> app.candidateId().equals(candidateId))
                    .toList();
            }
        } else if (role == UserContext.Role.RECRUITER) {
            Long recruiterId = UserContext.getRecruiterId();
            List<JobOfferService.JobOfferRow> recruiterOffers = JobOfferService.getByRecruiterId(recruiterId);
            List<Long> offerIds = recruiterOffers.stream()
                .map(JobOfferService.JobOfferRow::id)
                .toList();
            applications = applications.stream()
                .filter(app -> offerIds.contains(app.offerId()))
                .toList();
        }

        // Hide archived applications for non-admins
        if (role != UserContext.Role.ADMIN) {
            applications = applications.stream()
                .filter(app -> !app.isArchived())
                .toList();
        }

        currentApplications = applications;
        renderApplications(applications, role == UserContext.Role.RECRUITER);
    }

    private void renderApplications(List<ApplicationService.ApplicationRow> applications, boolean showBulkPanel) {
        if (candidateListContainer == null) return;
        candidateListContainer.getChildren().clear();
        selectedApplicationIds.clear();

        if (applications == null || applications.isEmpty()) {
            Label empty = new Label(showBulkPanel ? "No applications found" : "No applications found matching your search");
            empty.setStyle("-fx-text-fill: #999; -fx-font-size: 14px; -fx-padding: 30;");

        List<ApplicationService.ApplicationRow> apps = ApplicationService.getAll();

        // Filter applications based on user role
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (!isRecruiter) {
            // Candidate should only see their own applications
            // Filter by current candidate ID (you can get this from UserContext or session)
            Long currentCandidateId = Utils.UserContext.getCandidateId(); // You'll need to add this method
            if (currentCandidateId != null) {
                apps = apps.stream()
                          .filter(app -> app.candidateId().equals(currentCandidateId))
                          .toList();
            }
        }

        if (apps.isEmpty()) {
            Label empty = new Label(isRecruiter ? "Aucune candidature trouvée" : "Vous n'avez postulé à aucun poste pour le moment");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 30;");
            candidateListContainer.getChildren().add(empty);
            return;
        }

        if (showBulkPanel) {
            ensureBulkActionPanel(candidateListContainer);
            bulkActionPanel.setVisible(true);
            bulkActionPanel.setManaged(true);
        } else if (bulkActionPanel != null) {
            bulkActionPanel.setVisible(false);
            bulkActionPanel.setManaged(false);
        }

        List<ApplicationService.ApplicationRow> displayList = new ArrayList<>(applications);
        if (rankingActive) {
            displayList.sort((a, b) -> Integer.compare(getRankScore(b.id()), getRankScore(a.id())));
        }

        boolean first = true;
        for (ApplicationService.ApplicationRow app : displayList) {
            VBox card = createApplicationCard(app);
            candidateListContainer.getChildren().add(card);

            if (first) {
                selectApplication(app, card);
                first = false;
            }
        }
    }

    private int getRankScore(Long appId) {
        OllamaRankingService.RankResult result = rankingCache.get(appId);
        return result != null ? result.score() : -1;
    }

    private void createAndShowBulkActionPanel(VBox container) {
        bulkActionPanel = new VBox(12);
        bulkActionPanel.setStyle("-fx-border-color: #007bff; -fx-border-radius: 4; -fx-padding: 15; -fx-background-color: #e7f3ff; -fx-border-width: 2;");
        bulkActionPanel.setVisible(false);
        bulkActionPanel.setManaged(false);

        Label titleLabel = new Label("Bulk Actions");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #004085;");

        HBox selectionInfo = new HBox(10);
        selectionTextLabel = new Label("No applications selected");
        selectionTextLabel.setStyle("-fx-text-fill: #004085;");
        selectionInfo.getChildren().add(selectionTextLabel);

        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label("Change Status:");
        statusLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        statusCombo.setPrefWidth(180);
        statusCombo.setPromptText("Select status...");

        TextArea noteArea = new TextArea();
        noteArea.setPromptText("Add note (optional)");
        noteArea.setPrefRowCount(2);
        noteArea.setWrapText(true);
        noteArea.setStyle("-fx-font-size: 12px;");

        Button btnBulkUpdate = new Button("Update");
        btnBulkUpdate.setStyle("-fx-padding: 6 12; -fx-background-color: #007bff; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        btnBulkUpdate.setOnAction(e -> {
            if (selectedApplicationIds.isEmpty()) {
                showAlert("Warning", "No applications selected", Alert.AlertType.WARNING);
                return;
            }
            if (statusCombo.getValue() == null) {
                showAlert("Warning", "Please select a status", Alert.AlertType.WARNING);
                return;
            }
            bulkUpdateStatus(new ArrayList<>(selectedApplicationIds), statusCombo.getValue(), noteArea.getText());
        });

        actionRow.getChildren().addAll(statusLabel, statusCombo, btnBulkUpdate);
        VBox.setVgrow(actionRow, Priority.NEVER);

        HBox rankingRow = new HBox(10);
        rankingRow.setAlignment(Pos.CENTER_LEFT);

        Button btnRank = new Button("Rank with AI");
        btnRank.setStyle("-fx-padding: 6 12; -fx-background-color: #6f42c1; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        btnRank.setOnAction(e -> rankApplicationsWithAI());

        rankingStatusLabel = new Label("Ranking: OFF");
        rankingStatusLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");

        rankingRow.getChildren().addAll(btnRank, rankingStatusLabel);

        bulkActionPanel.getChildren().addAll(titleLabel, selectionInfo, actionRow, rankingRow);
        container.getChildren().add(0, bulkActionPanel);
    }

    private void ensureBulkActionPanel(VBox container) {
        if (bulkActionPanel == null) {
            createAndShowBulkActionPanel(container);
            return;
        }
        if (!container.getChildren().contains(bulkActionPanel)) {
            container.getChildren().add(0, bulkActionPanel);
        }
    }

    private void updateBulkActionPanelUI() {
        if (bulkActionPanel == null || selectionTextLabel == null) return;

        if (selectedApplicationIds.isEmpty()) {
            selectionTextLabel.setText("No applications selected");
        } else {
            selectionTextLabel.setText(selectedApplicationIds.size() + " application(s) selected");
        }
    }

    private VBox createApplicationCard(ApplicationService.ApplicationRow app) {
        VBox card = new VBox(8);
        card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 4; -fx-padding: 12; -fx-background-color: white; -fx-cursor: hand;");
        card.setUserData(app);

        HBox cardContent = new HBox(10);
        cardContent.setAlignment(Pos.CENTER_LEFT);

        // Checkbox for recruiter bulk select
        if (UserContext.getRole() == UserContext.Role.RECRUITER) {
            CheckBox selectCheckbox = new CheckBox();
            selectCheckbox.setStyle("-fx-font-size: 14px;");
            selectCheckbox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    if (!selectedApplicationIds.contains(app.id())) {
                        selectedApplicationIds.add(app.id());
                    }
                } else {
                    selectedApplicationIds.remove(app.id());
                }
                updateBulkActionPanelUI();
            });
            cardContent.getChildren().add(selectCheckbox);
            card.setUserData(new Object[]{app, selectCheckbox});
        }

        VBox infoBox = new VBox(4);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        Label candidateName = new Label(app.candidateName() != null && !app.candidateName().trim().isEmpty()
            ? app.candidateName()
            : "Candidate #" + app.id());
        candidateName.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        Label jobTitle = new Label(app.jobTitle() != null ? app.jobTitle() : "Job Application");
        jobTitle.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        HBox statusBox = new HBox(10);
        Label statusBadge = new Label(app.currentStatus());
        statusBadge.setStyle("-fx-padding: 4 8; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-border-radius: 3; -fx-font-size: 11;");

        if (UserContext.getRole() == UserContext.Role.RECRUITER && rankingActive) {
            OllamaRankingService.RankResult rankResult = rankingCache.get(app.id());
            if (rankResult != null) {
                Label rankBadge = new Label("AI " + rankResult.score());
                rankBadge.setStyle("-fx-padding: 4 8; -fx-background-color: #6f42c1; -fx-text-fill: white; -fx-border-radius: 3; -fx-font-size: 11; -fx-font-weight: bold;");
                statusBox.getChildren().add(rankBadge);
            }
        }

        // Archived badge
        if (app.isArchived()) {
            Label archivedBadge = new Label("ARCHIVED");
            archivedBadge.setStyle("-fx-padding: 4 8; -fx-background-color: #6c757d; -fx-text-fill: white; -fx-border-radius: 3; -fx-font-size: 11;");
            statusBox.getChildren().addAll(statusBadge, archivedBadge);
        } else {
            statusBox.getChildren().addAll(statusBadge);
        }

        Label appliedDate = new Label(app.appliedAt() != null
            ? app.appliedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            : "N/A");
        appliedDate.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

        statusBox.getChildren().add(appliedDate);

        infoBox.getChildren().addAll(candidateName, jobTitle, statusBox);
        cardContent.getChildren().add(infoBox);
        card.getChildren().add(cardContent);
        card.setOnMouseClicked(e -> {
            // Only select if not clicking on checkbox
            if (!(e.getTarget() instanceof CheckBox)) {
                selectApplication(app, card);
            }
        });
        // Load first application by default
        boolean firstLoad = true;

        for (ApplicationService.ApplicationRow app : apps) {
            VBox card = createCandidateCard(app);
            candidateListContainer.getChildren().add(card);

            if (firstLoad) {
                selectApplication(app, card);
                firstLoad = false;
            }
        }
    }

    private VBox createCandidateCard(ApplicationService.ApplicationRow app) {
        VBox card = new VBox(10);
        card.getStyleClass().add("candidate-card");
        card.setPadding(new Insets(18));
        card.setUserData(app);

        // Header with name and rating
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(4);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label name = new Label(app.candidateName() != null && !app.candidateName().trim().isEmpty()
                                ? app.candidateName()
                                : "Candidate #" + app.id());
        name.getStyleClass().add("candidate-name");

        Label position = new Label(app.jobTitle() != null ? app.jobTitle() : "Application");
        position.getStyleClass().add("candidate-position");

        nameBox.getChildren().addAll(name, position);

        Label rating = new Label("⭐ 4.5");
        rating.getStyleClass().add("rating-badge");

        header.getChildren().addAll(nameBox, rating);

        // Info section
        HBox info = new HBox(8);
        Label location = new Label("📍 Location TBD");
        location.getStyleClass().add("candidate-info");
        info.getChildren().add(location);

        // Status and date
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label(app.currentStatus());
        statusBadge.getStyleClass().addAll("status-badge", getStatusClass(app.currentStatus()));

        Label date = new Label("1/23/2026"); // Placeholder - ApplicationRow doesn't have appliedAt field
        date.getStyleClass().add("date-label");

        statusBox.getChildren().addAll(statusBadge, date);

        card.getChildren().addAll(header, info, statusBox);

        // Click handler
        card.setOnMouseClicked(e -> selectApplication(app, card));

        return card;
    }

    private void selectApplication(ApplicationService.ApplicationRow app, VBox card) {
        candidateListContainer.getChildren().forEach(node -> {
            if (node instanceof VBox) {
                node.setStyle("-fx-border-color: #ddd; -fx-border-radius: 4; -fx-padding: 12; -fx-background-color: white; -fx-cursor: hand;");
            }
        });

        card.setStyle("-fx-border-color: #5BA3F5; -fx-border-radius: 4; -fx-padding: 12; -fx-background-color: #f0f4ff; -fx-cursor: hand;");
        selectedApplication = app;
        displayApplicationDetails(app);
    }

    private void displayApplicationDetails(ApplicationService.ApplicationRow app) {
        detailContainer.getChildren().clear();

        // Get role at the beginning so it's available throughout the method
        UserContext.Role role = UserContext.getRole();

        // Header section
        VBox headerBox = new VBox(10);
        headerBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15; -fx-background-color: #f8f9fa;");

        Label candidateName = new Label(app.candidateName());
        candidateName.setStyle("-fx-font-weight: bold; -fx-font-size: 16;");

        Label jobPosition = new Label("Position: " + (app.jobTitle() != null ? app.jobTitle() : "N/A"));
        jobPosition.setStyle("-fx-text-fill: #666;");

        Label email = new Label("Email: " + (app.candidateEmail() != null ? app.candidateEmail() : "N/A"));
        email.setStyle("-fx-text-fill: #666;");

        Label phone = new Label("Phone: " + (app.phone() != null ? app.phone() : "N/A"));
        phone.setStyle("-fx-text-fill: #666;");

        Label appliedDate = new Label("Applied: " + (app.appliedAt() != null
            ? app.appliedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
            : "N/A"));
        appliedDate.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        Label currentStatus = new Label("Current Status: " + app.currentStatus());
        currentStatus.setStyle("-fx-font-weight: bold; -fx-text-fill: #5BA3F5;");

        headerBox.getChildren().addAll(candidateName, jobPosition, email, phone, appliedDate, currentStatus);
        detailContainer.getChildren().add(headerBox);

        if (role == UserContext.Role.RECRUITER && rankingActive) {
            VBox aiBox = new VBox(6);
            aiBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 12; -fx-background-color: #f8f0ff;");

            Label aiLabel = new Label("AI Ranking");
            aiLabel.setStyle("-fx-font-weight: bold;");

            OllamaRankingService.RankResult rankResult = rankingCache.get(app.id());
            if (rankResult != null) {
                Label scoreLabel = new Label("Score: " + rankResult.score() + "/100");
                scoreLabel.setStyle("-fx-text-fill: #6f42c1; -fx-font-weight: bold;");

                Label rationaleLabel = new Label(rankResult.rationale());
                rationaleLabel.setWrapText(true);
                rationaleLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 12;");

                aiBox.getChildren().addAll(aiLabel, scoreLabel, rationaleLabel);
            } else {
                Label noRankLabel = new Label("Not ranked yet.");
                noRankLabel.setStyle("-fx-text-fill: #999;");
                aiBox.getChildren().addAll(aiLabel, noRankLabel);
            }

            detailContainer.getChildren().add(aiBox);
        }

        // Cover Letter section
        if (app.coverLetter() != null && !app.coverLetter().isEmpty()) {
            VBox coverLetterBox = new VBox(5);
            coverLetterBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15;");

            HBox coverHeaderBox = new HBox(10);
            coverHeaderBox.setAlignment(Pos.CENTER_LEFT);

            Label coverLabel = new Label("Cover Letter:");
            coverLabel.setStyle("-fx-font-weight: bold;");

            coverHeaderBox.getChildren().add(coverLabel);

            TextArea coverText = new TextArea(app.coverLetter());
            coverText.setEditable(false);
            coverText.setWrapText(true);
            coverText.setPrefRowCount(5);
            coverText.setStyle("-fx-control-inner-background: #f8f9fa; -fx-text-fill: #333;");

            // Translate + Original buttons only for ADMIN and RECRUITER
            if (role == UserContext.Role.ADMIN || role == UserContext.Role.RECRUITER) {
                final String originalText = app.coverLetter();

                Button btnTranslate = new Button("🌐 Translate");
                btnTranslate.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 4;");

                Button btnOriginal = new Button("🔄 Original");
                btnOriginal.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-size: 11; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 4;");
                btnOriginal.setDisable(true); // disabled until a translation is shown

                btnOriginal.setOnAction(e -> {
                    coverText.setText(originalText);
                    coverLabel.setText("Cover Letter:");
                    btnOriginal.setDisable(true);
                });

                btnTranslate.setOnAction(e -> {
                    ChoiceDialog<String> dialog = new ChoiceDialog<>("French", "French", "English", "Arabic");
                    dialog.setTitle("Translate Cover Letter");
                    dialog.setHeaderText("Select target language");
                    dialog.setContentText("Language:");

                    dialog.showAndWait().ifPresent(language -> {
                        btnTranslate.setText("Translating...");
                        btnTranslate.setDisable(true);
                        btnOriginal.setDisable(true);

                        javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
                            @Override
                            protected String call() {
                                return GrokAIService.translateCoverLetter(originalText, language);
                            }
                        };

                        task.setOnSucceeded(ev -> {
                            String translated = task.getValue();
                            if (translated != null && !translated.isEmpty()) {
                                coverText.setText(translated);
                                coverLabel.setText("Cover Letter (" + language + "):");
                                btnOriginal.setDisable(false);
                            }
                            btnTranslate.setText("🌐 Translate");
                            btnTranslate.setDisable(false);
                        });

                        task.setOnFailed(ev -> {
                            btnTranslate.setText("🌐 Translate");
                            btnTranslate.setDisable(false);
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Translation failed. Please try again.", ButtonType.OK);
                            alert.showAndWait();
                        });

                        new Thread(task).start();
                    });
                });

                coverHeaderBox.getChildren().addAll(btnTranslate, btnOriginal);
            }

            coverLetterBox.getChildren().addAll(coverHeaderBox, coverText);
            detailContainer.getChildren().add(coverLetterBox);
        }

        // CV Path section
        if (app.cvPath() != null && !app.cvPath().isEmpty()) {
            VBox cvBox = new VBox(5);
            cvBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15;");

            HBox cvLabelBox = new HBox(10);
            Label cvLabel = new Label("CV Path: " + app.cvPath());
            cvLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

            // Download button for recruiters and admins
            if (role == UserContext.Role.RECRUITER || role == UserContext.Role.ADMIN) {
                Button btnDownload = new Button("Download CV");
                btnDownload.setStyle("-fx-padding: 4 10; -fx-font-size: 11; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand;");
                btnDownload.setOnAction(e -> downloadPDF(app));
                cvLabelBox.getChildren().addAll(cvLabel, btnDownload);
            } else {
                cvLabelBox.getChildren().add(cvLabel);
            }

            cvBox.getChildren().add(cvLabelBox);
            detailContainer.getChildren().add(cvBox);
        }

        // Status History section
        VBox historyBox = new VBox(8);
        historyBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15;");

        Label historyLabel = new Label("Status History:");
        historyLabel.setStyle("-fx-font-weight: bold;");
        historyBox.getChildren().add(historyLabel);

        List<ApplicationStatusHistoryService.StatusHistoryRow> history =
            ApplicationStatusHistoryService.getByApplicationId(app.id());

        if (history.isEmpty()) {
            Label noHistory = new Label("No history available");
            noHistory.setStyle("-fx-text-fill: #999;");
            historyBox.getChildren().add(noHistory);
        } else {
            for (ApplicationStatusHistoryService.StatusHistoryRow record : history) {
                VBox historyItem = new VBox(3);
                historyItem.setStyle("-fx-border-color: #dee2e6; -fx-border-radius: 3; -fx-padding: 8; -fx-background-color: white;");

                Label statusLabel = new Label(record.status());
                statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");

                Label dateLabel = new Label(record.changedAt() != null
                    ? record.changedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                    : "N/A");
                dateLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

                historyItem.getChildren().addAll(statusLabel, dateLabel);

                if (record.note() != null && !record.note().isEmpty()) {
                    Label noteLabel = new Label("Note: " + record.note());
                    noteLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11; -fx-wrap-text: true;");
                    noteLabel.setWrapText(true);
                    historyItem.getChildren().add(noteLabel);
                }

                // Determine if current user can edit/delete this history record
                boolean canEditHistory = false;
                boolean canDeleteHistory = false;

                if (role == UserContext.Role.ADMIN) {
                    canEditHistory = true;
                    canDeleteHistory = true;
                } else if (role == UserContext.Role.RECRUITER) {
                    try {
                        var offer = JobOfferService.getById(app.offerId());
                        if (offer != null && offer.recruiterId() != null && UserContext.getRecruiterId() != null
                                && offer.recruiterId().equals(UserContext.getRecruiterId())) {
                            canEditHistory = true; // recruiter owning the offer may edit history
                        }
                    } catch (Exception ignored) {
                        // If any issue, default to not allowing edit
                    }
                }

                if (canEditHistory || canDeleteHistory) {
                    HBox adminButtonBox = new HBox(10);
                    adminButtonBox.setAlignment(Pos.CENTER_RIGHT);

                    if (canEditHistory) {
                        Button btnEdit = new Button("Edit");
                        btnEdit.setStyle("-fx-padding: 4 8; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand;");
                        btnEdit.setOnAction(e -> editStatusHistory(record));
                        adminButtonBox.getChildren().add(btnEdit);
                    }

                    if (canDeleteHistory) {
                        Button btnDelete = new Button("Delete");
                        btnDelete.setStyle("-fx-padding: 4 8; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand;");
                        btnDelete.setOnAction(e -> deleteStatusHistory(record, app));
                        adminButtonBox.getChildren().add(btnDelete);
                    }

                    historyItem.getChildren().add(adminButtonBox);
                }

                historyBox.getChildren().add(historyItem);
            }
        }

        detailContainer.getChildren().add(historyBox);

        // Actions section

        VBox actionsBox = new VBox(10);
        actionsBox.setStyle("-fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 15; -fx-background-color: #f8f9fa;");

        Label actionsLabel = new Label("Actions:");
        actionsLabel.setStyle("-fx-font-weight: bold;");
        actionsBox.getChildren().add(actionsLabel);

        if (role == UserContext.Role.CANDIDATE) {
            HBox buttonBox = new HBox(10);

            Button btnEdit = new Button("Edit");
            btnEdit.setStyle("-fx-padding: 6 12; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand;");
            btnEdit.setOnAction(e -> showEditApplicationDialog(app));

            // Only enable edit if status is SUBMITTED
            boolean canEdit = "SUBMITTED".equals(app.currentStatus());
            btnEdit.setDisable(!canEdit);
            if (!canEdit) {
                btnEdit.setStyle("-fx-padding: 6 12; -fx-background-color: #ccc; -fx-text-fill: #666; -fx-cursor: not-allowed;");
            }

            Button btnDelete = new Button("Delete");
            btnDelete.setStyle("-fx-padding: 6 12; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> deleteApplication(app));

            buttonBox.getChildren().addAll(btnEdit, btnDelete);
            actionsBox.getChildren().add(buttonBox);

        } else if (role == UserContext.Role.RECRUITER) {
            VBox statusUpdateBox = new VBox(8);

            Label statusLabel = new Label("Change Status:");
            statusLabel.setStyle("-fx-font-weight: bold;");

            ComboBox<String> statusCombo = new ComboBox<>();
            statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
            statusCombo.setValue(app.currentStatus());
            statusCombo.setPrefWidth(250);

            TextArea noteArea = new TextArea();
            noteArea.setPromptText("Add note (optional)");
            noteArea.setPrefRowCount(3);
            noteArea.setWrapText(true);

            Button btnUpdate = new Button("Update Status");
            btnUpdate.setStyle("-fx-padding: 6 12; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand;");
            btnUpdate.setOnAction(e -> updateApplicationStatus(app, statusCombo.getValue(), noteArea.getText()));

            // Quick Action Buttons
            HBox quickActionsBox = new HBox(10);
            quickActionsBox.setStyle("-fx-padding: 10 0; -fx-border-top: 1px solid #ddd; -fx-padding: 10;");

            Button btnInReview = new Button("📋 In Review");
            btnInReview.setStyle("-fx-padding: 8 15; -fx-background-color: #17a2b8; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
            btnInReview.setOnAction(e -> startReviewApplication(app));

            Button btnAccept = new Button("✓ Accept (Shortlist)");
            btnAccept.setStyle("-fx-padding: 8 15; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
            btnAccept.setOnAction(e -> acceptApplication(app));

            Button btnReject = new Button("✕ Reject");
            btnReject.setStyle("-fx-padding: 8 15; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
            btnReject.setOnAction(e -> rejectApplication(app));

            quickActionsBox.getChildren().addAll(btnInReview, btnAccept, btnReject);

            statusUpdateBox.getChildren().addAll(statusLabel, statusCombo, new Label("Note:"), noteArea, btnUpdate, quickActionsBox);
            actionsBox.getChildren().add(statusUpdateBox);
        } else if (role == UserContext.Role.ADMIN) {
            HBox buttonBox = new HBox(10);

            Button btnDelete = new Button("Delete");
            btnDelete.setStyle("-fx-padding: 6 12; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> deleteApplication(app));

            // Archive / Unarchive button
            Button btnArchive = new Button(app.isArchived() ? "Unarchive" : "Archive");
            btnArchive.setStyle("-fx-padding: 6 12; -fx-background-color: #6c757d; -fx-text-fill: white; -fx-cursor: hand;");
            btnArchive.setOnAction(e -> {
                boolean toArchive = !app.isArchived();
                ApplicationService.setArchived(app.id(), toArchive, UserContext.getAdminId());
                loadApplications();
                showAlert("Success", toArchive ? "Application archived." : "Application unarchived.", Alert.AlertType.INFORMATION);
            });

            buttonBox.getChildren().addAll(btnArchive, btnDelete);
            actionsBox.getChildren().add(buttonBox);
        }

        detailContainer.getChildren().add(actionsBox);
    }

    private void showEditApplicationDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Application");
        dialog.setHeaderText("Edit Application #" + app.id());

        ScrollPane scrollPane = new ScrollPane();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-padding: 20;");
        scrollPane.setContent(content);
        scrollPane.setFitToWidth(true);

        // Phone field with country selection
        Label phoneLabel = new Label("Phone Number *");
        phoneLabel.setStyle("-fx-font-weight: bold;");

        HBox phoneContainer = new HBox(10);
        phoneContainer.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> countryCombo = new ComboBox<>();
        countryCombo.getItems().addAll("Tunisia (+216)", "France (+33)");
        countryCombo.setValue("Tunisia (+216)");
        countryCombo.setPrefWidth(150);
        countryCombo.setStyle("-fx-font-size: 13px;");

        TextField phoneField = new TextField(app.phone() != null ? app.phone() : "");
        phoneField.setPromptText("Enter your phone number");
        phoneField.setPrefWidth(250);

        Label phoneErrorLabel = new Label();
        phoneErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        phoneErrorLabel.setVisible(false);

        phoneContainer.getChildren().addAll(countryCombo, phoneField);

        // Cover Letter field
        Label letterLabel = new Label("Cover Letter * (50-2000 characters)");
        letterLabel.setStyle("-fx-font-weight: bold;");

        TextArea letterArea = new TextArea(app.coverLetter() != null ? app.coverLetter() : "");
        letterArea.setPromptText("Tell us why you're interested in this position...");
        letterArea.setPrefRowCount(8);
        letterArea.setWrapText(true);
        letterArea.setStyle("-fx-font-size: 13px;");

        String initialCoverLetter = app.coverLetter() != null ? app.coverLetter() : "";
        Label letterCharCount = new Label(initialCoverLetter.length() + "/2000");
        letterCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        // Update character count in real-time
        letterArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int length = newVal != null ? newVal.length() : 0;
            letterCharCount.setText(length + "/2000");

            if (length > 2000) {
                letterCharCount.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else if (length < 50 && length > 0) {
                letterCharCount.setStyle("-fx-text-fill: #fd7e14; -fx-font-size: 11px;");
            } else {
                letterCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
            }
        });

        Label letterErrorLabel = new Label();
        letterErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        letterErrorLabel.setVisible(false);

        VBox letterBox = new VBox(8);
        letterBox.getChildren().addAll(letterLabel, letterArea, letterCharCount);

        // CV/PDF file selection
        Label pdfLabel = new Label("Upload CV (PDF) - Optional");
        pdfLabel.setStyle("-fx-font-weight: bold;");

        HBox cvBox = new HBox(10);
        cvBox.setAlignment(Pos.CENTER_LEFT);
        TextField cvPathField = new TextField();
        cvPathField.setPromptText(app.cvPath() != null && !app.cvPath().isEmpty() ? "Current: " + app.cvPath() : "No file selected");
        cvPathField.setEditable(false);
        cvPathField.setPrefWidth(280);

        Button btnBrowseCV = new Button("Browse");
        btnBrowseCV.setStyle("-fx-padding: 6 12; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-cursor: hand;");
        btnBrowseCV.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select PDF File");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(null);
            if (selectedFile != null) {
                cvPathField.setText(selectedFile.getAbsolutePath());
            }
        });

        cvBox.getChildren().addAll(cvPathField, btnBrowseCV);

        Button btnGenerateLetter = new Button("🤖 Generate with AI");
        btnGenerateLetter.setStyle("-fx-padding: 6 12; -fx-background-color: #6f42c1; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
        btnGenerateLetter.setOnAction(e -> generateCoverLetterForEdit(app, letterArea, cvPathField));

        HBox generateRow = new HBox(btnGenerateLetter);
        generateRow.setAlignment(Pos.CENTER_LEFT);

        // Add validation on input change for real-time feedback
        phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
            String country = countryCombo.getValue();
            boolean isValid = false;
            if ("Tunisia (+216)".equals(country)) {
                isValid = ValidationUtils.isValidTunisianPhone(newVal);
            } else if ("France (+33)".equals(country)) {
                isValid = ValidationUtils.isValidFrenchPhone(newVal);
            }

            if (newVal.isEmpty()) {
                phoneErrorLabel.setVisible(false);
            } else if (!isValid) {
                phoneErrorLabel.setText(ValidationUtils.getPhoneErrorMessage(
                    "Tunisia (+216)".equals(country) ? "TN" : "FR", newVal));
                phoneErrorLabel.setVisible(true);
            } else {
                phoneErrorLabel.setVisible(false);
            }
        });

        countryCombo.setOnAction(e -> {
            // Re-validate on country change
            String newVal = phoneField.getText();
            String country = countryCombo.getValue();
            boolean isValid = false;
            if ("Tunisia (+216)".equals(country)) {
                isValid = ValidationUtils.isValidTunisianPhone(newVal);
            } else if ("France (+33)".equals(country)) {
                isValid = ValidationUtils.isValidFrenchPhone(newVal);
            }

            if (newVal.isEmpty()) {
                phoneErrorLabel.setVisible(false);
            } else if (!isValid) {
                phoneErrorLabel.setText(ValidationUtils.getPhoneErrorMessage(
                    "Tunisia (+216)".equals(country) ? "TN" : "FR", newVal));
                phoneErrorLabel.setVisible(true);
            } else {
                phoneErrorLabel.setVisible(false);
            }
        });

        content.getChildren().addAll(
            phoneLabel, phoneContainer, phoneErrorLabel,
            letterBox, generateRow, letterErrorLabel,
            pdfLabel, cvBox
        );

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String country = countryCombo.getValue();
                String phone = phoneField.getText();
                String coverLetter = letterArea.getText();
                String cvPath = cvPathField.getText();

                // Validate phone based on selected country
                boolean phoneValid = false;
                if ("Tunisia (+216)".equals(country)) {
                    phoneValid = ValidationUtils.isValidTunisianPhone(phone);
                } else if ("France (+33)".equals(country)) {
                    phoneValid = ValidationUtils.isValidFrenchPhone(phone);
                }

                if (!phoneValid) {
                    showAlert("Validation Error",
                        ValidationUtils.getPhoneErrorMessage(
                            "Tunisia (+216)".equals(country) ? "TN" : "FR", phone),
                        Alert.AlertType.ERROR);
                    return;
                }

                if (!ValidationUtils.isValidCoverLetter(coverLetter)) {
                    showAlert("Validation Error",
                        ValidationUtils.getCoverLetterErrorMessage(coverLetter),
                        Alert.AlertType.ERROR);
                    return;
                }

                updateApplicationWithTracking(app, phone, coverLetter, cvPath);
            }
        });
    }

    private void generateCoverLetterForEdit(ApplicationService.ApplicationRow app, TextArea letterArea, TextField cvPathField) {
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Generating Cover Letter");
        loadingAlert.setHeaderText(null);
        loadingAlert.setContentText("Generating your personalized cover letter...\nThis may take a moment.");
        loadingAlert.getButtonTypes().setAll(ButtonType.CANCEL);
        loadingAlert.initModality(javafx.stage.Modality.NONE);
        loadingAlert.show();

        new Thread(() -> {
            try {
                UserService.UserInfo candidateInfo = UserService.getUserInfo(app.candidateId());
                if (candidateInfo == null) {
                    javafx.application.Platform.runLater(() -> {
                        loadingAlert.close();
                        showAlert("Error", "Could not retrieve candidate information.", Alert.AlertType.ERROR);
                    });
                    return;
                }

                JobOfferService.JobOfferRow offer = JobOfferService.getById(app.offerId());
                String jobTitle = offer != null ? offer.title() : (app.jobTitle() != null ? app.jobTitle() : "Job Offer");
                String companyName = offer != null ? UserService.getRecruiterCompanyName(offer.recruiterId()) : null;
                if (companyName == null || companyName.isEmpty()) {
                    companyName = "Our Company";
                }

                String experience = candidateInfo.experienceYears() != null && candidateInfo.experienceYears() > 0
                    ? candidateInfo.experienceYears() + " years of experience"
                    : "No specific experience years provided";

                String education = candidateInfo.educationLevel() != null && !candidateInfo.educationLevel().isEmpty()
                    ? candidateInfo.educationLevel()
                    : "Not specified";

                java.util.List<String> candidateSkills = UserService.getCandidateSkills(app.candidateId());

                String cvContent = "";
                String cvPath = cvPathField.getText();
                if (cvPath == null || cvPath.isBlank()) {
                    cvPath = app.cvPath();
                }
                if (cvPath != null && cvPath.startsWith("Current: ")) {
                    cvPath = cvPath.substring("Current: ".length()).trim();
                }
                if (cvPath != null && !cvPath.isBlank()) {
                    try {
                        FileService fileService = new FileService();
                        cvContent = fileService.extractTextFromPDF(cvPath);
                        if (cvContent == null) {
                            cvContent = "";
                        }
                    } catch (Exception e) {
                        System.err.println("Could not extract CV text: " + e.getMessage());
                        cvContent = "";
                    }
                }

                String generatedCoverLetter = GrokAIService.generateCoverLetter(
                    candidateInfo.firstName() + " " + candidateInfo.lastName(),
                    candidateInfo.email(),
                    candidateInfo.phone(),
                    jobTitle,
                    companyName,
                    experience,
                    education,
                    candidateSkills,
                    cvContent
                );

                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();

                    if (generatedCoverLetter != null && !generatedCoverLetter.isEmpty()) {
                        Alert reviewAlert = new Alert(Alert.AlertType.INFORMATION);
                        reviewAlert.setTitle("Generated Cover Letter");
                        reviewAlert.setHeaderText("Review and edit as needed:");

                        TextArea textArea = new TextArea(generatedCoverLetter);
                        textArea.setWrapText(true);
                        textArea.setPrefRowCount(15);
                        textArea.setStyle("-fx-font-size: 12px;");

                        reviewAlert.getDialogPane().setContent(textArea);
                        reviewAlert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

                        var result = reviewAlert.showAndWait();
                        if (result.isPresent() && result.get() == ButtonType.OK) {
                            letterArea.setText(generatedCoverLetter);
                            showAlert("Success", "Cover letter inserted! You can still edit it.", Alert.AlertType.INFORMATION);
                        }
                    } else {
                        showAlert("Error", "Failed to generate cover letter. Please write one manually.", Alert.AlertType.ERROR);
                    }
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    loadingAlert.close();
                    showAlert("Error", "Error generating cover letter: " + e.getMessage(), Alert.AlertType.ERROR);
                });
            }
        }).start();
    }

    private void updateApplicationWithTracking(ApplicationService.ApplicationRow app, String newPhone, String newCoverLetter, String newCvPath) {
        // Track what changed
        List<String> changes = new ArrayList<>();
        String oldPhone = app.phone() != null ? app.phone() : "";
        String oldCoverLetter = app.coverLetter() != null ? app.coverLetter() : "";
        String oldCvPath = app.cvPath() != null ? app.cvPath() : "";

        if (!oldPhone.equals(newPhone)) {
            changes.add("phone number");
        }
        if (!oldCoverLetter.equals(newCoverLetter)) {
            changes.add("cover letter");
        }

        // Handle CV file upload if new file selected
        String finalCvPath = oldCvPath;
        if (newCvPath != null && !newCvPath.isEmpty() && !newCvPath.equals(oldCvPath)) {
            try {
                java.io.File newCvFile = new java.io.File(newCvPath);
                if (newCvFile.exists()) {
                    // Delete old CV if it exists
                    if (!oldCvPath.isEmpty()) {
                        try {
                            FileService fileService = new FileService();
                            fileService.deletePDF(oldCvPath);
                            System.out.println("Old PDF deleted: " + oldCvPath);
                        } catch (Exception e) {
                            System.err.println("Error deleting old PDF: " + e.getMessage());
                        }
                    }

                    // Upload new CV
                    FileService fileService = new FileService();
                    finalCvPath = fileService.uploadPDF(newCvFile);
                    System.out.println("New PDF uploaded: " + finalCvPath);
                    changes.add("CV");
                }
            } catch (Exception e) {
                showAlert("Error", "Failed to upload new CV: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
                return;
            }
        }

        if (changes.isEmpty()) {
            showAlert("Info", "No changes made", Alert.AlertType.INFORMATION);
            return;
        }

        // Generate note based on changes
        String note = "Candidate changed the " + String.join(" and ", changes);
        System.out.println("Change note: " + note);

        // Update application
        try {
            ApplicationService.update(app.id(), newPhone, newCoverLetter, finalCvPath);
            System.out.println("Application updated in database");

            // Add to status history
            Long candidateId = UserContext.getCandidateId();
            ApplicationStatusHistoryService.addStatusHistory(app.id(), app.currentStatus(), candidateId, note);
            System.out.println("Status history added for application: " + app.id());

            loadApplications();
            showAlert("Success", "Application updated!\n\n" + note, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to update application: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void updateApplicationStatus(ApplicationService.ApplicationRow app, String newStatus, String note) {
        try {
            Long recruiterId = UserContext.getRecruiterId();

            // Generate automatic note if empty
            String finalNote = note;
            if (note == null || note.trim().isEmpty()) {
                finalNote = generateStatusChangeNote(app.currentStatus(), newStatus);
            }

            ApplicationService.updateStatus(app.id(), newStatus, recruiterId, finalNote);
            loadApplications();
            showAlert("Success", "Status updated to: " + newStatus + "\nNote: " + finalNote, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to update status: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private String generateStatusChangeNote(String oldStatus, String newStatus) {
        return switch (newStatus) {
            case "SUBMITTED" -> "Application re-submitted for review";
            case "IN_REVIEW" -> "Recruiter is now reviewing this application";
            case "SHORTLISTED" -> "Candidate has been shortlisted";
            case "REJECTED" -> "Application has been rejected";
            case "INTERVIEW" -> "Candidate is scheduled for interview";
            case "HIRED" -> "Candidate has been hired";
            default -> "Status updated to " + newStatus;
        };
    }

    private void acceptApplication(ApplicationService.ApplicationRow app) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String note = "Recruiter likes this profile and has shortlisted the candidate";

            ApplicationService.updateStatus(app.id(), "SHORTLISTED", recruiterId, note);
            loadApplications();
            showAlert("Success", "Application accepted!\n\nCandidate has been shortlisted", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to accept application: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void startReviewApplication(ApplicationService.ApplicationRow app) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String note = "Recruiter has started reviewing this application";

            ApplicationService.updateStatus(app.id(), "IN_REVIEW", recruiterId, note);
            loadApplications();
            showAlert("Success", "Application status changed to In Review", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to change status: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void rejectApplication(ApplicationService.ApplicationRow app) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            String note = "Recruiter has reviewed the profile and decided not to proceed";

            ApplicationService.updateStatus(app.id(), "REJECTED", recruiterId, note);
            loadApplications();
            showAlert("Success", "Application rejected", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to reject application: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void deleteApplication(ApplicationService.ApplicationRow app) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Application");
        confirmation.setHeaderText("Are you sure?");
        confirmation.setContentText("This action cannot be undone.");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                ApplicationService.delete(app.id());
                loadApplications();
                showAlert("Success", "Application deleted!", Alert.AlertType.INFORMATION);
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

    private void downloadPDF(ApplicationService.ApplicationRow app) {
        try {
            java.io.File pdfFile = ApplicationService.downloadPDF(app.id());

            // Open file with default PDF viewer
            if (javafx.application.HostServices.class != null) {
                Desktop.getDesktop().open(pdfFile);
                showAlert("Success", "Opening PDF file...", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            showAlert("Error", "Could not download PDF: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void editStatusHistory(ApplicationStatusHistoryService.StatusHistoryRow record) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Status History");
        dialog.setHeaderText("Edit Status History Entry #" + record.id());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        // Status field (editable for admin)
        Label statusLabel = new Label("Status:");
        statusLabel.setStyle("-fx-font-weight: bold;");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        statusCombo.setValue(record.status() != null ? record.status() : "SUBMITTED");
        statusCombo.setPrefWidth(240);

        // Date field (read-only)
        Label dateLabel = new Label("Changed At:");
        dateLabel.setStyle("-fx-font-weight: bold;");
        TextField dateField = new TextField(record.changedAt() != null ?
            record.changedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) : "N/A");
        dateField.setEditable(false);
        dateField.setStyle("-fx-opacity: 0.7;");

        // Note field (editable) with validation
        Label noteLabel = new Label("Note * (Min 5 - Max 255 characters)");
        noteLabel.setStyle("-fx-font-weight: bold;");
        TextArea noteArea = new TextArea(record.note() != null ? record.note() : "");
        noteArea.setPromptText("Edit note... (min 5, max 255 characters)");
        noteArea.setPrefRowCount(6);
        noteArea.setWrapText(true);
        noteArea.setStyle("-fx-font-size: 13px;");

        String initialNote = record.note() != null ? record.note() : "";
        Label noteCharCount = new Label(initialNote.length() + "/255");
        noteCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        Label noteErrorLabel = new Label();
        noteErrorLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px;");
        noteErrorLabel.setVisible(false);

        // Update character count in real-time
        noteArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int length = newVal != null ? newVal.length() : 0;
            noteCharCount.setText(length + "/255");

            if (length > 255) {
                noteCharCount.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-weight: bold;");
                noteErrorLabel.setText(ValidationUtils.getNoteErrorMessage(newVal));
                noteErrorLabel.setVisible(true);
            } else if (length > 0 && length < 5) {
                noteCharCount.setStyle("-fx-text-fill: #fd7e14; -fx-font-size: 11px;");
                noteErrorLabel.setText(ValidationUtils.getNoteErrorMessage(newVal));
                noteErrorLabel.setVisible(true);
            } else if (length == 0) {
                noteCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                noteErrorLabel.setText(ValidationUtils.getNoteErrorMessage(newVal));
                noteErrorLabel.setVisible(true);
            } else {
                noteCharCount.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                noteErrorLabel.setVisible(false);
            }
        });

        VBox noteBox = new VBox(8);
        noteBox.getChildren().addAll(noteLabel, noteArea, noteCharCount, noteErrorLabel);

        HBox statusRow = new HBox(12);
        statusRow.getChildren().addAll(statusLabel, statusCombo);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        content.getChildren().addAll(
            statusRow,
            dateLabel, dateField,
            noteBox
    private String getStatusClass(String status) {
        return switch (status.toLowerCase()) {
            case "pending" -> "status-new";
            case "shortlisted" -> "status-shortlisted";
            case "reviewing" -> "status-reviewing";
            default -> "status-new";
        };
    }

    private void selectApplication(ApplicationService.ApplicationRow app, VBox card) {
        // Remove selection from all cards
        if (candidateListContainer != null) {
            candidateListContainer.getChildren().forEach(node -> {
                if (node instanceof VBox) {
                    node.getStyleClass().remove("candidate-card-selected");
                }
            });
        }

        // Add selection to clicked card
        card.getStyleClass().add("candidate-card-selected");
        selectedApplication = app;

        // Update detail view
        loadDetailView(app);
    }

    private void loadDetailView(ApplicationService.ApplicationRow app) {
        // Find the detail container in the parent scene
        if (detailContainer == null) {
            detailContainer = findDetailContainer();
        }

        if (detailContainer == null) return;

        detailContainer.getChildren().clear();

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (isRecruiter) {
            // Recruiter view: Show candidate details
            loadRecruiterDetailView(app);
        } else {
            // Candidate view: Show job details and application status
            loadCandidateDetailView(app);
        }
    }

    private void loadCandidateDetailView(ApplicationService.ApplicationRow app) {
        // Header with job title
        VBox headerCard = new VBox(15);
        headerCard.getStyleClass().add("detail-header-card");

        Label jobTitle = new Label(app.jobTitle() != null ? app.jobTitle() : "Job Position");
        jobTitle.getStyleClass().add("detail-candidate-name");

        Label appliedDate = new Label("Applied on: " +
            (app.appliedAt() != null ? app.appliedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A"));
        appliedDate.getStyleClass().add("detail-candidate-position");

        headerCard.getChildren().addAll(jobTitle, appliedDate);
        detailContainer.getChildren().add(headerCard);

        // Application Status Card
        VBox statusCard = new VBox(15);
        statusCard.getStyleClass().add("detail-section-card");

        Label statusTitle = new Label("Application Status");
        statusTitle.getStyleClass().add("detail-section-title");

        Label statusBadge = new Label(app.currentStatus() != null ? app.currentStatus() : "SUBMITTED");
        statusBadge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 8 16; " +
                           "-fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: 600;");

        statusCard.getChildren().addAll(statusTitle, statusBadge);
        detailContainer.getChildren().add(statusCard);

        // Cover Letter Section
        if (app.coverLetter() != null && !app.coverLetter().trim().isEmpty()) {
            VBox coverLetterCard = new VBox(12);
            coverLetterCard.getStyleClass().add("detail-section-card");

            Label coverLetterTitle = new Label("Your Cover Letter");
            coverLetterTitle.getStyleClass().add("detail-section-title");

            Label coverLetterText = new Label(app.coverLetter());
            coverLetterText.setWrapText(true);
            coverLetterText.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px; -fx-line-spacing: 2;");

            coverLetterCard.getChildren().addAll(coverLetterTitle, coverLetterText);
            detailContainer.getChildren().add(coverLetterCard);
        }

        // Contact Information
        VBox contactCard = new VBox(15);
        contactCard.getStyleClass().add("detail-section-card");

        Label contactTitle = new Label("Your Contact Information");
        contactTitle.getStyleClass().add("detail-section-title");

        HBox contactInfo = new HBox(25);

        VBox phoneBox = new VBox(6);
        Label phoneLabel = new Label("📞 Phone");
        phoneLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #6c757d; -fx-font-size: 12px;");
        Label phoneValue = new Label(app.phone() != null ? app.phone() : "Not provided");
        phoneValue.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        phoneBox.getChildren().addAll(phoneLabel, phoneValue);

        VBox cvBox = new VBox(6);
        Label cvLabel = new Label("📄 CV");
        cvLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #6c757d; -fx-font-size: 12px;");
        Label cvValue = new Label(app.cvPath() != null ? "Uploaded" : "Not uploaded");
        cvValue.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        cvBox.getChildren().addAll(cvLabel, cvValue);

        contactInfo.getChildren().addAll(phoneBox, cvBox);
        contactCard.getChildren().addAll(contactTitle, contactInfo);
        detailContainer.getChildren().add(contactCard);
    }

    private void loadRecruiterDetailView(ApplicationService.ApplicationRow app) {
        // Create detail header card
        VBox headerCard = createDetailHeader(app);
        detailContainer.getChildren().add(headerCard);

        // Cover Letter Section
        if (app.coverLetter() != null && !app.coverLetter().trim().isEmpty()) {
            VBox coverLetterCard = new VBox(12);
            coverLetterCard.getStyleClass().add("detail-section-card");

            Label coverLetterTitle = new Label("Cover Letter");
            coverLetterTitle.getStyleClass().add("detail-section-title");

            Label coverLetterText = new Label(app.coverLetter());
            coverLetterText.setWrapText(true);
            coverLetterText.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px; -fx-line-spacing: 2;");

            coverLetterCard.getChildren().addAll(coverLetterTitle, coverLetterText);
            detailContainer.getChildren().add(coverLetterCard);
        }

        // Application Status Section
        VBox statusSection = new VBox(15);
        statusSection.getStyleClass().add("detail-section-card");

        Label statusTitle = new Label("Application Status");
        statusTitle.getStyleClass().add("detail-section-title");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getStyleClass().add("modern-combo");
        statusCombo.setPrefWidth(300);
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED",
                                      "INTERVIEW", "REJECTED", "HIRED");
        statusCombo.setValue(app.currentStatus());
        statusCombo.setOnAction(e -> {
            ApplicationService.updateStatus(app.id(), statusCombo.getValue());
        });

        statusSection.getChildren().addAll(statusTitle, statusCombo);
        detailContainer.getChildren().add(statusSection);

        // Action Buttons (only for recruiters)
        HBox actionButtons = new HBox(15);
        actionButtons.getStyleClass().add("action-buttons-container");
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        Button btnSchedule = new Button("📅 Schedule Interview");
        btnSchedule.getStyleClass().addAll("btn-primary", "action-button");
        btnSchedule.setOnAction(e -> handleScheduleInterview(app));

        Button btnReject = new Button("✗ Reject");
        btnReject.getStyleClass().addAll("btn-danger", "action-button");
        btnReject.setOnAction(e -> handleRejectCandidate(app));

        Button btnDownload = new Button("📥 Download CV");
        btnDownload.getStyleClass().addAll("btn-secondary", "action-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionButtons.getChildren().addAll(btnSchedule, btnDownload, spacer, btnReject);
        detailContainer.getChildren().add(actionButtons);
    }

    private VBox createDetailHeader(ApplicationService.ApplicationRow app) {
        VBox headerCard = new VBox(15);
        headerCard.getStyleClass().add("detail-header-card");

        // Name and position
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(8);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label name = new Label(app.candidateName() != null && !app.candidateName().trim().isEmpty()
                              ? app.candidateName()
                              : "Candidate #" + app.id());
        name.getStyleClass().add("detail-candidate-name");

        Label position = new Label(app.jobTitle() != null ? "Applied for: " + app.jobTitle() : "Application");
        position.getStyleClass().add("detail-candidate-position");

        nameBox.getChildren().addAll(name, position);

        Label statusBadge = new Label(app.currentStatus() != null ? app.currentStatus() : "NEW");
        statusBadge.getStyleClass().add("detail-rating-badge");

        header.getChildren().addAll(nameBox, statusBadge);

        // Separator
        Separator sep = new Separator();
        sep.getStyleClass().add("detail-separator");

        // Contact info
        HBox contactInfo = new HBox(30);

        VBox emailBox = new VBox(5);
        Label emailLabel = new Label("📧 Email");
        emailLabel.getStyleClass().add("detail-label");
        Label emailValue = new Label(app.candidateEmail() != null ? app.candidateEmail() : "N/A");
        emailValue.getStyleClass().add("detail-value");
        emailBox.getChildren().addAll(emailLabel, emailValue);

        VBox phoneBox = new VBox(5);
        Label phoneLabel = new Label("📞 Phone");
        phoneLabel.getStyleClass().add("detail-label");
        Label phoneValue = new Label(app.phone() != null ? app.phone() : "N/A");
        phoneValue.getStyleClass().add("detail-value");
        phoneBox.getChildren().addAll(phoneLabel, phoneValue);

        VBox dateBox = new VBox(5);
        Label dateLabel = new Label("📅 Applied On");
        dateLabel.getStyleClass().add("detail-label");
        Label dateValue = new Label(app.appliedAt() != null ?
                                    app.appliedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A");
        dateValue.getStyleClass().add("detail-value");
        dateBox.getChildren().addAll(dateLabel, dateValue);

        contactInfo.getChildren().addAll(emailBox, phoneBox, dateBox);

        headerCard.getChildren().addAll(header, sep, contactInfo);
        return headerCard;
    }

    private VBox findDetailContainer() {
        // Navigate up the scene graph to find the detail container
        try {
            if (candidateListContainer != null && candidateListContainer.getScene() != null) {
                // Get the root of the scene
                Parent root = candidateListContainer.getScene().getRoot();

                // Find the main content HBox (the split view)
                VBox mainVBox = findMainVBox(root);
                if (mainVBox != null) {
                    // Find the HBox that contains the split view
                    for (var node : mainVBox.getChildren()) {
                        if (node instanceof HBox hbox && hbox.getStyleClass().contains("split-view-container")) {
                            // Get the right side (detail view container)
                            if (hbox.getChildren().size() > 1) {
                                var rightSide = hbox.getChildren().get(1);
                                if (rightSide instanceof VBox detailViewContainer) {
                                    // Find the ScrollPane inside
                                    for (var child : detailViewContainer.getChildren()) {
                                        if (child instanceof ScrollPane scroll) {
                                            return (VBox) scroll.getContent();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not find detail container: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private VBox findMainVBox(Parent root) {
        if (root instanceof VBox vbox) {
            return vbox;
        }
        if (root instanceof BorderPane borderPane) {
            if (borderPane.getCenter() instanceof StackPane stackPane) {
                if (stackPane.getChildren().size() > 0 && stackPane.getChildren().get(0) instanceof VBox vbox) {
                    return vbox;
                }
            }
        }
        return null;
    }

    private void handleAcceptCandidate(ApplicationService.ApplicationRow app) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Accepter le Candidat");
        confirm.setHeaderText("Accepter ce candidat?");
        confirm.setContentText("Cela changera le statut de la candidature à 'Accepté'.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Update application status
                showAlert("Succès", "Candidat accepté avec succès!", Alert.AlertType.INFORMATION);
                loadApplications();
            }
        });
    }

    private void handleScheduleInterview(ApplicationService.ApplicationRow app) {
        showInterviewScheduleDialog(app);
    }

    private void handleRejectCandidate(ApplicationService.ApplicationRow app) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rejeter le Candidat");
        confirm.setHeaderText("Rejeter ce candidat?");
        confirm.setContentText("Cette action ne peut pas être annulée.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Succès", "Candidat rejeté.", Alert.AlertType.INFORMATION);
                loadApplications();
            }
        });
    }

    private void showInterviewScheduleDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Planifier un Entretien");
        dialog.setHeaderText("Planifier un entretien pour la candidature #" + app.id());

        // Modern styled content
        VBox content = new VBox(18);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: white;");

        // Date picker with label
        VBox dateBox = new VBox(8);
        Label dateLabel = new Label("Date de l'entretien *");
        dateLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(java.time.LocalDate.now().plusDays(7));
        datePicker.setStyle("-fx-pref-width: 350px;");
        dateBox.getChildren().addAll(dateLabel, datePicker);

        // Time field with label
        VBox timeBox = new VBox(8);
        Label timeLabel = new Label("Heure (HH:mm) *");
        timeLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField timeField = new TextField("14:00");
        timeField.setPromptText("ex: 14:00");
        timeField.setStyle("-fx-pref-width: 350px;");
        timeBox.getChildren().addAll(timeLabel, timeField);

        // Duration with label
        VBox durationBox = new VBox(8);
        Label durationLabel = new Label("Durée (minutes) *");
        durationLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField durationField = new TextField("60");
        durationField.setPromptText("ex: 60");
        durationField.setStyle("-fx-pref-width: 350px;");
        durationBox.getChildren().addAll(durationLabel, durationField);

        // Mode selection with label
        VBox modeBox = new VBox(8);
        Label modeLabel = new Label("Mode d'entretien *");
        modeLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("ONLINE", "ON_SITE");
        modeCombo.setValue("ONLINE");
        modeCombo.setStyle("-fx-pref-width: 350px;");
        modeBox.getChildren().addAll(modeLabel, modeCombo);

        // Meeting Link (for ONLINE)
        VBox linkBox = new VBox(8);
        Label linkLabel = new Label("Lien de réunion (généré automatiquement pour ONLINE)");
        linkLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        HBox linkInputBox = new HBox(10);
        linkInputBox.setAlignment(Pos.CENTER_LEFT);
        TextField linkField = new TextField();
        linkField.setPromptText("Cliquez sur Générer...");
        linkField.setEditable(false);
        linkField.setStyle("-fx-pref-width: 230px; -fx-background-color: #f8f9fa;");

        Button generateLinkBtn = new Button("Générer le lien");
        generateLinkBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 14;");

        // "Open" hyperlink — only visible after generation
        Hyperlink testLink = new Hyperlink("Tester");
        testLink.setStyle("-fx-text-fill: #5BA3F5; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        testLink.setVisible(false);
        testLink.setManaged(false);
        testLink.setOnAction(ev -> {
            String url = linkField.getText().trim();
            if (!url.isEmpty()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    showAlert("Lien de réunion", "Copiez ce lien dans votre navigateur:\n" + url, Alert.AlertType.INFORMATION);
                }
            }
        });

        generateLinkBtn.setOnAction(e -> {
            if (datePicker.getValue() != null && !timeField.getText().trim().isEmpty()) {
                try {
                    LocalDateTime scheduledAt = LocalDateTime.of(
                        datePicker.getValue(),
                        java.time.LocalTime.parse(timeField.getText())
                    );
                    int dur = 60;
                    try { dur = Integer.parseInt(durationField.getText().trim()); } catch (NumberFormatException ignored) {}
                    // Use app.id as the interview identifier for the room name
                    String meetingLink = MeetingService.generateMeetingLink(app.id(), scheduledAt, dur);
                    linkField.setText(meetingLink);
                    linkField.setStyle("-fx-pref-width: 230px; -fx-background-color: #d4edda;");
                    testLink.setVisible(true);
                    testLink.setManaged(true);
                } catch (Exception ex) {
                    showAlert("Erreur", "Veuillez d'abord sélectionner une date et heure valides.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Erreur", "Veuillez d'abord sélectionner une date et heure.", Alert.AlertType.WARNING);
            }
        });

        Label autoGenNote = new Label("Le lien sera généré automatiquement si vous ne cliquez pas sur Générer");
        autoGenNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        autoGenNote.setWrapText(true);

        linkInputBox.getChildren().addAll(linkField, generateLinkBtn, testLink);
        linkBox.getChildren().addAll(linkLabel, linkInputBox, autoGenNote);

        // Location (for ON_SITE)
        VBox locationBox = new VBox(8);
        Label locationLabel = new Label("Lieu (pour entretiens ON_SITE)");
        locationLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField locationField = new TextField();
        locationField.setPromptText("ex: Bâtiment A, Salle 301");
        locationField.setStyle("-fx-pref-width: 350px;");
        locationBox.getChildren().addAll(locationLabel, locationField);

        // Notes section
        VBox notesBox = new VBox(8);
        Label notesLabel = new Label("Notes supplémentaires (Optionnel)");
        notesLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Entrez des notes supplémentaires pour l'entretien...");
        notesArea.setPrefRowCount(3);
        notesArea.setStyle("-fx-pref-width: 350px;");
        notesBox.getChildren().addAll(notesLabel, notesArea);

        // Toggle visibility based on mode
        Runnable toggleFields = () -> {
            boolean isOnline = "ONLINE".equals(modeCombo.getValue());
            linkBox.setVisible(isOnline);
            linkBox.setManaged(isOnline);
            locationBox.setVisible(!isOnline);
            locationBox.setManaged(!isOnline);
        };
        modeCombo.valueProperty().addListener((obs, oldVal, newVal) -> toggleFields.run());
        toggleFields.run();

        // Add separator for visual organization
        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10 0;");

        content.getChildren().addAll(
            dateBox, timeBox, durationBox, modeBox,
            separator, linkBox, locationBox, notesBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                String noteText = noteArea.getText();
                String selectedStatus = statusCombo.getValue();

                // Validate note before saving
                if (!ValidationUtils.isValidNote(noteText)) {
                    showAlert("Validation Error",
                        ValidationUtils.getNoteErrorMessage(noteText),
                        Alert.AlertType.ERROR);
                    return;
                }

                try {
                    // Use the new service method to update both status and note (which also syncs application current_status)
                    ApplicationStatusHistoryService.updateStatusHistory(record.id(), selectedStatus, noteText);
                    loadApplications();
                    showAlert("Success", "Status history updated!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Error", "Failed to update status history: " + e.getMessage(), Alert.AlertType.ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    private void deleteStatusHistory(ApplicationStatusHistoryService.StatusHistoryRow record, ApplicationService.ApplicationRow app) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Status History");
        confirmation.setHeaderText("Are you sure?");
        confirmation.setContentText("Delete this status history entry?\nThis action cannot be undone.");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    ApplicationStatusHistoryService.deleteStatusHistory(record.id());
                    loadApplications();
                    showAlert("Success", "Status history entry deleted!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Error", "Failed to delete status history: " + e.getMessage(), Alert.AlertType.ERROR);
                    e.printStackTrace();
                }
            }
        });
    }

    private void bulkUpdateStatus(List<Long> applicationIds, String newStatus, String note) {
        try {
            Long recruiterId = UserContext.getRecruiterId();

            // Generate automatic note if empty
            String finalNote = note;
            if (note == null || note.trim().isEmpty()) {
                finalNote = generateStatusChangeNote("MULTIPLE", newStatus);
            }

            ApplicationService.bulkUpdateStatus(applicationIds, newStatus, recruiterId, finalNote);
            selectedApplicationIds.clear();
            loadApplications();
            showAlert("Success", "Updated " + applicationIds.size() + " application(s) to: " + newStatus, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to update statuses: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void updateRankingStatus(String text, boolean success) {
        if (rankingStatusLabel == null) return;
        rankingStatusLabel.setText(text);
        rankingStatusLabel.setStyle(success
            ? "-fx-text-fill: #28a745; -fx-font-weight: bold;"
            : "-fx-text-fill: #dc3545; -fx-font-weight: bold;");
    }

    private void rankApplicationsWithAI() {
        if (UserContext.getRole() != UserContext.Role.RECRUITER) {
            showAlert("Warning", "Only recruiters can rank applications", Alert.AlertType.WARNING);
            return;
        }
        if (currentApplications == null || currentApplications.isEmpty()) {
            showAlert("Info", "No applications to rank", Alert.AlertType.INFORMATION);
            return;
        }

        rankingCache.clear();
        rankingActive = true;
        updateRankingStatus("Ranking: 0/" + currentApplications.size(), false);

        new Thread(() -> {
            int total = currentApplications.size();
            int done = 0;

            for (ApplicationService.ApplicationRow app : currentApplications) {
                OllamaRankingService.RankResult result = buildRankForApplication(app);
                if (result != null) {
                    rankingCache.put(app.id(), result);
                }
                done++;
                int progress = done;

                javafx.application.Platform.runLater(() -> {
                    updateRankingStatus("Ranking: " + progress + "/" + total, false);
                    if (progress == total) {
                        updateRankingStatus("Ranking: COMPLETE", true);
                        renderApplications(currentApplications, true);
                    }
                });
            }
        }).start();
    }

    private OllamaRankingService.RankResult buildRankForApplication(ApplicationService.ApplicationRow app) {
        try {
            JobOfferService.JobOfferRow offer = JobOfferService.getById(app.offerId());
            List<String> offerSkills = JobOfferService.getOfferSkills(app.offerId());

            UserService.UserInfo userInfo = UserService.getUserInfo(app.candidateId());
            String experience = userInfo != null && userInfo.experienceYears() != null && userInfo.experienceYears() > 0
                ? userInfo.experienceYears() + " years of experience"
                : "No specific experience years provided";
            String education = userInfo != null && userInfo.educationLevel() != null && !userInfo.educationLevel().isEmpty()
                ? userInfo.educationLevel()
                : "Not specified";

            List<String> candidateSkills = UserService.getCandidateSkills(app.candidateId());
            String cvContent = extractCvText(app.cvPath());

            String jobTitle = offer != null ? offer.title() : (app.jobTitle() != null ? app.jobTitle() : "Job Offer");
            String jobDescription = offer != null ? offer.description() : "";

            return OllamaRankingService.rankApplication(
                jobTitle,
                jobDescription,
                offerSkills,
                app.candidateName() != null ? app.candidateName() : "Candidate",
                experience,
                education,
                candidateSkills,
                app.coverLetter(),
                cvContent
            );
        } catch (Exception e) {
            System.err.println("Ranking failed for application " + app.id() + ": " + e.getMessage());
            return null;
        }
    }

    private String extractCvText(String cvPath) {
        if (cvPath == null || cvPath.isEmpty()) {
            return "";
        }
        try {
            FileService fileService = new FileService();
            String text = fileService.extractTextFromPDF(cvPath);
            return text == null ? "" : text;
        } catch (Exception e) {
            System.err.println("Failed to extract CV text: " + e.getMessage());
            return "";
        }
        // Style the buttons
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        dialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Validation
                    if (datePicker.getValue() == null) {
                        showAlert("Erreur de Validation", "Veuillez sélectionner une date.", Alert.AlertType.WARNING);
                        return;
                    }

                    LocalDateTime scheduledAt = LocalDateTime.of(
                        datePicker.getValue(),
                        java.time.LocalTime.parse(timeField.getText())
                    );
                    int duration = Integer.parseInt(durationField.getText());
                    String mode = modeCombo.getValue();

                    // Validate required fields based on mode
                    if ("ONLINE".equals(mode)) {
                        // Auto-generate meeting link if not already set
                        if (linkField.getText() == null || linkField.getText().trim().isEmpty()) {
                            int autoDuration = duration > 0 ? duration : 60;
                            String autoGeneratedLink = MeetingService.generateMeetingLink(app.id(), scheduledAt, autoDuration);
                            linkField.setText(autoGeneratedLink);
                        }
                    }

                    if ("ON_SITE".equals(mode) && (locationField.getText() == null || locationField.getText().trim().isEmpty())) {
                        showAlert("Erreur de Validation", "Le lieu est requis pour les entretiens ON_SITE.", Alert.AlertType.WARNING);
                        return;
                    }

                    // Create interview linked to application
                    Models.Interview interview = new Models.Interview(
                        app.id(),
                        Utils.UserContext.getRecruiterId(), // Use actual recruiter ID from context
                        scheduledAt,
                        duration,
                        mode
                    );
                    interview.setStatus("SCHEDULED");
                    interview.setNotes(notesArea.getText());

                    // Set meeting link or location based on mode
                    if ("ONLINE".equals(mode)) {
                        interview.setMeetingLink(linkField.getText().trim());
                    } else {
                        interview.setLocation(locationField.getText().trim());
                    }

                    InterviewService.addInterview(interview);

                    // Email reminder will be sent automatically by InterviewReminderScheduler
                    // (24 hours before the interview)

                    showAlert("Succès", "Entretien planifié avec succès! Un rappel par email sera envoyé 24h avant.", Alert.AlertType.INFORMATION);
                    loadApplications();
                } catch (NumberFormatException e) {
                    showAlert("Erreur de Validation", "La durée doit être un nombre valide.", Alert.AlertType.WARNING);
                } catch (Exception e) {
                    showAlert("Erreur", "Échec de la planification de l'entretien: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadApplications();
    }

    @FXML
    private void handleAcceptAndSchedule() {
        if (selectedApplication != null) {
            handleScheduleInterview(selectedApplication);
        } else {
            showAlert("Aucune Sélection", "Veuillez d'abord sélectionner une candidature.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleReject() {
        if (selectedApplication != null) {
            handleRejectCandidate(selectedApplication);
        } else {
            showAlert("Aucune Sélection", "Veuillez d'abord sélectionner une candidature.", Alert.AlertType.WARNING);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
