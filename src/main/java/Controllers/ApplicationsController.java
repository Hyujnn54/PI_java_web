package Controllers;

import Services.ApplicationService;
import Services.InterviewService;
import Services.EmailService;
import Services.MeetingService;
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
            cbSearchCriteria.getItems().addAll("Nom", "Email", "Poste", "Statut", "Date");
            cbSearchCriteria.setValue("Nom");
        }

        if (cbApplicationStatus != null) {
            cbApplicationStatus.getItems().addAll("Nouveau", "En révision", "Présélectionné",
                                                  "Entretien planifié", "Accepté", "Rejeté");
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
            Long currentCandidateId = Utils.UserContext.getCandidateId(); // You'll need to add this method
            if (currentCandidateId != null) {
                apps = apps.stream()
                          .filter(app -> app.candidateId().equals(currentCandidateId))
                          .toList();
            }
        }

        if (apps.isEmpty()) {
            Label empty = new Label(isRecruiter ? "Aucune candidature trouvée" : "Vous n'avez postulé à aucun poste pour le moment");
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

        Label name = new Label(app.candidateName() != null && !app.candidateName().trim().isEmpty()
                                ? app.candidateName()
                                : "Candidate #" + app.id());
        name.getStyleClass().add("candidate-name");

        Label position = new Label(app.jobTitle() != null ? app.jobTitle() : "Application");
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

        Label statusBadge = new Label(app.currentStatus());
        statusBadge.getStyleClass().addAll("status-badge", getStatusClass(app.currentStatus()));

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

        boolean isRecruiter = Utils.UserContext.getRole() == Utils.UserContext.Role.RECRUITER;

        if (isRecruiter) {
            // Recruiter view: Show candidate details
            loadRecruiterDetailView(app);
        } else {
            // Candidate view: Show job details and application status
            loadCandidateDetailView(app);
        }
    }

    private void loadCandidateDetailView(ApplicationService.ApplicationRow app) {
        // Header with job title
        VBox headerCard = new VBox(15);
        headerCard.getStyleClass().add("detail-header-card");

        Label jobTitle = new Label(app.jobTitle() != null ? app.jobTitle() : "Job Position");
        jobTitle.getStyleClass().add("detail-candidate-name");

        Label appliedDate = new Label("Applied on: " +
            (app.appliedAt() != null ? app.appliedAt().format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy")) : "N/A"));
        appliedDate.getStyleClass().add("detail-candidate-position");

        headerCard.getChildren().addAll(jobTitle, appliedDate);
        detailContainer.getChildren().add(headerCard);

        // Application Status Card
        VBox statusCard = new VBox(15);
        statusCard.getStyleClass().add("detail-section-card");

        Label statusTitle = new Label("Application Status");
        statusTitle.getStyleClass().add("detail-section-title");

        Label statusBadge = new Label(app.currentStatus() != null ? app.currentStatus() : "SUBMITTED");
        statusBadge.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-padding: 8 16; " +
                           "-fx-background-radius: 6; -fx-font-size: 14px; -fx-font-weight: 600;");

        statusCard.getChildren().addAll(statusTitle, statusBadge);
        detailContainer.getChildren().add(statusCard);

        // Cover Letter Section
        if (app.coverLetter() != null && !app.coverLetter().trim().isEmpty()) {
            VBox coverLetterCard = new VBox(12);
            coverLetterCard.getStyleClass().add("detail-section-card");

            Label coverLetterTitle = new Label("Your Cover Letter");
            coverLetterTitle.getStyleClass().add("detail-section-title");

            Label coverLetterText = new Label(app.coverLetter());
            coverLetterText.setWrapText(true);
            coverLetterText.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px; -fx-line-spacing: 2;");

            coverLetterCard.getChildren().addAll(coverLetterTitle, coverLetterText);
            detailContainer.getChildren().add(coverLetterCard);
        }

        // Contact Information
        VBox contactCard = new VBox(15);
        contactCard.getStyleClass().add("detail-section-card");

        Label contactTitle = new Label("Your Contact Information");
        contactTitle.getStyleClass().add("detail-section-title");

        HBox contactInfo = new HBox(25);

        VBox phoneBox = new VBox(6);
        Label phoneLabel = new Label("📞 Phone");
        phoneLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #6c757d; -fx-font-size: 12px;");
        Label phoneValue = new Label(app.phone() != null ? app.phone() : "Not provided");
        phoneValue.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        phoneBox.getChildren().addAll(phoneLabel, phoneValue);

        VBox cvBox = new VBox(6);
        Label cvLabel = new Label("📄 CV");
        cvLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #6c757d; -fx-font-size: 12px;");
        Label cvValue = new Label(app.cvPath() != null ? "Uploaded" : "Not uploaded");
        cvValue.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 14px;");
        cvBox.getChildren().addAll(cvLabel, cvValue);

        contactInfo.getChildren().addAll(phoneBox, cvBox);
        contactCard.getChildren().addAll(contactTitle, contactInfo);
        detailContainer.getChildren().add(contactCard);
    }

    private void loadRecruiterDetailView(ApplicationService.ApplicationRow app) {
        // Create detail header card
        VBox headerCard = createDetailHeader(app);
        detailContainer.getChildren().add(headerCard);

        // Cover Letter Section
        if (app.coverLetter() != null && !app.coverLetter().trim().isEmpty()) {
            VBox coverLetterCard = new VBox(12);
            coverLetterCard.getStyleClass().add("detail-section-card");

            Label coverLetterTitle = new Label("Cover Letter");
            coverLetterTitle.getStyleClass().add("detail-section-title");

            Label coverLetterText = new Label(app.coverLetter());
            coverLetterText.setWrapText(true);
            coverLetterText.setStyle("-fx-text-fill: #495057; -fx-font-size: 13px; -fx-line-spacing: 2;");

            coverLetterCard.getChildren().addAll(coverLetterTitle, coverLetterText);
            detailContainer.getChildren().add(coverLetterCard);
        }

        // Application Status Section
        VBox statusSection = new VBox(15);
        statusSection.getStyleClass().add("detail-section-card");

        Label statusTitle = new Label("Application Status");
        statusTitle.getStyleClass().add("detail-section-title");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getStyleClass().add("modern-combo");
        statusCombo.setPrefWidth(300);
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED",
                                      "INTERVIEW", "REJECTED", "HIRED");
        statusCombo.setValue(app.currentStatus());
        statusCombo.setOnAction(e -> {
            ApplicationService.updateStatus(app.id(), statusCombo.getValue());
        });

        statusSection.getChildren().addAll(statusTitle, statusCombo);
        detailContainer.getChildren().add(statusSection);

        // Action Buttons (only for recruiters)
        HBox actionButtons = new HBox(15);
        actionButtons.getStyleClass().add("action-buttons-container");
        actionButtons.setAlignment(Pos.CENTER_LEFT);

        Button btnSchedule = new Button("📅 Schedule Interview");
        btnSchedule.getStyleClass().addAll("btn-primary", "action-button");
        btnSchedule.setOnAction(e -> handleScheduleInterview(app));

        Button btnReject = new Button("✗ Reject");
        btnReject.getStyleClass().addAll("btn-danger", "action-button");
        btnReject.setOnAction(e -> handleRejectCandidate(app));

        Button btnDownload = new Button("📥 Download CV");
        btnDownload.getStyleClass().addAll("btn-secondary", "action-button");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        actionButtons.getChildren().addAll(btnSchedule, btnDownload, spacer, btnReject);
        detailContainer.getChildren().add(actionButtons);
    }

    private VBox createDetailHeader(ApplicationService.ApplicationRow app) {
        VBox headerCard = new VBox(15);
        headerCard.getStyleClass().add("detail-header-card");

        // Name and position
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox nameBox = new VBox(8);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label name = new Label(app.candidateName() != null && !app.candidateName().trim().isEmpty()
                              ? app.candidateName()
                              : "Candidate #" + app.id());
        name.getStyleClass().add("detail-candidate-name");

        Label position = new Label(app.jobTitle() != null ? "Applied for: " + app.jobTitle() : "Application");
        position.getStyleClass().add("detail-candidate-position");

        nameBox.getChildren().addAll(name, position);

        Label statusBadge = new Label(app.currentStatus() != null ? app.currentStatus() : "NEW");
        statusBadge.getStyleClass().add("detail-rating-badge");

        header.getChildren().addAll(nameBox, statusBadge);

        // Separator
        Separator sep = new Separator();
        sep.getStyleClass().add("detail-separator");

        // Contact info
        HBox contactInfo = new HBox(30);

        VBox emailBox = new VBox(5);
        Label emailLabel = new Label("📧 Email");
        emailLabel.getStyleClass().add("detail-label");
        Label emailValue = new Label(app.candidateEmail() != null ? app.candidateEmail() : "N/A");
        emailValue.getStyleClass().add("detail-value");
        emailBox.getChildren().addAll(emailLabel, emailValue);

        VBox phoneBox = new VBox(5);
        Label phoneLabel = new Label("📞 Phone");
        phoneLabel.getStyleClass().add("detail-label");
        Label phoneValue = new Label(app.phone() != null ? app.phone() : "N/A");
        phoneValue.getStyleClass().add("detail-value");
        phoneBox.getChildren().addAll(phoneLabel, phoneValue);

        VBox dateBox = new VBox(5);
        Label dateLabel = new Label("📅 Applied On");
        dateLabel.getStyleClass().add("detail-label");
        Label dateValue = new Label(app.appliedAt() != null ?
                                    app.appliedAt().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A");
        dateValue.getStyleClass().add("detail-value");
        dateBox.getChildren().addAll(dateLabel, dateValue);

        contactInfo.getChildren().addAll(emailBox, phoneBox, dateBox);

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
        confirm.setTitle("Accepter le Candidat");
        confirm.setHeaderText("Accepter ce candidat?");
        confirm.setContentText("Cela changera le statut de la candidature à 'Accepté'.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Update application status
                showAlert("Succès", "Candidat accepté avec succès!", Alert.AlertType.INFORMATION);
                loadApplications();
            }
        });
    }

    private void handleScheduleInterview(ApplicationService.ApplicationRow app) {
        showInterviewScheduleDialog(app);
    }

    private void handleRejectCandidate(ApplicationService.ApplicationRow app) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Rejeter le Candidat");
        confirm.setHeaderText("Rejeter ce candidat?");
        confirm.setContentText("Cette action ne peut pas être annulée.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showAlert("Succès", "Candidat rejeté.", Alert.AlertType.INFORMATION);
                loadApplications();
            }
        });
    }

    private void showInterviewScheduleDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Planifier un Entretien");
        dialog.setHeaderText("Planifier un entretien pour la candidature #" + app.id());

        // Modern styled content
        VBox content = new VBox(18);
        content.setPadding(new Insets(25));
        content.setStyle("-fx-background-color: white;");

        // Date picker with label
        VBox dateBox = new VBox(8);
        Label dateLabel = new Label("Date de l'entretien *");
        dateLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(java.time.LocalDate.now().plusDays(7));
        datePicker.setStyle("-fx-pref-width: 350px;");
        dateBox.getChildren().addAll(dateLabel, datePicker);

        // Time field with label
        VBox timeBox = new VBox(8);
        Label timeLabel = new Label("Heure (HH:mm) *");
        timeLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField timeField = new TextField("14:00");
        timeField.setPromptText("ex: 14:00");
        timeField.setStyle("-fx-pref-width: 350px;");
        timeBox.getChildren().addAll(timeLabel, timeField);

        // Duration with label
        VBox durationBox = new VBox(8);
        Label durationLabel = new Label("Durée (minutes) *");
        durationLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField durationField = new TextField("60");
        durationField.setPromptText("ex: 60");
        durationField.setStyle("-fx-pref-width: 350px;");
        durationBox.getChildren().addAll(durationLabel, durationField);

        // Mode selection with label
        VBox modeBox = new VBox(8);
        Label modeLabel = new Label("Mode d'entretien *");
        modeLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("ONLINE", "ON_SITE");
        modeCombo.setValue("ONLINE");
        modeCombo.setStyle("-fx-pref-width: 350px;");
        modeBox.getChildren().addAll(modeLabel, modeCombo);

        // Meeting Link (for ONLINE)
        VBox linkBox = new VBox(8);
        Label linkLabel = new Label("Lien de réunion (généré automatiquement pour ONLINE)");
        linkLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        HBox linkInputBox = new HBox(10);
        linkInputBox.setAlignment(Pos.CENTER_LEFT);
        TextField linkField = new TextField();
        linkField.setPromptText("Cliquez sur Générer...");
        linkField.setEditable(false);
        linkField.setStyle("-fx-pref-width: 230px; -fx-background-color: #f8f9fa;");

        Button generateLinkBtn = new Button("Générer le lien");
        generateLinkBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 14;");

        // "Open" hyperlink — only visible after generation
        Hyperlink testLink = new Hyperlink("Tester");
        testLink.setStyle("-fx-text-fill: #5BA3F5; -fx-font-size: 12px; -fx-border-color: transparent; -fx-padding: 0;");
        testLink.setVisible(false);
        testLink.setManaged(false);
        testLink.setOnAction(ev -> {
            String url = linkField.getText().trim();
            if (!url.isEmpty()) {
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception ex) {
                    showAlert("Lien de réunion", "Copiez ce lien dans votre navigateur:\n" + url, Alert.AlertType.INFORMATION);
                }
            }
        });

        generateLinkBtn.setOnAction(e -> {
            if (datePicker.getValue() != null && !timeField.getText().trim().isEmpty()) {
                try {
                    LocalDateTime scheduledAt = LocalDateTime.of(
                        datePicker.getValue(),
                        java.time.LocalTime.parse(timeField.getText())
                    );
                    int dur = 60;
                    try { dur = Integer.parseInt(durationField.getText().trim()); } catch (NumberFormatException ignored) {}
                    // Use app.id as the interview identifier for the room name
                    String meetingLink = MeetingService.generateMeetingLink(app.id(), scheduledAt, dur);
                    linkField.setText(meetingLink);
                    linkField.setStyle("-fx-pref-width: 230px; -fx-background-color: #d4edda;");
                    testLink.setVisible(true);
                    testLink.setManaged(true);
                } catch (Exception ex) {
                    showAlert("Erreur", "Veuillez d'abord sélectionner une date et heure valides.", Alert.AlertType.WARNING);
                }
            } else {
                showAlert("Erreur", "Veuillez d'abord sélectionner une date et heure.", Alert.AlertType.WARNING);
            }
        });

        Label autoGenNote = new Label("Le lien sera généré automatiquement si vous ne cliquez pas sur Générer");
        autoGenNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
        autoGenNote.setWrapText(true);

        linkInputBox.getChildren().addAll(linkField, generateLinkBtn, testLink);
        linkBox.getChildren().addAll(linkLabel, linkInputBox, autoGenNote);

        // Location (for ON_SITE)
        VBox locationBox = new VBox(8);
        Label locationLabel = new Label("Lieu (pour entretiens ON_SITE)");
        locationLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextField locationField = new TextField();
        locationField.setPromptText("ex: Bâtiment A, Salle 301");
        locationField.setStyle("-fx-pref-width: 350px;");
        locationBox.getChildren().addAll(locationLabel, locationField);

        // Notes section
        VBox notesBox = new VBox(8);
        Label notesLabel = new Label("Notes supplémentaires (Optionnel)");
        notesLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Entrez des notes supplémentaires pour l'entretien...");
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
                        showAlert("Erreur de Validation", "Veuillez sélectionner une date.", Alert.AlertType.WARNING);
                        return;
                    }

                    LocalDateTime scheduledAt = LocalDateTime.of(
                        datePicker.getValue(),
                        java.time.LocalTime.parse(timeField.getText())
                    );
                    int duration = Integer.parseInt(durationField.getText());
                    String mode = modeCombo.getValue();

                    // Validate required fields based on mode
                    if ("ONLINE".equals(mode)) {
                        // Auto-generate meeting link if not already set
                        if (linkField.getText() == null || linkField.getText().trim().isEmpty()) {
                            int autoDuration = duration > 0 ? duration : 60;
                            String autoGeneratedLink = MeetingService.generateMeetingLink(app.id(), scheduledAt, autoDuration);
                            linkField.setText(autoGeneratedLink);
                        }
                    }

                    if ("ON_SITE".equals(mode) && (locationField.getText() == null || locationField.getText().trim().isEmpty())) {
                        showAlert("Erreur de Validation", "Le lieu est requis pour les entretiens ON_SITE.", Alert.AlertType.WARNING);
                        return;
                    }

                    // Create interview linked to application
                    Models.Interview interview = new Models.Interview(
                        app.id(),
                        Utils.UserContext.getRecruiterId(), // Use actual recruiter ID from context
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

                    // Email reminder will be sent automatically by InterviewReminderScheduler
                    // (24 hours before the interview)

                    showAlert("Succès", "Entretien planifié avec succès! Un rappel par email sera envoyé 24h avant.", Alert.AlertType.INFORMATION);
                    loadApplications();
                } catch (NumberFormatException e) {
                    showAlert("Erreur de Validation", "La durée doit être un nombre valide.", Alert.AlertType.WARNING);
                } catch (Exception e) {
                    showAlert("Erreur", "Échec de la planification de l'entretien: " + e.getMessage(), Alert.AlertType.ERROR);
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
            showAlert("Aucune Sélection", "Veuillez d'abord sélectionner une candidature.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    private void handleReject() {
        if (selectedApplication != null) {
            handleRejectCandidate(selectedApplication);
        } else {
            showAlert("Aucune Sélection", "Veuillez d'abord sélectionner une candidature.", Alert.AlertType.WARNING);
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
