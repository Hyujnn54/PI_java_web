package Controllers.interview;

import Models.interview.Interview;
import Models.interview.InterviewFeedback;
import Services.interview.InterviewFeedbackService;
import Services.interview.InterviewService;
import Utils.MyDatabase;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Interview Management Controller — recruiter + candidate, with search/sort.
 */
public class InterviewManagementController {

    @FXML private VBox  interviewsListContainer;
    @FXML private Button btnScheduleNew;
    @FXML private VBox  editDialog;
    @FXML private HBox  bottomActionButtons;

    // Search / sort bar
    @FXML private VBox              searchBox;
    @FXML private ComboBox<String>  comboSearchCriteria;
    @FXML private TextField         txtSearchInterview;
    @FXML private DatePicker        dateSearchPicker;
    @FXML private ComboBox<String>  comboSearchMode;
    @FXML private ComboBox<String>  comboSearchStatus;
    @FXML private Button            btnSearchInterview;
    @FXML private Button            btnClearSearch;
    @FXML private ComboBox<String>  comboSortBy;
    @FXML private ComboBox<String>  comboSortDir;

    // Feedback panel
    @FXML private VBox             feedbackPanel;
    @FXML private ComboBox<String> comboFeedbackDecision;
    @FXML private TextField        txtFeedbackScore;
    @FXML private Label            lblScoreIndicator;
    @FXML private TextArea         txtFeedbackComments;
    @FXML private Button           btnUpdateFeedbackAction;
    @FXML private Button           btnDeleteFeedback;

    // Edit form
    @FXML private DatePicker       datePicker;
    @FXML private TextField        txtTime;
    @FXML private TextField        txtDuration;
    @FXML private ComboBox<String> comboMode;
    @FXML private TextField        txtMeetingLink;
    @FXML private HBox             meetingLinkBox;
    @FXML private Button           btnGenerateMeetingLink;
    @FXML private Button           btnOpenMeetingLink;
    @FXML private TextField        txtLocation;
    @FXML private TextArea         txtNotes;
    @FXML private Label            lblMeetingLink;
    @FXML private Label            lblLocation;
    @FXML private Button           btnSave;
    @FXML private TextField        txtApplicationId;
    @FXML private TextField        txtRecruiterId;
    @FXML private ComboBox<String> comboStatus;

    private Interview selectedInterview = null;
    private boolean   isEditMode        = false;

    // The full list currently displayed (for in-place sort without re-fetching)
    private List<Interview> currentList = new ArrayList<>();

    // =========================================================================
    // Init
    // =========================================================================

    @FXML
    public void initialize() {
        Utils.DatabaseSchemaUtil.cleanupCorruptedData();
        Utils.DatabaseSchemaUtil.verifyInterviewData();
        setupComboBoxes();
        loadInterviews();
        hideEditDialog();
        hideBottomActionButtons();
    }

