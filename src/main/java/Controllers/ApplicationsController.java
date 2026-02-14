package Controllers;

import Services.ApplicationService;
import Services.ApplicationStatusHistoryService;
import Services.FileService;
import Services.JobOfferService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
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

            Button btnAccept = new Button("✓ Accept (Shortlist)");
            btnAccept.setStyle("-fx-padding: 8 15; -fx-background-color: #28a745; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
            btnAccept.setOnAction(e -> acceptApplication(app));

            Button btnReject = new Button("✕ Reject");
            btnReject.setStyle("-fx-padding: 8 15; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
            btnReject.setOnAction(e -> rejectApplication(app));

            quickActionsBox.getChildren().addAll(btnAccept, btnReject);

            statusUpdateBox.getChildren().addAll(statusLabel, statusCombo, new Label("Note:"), noteArea, btnUpdate, quickActionsBox);
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

        // CV/PDF file selection
        HBox cvBox = new HBox(10);
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

        content.getChildren().addAll(
            new Label("Phone:"),
            phoneField,
            new Label("Cover Letter:"),
            coverLetterArea,
            new Label("CV/PDF (optional):"),
            cvBox
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                updateApplicationWithTracking(app, phoneField.getText(), coverLetterArea.getText(), cvPathField.getText());
            }
        });
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
}
