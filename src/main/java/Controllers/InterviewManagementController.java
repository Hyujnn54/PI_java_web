package Controllers;

import Models.Interview;
import Models.InterviewFeedback;
import Services.InterviewFeedbackService;
import Services.InterviewService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Modern Interview Management Controller
 * Role-based UI with no visible IDs and modern card design
 */
public class InterviewManagementController {

    @FXML private VBox interviewsListContainer;
    @FXML private Button btnScheduleNew;
    @FXML private VBox editDialog;
    @FXML private HBox bottomActionButtons;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;

    // Modern form fields (no visible IDs)
    @FXML private DatePicker datePicker;
    @FXML private TextField txtTime;
    @FXML private TextField txtDuration;
    @FXML private ComboBox<String> comboMode;
    @FXML private TextField txtMeetingLink;
    @FXML private TextField txtLocation;
    @FXML private TextArea txtNotes;
    @FXML private Label lblMeetingLink;
    @FXML private Label lblLocation;
    @FXML private Button btnSave;

    // Hidden fields for service compatibility
    @FXML private TextField txtApplicationId;
    @FXML private TextField txtRecruiterId;
    @FXML private ComboBox<String> comboStatus;

    private Interview selectedInterview = null;
    private String userRole = "Candidate"; // Mock role
    private boolean isEditMode = false;

    public void setUserRole(String role) {
        this.userRole = role != null ? role : "Candidate";
        updateUIForRole();
    }

    @FXML
    public void initialize() {
        // Clean up any corrupted data and verify database state
        Utils.DatabaseSchemaUtil.cleanupCorruptedData();
        Utils.DatabaseSchemaUtil.verifyInterviewData();

        setupComboBoxes();
        loadInterviews();
        hideEditDialog();
        hideBottomActionButtons(); // Hide action buttons initially
    }

    private void setupComboBoxes() {
        if (comboMode != null) {
            // Use exact database enum values: ONLINE and ON_SITE
            comboMode.setItems(FXCollections.observableArrayList("ONLINE", "ON_SITE"));

            // Add listener to toggle meeting link/location visibility based on mode
            comboMode.valueProperty().addListener((obs, oldVal, newVal) -> {
                toggleModeFields(newVal);
            });
        }
        if (comboStatus != null) {
            comboStatus.setItems(FXCollections.observableArrayList("SCHEDULED", "COMPLETED", "CANCELLED", "RESCHEDULED"));
        }
    }

    private void toggleModeFields(String mode) {
        if (mode == null) return;

        boolean isOnline = "ONLINE".equals(mode);

        // Show meeting link for ONLINE, hide location
        if (txtMeetingLink != null) {
            txtMeetingLink.setVisible(isOnline);
            txtMeetingLink.setManaged(isOnline);
        }
        if (lblMeetingLink != null) {
            lblMeetingLink.setVisible(isOnline);
            lblMeetingLink.setManaged(isOnline);
        }

        // Show location for ON_SITE, hide meeting link
        if (txtLocation != null) {
            txtLocation.setVisible(!isOnline);
            txtLocation.setManaged(!isOnline);
        }
        if (lblLocation != null) {
            lblLocation.setVisible(!isOnline);
            lblLocation.setManaged(!isOnline);
        }
    }

    private void updateUIForRole() {
        if (btnScheduleNew != null) {
            // New rule: interviewer scheduling happens from Applications, not directly here.
            btnScheduleNew.setVisible(false);
            btnScheduleNew.setManaged(false);
        }
    }

    @FXML
    private void handleScheduleNew() {
        showEditDialog(null);
    }

    @FXML
    private void handleCancelEdit() {
        hideEditDialog();
    }