    private void setupComboBoxes() {
        // Edit-form mode
        if (comboMode != null) {
            comboMode.setItems(FXCollections.observableArrayList("En Ligne", "Sur Site"));
            comboMode.valueProperty().addListener((obs, o, n) -> toggleModeFields(n));
        }

        // Feedback decision
        if (comboFeedbackDecision != null)
            comboFeedbackDecision.setItems(FXCollections.observableArrayList("Accepté", "Rejeté"));

        // Score live indicator
        if (txtFeedbackScore != null && lblScoreIndicator != null) {
            txtFeedbackScore.textProperty().addListener((obs, o, n) -> {
                try {
                    if (!n.trim().isEmpty()) {
                        int s = Integer.parseInt(n);
                        if (s >= 70) { lblScoreIndicator.setText("✓ ÉLEVÉ");  lblScoreIndicator.setStyle("-fx-text-fill:#28a745;-fx-font-size:12px;-fx-font-weight:600;"); }
                        else if (s >= 50) { lblScoreIndicator.setText("⚠ MOYEN"); lblScoreIndicator.setStyle("-fx-text-fill:#f0ad4e;-fx-font-size:12px;-fx-font-weight:600;"); }
                        else { lblScoreIndicator.setText("✗ FAIBLE"); lblScoreIndicator.setStyle("-fx-text-fill:#dc3545;-fx-font-size:12px;-fx-font-weight:600;"); }
                    } else lblScoreIndicator.setText("");
                } catch (NumberFormatException e) {
                    lblScoreIndicator.setText("Invalide"); lblScoreIndicator.setStyle("-fx-text-fill:#dc3545;-fx-font-size:12px;-fx-font-weight:600;");
                }
            });
        }

        // Search criteria
        if (comboSearchCriteria != null) {
            comboSearchCriteria.setItems(FXCollections.observableArrayList("Nom", "Date", "Mode", "Statut", "Lieu"));
            comboSearchCriteria.setValue("Nom");
            // Dynamically swap input widget when criteria changes
            comboSearchCriteria.valueProperty().addListener((obs, o, n) -> updateSearchInputVisibility(n));
        }

        // Search mode combo
        if (comboSearchMode != null)
            comboSearchMode.setItems(FXCollections.observableArrayList("En Ligne", "Sur Site"));

        // Search status combo
        if (comboSearchStatus != null)
            comboSearchStatus.setItems(FXCollections.observableArrayList("Planifié", "Terminé", "Annulé"));

        // Sort combos
        if (comboSortBy != null)
            comboSortBy.setItems(FXCollections.observableArrayList(
                "Date (planifiée)", "Durée", "Mode", "Statut", "Nom candidat"));
        if (comboSortBy != null) comboSortBy.setValue("Date (planifiée)");

        if (comboSortDir != null)
            comboSortDir.setItems(FXCollections.observableArrayList("Croissant ↑", "Décroissant ↓"));
        if (comboSortDir != null) comboSortDir.setValue("Croissant ↑");

        updateUIForRole();
    }

    /** Swap the search input widget (TextField / DatePicker / ComboBox) based on criteria */
    private void updateSearchInputVisibility(String criteria) {
        boolean showText   = criteria == null || criteria.equals("Nom") || criteria.equals("Lieu");
        boolean showDate   = "Date".equals(criteria);
        boolean showMode   = "Mode".equals(criteria);
        boolean showStatus = "Statut".equals(criteria);

        if (txtSearchInterview  != null) { txtSearchInterview.setVisible(showText);   txtSearchInterview.setManaged(showText); }
        if (dateSearchPicker    != null) { dateSearchPicker.setVisible(showDate);      dateSearchPicker.setManaged(showDate); }
        if (comboSearchMode     != null) { comboSearchMode.setVisible(showMode);       comboSearchMode.setManaged(showMode); }
        if (comboSearchStatus   != null) { comboSearchStatus.setVisible(showStatus);   comboSearchStatus.setManaged(showStatus); }
    }

    private void toggleModeFields(String mode) {
        if (mode == null) return;
        boolean online = "En Ligne".equals(mode) || "ONLINE".equals(mode);
        if (txtMeetingLink != null) { txtMeetingLink.setVisible(online);  txtMeetingLink.setManaged(online); }
        if (lblMeetingLink != null) { lblMeetingLink.setVisible(online);  lblMeetingLink.setManaged(online); }
        if (meetingLinkBox != null) { meetingLinkBox.setVisible(online);  meetingLinkBox.setManaged(online); }
        if (txtLocation    != null) { txtLocation.setVisible(!online);    txtLocation.setManaged(!online); }
        if (lblLocation    != null) { lblLocation.setVisible(!online);    lblLocation.setManaged(!online); }
    }

    private void updateUIForRole() {
        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;
        boolean isCandidate = Utils.UserContext.getRole() == Utils.UserContext.Role.CANDIDATE;

        if (btnScheduleNew != null) { btnScheduleNew.setVisible(false); btnScheduleNew.setManaged(false); }

        // Show search/sort bar for both recruiter and candidate
        if (searchBox != null) {
            boolean show = isRecruiter || isCandidate;
            searchBox.setVisible(show); searchBox.setManaged(show);
        }
    }

    // =========================================================================
    // Search
    // =========================================================================

