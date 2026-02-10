package Controllers;

import Models.Interview;
import Services.InterviewService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class InterviewManagementController {

    @FXML private TableView<Interview> tableInterviews;
    @FXML private TableColumn<Interview, Integer> colId;
    @FXML private TableColumn<Interview, Integer> colApplicationId;
    @FXML private TableColumn<Interview, Integer> colRecruiterId;
    @FXML private TableColumn<Interview, String> colScheduledAt;
    @FXML private TableColumn<Interview, Integer> colDuration;
    @FXML private TableColumn<Interview, String> colMode;
    @FXML private TableColumn<Interview, String> colStatus;

    @FXML private TextField txtApplicationId;
    @FXML private TextField txtRecruiterId;
    @FXML private TextField txtScheduledAt;
    @FXML private TextField txtDuration;
    @FXML private ComboBox<String> comboMode;
    @FXML private ComboBox<String> comboStatus;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;

    private ObservableList<Interview> interviewList = FXCollections.observableArrayList();
    private Interview selectedInterview = null;

    @FXML
    public void initialize() {
        // Initialize table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colApplicationId.setCellValueFactory(new PropertyValueFactory<>("applicationId"));
        colRecruiterId.setCellValueFactory(new PropertyValueFactory<>("recruiterId"));
        colScheduledAt.setCellValueFactory(new PropertyValueFactory<>("scheduledAt"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        colMode.setCellValueFactory(new PropertyValueFactory<>("mode"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Initialize combo boxes
        comboMode.setItems(FXCollections.observableArrayList("ONLINE", "IN_PERSON"));
        comboStatus.setItems(FXCollections.observableArrayList("SCHEDULED", "COMPLETED", "CANCELLED", "APPROVED", "REJECTED"));

        // Load data
        loadInterviews();

        // Add selection listener
        tableInterviews.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedInterview = newSelection;
                fillFormWithSelectedInterview();
            }
        });
    }

    @FXML
    private void handleAddInterview() {
        try {
            Interview interview = new Interview(
                Integer.parseInt(txtApplicationId.getText()),
                Integer.parseInt(txtRecruiterId.getText()),
                parseDateTime(txtScheduledAt.getText()),
                Integer.parseInt(txtDuration.getText()),
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

        try {
            Interview interview = new Interview(
                Integer.parseInt(txtApplicationId.getText()),
                Integer.parseInt(txtRecruiterId.getText()),
                parseDateTime(txtScheduledAt.getText()),
                Integer.parseInt(txtDuration.getText()),
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
        interviewList.clear();
        interviewList.setAll(InterviewService.getAll());
        tableInterviews.setItems(interviewList);
    }

    private void fillFormWithSelectedInterview() {
        txtApplicationId.setText(String.valueOf(selectedInterview.getApplicationId()));
        txtRecruiterId.setText(String.valueOf(selectedInterview.getRecruiterId()));
        txtScheduledAt.setText(selectedInterview.getScheduledAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        txtDuration.setText(String.valueOf(selectedInterview.getDurationMinutes()));
        comboMode.setValue(selectedInterview.getMode());
        comboStatus.setValue(selectedInterview.getStatus());
    }

    private void clearForm() {
        txtApplicationId.clear();
        txtRecruiterId.clear();
        txtScheduledAt.clear();
        txtDuration.clear();
        comboMode.setValue(null);
        comboStatus.setValue(null);
        selectedInterview = null;
        tableInterviews.getSelectionModel().clearSelection();
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return LocalDateTime.parse(dateTimeStr, formatter);
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}