    @FXML
    private void handleSaveInterview() {
        // New flow: interviews are created from Applications.
        // Keep Update only for existing interviews.
        if (!isEditMode) {
            showAlert("Not allowed", "Interviews are created from Applications. Select an application and schedule from there.", Alert.AlertType.INFORMATION);
            hideEditDialog();
            return;
        }

        System.out.println("Save interview called");

        if (!validateInput()) {
            System.out.println("Validation failed");
            return;
        }

        try {
            LocalDateTime scheduledAt = LocalDateTime.of(
                datePicker.getValue(),
                LocalTime.parse(txtTime.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"))
            );

            int duration = Integer.parseInt(txtDuration.getText().trim());
            String mode = comboMode.getValue();

            if (selectedInterview != null) {
                selectedInterview.setScheduledAt(scheduledAt);
                selectedInterview.setDurationMinutes(duration);
                selectedInterview.setMode(mode);

                // Update meeting link or location based on mode
                if ("ONLINE".equals(mode)) {
                    selectedInterview.setMeetingLink(txtMeetingLink.getText() != null ? txtMeetingLink.getText().trim() : "");
                    selectedInterview.setLocation(null);
                } else {
                    selectedInterview.setLocation(txtLocation.getText() != null ? txtLocation.getText().trim() : "");
                    selectedInterview.setMeetingLink(null);
                }

                // Update notes
                selectedInterview.setNotes(txtNotes.getText() != null ? txtNotes.getText().trim() : "");

                try {
                    InterviewService.updateInterview(selectedInterview.getId(), selectedInterview);
                } catch (RuntimeException e) {
                    showAlert("Database Error", "Failed to update interview: " + e.getMessage(), Alert.AlertType.ERROR);
                    return;
                }

                hideEditDialog();
                loadInterviews();
                showAlert("Success", "Interview updated successfully!", Alert.AlertType.INFORMATION);
            }

        } catch (Exception e) {
            showAlert("Error", "Failed to save interview: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadInterviews() {
        if (interviewsListContainer == null) return;

        interviewsListContainer.getChildren().clear();
        List<Interview> interviews = InterviewService.getAll();

        if (interviews.isEmpty()) {
            Label emptyLabel = new Label("No interviews scheduled yet");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 16px; -fx-padding: 50;");
            interviewsListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Interview interview : interviews) {
            VBox card = createModernInterviewCard(interview);
            interviewsListContainer.getChildren().add(card);
        }
    }

    private VBox createModernInterviewCard(Interview interview) {
        VBox card = new VBox(15);
        card.getStyleClass().add("interview-card");
        card.setPadding(new Insets(20));

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        // Header with title and status
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label(isRecruiter ? "Interview #" + interview.getId() : "Your Upcoming Interview");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusTag = new Label(interview.getStatus() != null ? interview.getStatus() : "SCHEDULED");
        statusTag.getStyleClass().addAll("status-tag", getStatusClass(interview.getStatus()));

        header.getChildren().addAll(title, spacer, statusTag);

        // Interview Status Box for Candidates (Prominent Display)
        VBox statusBox = new VBox(10);
        statusBox.setStyle("-fx-background-color: rgba(91, 163, 245, 0.1); -fx-padding: 15; -fx-background-radius: 8;");

        Label statusLabel = new Label("Interview Status");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Label statusValue = new Label(getStatusDescription(interview.getStatus()));
        statusValue.setWrapText(true);
        statusValue.setStyle("-fx-text-fill: #5BA3F5; -fx-font-size: 16px; -fx-font-weight: bold;");

        statusBox.getChildren().addAll(statusLabel, statusValue);

        // Interview details (no raw IDs shown)
        HBox detailsRow = new HBox(30);
        detailsRow.getChildren().addAll(
            createInfoBox("📅 Date & Time", formatDateTime(interview.getScheduledAt())),
            createInfoBox("⏱ Duration", interview.getDurationMinutes() + " min"),
            createInfoBox("🎯 Mode", interview.getMode())
        );

        // Add meeting link or location info
        if ("ONLINE".equals(interview.getMode()) && interview.getMeetingLink() != null && !interview.getMeetingLink().trim().isEmpty()) {
            Label linkLabel = new Label("🔗 Meeting Link: " + interview.getMeetingLink());
            linkLabel.setWrapText(true);
            linkLabel.setStyle("-fx-text-fill: #5BA3F5; -fx-font-size: 12px; -fx-padding: 5 0;");
            card.getChildren().add(linkLabel);
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null && !interview.getLocation().trim().isEmpty()) {
            Label locLabel = new Label("📍 Location: " + interview.getLocation());
            locLabel.setWrapText(true);
            locLabel.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-padding: 5 0;");
            card.getChildren().add(locLabel);
        }

        // Action buttons based on role
        HBox actionRow = createActionButtons(interview);

        if (isRecruiter) {
            card.getChildren().addAll(header, detailsRow, actionRow);
        } else {
            // For candidates, show status prominently
            card.getChildren().addAll(header, statusBox, detailsRow, actionRow);
        }

        // Click to select interview and show bottom action buttons (recruiter only)
        if (isRecruiter) {
            card.setOnMouseClicked(e -> {
                selectedInterview = interview;
                highlightSelectedCard(card);
                showBottomActionButtons();
            });
        }

        return card;
    }

    private String getStatusDescription(String status) {
        if (status == null) return "Pending Confirmation";

        return switch (status.toUpperCase()) {
            case "SCHEDULED" -> "✓ Confirmed - Interview is scheduled and confirmed";
            case "COMPLETED" -> "✓ Completed - Interview has been conducted";
            case "CANCELLED" -> "✕ Cancelled - Interview has been cancelled";
            case "RESCHEDULED" -> "🔄 Rescheduled - New time has been set";
            default -> "Pending Confirmation";
        };
    }

    private VBox createInfoBox(String label, String value) {
        VBox box = new VBox(5);

        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("info-label");

        Label valueNode = new Label(value);
        valueNode.getStyleClass().add("info-value");

        box.getChildren().addAll(labelNode, valueNode);
        return box;
    }

    private HBox createActionButtons(Interview interview) {
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        // Only recruiters can manage feedback
        if (!isRecruiter) {
            // Candidates see a simple message
            Label candidateMsg = new Label("You will be notified about interview results");
            candidateMsg.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-font-style: italic;");
            actionBox.getChildren().add(candidateMsg);
            return actionBox;
        }

        // Check if feedback exists for this interview (recruiter only)
        boolean hasFeedback = checkIfFeedbackExists(interview.getId());

        if (hasFeedback) {
            // Show "View Feedback" button if feedback exists
            Button viewFeedbackBtn = new Button("📋 View Feedback");
            viewFeedbackBtn.getStyleClass().add("btn-primary");
            viewFeedbackBtn.setOnAction(e -> viewExistingFeedback(interview));
            actionBox.getChildren().add(viewFeedbackBtn);
        }

        // Always show "Add Feedback" button for recruiters
        Button addFeedbackBtn = new Button("✍️ Add Feedback");
        addFeedbackBtn.getStyleClass().add("btn-success");
        addFeedbackBtn.setOnAction(e -> createFeedbackDialog(interview));
        actionBox.getChildren().add(addFeedbackBtn);

        return actionBox;
    }

    private int getEffectiveRecruiterIdForInterview(Interview interview) {
        // Use the recruiter_id already on the interview row.
        // When you add authentication later, replace with current user id.
        return interview != null ? interview.getRecruiterId() : 0;
    }

    private boolean checkIfFeedbackExists(int interviewId) {
        try {
            return InterviewFeedbackService.existsForInterview(interviewId);
        } catch (Exception e) {
            System.err.println("Error checking feedback existence: " + e.getMessage());
            return false;
        }
    }

    private void viewExistingFeedback(Interview interview) {
        if (interview == null) return;

        var list = InterviewFeedbackService.getByInterviewId(interview.getId());
        if (list.isEmpty()) {
            showAlert("Info", "No feedback found for this interview.", Alert.AlertType.INFORMATION);
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Interview Feedback");
        dialog.setHeaderText("Feedbacks for interview #" + interview.getId());

        VBox content = new VBox(10);
        content.getStyleClass().add("dialog-content");

        // List feedbacks
        ListView<String> listView = new ListView<>();
        listView.setPrefHeight(200);
        listView.getItems().addAll(list.stream().map(f ->
                "#" + f.getId() + " | Overall: " + f.getOverallScore() + " | Decision: " + f.getDecision()
        ).toList());

        // Actions (Update/Delete) for recruiter only
        Button btnEdit = new Button("✏️ Update");
        btnEdit.getStyleClass().add("btn-primary");
        Button btnDelete = new Button("🗑️ Delete");
        btnDelete.getStyleClass().add("btn-danger");

        boolean canEdit = "Recruiter".equalsIgnoreCase(userRole);
        btnEdit.setDisable(!canEdit);
        btnDelete.setDisable(!canEdit);

        HBox actions = new HBox(10, btnEdit, btnDelete);

        btnEdit.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                showAlert("Warning", "Select a feedback to update.", Alert.AlertType.WARNING);
                return;
            }
            InterviewFeedback fb = list.get(idx);
            dialog.close();
            openFeedbackEditor(interview, fb);
        });

        btnDelete.setOnAction(e -> {
            int idx = listView.getSelectionModel().getSelectedIndex();
            if (idx < 0) {
                showAlert("Warning", "Select a feedback to delete.", Alert.AlertType.WARNING);
                return;
            }
            InterviewFeedback fb = list.get(idx);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Feedback");
            confirm.setHeaderText("Delete feedback #" + fb.getId());
            confirm.setContentText("This action cannot be undone.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    InterviewFeedbackService.deleteFeedback(fb.getId());
                    dialog.close();
                    loadInterviews();
                }
            });
        });

