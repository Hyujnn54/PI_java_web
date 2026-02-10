package Controllers;

import Models.Interview;
import Services.InterviewService;
import Utils.InputValidator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller for Interview Management view
 * Handles CRUD operations for interviews with input validation
 */
public class InterviewManagementController {

    @FXML
    private VBox interviewsListContainer;

    @FXML
    private TextField txtApplicationId;
    @FXML
    private TextField txtRecruiterId;
    @FXML
    private TextField txtScheduledAt;
    @FXML
    private TextField txtDuration;
    @FXML
    private ComboBox<String> comboMode;
    @FXML
    private ComboBox<String> comboStatus;

    @FXML
    private Button btnAdd;
    @FXML
    private Button btnUpdate;
    @FXML
    private Button btnDelete;
    @FXML
    private Button btnRefresh;

    private InputValidator validator = new InputValidator();
    private Interview selectedInterview = null;

    @FXML
    public void initialize() {
        // Initialize combo boxes
        comboMode.setItems(FXCollections.observableArrayList("ONLINE", "IN_PERSON", "PHONE", "VIDEO"));
        comboStatus.setItems(FXCollections.observableArrayList("SCHEDULED", "COMPLETED", "CANCELLED", "RESCHEDULED"));

        // Load data
        loadInterviews();

        // Add input placeholders and format hints
        txtApplicationId.setPromptText("Enter Application ID");
        txtRecruiterId.setPromptText("Enter Recruiter ID");
        txtScheduledAt.setPromptText("yyyy-MM-dd HH:mm");
        txtDuration.setPromptText("Duration in minutes");

        // Real-time validation on text fields
        txtApplicationId.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateApplicationId();
        });

        txtRecruiterId.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateRecruiterId();
        });

        txtScheduledAt.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateScheduledAt();
        });

        txtDuration.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) validateDuration();
        });
    }

    @FXML
    private void handleAddInterview() {
        if (!validateAllInputs()) {
            return;
        }

        try {
            Interview interview = new Interview(
                Integer.parseInt(txtApplicationId.getText().trim()),
                Integer.parseInt(txtRecruiterId.getText().trim()),
                InputValidator.parseDateTime(txtScheduledAt.getText().trim()),
                Integer.parseInt(txtDuration.getText().trim()),
                comboMode.getValue()
            );

            if (comboStatus.getValue() != null) {
                interview.setStatus(comboStatus.getValue());
            }

            InterviewService.addInterview(interview);
            showAlert("Success", "Interview added successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadInterviews();
        } catch (Exception e) {
            showAlert("Error", "Failed to add interview: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleUpdateInterview() {
        if (selectedInterview == null) {
            showAlert("Warning", "Please select an interview to update", Alert.AlertType.WARNING);
            return;
        }

        if (!validateAllInputs()) {
            return;
        }

        try {
            Interview interview = new Interview(
                Integer.parseInt(txtApplicationId.getText().trim()),
                Integer.parseInt(txtRecruiterId.getText().trim()),
                InputValidator.parseDateTime(txtScheduledAt.getText().trim()),
                Integer.parseInt(txtDuration.getText().trim()),
                comboMode.getValue()
            );

            if (comboStatus.getValue() != null) {
                interview.setStatus(comboStatus.getValue());
            }

            InterviewService.updateInterview(selectedInterview.getId(), interview);
            showAlert("Success", "Interview updated successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadInterviews();
        } catch (Exception e) {
            showAlert("Error", "Failed to update interview: " + e.getMessage(), Alert.AlertType.ERROR);
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
        confirmAlert.setContentText("Are you sure you want to delete this interview?");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            InterviewService.delete(selectedInterview.getId());
            showAlert("Success", "Interview deleted successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadInterviews();
        }
    }

    @FXML
    private void handleRefresh() {
        loadInterviews();
        clearForm();
    }

    private void loadInterviews() {
        interviewsListContainer.getChildren().clear();
        List<Interview> interviews = InterviewService.getAll();

        if (interviews.isEmpty()) {
            Label emptyLabel = new Label("No interviews found");
            emptyLabel.setStyle("-fx-text-fill: #999999; -fx-font-size: 14;");
            interviewsListContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Interview interview : interviews) {
            VBox card = createInterviewCard(interview);
            interviewsListContainer.getChildren().add(card);
        }
    }

    private VBox createInterviewCard(Interview interview) {
        VBox card = new VBox(8);
        card.setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-padding: 12; -fx-background-color: white; -fx-cursor: hand;");
        card.setPadding(new Insets(12));

        // Click to select
        card.setOnMouseClicked(e -> {
            selectedInterview = interview;
            fillFormWithSelectedInterview();
            highlightCard(card);
        });

        // Header with ID
        HBox headerBox = new HBox(15);
        Label idLabel = new Label("ID: " + interview.getId());
        idLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #0066CC;");
        Label appIdLabel = new Label("App: " + interview.getApplicationId());
        appIdLabel.setStyle("-fx-font-size: 11;");
        Label recruiterLabel = new Label("Recruiter: " + interview.getRecruiterId());
        recruiterLabel.setStyle("-fx-font-size: 11;");
        headerBox.getChildren().addAll(idLabel, appIdLabel, recruiterLabel);

        // Date and time
        Label dateLabel = new Label("📅 " + InputValidator.formatDateTime(interview.getScheduledAt()));
        dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #555555;");

        // Mode, Duration, and Status
        HBox detailsBox = new HBox(15);
        Label modeLabel = new Label("Mode: " + interview.getMode());
        modeLabel.setStyle("-fx-font-size: 11;");
        Label durationLabel = new Label("Duration: " + interview.getDurationMinutes() + " min");
        durationLabel.setStyle("-fx-font-size: 11;");
        Label statusLabel = new Label("Status: " + interview.getStatus());
        statusLabel.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: " + getStatusColor(interview.getStatus()) + ";");
        detailsBox.getChildren().addAll(modeLabel, durationLabel, statusLabel);

        card.getChildren().addAll(headerBox, dateLabel, detailsBox);
        return card;
    }

    private void highlightCard(VBox card) {
        // Reset all cards
        for (javafx.scene.Node node : interviewsListContainer.getChildren()) {
            if (node instanceof VBox) {
                ((VBox) node).setStyle("-fx-border-color: #E0E0E0; -fx-border-width: 1; -fx-padding: 12; -fx-background-color: white; -fx-cursor: hand;");
            }
        }
        // Highlight selected
        card.setStyle("-fx-border-color: #0066CC; -fx-border-width: 2; -fx-padding: 11; -fx-background-color: #F0F8FF; -fx-cursor: hand;");
    }

    private String getStatusColor(String status) {
        return switch (status) {
            case "COMPLETED" -> "#28A745";
            case "SCHEDULED" -> "#0066CC";
            case "CANCELLED" -> "#DC3545";
            case "RESCHEDULED" -> "#FFC107";
            default -> "#666666";
        };
    }

    private void fillFormWithSelectedInterview() {
        if (selectedInterview != null) {
            txtApplicationId.setText(String.valueOf(selectedInterview.getApplicationId()));
            txtRecruiterId.setText(String.valueOf(selectedInterview.getRecruiterId()));
            txtScheduledAt.setText(InputValidator.formatDateTime(selectedInterview.getScheduledAt()));
            txtDuration.setText(String.valueOf(selectedInterview.getDurationMinutes()));
            comboMode.setValue(selectedInterview.getMode());
            comboStatus.setValue(selectedInterview.getStatus());
        }
    }

    // Input validation methods
    private boolean validateApplicationId() {
        String value = txtApplicationId.getText().trim();
        if (!validator.isValidInteger(value)) {
            showValidationError("Application ID must be a valid number");
            txtApplicationId.setStyle("-fx-border-color: #DC3545; -fx-border-width: 2;");
            return false;
        }
        txtApplicationId.setStyle("-fx-border-color: #28A745; -fx-border-width: 1;");
        return true;
    }

    private boolean validateRecruiterId() {
        String value = txtRecruiterId.getText().trim();
        if (!validator.isValidInteger(value)) {
            showValidationError("Recruiter ID must be a valid number");
            txtRecruiterId.setStyle("-fx-border-color: #DC3545; -fx-border-width: 2;");
            return false;
        }
        txtRecruiterId.setStyle("-fx-border-color: #28A745; -fx-border-width: 1;");
        return true;
    }

    private boolean validateScheduledAt() {
        String value = txtScheduledAt.getText().trim();
        if (!validator.isValidDateTime(value)) {
            showValidationError("Date must be in format: yyyy-MM-dd HH:mm");
            txtScheduledAt.setStyle("-fx-border-color: #DC3545; -fx-border-width: 2;");
            return false;
        }
        txtScheduledAt.setStyle("-fx-border-color: #28A745; -fx-border-width: 1;");
        return true;
    }

    private boolean validateDuration() {
        String value = txtDuration.getText().trim();
        if (!validator.isValidInteger(value)) {
            showValidationError("Duration must be a valid number");
            txtDuration.setStyle("-fx-border-color: #DC3545; -fx-border-width: 2;");
            return false;
        }
        int duration = Integer.parseInt(value);
        if (!validator.isInRange(duration, 1, 480)) {
            showValidationError("Duration must be between 1 and 480 minutes");
            txtDuration.setStyle("-fx-border-color: #DC3545; -fx-border-width: 2;");
            return false;
        }
        txtDuration.setStyle("-fx-border-color: #28A745; -fx-border-width: 1;");
        return true;
    }

    private boolean validateAllInputs() {
        boolean isValid = true;
        isValid &= validateApplicationId();
        isValid &= validateRecruiterId();
        isValid &= validateScheduledAt();
        isValid &= validateDuration();

        if (comboMode.getValue() == null) {
            showValidationError("Please select an interview mode");
            isValid = false;
        }

        if (comboStatus.getValue() == null) {
            showValidationError("Please select an interview status");
            isValid = false;
        }

        return isValid;
    }

    private void clearForm() {
        txtApplicationId.clear();
        txtRecruiterId.clear();
        txtScheduledAt.clear();
        txtDuration.clear();
        comboMode.setValue(null);
        comboStatus.setValue(null);
        selectedInterview = null;
        resetFieldStyles();
    }

    private void resetFieldStyles() {
        txtApplicationId.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1;");
        txtRecruiterId.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1;");
        txtScheduledAt.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1;");
        txtDuration.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 1;");
    }

    private void showValidationError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

