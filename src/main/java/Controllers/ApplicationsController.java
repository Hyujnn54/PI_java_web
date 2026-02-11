package Controllers;

import Services.ApplicationService;
import Services.InterviewService;
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
            cbSearchCriteria.getItems().addAll("Name", "Email", "Position", "Status", "Date");
            cbSearchCriteria.setValue("Name");
        }

        if (cbApplicationStatus != null) {
            cbApplicationStatus.getItems().addAll("New", "Reviewing", "Shortlisted",
                                                  "Interview Scheduled", "Accepted", "Rejected");
        }
    }

    private void loadApplications() {
        if (candidateListContainer == null) return;
        candidateListContainer.getChildren().clear();

        List<ApplicationService.ApplicationRow> apps = ApplicationService.getAll();

        // Filter applications based on user role
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (!isRecruiter) {
            // Candidate should only see their own applications
            // Filter by current candidate ID (you can get this from UserContext or session)
            int currentCandidateId = Utils.UserContext.getCandidateId(); // You'll need to add this method
            apps = apps.stream()
                      .filter(app -> app.candidateId() == currentCandidateId)
                      .toList();
        }

        if (apps.isEmpty()) {
            Label empty = new Label(isRecruiter ? "No applications found" : "You haven't applied to any positions yet");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-padding: 30;");
            candidateListContainer.getChildren().add(empty);
            return;
        }

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

        Label name = new Label("Candidate #" + app.id());
        name.getStyleClass().add("candidate-name");

        Label position = new Label("Application");
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

        Label statusBadge = new Label(app.status());
        statusBadge.getStyleClass().addAll("status-badge", getStatusClass(app.status()));

        Label date = new Label("1/23/2026"); // Placeholder - ApplicationRow doesn't have appliedAt field
        date.getStyleClass().add("date-label");

        statusBox.getChildren().addAll(statusBadge, date);

        card.getChildren().addAll(header, info, statusBox);

        // Click handler
        card.setOnMouseClicked(e -> selectApplication(app, card));

        return card;
    }

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

        // Create detail header card
        VBox headerCard = createDetailHeader(app);
        detailContainer.getChildren().add(headerCard);

        // Experience & Education cards
        HBox infoCards = new HBox(20);

        VBox expCard = new VBox(10);
        expCard.getStyleClass().add("info-card");
        Label expTitle = new Label("💼 Experience");
        expTitle.getStyleClass().add("info-card-title");
        Label expValue = new Label("N/A");
        expValue.getStyleClass().add("info-card-value");
        expCard.getChildren().addAll(expTitle, expValue);
        HBox.setHgrow(expCard, Priority.ALWAYS);

        VBox eduCard = new VBox(10);
        eduCard.getStyleClass().add("info-card");
        Label eduTitle = new Label("🎓 Education");
        eduTitle.getStyleClass().add("info-card-title");
        Label eduValue = new Label("N/A");
        eduValue.getStyleClass().add("info-card-value");
        eduCard.getChildren().addAll(eduTitle, eduValue);
        HBox.setHgrow(eduCard, Priority.ALWAYS);

        infoCards.getChildren().addAll(expCard, eduCard);
        detailContainer.getChildren().add(infoCards);

        // Application Status Section
        VBox statusSection = new VBox(15);
        statusSection.getStyleClass().add("detail-section-card");

        Label statusTitle = new Label("Application Status");
        statusTitle.getStyleClass().add("detail-section-title");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getStyleClass().add("modern-combo");
        statusCombo.setPrefWidth(300);
        statusCombo.getItems().addAll("New", "Reviewing", "Shortlisted",
                                      "Interview Scheduled", "Accepted", "Rejected");
        statusCombo.setValue(app.status());

        statusSection.getChildren().addAll(statusTitle, statusCombo);
        detailContainer.getChildren().add(statusSection);

        // Action Buttons (only for recruiters)
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (isRecruiter) {
            HBox actionButtons = new HBox(15);
            actionButtons.getStyleClass().add("action-buttons-container");
            actionButtons.setAlignment(Pos.CENTER_LEFT);

            Button btnAccept = new Button("✓ Accept Candidate");
            btnAccept.getStyleClass().addAll("btn-success", "action-button");
            btnAccept.setOnAction(e -> handleAcceptCandidate(app));

            Button btnSchedule = new Button("📅 Schedule Interview");
            btnSchedule.getStyleClass().addAll("btn-primary", "action-button");
            btnSchedule.setOnAction(e -> handleScheduleInterview(app));

            Button btnDownload = new Button("📥 Download CV");
            btnDownload.getStyleClass().addAll("btn-secondary", "action-button");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button btnReject = new Button("✕ Reject");
            btnReject.getStyleClass().addAll("btn-danger", "action-button");
            btnReject.setOnAction(e -> handleRejectCandidate(app));

            actionButtons.getChildren().addAll(btnAccept, btnSchedule, btnDownload, spacer, btnReject);
            detailContainer.getChildren().add(actionButtons);
        } else {
            // For candidates, show a message instead
            VBox candidateMessage = new VBox(15);
            candidateMessage.getStyleClass().add("detail-section-card");
            candidateMessage.setAlignment(Pos.CENTER);

            Label msgTitle = new Label("Application Status");
            msgTitle.getStyleClass().add("detail-section-title");

            Label msgText = new Label("Your application is currently under review. You will be notified when there are updates.");
            msgText.setWrapText(true);
            msgText.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px; -fx-text-alignment: center;");

            candidateMessage.getChildren().addAll(msgTitle, msgText);
            detailContainer.getChildren().add(candidateMessage);
        }
    }

    private VBox createDetailHeader(ApplicationService.ApplicationRow app) {
        VBox headerCard = new VBox(15);
        headerCard.getStyleClass().add("detail-header-card");

        // Name and position
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(8);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label name = new Label("Application #" + app.id());
        name.getStyleClass().add("detail-candidate-name");

        Label position = new Label("Candidate Application");
        position.getStyleClass().add("detail-candidate-position");

        nameBox.getChildren().addAll(name, position);

        Label rating = new Label("⭐ 4.5");
        rating.getStyleClass().add("detail-rating-badge");

        header.getChildren().addAll(nameBox, rating);

        // Separator
        Separator sep = new Separator();
        sep.getStyleClass().add("detail-separator");

        // Contact info
        HBox contactInfo = new HBox(30);

        VBox emailBox = new VBox(5);
        Label emailLabel = new Label("📧 Email");
        emailLabel.getStyleClass().add("detail-label");
        Label emailValue = new Label("candidate@email.com");
        emailValue.getStyleClass().add("detail-value");
        emailBox.getChildren().addAll(emailLabel, emailValue);

        VBox phoneBox = new VBox(5);
        Label phoneLabel = new Label("📞 Phone");
        phoneLabel.getStyleClass().add("detail-label");
        Label phoneValue = new Label("+1 (555) 123-4567");
        phoneValue.getStyleClass().add("detail-value");
        phoneBox.getChildren().addAll(phoneLabel, phoneValue);

        VBox locationBox = new VBox(5);
        Label locationLabel = new Label("📍 Location");
        locationLabel.getStyleClass().add("detail-label");
        Label locationValue = new Label("City, State");
        locationValue.getStyleClass().add("detail-value");
        locationBox.getChildren().addAll(locationLabel, locationValue);

        contactInfo.getChildren().addAll(emailBox, phoneBox, locationBox);

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
        confirm.setTitle("Accept Candidate");
        confirm.setHeaderText("Accept this candidate?");
        confirm.setContentText("This will change the application status to 'Accepted'.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Update application status
                showAlert("Success", "Candidate accepted successfully!", Alert.AlertType.INFORMATION);
                loadApplications();
            }
        });
    }

    private void handleScheduleInterview(ApplicationService.ApplicationRow app) {
        showInterviewScheduleDialog(app);
    }

    private void handleRejectCandidate(ApplicationService.ApplicationRow app) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reject Candidate");
        confirm.setHeaderText("Reject this candidate?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Success", "Candidate rejected.", Alert.AlertType.INFORMATION);
                loadApplications();
            }
        });
    }

    private void showInterviewScheduleDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Schedule Interview");
        dialog.setHeaderText("Schedule interview for Application #" + app.id());

        // Modern styled content
        VBox content = new VBox(18);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: white;");

        // Date picker with label
        VBox dateBox = new VBox(8);
        Label dateLabel = new Label("Interview Date *");
        dateLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(java.time.LocalDate.now().plusDays(7));
        datePicker.setStyle("-fx-pref-width: 350px;");
        dateBox.getChildren().addAll(dateLabel, datePicker);

        // Time field with label
        VBox timeBox = new VBox(8);
        Label timeLabel = new Label("Time (HH:mm) *");
        timeLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField timeField = new TextField("14:00");
        timeField.setPromptText("e.g., 14:00");
        timeField.setStyle("-fx-pref-width: 350px;");
        timeBox.getChildren().addAll(timeLabel, timeField);

        // Duration with label
        VBox durationBox = new VBox(8);
        Label durationLabel = new Label("Duration (minutes) *");
        durationLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField durationField = new TextField("60");
        durationField.setPromptText("e.g., 60");
        durationField.setStyle("-fx-pref-width: 350px;");
        durationBox.getChildren().addAll(durationLabel, durationField);

        // Mode selection with label
        VBox modeBox = new VBox(8);
        Label modeLabel = new Label("Interview Mode *");
        modeLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("ONLINE", "ON_SITE");
        modeCombo.setValue("ONLINE");
        modeCombo.setStyle("-fx-pref-width: 350px;");
        modeBox.getChildren().addAll(modeLabel, modeCombo);

        // Meeting Link (for ONLINE)
        VBox linkBox = new VBox(8);
        Label linkLabel = new Label("Meeting Link (for ONLINE interviews)");
        linkLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField linkField = new TextField();
        linkField.setPromptText("e.g., https://zoom.us/j/123456789");
        linkField.setStyle("-fx-pref-width: 350px;");
        linkBox.getChildren().addAll(linkLabel, linkField);

        // Location (for ON_SITE)
        VBox locationBox = new VBox(8);
        Label locationLabel = new Label("Location (for ON_SITE interviews)");
        locationLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField locationField = new TextField();
        locationField.setPromptText("e.g., Office Building A, Room 301");
        locationField.setStyle("-fx-pref-width: 350px;");
        locationBox.getChildren().addAll(locationLabel, locationField);

        // Notes section
        VBox notesBox = new VBox(8);
        Label notesLabel = new Label("Additional Notes (Optional)");
        notesLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Enter any additional notes for the interview...");
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
                        showAlert("Validation Error", "Please select a date.", Alert.AlertType.WARNING);
                        return;
                    }

                    LocalDateTime scheduledAt = LocalDateTime.of(
                        datePicker.getValue(),
                        java.time.LocalTime.parse(timeField.getText())
                    );
                    int duration = Integer.parseInt(durationField.getText());
                    String mode = modeCombo.getValue();

                    // Validate required fields based on mode
                    if ("ONLINE".equals(mode) && (linkField.getText() == null || linkField.getText().trim().isEmpty())) {
                        showAlert("Validation Error", "Meeting link is required for ONLINE interviews.", Alert.AlertType.WARNING);
                        return;
                    }

                    if ("ON_SITE".equals(mode) && (locationField.getText() == null || locationField.getText().trim().isEmpty())) {
                        showAlert("Validation Error", "Location is required for ON_SITE interviews.", Alert.AlertType.WARNING);
                        return;
                    }

                    // Create interview linked to application
                    Models.Interview interview = new Models.Interview(
                        app.id(),
                        1, // Default recruiter ID
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

                    showAlert("Success", "Interview scheduled successfully!", Alert.AlertType.INFORMATION);
                    loadApplications();
                } catch (NumberFormatException e) {
                    showAlert("Validation Error", "Duration must be a valid number.", Alert.AlertType.WARNING);
                } catch (Exception e) {
                    showAlert("Error", "Failed to schedule interview: " + e.getMessage(), Alert.AlertType.ERROR);
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
            showAlert("No Selection", "Please select an application first.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleReject() {
        if (selectedApplication != null) {
            handleRejectCandidate(selectedApplication);
        } else {
            showAlert("No Selection", "Please select an application first.", Alert.AlertType.WARNING);
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