        content.getChildren().addAll(new Label("Feedbacks:"), listView, actions);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void createFeedbackDialog(Interview interview) {
        openFeedbackEditor(interview, null);
    }

    private void openFeedbackEditor(Interview interview, InterviewFeedback existing) {
        if (interview == null) return;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add Interview Feedback" : "Update Interview Feedback");
        dialog.setHeaderText("Interview #" + interview.getId());

        VBox content = new VBox(18);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: white;");

        // Technical Score
        VBox techBox = new VBox(8);
        Label techLabel = new Label("Technical Skills *");
        techLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField tfTech = new TextField();
        tfTech.setPromptText("Score: 0-100");
        tfTech.setStyle("-fx-pref-width: 350px;");
        techBox.getChildren().addAll(techLabel, tfTech);

        // Communication Score
        VBox commBox = new VBox(8);
        Label commLabel = new Label("Communication Skills *");
        commLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField tfComm = new TextField();
        tfComm.setPromptText("Score: 0-100");
        tfComm.setStyle("-fx-pref-width: 350px;");
        commBox.getChildren().addAll(commLabel, tfComm);

        // Culture Fit Score
        VBox cultureBox = new VBox(8);
        Label cultureLabel = new Label("Culture Fit *");
        cultureLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField tfCulture = new TextField();
        tfCulture.setPromptText("Score: 0-100");
        tfCulture.setStyle("-fx-pref-width: 350px;");
        cultureBox.getChildren().addAll(cultureLabel, tfCulture);

        // Decision
        VBox decisionBox = new VBox(8);
        Label decisionLabel = new Label("Decision *");
        decisionLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        ComboBox<String> cbDecision = new ComboBox<>();
        cbDecision.getItems().addAll("ACCEPTED", "REJECTED");
        cbDecision.setPromptText("Select decision");
        cbDecision.setStyle("-fx-pref-width: 350px;");
        decisionBox.getChildren().addAll(decisionLabel, cbDecision);

        // Comments
        VBox commentsBox = new VBox(8);
        Label commentsLabel = new Label("Additional Comments");
        commentsLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextArea taComment = new TextArea();
        taComment.setPromptText("Enter detailed feedback and observations...");
        taComment.setPrefRowCount(4);
        taComment.setStyle("-fx-pref-width: 350px;");
        commentsBox.getChildren().addAll(commentsLabel, taComment);

        // Add separator
        Separator separator = new Separator();
        separator.setStyle("-fx-padding: 10 0;");

        content.getChildren().addAll(
            techBox, commBox, cultureBox,
            separator, decisionBox, commentsBox
        );

        // Pre-fill if editing
        if (existing != null) {
            tfTech.setText(String.valueOf(existing.getTechnicalScore()));
            tfComm.setText(String.valueOf(existing.getCommunicationScore()));
            tfCulture.setText(String.valueOf(existing.getCultureFitScore()));
            cbDecision.setValue(existing.getDecision());
            taComment.setText(existing.getComment() != null ? existing.getComment() : "");
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Style the buttons
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setText(existing == null ? "Save Feedback" : "Update Feedback");
        okButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");

        dialog.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    // Validation
                    if (tfTech.getText().trim().isEmpty() || tfComm.getText().trim().isEmpty() ||
                        tfCulture.getText().trim().isEmpty() || cbDecision.getValue() == null) {
                        showAlert("Validation Error", "Please fill in all required fields.", Alert.AlertType.WARNING);
                        return;
                    }

                    int tech = Integer.parseInt(tfTech.getText().trim());
                    int comm = Integer.parseInt(tfComm.getText().trim());
                    int culture = Integer.parseInt(tfCulture.getText().trim());

                    // Validate scores are between 0-100
                    if (tech < 0 || tech > 100 || comm < 0 || comm > 100 || culture < 0 || culture > 100) {
                        showAlert("Validation Error", "Scores must be between 0 and 100.", Alert.AlertType.WARNING);
                        return;
                    }

                    String decision = cbDecision.getValue();
                    String comment = taComment.getText();

                    int recruiterId = getEffectiveRecruiterIdForInterview(interview);
                    InterviewFeedback fb = existing != null ? existing : new InterviewFeedback();
                    fb.setInterviewId(interview.getId());
                    fb.setRecruiterId(recruiterId);
                    fb.setTechnicalScore(tech);
                    fb.setCommunicationScore(comm);
                    fb.setCultureFitScore(culture);
                    fb.setDecision(decision);
                    fb.setComment(comment);

                    if (existing == null) {
                        InterviewFeedbackService.addFeedback(fb);
                        showAlert("Success", "Feedback added successfully.", Alert.AlertType.INFORMATION);
                    } else {
                        InterviewFeedbackService.updateFeedback(fb.getId(), fb);
                        showAlert("Success", "Feedback updated successfully.", Alert.AlertType.INFORMATION);
                    }

                    loadInterviews();
                } catch (NumberFormatException e) {
                    showAlert("Validation Error", "Scores must be valid numbers.", Alert.AlertType.WARNING);
                } catch (Exception e) {
                    showAlert("Error", "Failed to save feedback: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void showEditDialog(Interview interview) {
        if (editDialog != null) {
            isEditMode = interview != null;
            selectedInterview = interview;

            if (isEditMode) {
                // Fill form with existing data for update
                datePicker.setValue(interview.getScheduledAt().toLocalDate());
                txtTime.setText(interview.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
                txtDuration.setText(String.valueOf(interview.getDurationMinutes()));
                comboMode.setValue(interview.getMode());
                txtMeetingLink.setText(interview.getMeetingLink());
                txtLocation.setText(interview.getLocation());
                txtNotes.setText(interview.getNotes());
                btnSave.setText("Update Interview");
                toggleModeFields(interview.getMode()); // Set visibility based on mode
                System.out.println("Edit dialog opened for update - Interview ID: " + interview.getId());
            } else {
                // Clear form for new interview with some default values
                datePicker.setValue(LocalDate.now().plusDays(1));
                txtTime.setText("14:00"); // Default to 2 PM
                txtDuration.setText("60"); // Default to 60 minutes
                comboMode.setValue("ON_SITE"); // Default to ON_SITE (matches database enum)
                txtMeetingLink.setText("");
                txtLocation.setText("");
                txtNotes.setText("");
                btnSave.setText("Create Interview");
                toggleModeFields("ON_SITE"); // Set visibility for default mode
                System.out.println("Edit dialog opened for new interview");
            }

            editDialog.setVisible(true);
            editDialog.setManaged(true);
        }
    }

    private void hideEditDialog() {
        if (editDialog != null) {
            editDialog.setVisible(false);
            editDialog.setManaged(false);
            isEditMode = false;
            selectedInterview = null;
        }
    }

    private void showBottomActionButtons() {
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (bottomActionButtons != null && isRecruiter) {
            bottomActionButtons.setVisible(true);
            bottomActionButtons.setManaged(true);
        }
    }

    private void hideBottomActionButtons() {
        if (bottomActionButtons != null) {
            bottomActionButtons.setVisible(false);
            bottomActionButtons.setManaged(false);
        }
    }

    @FXML
    private void handleUpdateInterview() {
        if (selectedInterview != null) {
            showEditDialog(selectedInterview);
        } else {
            showAlert("Warning", "Please select an interview to update", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDeleteInterview() {
        if (selectedInterview == null) {
            showAlert("Warning", "Please select an interview to delete", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Interview");
        confirmAlert.setContentText("Are you sure you want to delete this interview? This action cannot be undone.");

        confirmAlert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    InterviewService.delete(selectedInterview.getId());
                    showAlert("Success", "Interview deleted successfully!", Alert.AlertType.INFORMATION);
                    selectedInterview = null;
                    hideBottomActionButtons();
                    loadInterviews();
                } catch (Exception e) {
                    showAlert("Error", "Failed to delete interview: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private boolean validateInput() {
        System.out.println("Validating input...");

        // Check if form fields are properly initialized
        if (datePicker == null) {
            System.out.println("DatePicker is null!");
            showAlert("Error", "Form not properly initialized. Please try again.", Alert.AlertType.ERROR);
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert("Validation Error", "Please select a date", Alert.AlertType.WARNING);
            return false;
        }

        if (txtTime == null || txtTime.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter a time", Alert.AlertType.WARNING);
            return false;
        }

        try {
            LocalTime.parse(txtTime.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            showAlert("Validation Error", "Time must be in HH:mm format (e.g., 14:30)", Alert.AlertType.WARNING);
            return false;
        }

        if (txtDuration == null || txtDuration.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Please enter duration in minutes", Alert.AlertType.WARNING);
            return false;
        }

        try {
            int duration = Integer.parseInt(txtDuration.getText().trim());
            if (duration <= 0 || duration > 480) {
                showAlert("Validation Error", "Duration must be between 1 and 480 minutes", Alert.AlertType.WARNING);
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Duration must be a valid number", Alert.AlertType.WARNING);
            return false;
        }

        if (comboMode == null || comboMode.getValue() == null) {
            showAlert("Validation Error", "Please select an interview mode", Alert.AlertType.WARNING);
            return false;
        }

        System.out.println("Validation passed successfully");
        return true;
    }

    private void highlightSelectedCard(VBox card) {
        // Reset all cards
        for (javafx.scene.Node node : interviewsListContainer.getChildren()) {
            if (node instanceof VBox) {
                node.getStyleClass().removeAll("card-selected");
            }
        }
        // Highlight selected
        card.getStyleClass().add("card-selected");
    }

    private String getStatusClass(String status) {
        if (status == null) return "status-scheduled";
        return switch (status.toUpperCase()) {
            case "COMPLETED" -> "status-completed";
            case "CANCELLED" -> "status-cancelled";
            case "RESCHEDULED" -> "status-pending";
            default -> "status-scheduled";
        };
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy - HH:mm"));
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
