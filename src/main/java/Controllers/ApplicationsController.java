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

            Label coverLabel = new Label("Cover Letter:");
            coverLabel.setStyle("-fx-font-weight: bold;");

            TextArea coverText = new TextArea(app.coverLetter());
            coverText.setEditable(false);
            coverText.setWrapText(true);
            coverText.setPrefRowCount(5);
            coverText.setStyle("-fx-control-inner-background: #f8f9fa; -fx-text-fill: #333;");

            coverLetterBox.getChildren().addAll(coverLabel, coverText);
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
    }
}
