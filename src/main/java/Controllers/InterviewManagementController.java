package Controllers;

import Models.Interview;
import Models.InterviewFeedback;
import Services.InterviewFeedbackService;
import Services.InterviewService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modern Interview Management Controller
 * Role-based UI with no visible IDs and modern card design
 */
public class InterviewManagementController {

    @FXML private VBox interviewsListContainer;
    @FXML private Button btnScheduleNew;
    @FXML private VBox editDialog;
    @FXML private HBox bottomActionButtons;

    // Search and Calendar controls
    @FXML private HBox searchBox;
    @FXML private TextField txtSearchInterview;
    @FXML private Button btnCalendar;

    // Feedback panel controls (bottom panel like interview edit)
    @FXML private VBox feedbackPanel;
    @FXML private ComboBox<String> comboFeedbackDecision;
    @FXML private TextField txtFeedbackScore;
    @FXML private Label lblScoreIndicator;
    @FXML private TextArea txtFeedbackComments;
    @FXML private Button btnUpdateFeedbackAction;
    @FXML private Button btnDeleteFeedback;

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

    private Interview selectedInterview = null;
    private boolean isEditMode = false;

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
            comboMode.valueProperty().addListener((obs, oldVal, newVal) -> toggleModeFields(newVal));
        }

        // Setup feedback decision combobox
        if (comboFeedbackDecision != null) {
            comboFeedbackDecision.setItems(FXCollections.observableArrayList("ACCEPTED", "REJECTED"));
        }

        // Setup feedback score live indicator
        if (txtFeedbackScore != null && lblScoreIndicator != null) {
            txtFeedbackScore.textProperty().addListener((obs, old, newVal) -> {
                try {
                    if (!newVal.trim().isEmpty()) {
                        int score = Integer.parseInt(newVal);
                        if (score >= 70) {
                            lblScoreIndicator.setText("✓ HIGH");
                            lblScoreIndicator.setStyle("-fx-text-fill: #28a745; -fx-font-size: 12px; -fx-font-weight: 600;");
                        } else if (score >= 50) {
                            lblScoreIndicator.setText("⚠ MEDIUM");
                            lblScoreIndicator.setStyle("-fx-text-fill: #f0ad4e; -fx-font-size: 12px; -fx-font-weight: 600;");
                        } else {
                            lblScoreIndicator.setText("✗ LOW");
                            lblScoreIndicator.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: 600;");
                        }
                    } else {
                        lblScoreIndicator.setText("");
                    }
                } catch (NumberFormatException e) {
                    lblScoreIndicator.setText("Invalid");
                    lblScoreIndicator.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 12px; -fx-font-weight: 600;");
                }
            });
        }

        updateUIForRole();
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
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (btnScheduleNew != null) {
            // New rule: interviewer scheduling happens from Applications, not directly here.
            btnScheduleNew.setVisible(false);
            btnScheduleNew.setManaged(false);
        }

        // Show search bar and calendar button for recruiters only
        if (searchBox != null) {
            searchBox.setVisible(isRecruiter);
            searchBox.setManaged(isRecruiter);
        }

        if (btnCalendar != null) {
            btnCalendar.setVisible(isRecruiter);
            btnCalendar.setManaged(isRecruiter);
        }
    }

    @FXML
    private void handleScheduleNew() {
        showEditDialog(null);
    }

    @FXML
    private void handleSearchInterview() {
        if (txtSearchInterview == null || txtSearchInterview.getText().trim().isEmpty()) {
            loadInterviews();
            return;
        }

        String keyword = txtSearchInterview.getText().trim().toLowerCase();
        List<Interview> allInterviews = InterviewService.getAll();

        List<Interview> filtered = allInterviews.stream()
            .filter(interview -> {
                // Search by ID, mode, status, or date
                String searchText = String.valueOf(interview.getId()) + " " +
                                  (interview.getMode() != null ? interview.getMode() : "") + " " +
                                  (interview.getStatus() != null ? interview.getStatus() : "") + " " +
                                  formatDateTime(interview.getScheduledAt());
                return searchText.toLowerCase().contains(keyword);
            })
            .toList();

        displayFilteredInterviews(filtered);
    }

    @FXML
    private void handleShowCalendar() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Interview Calendar");
        dialog.setHeaderText(null);

        VBox content = new VBox(20);
        content.setStyle("-fx-padding: 25; -fx-background-color: white;");

        // Header
        Label title = new Label("📅 Interview Calendar");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        // Calendar grid
        GridPane calendar = createCalendarGrid();

        content.getChildren().addAll(title, calendar);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().setPrefHeight(600);

        dialog.showAndWait();
    }

    private GridPane createCalendarGrid() {
        GridPane calendar = new GridPane();
        calendar.setHgap(5);
        calendar.setVgap(5);
        calendar.setStyle("-fx-padding: 10;");

        // Get all interviews and group by date
        List<Interview> interviews = InterviewService.getAll();
        Map<LocalDate, List<Interview>> interviewsByDate = interviews.stream()
            .collect(Collectors.groupingBy(i -> i.getScheduledAt().toLocalDate()));

        // Current month view
        YearMonth currentMonth = YearMonth.now();
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int daysInMonth = currentMonth.lengthOfMonth();
        int startDayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7; // 0 = Sunday

        // Month/Year header
        HBox monthHeader = new HBox(15);
        monthHeader.setAlignment(Pos.CENTER);
        monthHeader.setStyle("-fx-padding: 10 0 20 0;");

        Label monthLabel = new Label(currentMonth.getMonth().toString() + " " + currentMonth.getYear());
        monthLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        monthHeader.getChildren().add(monthLabel);

        calendar.add(monthHeader, 0, 0, 7, 1);

        // Day headers (Sun-Sat)
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            dayLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #6c757d; -fx-font-size: 13px; -fx-alignment: center;");
            dayLabel.setPrefWidth(90);
            calendar.add(dayLabel, i, 1);
        }

        // Fill calendar days
        int row = 2;
        int col = startDayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            VBox dayCell = new VBox(5);
            dayCell.setPrefWidth(90);
            dayCell.setPrefHeight(80);
            dayCell.setAlignment(Pos.TOP_CENTER);
            dayCell.setStyle("-fx-background-color: white; -fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

            // Day number
            Label dayNum = new Label(String.valueOf(day));
            dayNum.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");

            // Check if there are interviews on this day
            if (interviewsByDate.containsKey(date)) {
                List<Interview> dayInterviews = interviewsByDate.get(date);
                // Highlight day with interview
                dayCell.setStyle("-fx-background-color: #FFF9E6; -fx-border-color: #FFD700; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");

                // Show interview count
                Label countLabel = new Label(dayInterviews.size() + " interview" + (dayInterviews.size() > 1 ? "s" : ""));
                countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #856404; -fx-font-weight: 600;");

                dayCell.getChildren().addAll(dayNum, countLabel);

                // Add click handler to show interview details
                dayCell.setOnMouseEntered(e -> dayCell.setStyle("-fx-background-color: #FFE082; -fx-border-color: #FFC107; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8; -fx-cursor: hand;"));
                dayCell.setOnMouseExited(e -> dayCell.setStyle("-fx-background-color: #FFF9E6; -fx-border-color: #FFD700; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;"));
                dayCell.setOnMouseClicked(e -> showDayInterviews(date, dayInterviews));
            } else {
                dayCell.getChildren().add(dayNum);

                // Highlight today
                if (date.equals(LocalDate.now())) {
                    dayCell.setStyle("-fx-background-color: #E3F2FD; -fx-border-color: #5BA3F5; -fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 8;");
                }
            }

            calendar.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }

        return calendar;
    }

    private void showDayInterviews(LocalDate date, List<Interview> interviews) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Interviews on " + date.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        alert.setHeaderText(interviews.size() + " interview(s) scheduled");

        VBox content = new VBox(12);
        content.setStyle("-fx-padding: 15;");

        for (Interview interview : interviews) {
            VBox interviewCard = new VBox(6);
            interviewCard.setStyle("-fx-background-color: #F5F6F8; -fx-padding: 12; -fx-background-radius: 8;");

            Label timeLabel = new Label("⏰ " + interview.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
            timeLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

            Label modeLabel = new Label("Mode: " + interview.getMode() + " | Duration: " + interview.getDurationMinutes() + " min");
            modeLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");

            Label statusLabel = new Label(interview.getStatus() != null ? interview.getStatus() : "SCHEDULED");
            statusLabel.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 3 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: 600;");

            interviewCard.getChildren().addAll(timeLabel, modeLabel, statusLabel);
            content.getChildren().add(interviewCard);
        }

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
    }

    private void displayFilteredInterviews(List<Interview> interviews) {
        if (interviewsListContainer == null) return;

        interviewsListContainer.getChildren().clear();

        if (interviews.isEmpty()) {
            Label emptyLabel = new Label("No matching interviews found");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 16px; -fx-padding: 50;");
            interviewsListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Interview interview : interviews) {
            VBox card = createModernInterviewCard(interview);
            interviewsListContainer.getChildren().add(card);
        }
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
            // Candidates see interview result if feedback exists
            boolean hasFeedback = checkIfFeedbackExists(interview.getId());
            if (hasFeedback) {
                var feedbacks = InterviewFeedbackService.getByInterviewId(interview.getId());
                if (!feedbacks.isEmpty()) {
                    InterviewFeedback feedback = feedbacks.get(0);
                    String result = calculateResult(feedback);

                    Label resultLabel = new Label(result);
                    if ("ACCEPTED".equals(result)) {
                        resultLabel.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 13px;");
                    } else {
                        resultLabel.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 700; -fx-font-size: 13px;");
                    }
                    actionBox.getChildren().add(resultLabel);
                }
            } else {
                Label candidateMsg = new Label("Pending Review");
                candidateMsg.setStyle("-fx-text-fill: #f0ad4e; -fx-font-size: 12px; -fx-font-weight: 600;");
                actionBox.getChildren().add(candidateMsg);
            }
            return actionBox;
        }

        // Recruiter sees feedback action buttons in the card itself
        boolean hasFeedback = checkIfFeedbackExists(interview.getId());

        if (hasFeedback) {
            // If feedback exists: show View, Update, Delete buttons
            Button btnViewFeedback = new Button("👁 View");
            btnViewFeedback.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand;");
            btnViewFeedback.setOnAction(e -> viewFeedback(interview));

            Button btnUpdateFeedback = new Button("✏ Update");
            btnUpdateFeedback.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand;");
            btnUpdateFeedback.setOnAction(e -> updateFeedback(interview));

            Button btnDeleteFeedback = new Button("🗑 Delete");
            btnDeleteFeedback.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand;");
            btnDeleteFeedback.setOnAction(e -> deleteFeedbackForInterview(interview));

            actionBox.getChildren().addAll(btnViewFeedback, btnUpdateFeedback, btnDeleteFeedback);
        } else {
            // If no feedback: show Create button
            Button btnCreateFeedback = new Button("📋 Create Feedback");
            btnCreateFeedback.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand;");
            btnCreateFeedback.setOnAction(e -> createFeedback(interview));

            actionBox.getChildren().add(btnCreateFeedback);
        }

        return actionBox;
    }

    private void viewFeedback(Interview interview) {
        var feedbacks = InterviewFeedbackService.getByInterviewId(interview.getId());
        if (!feedbacks.isEmpty()) {
            InterviewFeedback feedback = feedbacks.get(0);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Interview Feedback");
            alert.setHeaderText("Feedback for Interview #" + interview.getId());

            String decision = feedback.getDecision() != null ? feedback.getDecision() : "N/A";
            String content = "Decision: " + decision + "\n" +
                           "Overall Score: " + (feedback.getOverallScore() != null ? feedback.getOverallScore() : "N/A") + "/100\n\n" +
                           "Comments:\n" + (feedback.getComment() != null ? feedback.getComment() : "No comments");

            alert.setContentText(content);
            alert.showAndWait();
        }
    }

    private void updateFeedback(Interview interview) {
        System.out.println("[DEBUG] updateFeedback called with interview ID: " + interview.getId());
        selectedInterview = interview;
        System.out.println("[DEBUG] selectedInterview set to ID: " + selectedInterview.getId());
        showFeedbackPanelForInterview(interview);
    }

    private void createFeedback(Interview interview) {
        System.out.println("[DEBUG] createFeedback called with interview ID: " + interview.getId());
        selectedInterview = interview;
        System.out.println("[DEBUG] selectedInterview set to ID: " + selectedInterview.getId());

        // Clear form for new feedback
        if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(null);
        if (txtFeedbackScore != null) txtFeedbackScore.setText("");
        if (txtFeedbackComments != null) txtFeedbackComments.setText("");

        // Set button text for creating
        if (btnUpdateFeedbackAction != null) {
            btnUpdateFeedbackAction.setText("💾 Create Feedback");
        }

        // Hide delete button for new feedback
        if (btnDeleteFeedback != null) {
            btnDeleteFeedback.setVisible(false);
            btnDeleteFeedback.setManaged(false);
        }

        if (feedbackPanel != null) {
            feedbackPanel.setVisible(true);
            feedbackPanel.setManaged(true);
        }
    }

    private void deleteFeedbackForInterview(Interview interview) {
        var feedbacks = InterviewFeedbackService.getByInterviewId(interview.getId());
        if (!feedbacks.isEmpty()) {
            InterviewFeedback existing = feedbacks.get(0);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Feedback");
            confirm.setHeaderText("Delete this feedback?");
            confirm.setContentText("This action cannot be undone.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    InterviewFeedbackService.deleteFeedback(existing.getId());
                    showAlert("Success", "Feedback deleted successfully.", Alert.AlertType.INFORMATION);
                    loadInterviews();
                }
            });
        }
    }

    private String calculateResult(InterviewFeedback feedback) {
        // Use the decision field from database
        if (feedback.getDecision() != null) {
            return feedback.getDecision(); // ACCEPTED or REJECTED
        }
        return "PENDING";
    }

    private void showFeedbackPanelForInterview(Interview interview) {
        if (feedbackPanel == null) return;

        // CRITICAL: Set the selected interview for the update handler
        selectedInterview = interview;
        System.out.println("[DEBUG] showFeedbackPanelForInterview - selectedInterview set to ID: " + interview.getId());

        // Hide edit dialog if open
        hideEditDialog();

        // Get existing feedback
        var feedbacks = InterviewFeedbackService.getByInterviewId(interview.getId());
        InterviewFeedback existingFeedback = feedbacks.isEmpty() ? null : feedbacks.get(0);

        // Pre-fill if exists
        if (existingFeedback != null) {
            // Get decision from database field
            String decision = existingFeedback.getDecision();

            if (comboFeedbackDecision != null) {
                comboFeedbackDecision.setValue(decision);
            }

            if (existingFeedback.getOverallScore() != null) {
                txtFeedbackScore.setText(String.valueOf(existingFeedback.getOverallScore()));
            } else {
                txtFeedbackScore.setText("");
            }
            txtFeedbackComments.setText(existingFeedback.getComment() != null ? existingFeedback.getComment() : "");

            // Set button text for updating
            if (btnUpdateFeedbackAction != null) {
                btnUpdateFeedbackAction.setText("💾 Update Feedback");
            }
        } else {
            if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(null);
            txtFeedbackScore.setText("");
            txtFeedbackComments.setText("");

            // Set button text for creating
            if (btnUpdateFeedbackAction != null) {
                btnUpdateFeedbackAction.setText("💾 Create Feedback");
            }
        }

        // Always hide delete button in the panel - delete is only available on the card
        if (btnDeleteFeedback != null) {
            btnDeleteFeedback.setVisible(false);
            btnDeleteFeedback.setManaged(false);
        }

        feedbackPanel.setVisible(true);
        feedbackPanel.setManaged(true);
    }

    @FXML
    private void handleUpdateFeedbackAction() {
        System.out.println("\n!!!!!!!!!!!!! UPDATE BUTTON CLICKED !!!!!!!!!!!!!");

        if (selectedInterview == null) {
            System.err.println("ERROR: No interview selected");
            showAlert("Error", "No interview selected.", Alert.AlertType.ERROR);
            return;
        }

        System.out.println("\n============ FEEDBACK UPDATE STARTED ============");
        System.out.println("Interview ID: " + selectedInterview.getId());

        try {
            // Validation - decision is required
            if (comboFeedbackDecision == null || comboFeedbackDecision.getValue() == null || comboFeedbackDecision.getValue().trim().isEmpty()) {
                System.err.println("VALIDATION ERROR: Decision not selected");
                showAlert("Validation Error", "Please select a decision (ACCEPTED or REJECTED).", Alert.AlertType.WARNING);
                return;
            }

            // Validation - score is required
            if (txtFeedbackScore.getText().trim().isEmpty()) {
                System.err.println("VALIDATION ERROR: Score is empty");
                showAlert("Validation Error", "Please enter a score.", Alert.AlertType.WARNING);
                return;
            }

            int overallScore;
            try {
                overallScore = Integer.parseInt(txtFeedbackScore.getText().trim());
            } catch (NumberFormatException e) {
                System.err.println("VALIDATION ERROR: Score is not a number: " + txtFeedbackScore.getText());
                showAlert("Validation Error", "Score must be a valid number.", Alert.AlertType.WARNING);
                return;
            }

            if (overallScore < 0 || overallScore > 100) {
                System.err.println("VALIDATION ERROR: Score out of range: " + overallScore);
                showAlert("Validation Error", "Score must be between 0 and 100.", Alert.AlertType.WARNING);
                return;
            }

            String comment = txtFeedbackComments.getText();
            String decision = comboFeedbackDecision.getValue();
            Long recruiterId = (long) getEffectiveRecruiterIdForInterview(selectedInterview);

            System.out.println("Form Values:");
            System.out.println("  - Decision: " + decision);
            System.out.println("  - Score: " + overallScore);
            System.out.println("  - Comment length: " + (comment != null ? comment.length() : 0));
            System.out.println("  - Recruiter ID: " + recruiterId);

            // Check if feedback exists
            var feedbacks = InterviewFeedbackService.getByInterviewId(selectedInterview.getId());
            boolean isUpdate = !feedbacks.isEmpty();

            System.out.println("Feedback Status: " + (isUpdate ? "UPDATE MODE" : "CREATE MODE"));

            InterviewFeedback fb;
            if (isUpdate) {
                fb = feedbacks.get(0);
                System.out.println("Existing Feedback ID: " + fb.getId());
                System.out.println("Current values in DB - Decision: " + fb.getDecision() + ", Score: " + fb.getOverallScore());
            } else {
                fb = new InterviewFeedback();
                System.out.println("Creating new feedback object");
            }

            // Set all fields
            fb.setInterviewId(selectedInterview.getId());
            fb.setRecruiterId(recruiterId);
            fb.setOverallScore(overallScore);
            fb.setDecision(decision);
            fb.setComment(comment);

            System.out.println("Updated object values - Decision: " + fb.getDecision() + ", Score: " + fb.getOverallScore());

            // Save to database
            if (isUpdate) {
                System.out.println("Calling InterviewFeedbackService.updateFeedback() with ID: " + fb.getId());
                InterviewFeedbackService.updateFeedback(fb.getId(), fb);
                System.out.println("✓ Update completed successfully");
                showAlert("Success", "Feedback updated successfully.", Alert.AlertType.INFORMATION);
            } else {
                System.out.println("Calling InterviewFeedbackService.addFeedback()");
                InterviewFeedbackService.addFeedback(fb);
                System.out.println("✓ Create completed successfully");
                showAlert("Success", "Feedback created successfully.", Alert.AlertType.INFORMATION);
            }

            System.out.println("============ FEEDBACK UPDATE COMPLETED ============\n");

            hideFeedbackPanel();
            loadInterviews();
        } catch (Exception e) {
            System.err.println("ERROR during feedback save: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to save feedback: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteFeedback() {
        if (selectedInterview == null) return;

        var feedbacks = InterviewFeedbackService.getByInterviewId(selectedInterview.getId());
        if (!feedbacks.isEmpty()) {
            InterviewFeedback existing = feedbacks.get(0);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Feedback");
            confirm.setHeaderText("Delete this feedback?");
            confirm.setContentText("This action cannot be undone.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    InterviewFeedbackService.deleteFeedback(existing.getId());
                    showAlert("Success", "Feedback deleted successfully.", Alert.AlertType.INFORMATION);
                    hideFeedbackPanel();
                    loadInterviews();
                }
            });
        }
    }

    @FXML
    private void handleCancelFeedback() {
        hideFeedbackPanel();
    }

    private void hideFeedbackPanel() {
        if (feedbackPanel != null) {
            feedbackPanel.setVisible(false);
            feedbackPanel.setManaged(false);
        }
    }

    private int getEffectiveRecruiterIdForInterview(Interview interview) {
        // Use the recruiter_id already on the interview row.
        // When you add authentication later, replace with current user id.
        return interview != null ? interview.getRecruiterId().intValue() : 0;
    }

    private boolean checkIfFeedbackExists(Long interviewId) {
        try {
            return InterviewFeedbackService.existsForInterview(interviewId);
        } catch (Exception e) {
            System.err.println("Error checking feedback existence: " + e.getMessage());
            return false;
        }
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
            // DON'T clear selectedInterview here - it's needed for feedback operations
            // selectedInterview will be cleared when appropriate (e.g., after saving)
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
