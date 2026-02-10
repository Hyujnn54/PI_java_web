package Controllers;

import Models.InterviewFeedback;
import Services.InterviewFeedbackService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

public class FeedbackManagementController {

    @FXML private TableView<InterviewFeedback> tableFeedback;
    @FXML private TableColumn<InterviewFeedback, Integer> colId;
    @FXML private TableColumn<InterviewFeedback, Integer> colInterviewId;
    @FXML private TableColumn<InterviewFeedback, Integer> colRecruiterId;
    @FXML private TableColumn<InterviewFeedback, Integer> colTechnicalScore;
    @FXML private TableColumn<InterviewFeedback, Integer> colCommunicationScore;
    @FXML private TableColumn<InterviewFeedback, Integer> colCultureFitScore;
    @FXML private TableColumn<InterviewFeedback, Integer> colOverallScore;
    @FXML private TableColumn<InterviewFeedback, String> colDecision;

    @FXML private TextField txtInterviewId;
    @FXML private TextField txtRecruiterId;
    @FXML private TextField txtTechnicalScore;
    @FXML private TextField txtCommunicationScore;
    @FXML private TextField txtCultureFitScore;
    @FXML private ComboBox<String> comboDecision;
    @FXML private TextArea txtComment;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnRefresh;

    private ObservableList<InterviewFeedback> feedbackList = FXCollections.observableArrayList();
    private InterviewFeedback selectedFeedback = null;

    @FXML
    public void initialize() {
        // Initialize table columns
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colInterviewId.setCellValueFactory(new PropertyValueFactory<>("interviewId"));
        colRecruiterId.setCellValueFactory(new PropertyValueFactory<>("recruiterId"));
        colTechnicalScore.setCellValueFactory(new PropertyValueFactory<>("technicalScore"));
        colCommunicationScore.setCellValueFactory(new PropertyValueFactory<>("communicationScore"));
        colCultureFitScore.setCellValueFactory(new PropertyValueFactory<>("cultureFitScore"));
        colOverallScore.setCellValueFactory(new PropertyValueFactory<>("overallScore"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));

        // Initialize combo box
        comboDecision.setItems(FXCollections.observableArrayList("ACCEPTED", "REJECTED", "PENDING"));

        // Load data
        loadFeedback();

        // Add selection listener
        tableFeedback.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedFeedback = newSelection;
                fillFormWithSelectedFeedback();
            }
        });
    }

    @FXML
    private void handleAddFeedback() {
        try {
            InterviewFeedback feedback = new InterviewFeedback(
                Integer.parseInt(txtInterviewId.getText()),
                Integer.parseInt(txtRecruiterId.getText()),
                Integer.parseInt(txtTechnicalScore.getText()),
                Integer.parseInt(txtCommunicationScore.getText()),
                Integer.parseInt(txtCultureFitScore.getText()),
                comboDecision.getValue(),
                txtComment.getText()
            );

            InterviewFeedbackService.addFeedback(feedback);
            showAlert("Success", "Feedback added successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadFeedback();
        } catch (Exception e) {
            showAlert("Error", "Failed to add feedback: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleUpdateFeedback() {
        if (selectedFeedback == null) {
            showAlert("Warning", "Please select a feedback to update", Alert.AlertType.WARNING);
            return;
        }

        try {
            InterviewFeedback feedback = new InterviewFeedback(
                Integer.parseInt(txtInterviewId.getText()),
                Integer.parseInt(txtRecruiterId.getText()),
                Integer.parseInt(txtTechnicalScore.getText()),
                Integer.parseInt(txtCommunicationScore.getText()),
                Integer.parseInt(txtCultureFitScore.getText()),
                comboDecision.getValue(),
                txtComment.getText()
            );

            InterviewFeedbackService.updateFeedback(selectedFeedback.getId(), feedback);
            showAlert("Success", "Feedback updated successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadFeedback();
        } catch (Exception e) {
            showAlert("Error", "Failed to update feedback: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteFeedback() {
        if (selectedFeedback == null) {
            showAlert("Warning", "Please select a feedback to delete", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Feedback");
        confirmAlert.setContentText("Are you sure you want to delete this feedback?");

        if (confirmAlert.showAndWait().get() == ButtonType.OK) {
            InterviewFeedbackService.deleteFeedback(selectedFeedback.getId());
            showAlert("Success", "Feedback deleted successfully!", Alert.AlertType.INFORMATION);
            clearForm();
            loadFeedback();
        }
    }

    @FXML
    private void handleRefresh() {
        loadFeedback();
        clearForm();
    }

    private void loadFeedback() {
        feedbackList.clear();
        feedbackList.setAll(InterviewFeedbackService.getAll());
        tableFeedback.setItems(feedbackList);
    }

    private void fillFormWithSelectedFeedback() {
        txtInterviewId.setText(String.valueOf(selectedFeedback.getInterviewId()));
        txtRecruiterId.setText(String.valueOf(selectedFeedback.getRecruiterId()));
        txtTechnicalScore.setText(String.valueOf(selectedFeedback.getTechnicalScore()));
        txtCommunicationScore.setText(String.valueOf(selectedFeedback.getCommunicationScore()));
        txtCultureFitScore.setText(String.valueOf(selectedFeedback.getCultureFitScore()));
        comboDecision.setValue(selectedFeedback.getDecision());
        txtComment.setText(selectedFeedback.getComment());
    }

    private void clearForm() {
        txtInterviewId.clear();
        txtRecruiterId.clear();
        txtTechnicalScore.clear();
        txtCommunicationScore.clear();
        txtCultureFitScore.clear();
        comboDecision.setValue(null);
        txtComment.clear();
        selectedFeedback = null;
        tableFeedback.getSelectionModel().clearSelection();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}


