package Controllers;

import Services.ApplicationService;
import Services.ApplicationStatusHistoryService;
import Services.JobOfferService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
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

    @FXML
    public void initialize() {
        // mainContainer is injected from FXML - it's the right panel VBox
        if (mainContainer != null) {
            detailContainer = mainContainer;
        } else {
            detailContainer = new VBox(15);
        }
        loadApplications();
    }

    private void setupUI() {
        // setupUI is no longer needed as FXML handles the layout
    }

    private void loadApplications() {
        if (candidateListContainer == null) return;
        candidateListContainer.getChildren().clear();

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

        if (applications.isEmpty()) {
            Label empty = new Label("No applications found");
            empty.setStyle("-fx-text-fill: #999; -fx-font-size: 14px; -fx-padding: 30;");
            candidateListContainer.getChildren().add(empty);
            return;
        }

        // Add applications to list
        boolean first = true;
        for (ApplicationService.ApplicationRow app : applications) {
            VBox card = createApplicationCard(app);
            candidateListContainer.getChildren().add(card);

            if (first) {
                selectApplication(app, card);
                first = false;
            }
        }
    }

    private VBox createApplicationCard(ApplicationService.ApplicationRow app) {
        VBox card = new VBox(8);
        card.setStyle("-fx-border-color: #ddd; -fx-border-radius: 4; -fx-padding: 12; -fx-background-color: white; -fx-cursor: hand;");
        card.setUserData(app);

        Label candidateName = new Label(app.candidateName() != null && !app.candidateName().trim().isEmpty()
            ? app.candidateName()
            : "Candidate #" + app.id());
        candidateName.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");

        Label jobTitle = new Label(app.jobTitle() != null ? app.jobTitle() : "Job Application");
        jobTitle.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");

        HBox statusBox = new HBox(10);
        Label statusBadge = new Label(app.currentStatus());
        statusBadge.setStyle("-fx-padding: 4 8; -fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-border-radius: 3; -fx-font-size: 11;");

        Label appliedDate = new Label(app.appliedAt() != null
            ? app.appliedAt().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            : "N/A");
        appliedDate.setStyle("-fx-text-fill: #999; -fx-font-size: 11;");

        statusBox.getChildren().addAll(statusBadge, appliedDate);

        card.getChildren().addAll(candidateName, jobTitle, statusBox);
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
            Label cvLabel = new Label("CV Path: " + app.cvPath());
            cvLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12;");
            cvBox.getChildren().add(cvLabel);
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

                historyBox.getChildren().add(historyItem);
            }
        }

        detailContainer.getChildren().add(historyBox);

        // Actions section
        UserContext.Role role = UserContext.getRole();

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

            Button btnDelete = new Button("Delete Application");
            btnDelete.setStyle("-fx-padding: 6 12; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> deleteApplication(app));

            statusUpdateBox.getChildren().addAll(statusLabel, statusCombo, new Label("Note:"), noteArea, btnUpdate, btnDelete);
            actionsBox.getChildren().add(statusUpdateBox);
        } else if (role == UserContext.Role.ADMIN) {
            HBox buttonBox = new HBox(10);

            Button btnDelete = new Button("Delete");
            btnDelete.setStyle("-fx-padding: 6 12; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand;");
            btnDelete.setOnAction(e -> deleteApplication(app));

            buttonBox.getChildren().add(btnDelete);
            actionsBox.getChildren().add(buttonBox);
        }

        detailContainer.getChildren().add(actionsBox);
    }

    private void showEditApplicationDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Application");
        dialog.setHeaderText("Edit Application #" + app.id());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        TextField phoneField = new TextField(app.phone() != null ? app.phone() : "");
        phoneField.setPromptText("Phone number");
        phoneField.setPrefWidth(400);

        TextArea coverLetterArea = new TextArea(app.coverLetter() != null ? app.coverLetter() : "");
        coverLetterArea.setPromptText("Cover letter");
        coverLetterArea.setPrefRowCount(8);
        coverLetterArea.setWrapText(true);

        content.getChildren().addAll(
            new Label("Phone:"),
            phoneField,
            new Label("Cover Letter:"),
            coverLetterArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                ApplicationService.update(app.id(), phoneField.getText(), coverLetterArea.getText(), app.cvPath());
                loadApplications();
                showAlert("Success", "Application updated!", Alert.AlertType.INFORMATION);
            }
        });
    }

    private void updateApplicationStatus(ApplicationService.ApplicationRow app, String newStatus, String note) {
        try {
            Long recruiterId = UserContext.getRecruiterId();
            ApplicationService.updateStatus(app.id(), newStatus, recruiterId, note);
            loadApplications();
            showAlert("Success", "Status updated to: " + newStatus, Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            showAlert("Error", "Failed to update status: " + e.getMessage(), Alert.AlertType.ERROR);
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
}


