package Controllers;

import Services.ApplicationService;
import Services.InterviewService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Applications UI.
 * - Candidate: read-only list of applications.
 * - Recruiter: can schedule interviews FROM an application.
 */
public class ApplicationsController {

    @FXML private VBox applicationsList;
    @FXML private Label lblSubtitle;
    @FXML private Label lblRoleHint;
    @FXML private Button btnAcceptAndSchedule;
    @FXML private Button btnScheduleForSelected;
    @FXML private Button btnReject;

    private ApplicationService.ApplicationRow selected;

    public void setUserRole(String role) {
        // Shell passes role label. Keep in sync with UserContext.
        refreshRoleUI();
    }

    @FXML
    public void initialize() {
        refreshRoleUI();
        loadApplications();
    }

    private void refreshRoleUI() {
        boolean recruiter = UserContext.getRole() == UserContext.Role.RECRUITER;

        if (lblSubtitle != null) {
            lblSubtitle.setText(recruiter
                    ? "Review applications and schedule interviews"
                    : "Track your applications and statuses");
        }
        if (lblRoleHint != null) {
            lblRoleHint.setText(recruiter
                    ? "Select an application then schedule an interview (interview is always linked to application_id)."
                    : "Read-only for now. Later: apply to a job offer and track status." );
        }

        if (btnAcceptAndSchedule != null) {
            btnAcceptAndSchedule.setVisible(recruiter);
            btnAcceptAndSchedule.setManaged(recruiter);
        }
        if (btnScheduleForSelected != null) {
            btnScheduleForSelected.setVisible(recruiter);
            btnScheduleForSelected.setManaged(recruiter);
        }
    }

    @FXML
    private void handleRefresh() {
        loadApplications();
    }

    private void loadApplications() {
        if (applicationsList == null) return;
        applicationsList.getChildren().clear();

        List<ApplicationService.ApplicationRow> apps = ApplicationService.getAll();
        if (apps.isEmpty()) {
            Label empty = new Label("No applications found");
            empty.getStyleClass().add("info-label");
            empty.setPadding(new Insets(20));
            applicationsList.getChildren().add(empty);
            return;
        }

        for (ApplicationService.ApplicationRow a : apps) {
            VBox card = new VBox(10);
            card.getStyleClass().addAll("interview-card");
            card.setPadding(new Insets(15));

            HBox top = new HBox(10);
            Label title = new Label("Application #" + a.id());
            title.getStyleClass().add("card-title");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label status = new Label(a.status());
            status.getStyleClass().addAll("tag");
            top.getChildren().addAll(title, spacer, status);

            HBox row = new HBox(30);
            VBox c1 = info("Candidate", String.valueOf(a.candidateId()));
            VBox c2 = info("Status", a.status());
            row.getChildren().addAll(c1, c2);

            card.getChildren().addAll(top, row);

            card.setOnMouseClicked(e -> {
                selected = a;
                highlightSelected(card);
            });

            applicationsList.getChildren().add(card);
        }

        refreshActionButtonsForSelection();
    }

    private VBox info(String label, String value) {
        VBox v = new VBox(4);
        Label l = new Label(label);
        l.getStyleClass().add("info-label");
        Label val = new Label(value);
        val.getStyleClass().add("info-value");
        v.getChildren().addAll(l, val);
        return v;
    }

    private void highlightSelected(VBox selectedCard) {
        for (var n : applicationsList.getChildren()) {
            if (n instanceof VBox v) {
                v.getStyleClass().remove("card-selected");
            }
        }
        selectedCard.getStyleClass().add("card-selected");

        refreshActionButtonsForSelection();
    }

    private void refreshActionButtonsForSelection() {
        boolean recruiter = UserContext.getRole() == UserContext.Role.RECRUITER;
        boolean hasSelection = selected != null;
        boolean rejected = hasSelection && selected.status() != null && selected.status().equalsIgnoreCase("REJECTED");

        if (btnReject != null) {
            btnReject.setDisable(!recruiter || !hasSelection || rejected);
        }
        if (btnAcceptAndSchedule != null) {
            btnAcceptAndSchedule.setDisable(!recruiter || !hasSelection || rejected);
        }
        if (btnScheduleForSelected != null) {
            btnScheduleForSelected.setDisable(!recruiter || !hasSelection || rejected);
        }
    }

