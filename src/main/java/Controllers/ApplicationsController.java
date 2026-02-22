package Controllers;

import Services.ApplicationService;
import Services.ApplicationStatusHistoryService;
import Services.FileService;
import Services.JobOfferService;
import Utils.UserContext;
import Utils.ValidationUtils;
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

        // Hide archived applications for non-admins
        if (role != UserContext.Role.ADMIN) {
            applications = applications.stream()
                .filter(app -> !app.isArchived())
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
            letterBox, letterErrorLabel,
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
}
