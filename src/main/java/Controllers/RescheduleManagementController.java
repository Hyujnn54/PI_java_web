package Controllers;

import Models.InterviewRescheduleRequest;
import Services.InterviewRescheduleService;
import Services.InterviewService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RescheduleManagementController {

    @FXML private TableView<InterviewRescheduleRequest> tableReschedule;
    @FXML private TableColumn<InterviewRescheduleRequest, Integer> colId;
    @FXML private TableColumn<InterviewRescheduleRequest, Integer> colInterviewId;
    @FXML private TableColumn<InterviewRescheduleRequest, Integer> colCandidateId;
    @FXML private TableColumn<InterviewRescheduleRequest, String> colRequestedDateTime;
    @FXML private TableColumn<InterviewRescheduleRequest, String> colReason;
    @FXML private TableColumn<InterviewRescheduleRequest, String> colStatus;

    @FXML private TextField txtInterviewId;
    @FXML private TextField txtCandidateId;
    @FXML private TextField txtRequestedDateTime;
    @FXML private TextArea txtReason;
    @FXML private ComboBox<String> comboStatus;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;
    @FXML private Button btnApprove;
    @FXML private Button btnReject;

    private ObservableList<InterviewRescheduleRequest> rescheduleList = FXCollections.observableArrayList();
    private InterviewRescheduleRequest selectedRequest = null;

    @FXML
    public void initialize() {
        // Initialize table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colInterviewId.setCellValueFactory(new PropertyValueFactory<>("interviewId"));
        colCandidateId.setCellValueFactory(new PropertyValueFactory<>("candidateId"));
        colRequestedDateTime.setCellValueFactory(new PropertyValueFactory<>("requestedDateTime"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Initialize combo box
        comboStatus.setItems(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED"));

        // Load data
        loadRescheduleRequests();

        // Add selection listener
        tableReschedule.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedRequest = newSelection;
                fillFormWithSelectedRequest();
            }
        });
    }

    @FXML
    private void handleAddRequest() {
        try {
            InterviewRescheduleRequest request = new InterviewRescheduleRequest(
                Integer.parseInt(txtInterviewId.getText()),
                Integer.parseInt(txtCandidateId.getText()),
                parseDateTime(txtRequestedDateTime.getText()),
                txtReason.getText()
            );

            if (comboStatus.getValue() != null) {
                request.setStatus(comboStatus.getValue());
            }

            InterviewRescheduleService.addRequest(request);
            showAlert("Success", "Reschedule request added successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadRescheduleRequests();
        } catch (Exception e) {
            showAlert("Error", "Failed to add request: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleUpdateRequest() {
        if (selectedRequest == null) {
            showAlert("Warning", "Please select a request to update", Alert.AlertType.WARNING);
            return;
        }

        try {
            InterviewRescheduleRequest request = new InterviewRescheduleRequest(
                Integer.parseInt(txtInterviewId.getText()),
                Integer.parseInt(txtCandidateId.getText()),
                parseDateTime(txtRequestedDateTime.getText()),
                txtReason.getText()
            );

            if (comboStatus.getValue() != null) {
                request.setStatus(comboStatus.getValue());
            }

            InterviewRescheduleService.updateRequest(selectedRequest.getId(), request);
            showAlert("Success", "Request updated successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadRescheduleRequests();
        } catch (Exception e) {
            showAlert("Error", "Failed to update request: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteRequest() {
        if (selectedRequest == null) {
            showAlert("Warning", "Please select a request to delete", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Reschedule Request");
        confirmAlert.setContentText("Are you sure you want to delete this request?");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            InterviewRescheduleService.deleteRequest(selectedRequest.getId());
            showAlert("Success", "Request deleted successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadRescheduleRequests();
        }
    }

    @FXML
    private void handleApproveRequest() {
        if (selectedRequest == null) {
            showAlert("Warning", "Please select a request to approve", Alert.AlertType.WARNING);
            return;
        }

        try {
            // Approve the reschedule request and update the interview
            InterviewService.approveRescheduleRequest(
                selectedRequest.getInterviewId(),
                selectedRequest.getRequestedDateTime()
            );

            // Update the request status
            selectedRequest.setStatus("APPROVED");
            InterviewRescheduleService.updateRequest(selectedRequest.getId(), selectedRequest);

            showAlert("Success", "Request approved and interview rescheduled!", Alert.AlertType.INFORMATION);
            clearForm();
            loadRescheduleRequests();
        } catch (Exception e) {
            showAlert("Error", "Failed to approve request: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleRejectRequest() {
        if (selectedRequest == null) {
            showAlert("Warning", "Please select a request to reject", Alert.AlertType.WARNING);
            return;
        }

        try {
            // Reject the reschedule request
            InterviewService.rejectRescheduleRequest(selectedRequest.getInterviewId());

            // Update the request status
            selectedRequest.setStatus("REJECTED");
            InterviewRescheduleService.updateRequest(selectedRequest.getId(), selectedRequest);

            showAlert("Success", "Request rejected!", Alert.AlertType.INFORMATION);
            clearForm();
            loadRescheduleRequests();
        } catch (Exception e) {
            showAlert("Error", "Failed to reject request: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleRefresh() {
        loadRescheduleRequests();
        clearForm();
    }

    private void loadRescheduleRequests() {
        rescheduleList.clear();
        rescheduleList.setAll(InterviewRescheduleService.getAll());
        tableReschedule.setItems(rescheduleList);
    }

    private void fillFormWithSelectedRequest() {
        txtInterviewId.setText(String.valueOf(selectedRequest.getInterviewId()));
        txtCandidateId.setText(String.valueOf(selectedRequest.getCandidateId()));
        txtRequestedDateTime.setText(selectedRequest.getRequestedDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        txtReason.setText(selectedRequest.getReason());
        comboStatus.setValue(selectedRequest.getStatus());
    }

    private void clearForm() {
        txtInterviewId.clear();
        txtCandidateId.clear();
        txtRequestedDateTime.clear();
        txtReason.clear();
        comboStatus.setValue(null);
        selectedRequest = null;
        tableReschedule.getSelectionModel().clearSelection();
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