    @FXML
    private void handleSearchInterview() {
        String criteria = comboSearchCriteria != null ? comboSearchCriteria.getValue() : "Nom";
        List<Interview> base = getRoleFilteredInterviews(InterviewService.getAll());
        List<Interview> filtered;

        switch (criteria) {
            case "Date" -> {
                LocalDate chosen = dateSearchPicker != null ? dateSearchPicker.getValue() : null;
                if (chosen == null) { displayFilteredInterviews(base); return; }
                filtered = base.stream()
                    .filter(iv -> iv.getScheduledAt().toLocalDate().equals(chosen))
                    .toList();
            }
            case "Mode" -> {
                String sel = comboSearchMode != null ? comboSearchMode.getValue() : null;
                if (sel == null) { displayFilteredInterviews(base); return; }
                String dbMode = "En Ligne".equals(sel) ? "ONLINE" : "ON_SITE";
                filtered = base.stream()
                    .filter(iv -> dbMode.equals(iv.getMode()))
                    .toList();
            }
            case "Statut" -> {
                String sel = comboSearchStatus != null ? comboSearchStatus.getValue() : null;
                if (sel == null) { displayFilteredInterviews(base); return; }
                String dbStatus = switch (sel) {
                    case "Planifié" -> "SCHEDULED";
                    case "Terminé"  -> "DONE";
                    case "Annulé"   -> "CANCELLED";
                    default -> sel;
                };
                filtered = base.stream()
                    .filter(iv -> dbStatus.equalsIgnoreCase(iv.getStatus()))
                    .toList();
            }
            case "Nom" -> {
                String kw = txtSearchInterview != null ? txtSearchInterview.getText().trim().toLowerCase() : "";
                if (kw.isEmpty()) { displayFilteredInterviews(base); return; }
                filtered = base.stream()
                    .filter(iv -> getCandidateNameForSearch(iv.getApplicationId()).toLowerCase().contains(kw))
                    .toList();
            }
            case "Lieu" -> {
                String kw = txtSearchInterview != null ? txtSearchInterview.getText().trim().toLowerCase() : "";
                if (kw.isEmpty()) { displayFilteredInterviews(base); return; }
                filtered = base.stream()
                    .filter(iv -> {
                        String loc  = iv.getLocation()    != null ? iv.getLocation().toLowerCase()    : "";
                        String link = iv.getMeetingLink() != null ? iv.getMeetingLink().toLowerCase() : "";
                        return loc.contains(kw) || link.contains(kw);
                    })
                    .toList();
            }
            default -> filtered = base;
        }
        currentList = new ArrayList<>(filtered);
        displayFilteredInterviews(filtered);
    }

    @FXML
    private void handleClearSearch() {
        if (txtSearchInterview  != null) txtSearchInterview.clear();
        if (dateSearchPicker    != null) dateSearchPicker.setValue(null);
        if (comboSearchMode     != null) comboSearchMode.setValue(null);
        if (comboSearchStatus   != null) comboSearchStatus.setValue(null);
        if (comboSearchCriteria != null) { comboSearchCriteria.setValue("Nom"); updateSearchInputVisibility("Nom"); }
        if (comboSortBy  != null) comboSortBy.setValue("Date (planifiée)");
        if (comboSortDir != null) comboSortDir.setValue("Croissant ↑");
        loadInterviews();
    }

    // =========================================================================
    // Sort
    // =========================================================================

    @FXML
    private void handleApplySort() {
        if (currentList.isEmpty()) return;
        String by  = comboSortBy  != null ? comboSortBy.getValue()  : "Date (planifiée)";
        String dir = comboSortDir != null ? comboSortDir.getValue() : "Croissant ↑";
        boolean asc = dir == null || dir.startsWith("C");

        Comparator<Interview> cmp = switch (by != null ? by : "") {
            case "Durée"        -> Comparator.comparingInt(Interview::getDurationMinutes);
            case "Mode"         -> Comparator.comparing(iv -> iv.getMode() != null ? iv.getMode() : "");
            case "Statut"       -> Comparator.comparing(iv -> iv.getStatus() != null ? iv.getStatus() : "");
            case "Nom candidat" -> Comparator.comparing(iv -> getCandidateNameForSearch(iv.getApplicationId()));
            default             -> Comparator.comparing(Interview::getScheduledAt);
        };
        if (!asc) cmp = cmp.reversed();

        List<Interview> sorted = new ArrayList<>(currentList);
        sorted.sort(cmp);
        displayFilteredInterviews(sorted);
    }