    @FXML
    private void handleAcceptAndSchedule() {
        if (UserContext.getRole() != UserContext.Role.RECRUITER) return;
        if (selected == null) {
            alert("Select an application", "Please select an application first.", Alert.AlertType.WARNING);
            return;
        }

        // Mark accepted immediately, then schedule interview
        try {
            ApplicationService.accept(selected.id());
        } catch (Exception ex) {
            alert("Database Error", ex.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        openScheduleInterviewDialog(selected.id());
        loadApplications();
    }

    private void openScheduleInterviewDialog(int applicationId) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Schedule Interview");
        dialog.setHeaderText("Create interview for application #" + applicationId);

        VBox content = new VBox(10);
        content.getStyleClass().add("dialog-content");

        DatePicker dp = new DatePicker(LocalDate.now().plusDays(1));
        TextField tfTime = new TextField("14:00");
        TextField tfDuration = new TextField("60");
        ComboBox<String> cbMode = new ComboBox<>();
        cbMode.getItems().addAll("ONLINE", "ON_SITE");
        cbMode.setValue("ON_SITE");

        TextField tfLocation = new TextField();
        tfLocation.setPromptText("Location (required for ON_SITE)");

        TextField tfMeeting = new TextField();
        tfMeeting.setPromptText("Meeting link (required for ONLINE)");

        TextArea taNotes = new TextArea();
        taNotes.setPromptText("Notes (optional)");
        taNotes.setPrefRowCount(3);

        // show/hide link/location
        Runnable toggleFields = () -> {
            boolean online = "ONLINE".equals(cbMode.getValue());
            tfMeeting.setDisable(!online);
            tfMeeting.setManaged(online);
            tfLocation.setDisable(online);
            tfLocation.setManaged(!online);
        };
        cbMode.valueProperty().addListener((obs, o, n) -> toggleFields.run());
        toggleFields.run();

        content.getChildren().addAll(
                new Label("Date"), dp,
                new Label("Time (HH:mm)"), tfTime,
                new Label("Duration (minutes)"), tfDuration,
                new Label("Mode"), cbMode,
                tfMeeting,
                tfLocation,
                new Label("Notes"), taNotes
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(r -> {
            if (r != ButtonType.OK) return;

            // Parse + validate
            LocalDate date = dp.getValue();
            if (date == null) {
                alert("Validation", "Date is required.", Alert.AlertType.WARNING);
                return;
            }

            LocalTime time;
            try {
                time = LocalTime.parse(tfTime.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
            } catch (Exception e) {
                alert("Validation", "Time must be HH:mm (example 14:30).", Alert.AlertType.WARNING);
                return;
            }

            int duration;
            try {
                duration = Integer.parseInt(tfDuration.getText().trim());
                if (duration <= 0 || duration > 480) throw new NumberFormatException();
            } catch (Exception e) {
                alert("Validation", "Duration must be between 1 and 480.", Alert.AlertType.WARNING);
                return;
            }

            String mode = cbMode.getValue();
            if (mode == null) {
                alert("Validation", "Please select a mode.", Alert.AlertType.WARNING);
                return;
            }

            Models.Interview interview = new Models.Interview(
                    applicationId,
                    UserContext.getRecruiterId(),
                    LocalDateTime.of(date, time),
                    duration,
                    mode
            );
            interview.setStatus("SCHEDULED");
            interview.setNotes(taNotes.getText());

            if ("ONLINE".equals(mode)) {
                interview.setMeetingLink(tfMeeting.getText());
            } else {
                interview.setLocation(tfLocation.getText());
            }

            try {
                InterviewService.addInterview(interview);
                alert("Success", "Interview scheduled successfully.", Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                alert("Database Error", ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    @FXML
    private void handleReject() {
        if (UserContext.getRole() != UserContext.Role.RECRUITER) return;
        if (selected == null) {
            alert("Select an application", "Please select an application first.", Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Reject Applicant");
        confirm.setHeaderText("Reject application #" + selected.id());
        confirm.setContentText("This will delete the application from the database.");

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                try {
                    // Requirement: reject => delete
                    Services.ApplicationService.delete(selected.id());
                    alert("Success", "Application deleted.", Alert.AlertType.INFORMATION);
                    selected = null;
                    loadApplications();
                } catch (Exception ex) {
                    alert("Database Error", ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void alert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