    // ── DB helper: get candidate name from application id ─────────────────────
    private String getCandidateNameForSearch(Long appId) {
        if (appId == null) return "";
        String sql = "SELECT u.first_name, u.last_name FROM job_application ja " +
                     "JOIN users u ON ja.candidate_id = u.id WHERE ja.id = ?";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, appId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return (rs.getString("first_name") + " " + rs.getString("last_name")).trim();
        } catch (Exception e) { System.err.println("getCandidateNameForSearch: " + e.getMessage()); }
        return "";
    }

    @FXML
    private void handleScheduleNew() { showEditDialog(null); }

    private void displayFilteredInterviews(List<Interview> interviews) {
        if (interviewsListContainer == null) return;
        interviewsListContainer.getChildren().clear();
        if (interviews.isEmpty()) {
            Label empty = new Label("Aucun entretien correspondant trouvé");
            empty.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 15px; -fx-padding: 40;");
            interviewsListContainer.getChildren().add(empty);
            return;
        }
        for (Interview iv : interviews)
            interviewsListContainer.getChildren().add(createModernInterviewCard(iv));
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
            showAlert("Non autorisé", "Les entretiens sont créés à partir des candidatures. Sélectionnez une candidature et planifiez à partir de là.", Alert.AlertType.INFORMATION);
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

            // Convert French display names to database enum values
            String dbMode = convertModeToDatabase(mode);

            if (selectedInterview != null) {
                selectedInterview.setScheduledAt(scheduledAt);
                selectedInterview.setDurationMinutes(duration);
                selectedInterview.setMode(dbMode);

                // Update meeting link or location based on mode
                if ("ONLINE".equals(dbMode)) {
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

                    // Email reminder will be sent automatically by InterviewReminderScheduler
                    // (24 hours before the interview, if not already sent)

                } catch (RuntimeException e) {
                    showAlert("Erreur de Base de Données", "Échec de la mise à jour de l'entretien: " + e.getMessage(), Alert.AlertType.ERROR);
                    return;
                }

                hideEditDialog();
                loadInterviews();
                showAlert("Succès", "Entretien mis à jour avec succès! Un rappel par email sera envoyé 24h avant.", Alert.AlertType.INFORMATION);
            }

        } catch (Exception e) {
            showAlert("Erreur", "Échec de l'enregistrement de l'entretien: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void loadInterviews() {
        if (interviewsListContainer == null) return;
        List<Interview> interviews = getRoleFilteredInterviews(InterviewService.getAll());
        // Default sort: ascending by date
        interviews = new ArrayList<>(interviews);
        interviews.sort(Comparator.comparing(Interview::getScheduledAt));
        currentList = new ArrayList<>(interviews);
        displayFilteredInterviews(interviews);
    }

    /** Filter a list by the current user's role */
    private List<Interview> getRoleFilteredInterviews(List<Interview> all) {
        Utils.UserContext.Role role = Utils.UserContext.getRole();
        if (role == Utils.UserContext.Role.RECRUITER) {
            Long rid = Utils.UserContext.getRecruiterId();
            return all.stream().filter(i -> rid != null && rid.equals(i.getRecruiterId())).toList();
        } else if (role == Utils.UserContext.Role.CANDIDATE) {
            Long cid = Utils.UserContext.getCandidateId();
            List<Long> appIds = Services.application.ApplicationService.getByCandidateId(cid)
                    .stream().map(a -> a.id()).toList();
            return all.stream().filter(i -> i.getApplicationId() != null && appIds.contains(i.getApplicationId())).toList();
        }
        return all; // ADMIN sees all
    }

    private VBox createModernInterviewCard(Interview interview) {
        VBox card = new VBox(15);
        card.getStyleClass().add("interview-card");
        card.setPadding(new Insets(20));

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        // Header with title and status
        HBox header = new HBox(15);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label(isRecruiter ? "Entretien #" + interview.getId() : "Votre Prochain Entretien");
        title.getStyleClass().add("card-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label statusTag = new Label(interview.getStatus() != null ? interview.getStatus() : "PLANIFIÉ");
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
            String link = interview.getMeetingLink().trim();
            HBox linkRow = new HBox(8);
            linkRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            Label linkIcon = new Label("🔗 Lien de Réunion:");
            linkIcon.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50; -fx-font-weight: 600;");
            Hyperlink hyperlink = new Hyperlink(link);
            hyperlink.setStyle("-fx-text-fill: #5BA3F5; -fx-font-size: 12px; -fx-padding: 0;");
            hyperlink.setWrapText(true);
            hyperlink.setOnAction(e -> {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(link));
                } catch (Exception ex) {
                    // If Desktop browse fails, copy to clipboard
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(link);
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                    showAlert("Lien copié", "Le lien a été copié dans le presse-papiers.", Alert.AlertType.INFORMATION);
                }
            });
            linkRow.getChildren().addAll(linkIcon, hyperlink);
            card.getChildren().add(linkRow);
        } else if ("ON_SITE".equals(interview.getMode()) && interview.getLocation() != null && !interview.getLocation().trim().isEmpty()) {
            Label locLabel = new Label("📍 Lieu: " + interview.getLocation());
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
                Label candidateMsg = new Label("En Attente de Révision");
                candidateMsg.setStyle("-fx-text-fill: #f0ad4e; -fx-font-size: 12px; -fx-font-weight: 600;");
                actionBox.getChildren().add(candidateMsg);
            }
            return actionBox;
        }

        // Recruiter sees feedback action buttons in the card itself
        boolean hasFeedback = checkIfFeedbackExists(interview.getId());

        if (hasFeedback) {
            // If feedback exists: show View, Update, Delete buttons
            Button btnViewFeedback = new Button("👁 Voir");
            btnViewFeedback.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand;");
            btnViewFeedback.setOnAction(e -> viewFeedback(interview));

            Button btnUpdateFeedback = new Button("✏ Modifier");
            btnUpdateFeedback.setStyle("-fx-background-color: #f0ad4e; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand;");
            btnUpdateFeedback.setOnAction(e -> updateFeedback(interview));

            Button btnDeleteFeedback = new Button("🗑 Supprimer");
            btnDeleteFeedback.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 6; -fx-font-weight: 600; -fx-font-size: 12px; -fx-cursor: hand;");
            btnDeleteFeedback.setOnAction(e -> deleteFeedbackForInterview(interview));

            actionBox.getChildren().addAll(btnViewFeedback, btnUpdateFeedback, btnDeleteFeedback);
        } else {
            // If no feedback: show Create button
            Button btnCreateFeedback = new Button("📋 Créer Retour");
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
            alert.setTitle("Retour d'Entretien");
            alert.setHeaderText("Retour pour l'entretien #" + interview.getId());

            String decision = feedback.getDecision() != null ? feedback.getDecision() : "N/A";
            String content = "Décision: " + decision + "\n" +
                           "Score Global: " + (feedback.getOverallScore() != null ? feedback.getOverallScore() : "N/A") + "/100\n\n" +
                           "Commentaires:\n" + (feedback.getComment() != null ? feedback.getComment() : "Aucun commentaire");

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
            // Convert DB enum to French display value
            String decision = existingFeedback.getDecision();
            String decisionDisplay = "ACCEPTED".equals(decision) ? "Accepté"
                                   : "REJECTED".equals(decision) ? "Rejeté"
                                   : decision;

            if (comboFeedbackDecision != null) {
                comboFeedbackDecision.setValue(decisionDisplay);
            }

            if (existingFeedback.getOverallScore() != null) {
                txtFeedbackScore.setText(String.valueOf(existingFeedback.getOverallScore()));
            } else {
                txtFeedbackScore.setText("");
            }
            txtFeedbackComments.setText(existingFeedback.getComment() != null ? existingFeedback.getComment() : "");

            // Set button text for updating
            if (btnUpdateFeedbackAction != null) {
                btnUpdateFeedbackAction.setText("💾 Mettre à Jour Retour");
            }
        } else {
            if (comboFeedbackDecision != null) comboFeedbackDecision.setValue(null);
            txtFeedbackScore.setText("");
            txtFeedbackComments.setText("");

            // Set button text for creating
            if (btnUpdateFeedbackAction != null) {
                btnUpdateFeedbackAction.setText("💾 Créer Retour");
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
            showAlert("Erreur", "Aucun entretien sélectionné.", Alert.AlertType.ERROR);
            return;
        }

        System.out.println("\n============ FEEDBACK UPDATE STARTED ============");
        System.out.println("Interview ID: " + selectedInterview.getId());

        try {
            // Validation - decision is required
            if (comboFeedbackDecision == null || comboFeedbackDecision.getValue() == null || comboFeedbackDecision.getValue().trim().isEmpty()) {
                System.err.println("VALIDATION ERROR: Decision not selected");
                showAlert("Erreur de Validation", "Veuillez sélectionner une décision (Accepté ou Rejeté).", Alert.AlertType.WARNING);
                return;
            }

            // Validation - score is required
            if (txtFeedbackScore.getText().trim().isEmpty()) {
                System.err.println("VALIDATION ERROR: Score is empty");
                showAlert("Erreur de Validation", "Veuillez entrer un score.", Alert.AlertType.WARNING);
                return;
            }

            int overallScore;
            try {
                overallScore = Integer.parseInt(txtFeedbackScore.getText().trim());
            } catch (NumberFormatException e) {
                System.err.println("VALIDATION ERROR: Score is not a number: " + txtFeedbackScore.getText());
                showAlert("Erreur de Validation", "Le score doit être un nombre valide.", Alert.AlertType.WARNING);
                return;
            }

            if (overallScore < 0 || overallScore > 100) {
                System.err.println("VALIDATION ERROR: Score out of range: " + overallScore);
                showAlert("Erreur de Validation", "Le score doit être entre 0 et 100.", Alert.AlertType.WARNING);
                return;
            }

            String comment = txtFeedbackComments.getText();
            String decisionDisplay = comboFeedbackDecision.getValue();
            // Convert French display value to DB enum
            String decision = "Accepté".equals(decisionDisplay) ? "ACCEPTED"
                            : "Rejeté".equals(decisionDisplay)   ? "REJECTED"
                            : decisionDisplay; // fallback (already in English)
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
                showAlert("Succès", "Retour mis à jour avec succès.", Alert.AlertType.INFORMATION);
            } else {
                System.out.println("Calling InterviewFeedbackService.addFeedback()");
                InterviewFeedbackService.addFeedback(fb);
                System.out.println("✓ Create completed successfully");
                showAlert("Succès", "Retour créé avec succès.", Alert.AlertType.INFORMATION);
            }

            System.out.println("============ FEEDBACK UPDATE COMPLETED ============\n");

            hideFeedbackPanel();
            loadInterviews();
        } catch (Exception e) {
            System.err.println("ERROR during feedback save: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Échec de la sauvegarde du retour: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleDeleteFeedback() {
        if (selectedInterview == null) return;

        var feedbacks = InterviewFeedbackService.getByInterviewId(selectedInterview.getId());
        if (!feedbacks.isEmpty()) {
            InterviewFeedback existing = feedbacks.get(0);

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Supprimer le Retour");
            confirm.setHeaderText("Supprimer ce retour?");
            confirm.setContentText("Cette action ne peut pas être annulée.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    InterviewFeedbackService.deleteFeedback(existing.getId());
                    showAlert("Succès", "Retour supprimé avec succès.", Alert.AlertType.INFORMATION);
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
            showAlert("Attention", "Veuillez sélectionner un entretien à mettre à jour", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleDeleteInterview() {
        if (selectedInterview == null) {
            showAlert("Attention", "Veuillez sélectionner un entretien à supprimer", Alert.AlertType.WARNING);
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmer la Suppression");
        confirmAlert.setHeaderText("Supprimer l'Entretien");
        confirmAlert.setContentText("Êtes-vous sûr de vouloir supprimer cet entretien? Cette action ne peut pas être annulée.");

        confirmAlert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    InterviewService.delete(selectedInterview.getId());
                    showAlert("Succès", "Entretien supprimé avec succès!", Alert.AlertType.INFORMATION);
                    selectedInterview = null;
                    hideBottomActionButtons();
                    loadInterviews();
                } catch (Exception e) {
                    showAlert("Erreur", "Échec de la suppression de l'entretien: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private boolean validateInput() {
        System.out.println("Validating input...");

        // Check if form fields are properly initialized
        if (datePicker == null) {
            System.out.println("DatePicker is null!");
            showAlert("Erreur", "Formulaire non correctement initialisé. Veuillez réessayer.", Alert.AlertType.ERROR);
            return false;
        }

        if (datePicker.getValue() == null) {
            showAlert("Erreur de Validation", "Veuillez sélectionner une date", Alert.AlertType.WARNING);
            return false;
        }

        if (txtTime == null || txtTime.getText().trim().isEmpty()) {
            showAlert("Erreur de Validation", "Veuillez entrer une heure", Alert.AlertType.WARNING);
            return false;
        }

        try {
            LocalTime.parse(txtTime.getText().trim(), DateTimeFormatter.ofPattern("HH:mm"));
        } catch (Exception e) {
            showAlert("Erreur de Validation", "L'heure doit être au format HH:mm (ex: 14:30)", Alert.AlertType.WARNING);
            return false;
        }

        if (txtDuration == null || txtDuration.getText().trim().isEmpty()) {
            showAlert("Erreur de Validation", "Veuillez entrer la durée en minutes", Alert.AlertType.WARNING);
            return false;
        }

        try {
            int duration = Integer.parseInt(txtDuration.getText().trim());
            if (duration <= 0 || duration > 480) {
                showAlert("Erreur de Validation", "La durée doit être entre 1 et 480 minutes", Alert.AlertType.WARNING);
                return false;
            }
        } catch (NumberFormatException e) {
            showAlert("Erreur de Validation", "La durée doit être un nombre valide", Alert.AlertType.WARNING);
            return false;
        }

        if (comboMode == null || comboMode.getValue() == null) {
            showAlert("Erreur de Validation", "Veuillez sélectionner un mode d'entretien", Alert.AlertType.WARNING);
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

    /**
     * Convert French display mode to database enum value
     */
    private String convertModeToDatabase(String displayMode) {
        if (displayMode == null) return "ONLINE";
        if ("En Ligne".equals(displayMode)) return "ONLINE";
        if ("Sur Site".equals(displayMode)) return "ON_SITE";
        // If already a database value, return as-is
        return displayMode;
    }

    /**
     * Convert database mode value to French display name
     */
    private String convertModeToDisplay(String dbMode) {
        if (dbMode == null) return "En Ligne";
        if ("ONLINE".equals(dbMode)) return "En Ligne";
        if ("ON_SITE".equals(dbMode)) return "Sur Site";
        // If already a display value, return as-is
        return dbMode;
    }

    /**
     * Convert French feedback decision to database value
     */
    private String convertDecisionToDatabase(String displayDecision) {
        if (displayDecision == null) return "ACCEPTED";
        if ("Accepté".equals(displayDecision)) return "ACCEPTED";
        if ("Rejeté".equals(displayDecision)) return "REJECTED";
        return displayDecision;
    }

    /**
     * Convert database decision value to French display name
     */
    private String convertDecisionToDisplay(String dbDecision) {
        if (dbDecision == null) return "Accepté";
        if ("ACCEPTED".equals(dbDecision)) return "Accepté";
        if ("REJECTED".equals(dbDecision)) return "Rejeté";
        return dbDecision;
    }

    @FXML
    private void handleGenerateMeetingLink() {
        try {
            if (datePicker.getValue() == null || txtTime.getText().trim().isEmpty()) {
                showAlert("Erreur", "Veuillez sélectionner une date et une heure avant de générer le lien.", Alert.AlertType.WARNING);
                return;
            }
            java.time.LocalTime time = java.time.LocalTime.parse(txtTime.getText().trim());
            LocalDateTime scheduledAt = LocalDateTime.of(datePicker.getValue(), time);
            int duration = 60;
            try { duration = Integer.parseInt(txtDuration.getText().trim()); } catch (Exception ignored) {}

            // Use applicationId from hidden field if available
            Long appId = null;
            if (txtApplicationId != null && txtApplicationId.getText() != null && !txtApplicationId.getText().isBlank()) {
                try { appId = Long.parseLong(txtApplicationId.getText().trim()); } catch (Exception ignored) {}
            }
            if (appId == null && selectedInterview != null) {
                appId = selectedInterview.getApplicationId();
            }
            if (appId == null) appId = 0L;

            String link = Services.interview.MeetingService.generateMeetingLink(appId, scheduledAt, duration);
            if (txtMeetingLink != null) {
                txtMeetingLink.setText(link);
                txtMeetingLink.setStyle("-fx-background-color: #d4edda; -fx-background-radius: 6;");
            }
            if (btnOpenMeetingLink != null) {
                btnOpenMeetingLink.setVisible(true);
                btnOpenMeetingLink.setManaged(true);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible de générer le lien : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleOpenMeetingLink() {
        if (txtMeetingLink == null || txtMeetingLink.getText().isBlank()) {
            showAlert("Erreur", "Aucun lien de réunion disponible.", Alert.AlertType.WARNING);
            return;
        }
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(txtMeetingLink.getText().trim()));
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le lien : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
}
