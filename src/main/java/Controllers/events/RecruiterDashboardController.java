package Controllers.events;

import Models.events.*;
import Services.events.*;
import Utils.SchemaFixer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.Node;
import javafx.geometry.Pos;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class RecruiterDashboardController implements Initializable {

    @FXML
    private HBox eventsHBox;

    // Attendees Table
    @FXML
    private TableView<EventRegistration> attendeesTable;
    @FXML
    private TableColumn<EventRegistration, String> eventTitleCol;
    @FXML
    private TableColumn<EventRegistration, String> candLastNameCol;
    @FXML
    private TableColumn<EventRegistration, String> candFirstNameCol;
    @FXML
    private TableColumn<EventRegistration, String> candEmailCol;
    @FXML
    private TableColumn<EventRegistration, String> statusCol;
    @FXML
    private TableColumn<EventRegistration, LocalDateTime> regDateCol;

    @FXML
    private ComboBox<AttendanceStatusEnum> registrationStatusCombo;

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private ComboBox<String> typeCombo;
    @FXML private TextField locationField;
    @FXML private DatePicker dateField;
    @FXML private TextField capacityField;
    @FXML private TextField meetLinkField;
    @FXML private Label titleErrorLabel;
    @FXML private Label typeErrorLabel;
    @FXML private Label locationErrorLabel;
    @FXML private Label dateErrorLabel;
    @FXML private Label capacityErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label meetLinkErrorLabel;
    
    // Search fields
    @FXML private TextField recruiterSearchField;
    @FXML private ComboBox<String> recruiterTypeFilter;
    
    // Statistics Labels
    @FXML private Label totalStatsLabel;
    @FXML private Label confirmedStatsLabel;
    @FXML private Label pendingStatsLabel;
    @FXML private Label cancelledStatsLabel;

    @FXML
    private VBox eventsView;
    @FXML
    private VBox interviewsView;
    @FXML
    private Button eventsBtn;
    @FXML
    private Button interviewsBtn;

    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;

    private Services.events.EventRegistrationService registrationService;
    private RecruitmentEventService eventService;
    private RecruiterService recruiterService;
    private UserService userService;
    private Recruiter currentRecruiter;

    public RecruiterDashboardController() {
        eventService = new RecruitmentEventService();
        recruiterService = new RecruiterService();
        registrationService = new Services.events.EventRegistrationService();
        userService = new UserService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SchemaFixer.main(null);

        // Ensure only eventsView is shown at startup
        if (eventsView != null)     { eventsView.setVisible(true);  eventsView.setManaged(true); }
        if (interviewsView != null) { interviewsView.setVisible(false); interviewsView.setManaged(false); }

        if (typeCombo != null) setupComboBoxes();
        if (attendeesTable != null) setupAttendeesTable();
        loadRecruiterData();
    }

    private void setupComboBoxes() {
        typeCombo.setItems(FXCollections.observableArrayList("Job_Faire", "WEBINAIRE", "Interview day"));
        registrationStatusCombo.setItems(FXCollections.observableArrayList(AttendanceStatusEnum.values()));
        
        // Show/hide meet link field when event type changes
        typeCombo.setOnAction(e -> {
            if (meetLinkField != null) {
                boolean isWebinaire = "WEBINAIRE".equals(typeCombo.getValue());
                meetLinkField.setVisible(isWebinaire);
                meetLinkField.setManaged(isWebinaire);
                if (meetLinkErrorLabel != null) {
                    meetLinkErrorLabel.setVisible(isWebinaire);
                    meetLinkErrorLabel.setManaged(isWebinaire);
                }
            }
        });
        
        if (recruiterTypeFilter != null) {
            recruiterTypeFilter.setItems(FXCollections.observableArrayList("Tous les types", "Job_Faire", "WEBINAIRE", "Interview day"));
            recruiterTypeFilter.setValue("Tous les types");
        }
    }

    private void setupAttendeesTable() {
        if (eventTitleCol != null) eventTitleCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEvent() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvent().getTitle());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        if (candLastNameCol != null)  candLastNameCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLastName()));
        if (candFirstNameCol != null) candFirstNameCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFirstName()));
        if (candEmailCol != null) candEmailCol.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        if (statusCol != null) statusCol.setCellValueFactory(cellData -> {
            AttendanceStatusEnum s = cellData.getValue().getAttendanceStatus();
            return new javafx.beans.property.SimpleStringProperty(s != null ? s.name() : "");
        });
        if (regDateCol != null) regDateCol.setCellValueFactory(new PropertyValueFactory<>("registeredAt"));

        if (attendeesTable != null) {
            attendeesTable.setRowFactory(tv -> new TableRow<EventRegistration>() {
                @Override
                protected void updateItem(EventRegistration item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("urgent-row", "overdue-row");
                    setTooltip(null);
                    setStyle("");

                    if (item == null || empty || item.getEvent() == null
                            || item.getAttendanceStatus() != AttendanceStatusEnum.PENDING
                            || item.getEvent().getEventDate() == null) {
                        applyCellStyle("", this);
                        return;
                    }

                    long hoursUntil = java.time.temporal.ChronoUnit.HOURS
                            .between(LocalDateTime.now(), item.getEvent().getEventDate());

                    if (hoursUntil >= 0 && hoursUntil <= 48) {
                        // Imminent - event within 48h still PENDING → yellow
                        String style = "-fx-background-color: #fff3cd;";
                        setStyle(style);
                        applyCellStyle(style, this);
                        setTooltip(new Tooltip("\u26a0\ufe0f \u00c9v\u00e9nement imminent (dans " + hoursUntil + "h) - Statut toujours en attente!"));
                    } else if (hoursUntil < 0) {
                        // Overdue - event passed still PENDING → red
                        String style = "-fx-background-color: #f8d7da;";
                        setStyle(style);
                        applyCellStyle(style, this);
                        setTooltip(new Tooltip("\u274c Date d\u00e9pass\u00e9e - Candidat toujours en attente!"));
                    } else {
                        applyCellStyle("", this);
                    }
                }
            });

            attendeesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && registrationStatusCombo != null) {
                    registrationStatusCombo.setValue(newVal.getAttendanceStatus());
                }
            });
        }
    }

    private void setupTable() {
        // No longer using TableView setup
    }

    private void refreshAllAttendees() {
        if (currentRecruiter == null) return;
        try {
            ObservableList<EventRegistration> list = FXCollections
                    .observableArrayList(registrationService.getByRecruiter(currentRecruiter.getId()));
            attendeesTable.setItems(list);
            updateStatistics();
            checkForUrgentRegistrations();
        } catch (SQLException e) {
            showAlert("Erreur Participants", e.getMessage());
        }
    }

    private void refreshAttendees(long eventId) {
        try {
            ObservableList<EventRegistration> list = FXCollections
                    .observableArrayList(registrationService.getByEvent(eventId));
            attendeesTable.setItems(list);
            updateStatistics();
            checkForUrgentRegistrations();
        } catch (SQLException e) {
            showAlert("Erreur Participants", e.getMessage());
        }
    }

    private void updateStatistics() {
        if (attendeesTable == null || attendeesTable.getItems() == null) return;
        ObservableList<EventRegistration> items = attendeesTable.getItems();
        
        long total = items.size();
        long confirmed = items.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.CONFIRMED).count();
        long pending = items.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.PENDING || r.getAttendanceStatus() == AttendanceStatusEnum.REGISTERED).count();
        long cancelled = items.stream().filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.CANCELLED).count();
        
        if (totalStatsLabel != null) totalStatsLabel.setText(String.valueOf(total));
        if (confirmedStatsLabel != null) confirmedStatsLabel.setText(String.valueOf(confirmed));
        if (pendingStatsLabel != null) pendingStatsLabel.setText(String.valueOf(pending));
        if (cancelledStatsLabel != null) cancelledStatsLabel.setText(String.valueOf(cancelled));
    }

    private boolean isUrgent(EventRegistration registration) {
        if (registration == null || registration.getEvent() == null) return false;
        
        boolean isPending = registration.getAttendanceStatus() == AttendanceStatusEnum.PENDING || 
                           registration.getAttendanceStatus() == AttendanceStatusEnum.REGISTERED;
        
        if (!isPending) return false;
        
        LocalDateTime eventDate = registration.getEvent().getEventDate();
        if (eventDate == null) return false;
        
        LocalDateTime now = LocalDateTime.now();
        // Urgent if event starts within 48 hours and hasn't passed yet
        return eventDate.isAfter(now) && eventDate.isBefore(now.plusHours(48));
    }

    private void checkForUrgentRegistrations() {
        if (attendeesTable == null || attendeesTable.getItems() == null) return;

        long imminentCount = attendeesTable.getItems().stream()
                .filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.PENDING
                          && r.getEvent() != null
                          && r.getEvent().getEventDate() != null)
                .filter(r -> {
                    long h = java.time.temporal.ChronoUnit.HOURS
                            .between(LocalDateTime.now(), r.getEvent().getEventDate());
                    return h >= 0 && h <= 48;
                })
                .count();

        long overdueCount = attendeesTable.getItems().stream()
                .filter(r -> r.getAttendanceStatus() == AttendanceStatusEnum.PENDING
                          && r.getEvent() != null
                          && r.getEvent().getEventDate() != null
                          && r.getEvent().getEventDate().isBefore(LocalDateTime.now()))
                .count();

        if (overdueCount > 0) {
            Services.joboffers.NotificationService.showWarning(
                "\u26a0\ufe0f Inscriptions en retard",
                overdueCount + " candidat(s) en attente pour des \u00e9v\u00e9nements d\u00e9j\u00e0 pass\u00e9s !");
        } else if (imminentCount > 0) {
            Services.joboffers.NotificationService.showWarning(
                "\u26a0\ufe0f \u00c9v\u00e9nements imminents",
                imminentCount + " inscription(s) PENDING pour des \u00e9v\u00e9nements dans moins de 48h !");
        }
    }

    private void loadRecruiterData() {
        try {
            // Use the actual logged-in recruiter ID from UserContext
            Long contextId = Utils.UserContext.getUserId();
            long lookupId = (contextId != null) ? contextId : 4L;
            currentRecruiter = recruiterService.getByUserId(lookupId);

            // 2. Si non trouvé, prendre le premier recruteur de la table
            if (currentRecruiter == null) {
                java.util.List<Recruiter> all = recruiterService.getAll();
                if (!all.isEmpty()) {
                    currentRecruiter = all.get(0);
                }
            }

            // 3. Si toujours rien, créer un profil par défaut pour le test
            if (currentRecruiter == null) {
                String testEmail = "recruteur.test@talentbridge.com";

                // Vérifier si l'utilisateur existe déjà par email
                User testUser = null;
                for (User u : userService.getAll()) {
                    if (testEmail.equals(u.getEmail())) {
                        testUser = u;
                        break;
                    }
                }

                if (testUser == null) {
                    testUser = new User();
                    testUser.setEmail(testEmail);
                    testUser.setPassword("password123");
                    testUser.setFirstName("Test");
                    testUser.setLastName("Recruteur");
                    testUser.setRole(RoleEnum.RECRUITER);
                    testUser.setActive(true);
                    userService.add(testUser);
                }

                currentRecruiter = new Recruiter();
                currentRecruiter.setId(testUser.getId());
                currentRecruiter.setCompanyName("Société de Test");
                currentRecruiter.setCompanyLocation("Tunis");
                currentRecruiter.setCompanyDescription("Société créée pour les tests directs.");

                try {
                    recruiterService.add(currentRecruiter);
                } catch (SQLException e) {
                    // Déjà existant ou erreur de schéma gérée par SchemaFixer
                    currentRecruiter = recruiterService.getByUserId(testUser.getId());
                }
            }

            if (currentRecruiter != null) {
                refreshTable();

                // Fetch User details for top bar (only present in standalone dashboard, not embedded view)
                try {
                    User user = userService.getById(currentRecruiter.getId());
                    if (user != null) {
                        if (userNameLabel != null) userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                        if (userRoleLabel != null) {
                            userRoleLabel.setText("RECRUTEUR");
                            userRoleLabel.getStyleClass().add("badge-recruiter");
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error loading user name: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur Initialisation", "Impossible de préparer le profil test : " + e.getMessage());
        }
    }

    @FXML
    private void refreshTable() {
        if (currentRecruiter == null)
            return;
        try {
            java.util.List<RecruitmentEvent> events = eventService.getByRecruiter(currentRecruiter.getId());
            eventsHBox.getChildren().clear();
            
            for (RecruitmentEvent event : events) {
                eventsHBox.getChildren().add(createEventCard(event));
            }
        } catch (SQLException e) {
            showAlert("Erreur Chargement", e.getMessage());
        }
    }

    private Node createEventCard(RecruitmentEvent event) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(200);
        card.setMinWidth(200);
        card.setMaxWidth(220);
        card.setMinHeight(220);

        // Check popularity upfront — used for border color
        boolean popularCheck = false;
        try {
            popularCheck = eventService.isEventPopular(event.getId());
        } catch (SQLException ex) {
            System.err.println("Error checking popularity: " + ex.getMessage());
        }
        final boolean isPopular = popularCheck;

        String borderColor = isPopular ? "#F97316" : "#E4EBF5";
        String borderWidth = isPopular ? "2" : "1";
        card.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 14;" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: " + borderWidth + ";" +
            "-fx-border-radius: 14;" +
            "-fx-padding: 16 16 16 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(100,150,220,0.10), 10, 0, 0, 3);" +
            "-fx-cursor: hand;"
        );

        // Title
        Label titleLabel = new Label(event.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(180);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");

        // Type badge
        String typeColor = switch (event.getEventType() != null ? event.getEventType() : "") {
            case "WEBINAIRE"     -> "#EBF3FF;-fx-text-fill:#1565C0";
            case "Interview day" -> "#F0FDF4;-fx-text-fill:#15803D";
            default              -> "#FFF7ED;-fx-text-fill:#C2410C"; // Job_Faire
        };
        Label typeLabel = new Label(event.getEventType());
        typeLabel.setStyle(
            "-fx-background-color: " + typeColor + ";" +
            "-fx-font-size: 11px; -fx-font-weight: 600;" +
            "-fx-background-radius: 20; -fx-padding: 3 10;"
        );

        // Description
        Label descriptionLabel = new Label(event.getDescription());
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxHeight(55);
        descriptionLabel.setMaxWidth(180);
        descriptionLabel.setEllipsisString("...");
        descriptionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        // Info section
        Label locationLabel = new Label("\uD83D\uDCCD " + event.getLocation());
        locationLabel.setMaxWidth(180);
        locationLabel.setEllipsisString("...");
        locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        String dateStr = event.getEventDate() != null
            ? event.getEventDate().toString().replace("T", " ") : "N/A";
        Label dateLabel = new Label("\uD83D\uDCC5 " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        Label capacityLabel = new Label("\uD83D\uDC65 " + event.getCapacity() + " places");
        capacityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

        VBox infoBox = new VBox(5, locationLabel, dateLabel, capacityLabel);
        infoBox.setStyle("-fx-padding: 8 0 0 0;");

        card.getChildren().addAll(titleLabel, typeLabel, descriptionLabel, infoBox);

        // Popular badge (added at top)
        if (isPopular) {
            Label popularBadge = new Label("\uD83D\uDD25 Populaire");
            popularBadge.setStyle(
                "-fx-background-color: #FFF3CD; -fx-text-fill: #F97316;" +
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-padding: 4 8; -fx-background-radius: 12;"
            );
            card.getChildren().add(0, popularBadge);
        }

        // Hover & selection styling
        String defaultStyle = card.getStyle();
        card.setOnMouseEntered(e -> card.setStyle(defaultStyle +
            "-fx-effect: dropshadow(gaussian, rgba(100,150,220,0.22), 16, 0, 0, 5);" +
            "-fx-border-color: " + (isPopular ? "#EA580C" : "#BBDEFB") + ";"
        ));
        card.setOnMouseExited(e -> {
            if (!card.getStyleClass().contains("event-card-selected")) {
                card.setStyle(defaultStyle);
            }
        });

        card.setOnMouseClicked(e -> {
            eventsHBox.getChildren().forEach(node -> {
                node.getStyleClass().remove("event-card-selected");
            });
            card.setStyle(defaultStyle +
                "-fx-border-color: #1565C0; -fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(21,101,192,0.25), 16, 0, 0, 5);"
            );
            card.getStyleClass().add("event-card-selected");

            populateForm(event);
            refreshAttendees(event.getId());
            selectedEvent = event;
        });
        
        return card;
    }

    private RecruitmentEvent selectedEvent;

    @FXML
    private void handleAdd() {
        if (currentRecruiter == null) {
            showAlert("Erreur", "Profil recruteur non chargé. Impossible d'ajouter.");
            return;
        }
        if (!validateForm())
            return;
        try {
            RecruitmentEvent event = new RecruitmentEvent();
            event.setRecruiterId(currentRecruiter.getId());
            updateEventFromForm(event);
            event.setCreatedAt(LocalDateTime.now());

            eventService.add(event);
            refreshTable();
            clearForm();
        } catch (SQLException e) {
            showAlert("Erreur Ajout", e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        if (selectedEvent == null) {
            showAlert("Avertissement", "Veuillez sélectionner un événement en cliquant sur sa carte.");
            return;
        }
        if (currentRecruiter == null) {
            showAlert("Erreur", "Profil recruteur non chargé.");
            return;
        }
        if (!validateForm())
            return;
        try {
            updateEventFromForm(selectedEvent);
            eventService.update(selectedEvent);
            refreshTable();
            clearForm();
            selectedEvent = null;
        } catch (SQLException e) {
            showAlert("Erreur Modification", e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        if (selectedEvent == null) {
            showAlert("Avertissement", "Veuillez sélectionner un événement en cliquant sur sa carte.");
            return;
        }
        try {
            eventService.delete(selectedEvent.getId());
            refreshTable();
            clearForm();
            selectedEvent = null;
        } catch (SQLException e) {
            showAlert("Erreur Suppression", e.getMessage());
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
        selectedEvent = null;
        eventsHBox.getChildren().forEach(node -> node.getStyleClass().remove("event-card-selected"));
    }

    @FXML
    private void handleLogout() {
        // Action désactivée car le login est skippé
        showAlert("Information", "Le logout est désactivé dans cette version simplifiée.");
    }

    @FXML
    public void goToCandidateView() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) eventsHBox.getScene().getWindow();
            javafx.scene.Parent root = javafx.fxml.FXMLLoader
                    .load(getClass().getResource("/GUI/candidate_dashboard.fxml"));
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            showAlert("Erreur Navigation", "Impossible de charger le dashboard candidat.");
        }
    }

    @FXML
    public void goToInterviews() {
        switchView(false);
        // Refresh attendees for the currently selected event if any, otherwise show all
        if (selectedEvent != null) {
            refreshAttendees(selectedEvent.getId());
        } else {
            refreshAllAttendees();
        }
    }

    @FXML
    public void goToEvents() {
        switchView(true);
        refreshTable();
    }

    @FXML
    private void handleRecruiterSearch() {
        String query = recruiterSearchField != null ? recruiterSearchField.getText().toLowerCase() : "";
        String typeFilter = recruiterTypeFilter != null ? recruiterTypeFilter.getValue() : "Tous les types";

        if (currentRecruiter == null) return;
        
        try {
            java.util.List<RecruitmentEvent> allEvents = eventService.getByRecruiter(currentRecruiter.getId());
            eventsHBox.getChildren().clear();

            for (RecruitmentEvent event : allEvents) {
                boolean matchesTitle = query.isEmpty() || event.getTitle().toLowerCase().contains(query);
                boolean matchesType = "Tous les types".equals(typeFilter) || event.getEventType().equals(typeFilter);

                if (matchesTitle && matchesType) {
                    eventsHBox.getChildren().add(createEventCard(event));
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur Chargement", e.getMessage());
        }
    }

    private void switchView(boolean isEvents) {
        if (eventsView != null) { eventsView.setVisible(isEvents); eventsView.setManaged(isEvents); }
        if (interviewsView != null) { interviewsView.setVisible(!isEvents); interviewsView.setManaged(!isEvents); }

        if (eventsBtn != null) {
            eventsBtn.getStyleClass().removeAll("sidebar-button-active", "sidebar-button");
            eventsBtn.getStyleClass().add(isEvents ? "sidebar-button-active" : "sidebar-button");
        }
        if (interviewsBtn != null) {
            interviewsBtn.getStyleClass().removeAll("sidebar-button-active", "sidebar-button");
            interviewsBtn.getStyleClass().add(isEvents ? "sidebar-button" : "sidebar-button-active");
        }
    }

    @FXML
    private void handleUpdateRegistrationStatus() {
        EventRegistration selected = attendeesTable.getSelectionModel().getSelectedItem();
        AttendanceStatusEnum newStatus = registrationStatusCombo.getValue();

        if (selected == null || newStatus == null) {
            showAlert("Avertissement", "Veuillez sélectionner une inscription et un nouveau statut.");
            return;
        }

        try {
            selected.setAttendanceStatus(newStatus);
            registrationService.update(selected);
            showAlert("Succès", "Le statut de l'inscription a été mis à jour.");

            // Send status notification email in background
            if (selected.getEmail() != null && !selected.getEmail().isBlank()) {
                String email = selected.getEmail();
                String name = selected.getCandidateName() != null ? selected.getCandidateName() : "Candidat";
                
                // Always look up the real event to ensure we have data, even if selectedEvent is null
                RecruitmentEvent realEvent = null;
                try {
                    realEvent = eventService.getById(selected.getEventId());
                } catch (SQLException ignored) {}
                
                final RecruitmentEvent evToUse = realEvent != null ? realEvent : selectedEvent;

                String eventTitle = evToUse != null ? evToUse.getTitle() : "";
                String eventDate = evToUse != null && evToUse.getEventDate() != null
                        ? evToUse.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                String eventLocation = evToUse != null && evToUse.getLocation() != null
                        ? evToUse.getLocation() : "";
                String eventType = evToUse != null && evToUse.getEventType() != null
                        ? evToUse.getEventType() : "";
                String meetLink = evToUse != null && evToUse.getMeetLink() != null
                        ? evToUse.getMeetLink() : "";
                new Thread(() -> Services.EmailService.sendEventStatusNotification(
                        email, name, eventTitle, eventDate, eventLocation, newStatus.name(), null, eventType, meetLink),
                        "event-status-email").start();
            }

            if (selectedEvent != null) refreshAttendees(selectedEvent.getId());
            else refreshAllAttendees();
            updateStatistics();
        } catch (SQLException e) {
            showAlert("Erreur Mise à jour", e.getMessage());
        }
    }

    @FXML
    private void handleSendEmail() {
        EventRegistration selected = attendeesTable != null
                ? attendeesTable.getSelectionModel().getSelectedItem() : null;
        if (selected == null) {
            showAlert("Avertissement", "Sélectionnez un candidat dans la liste.");
            return;
        }
        if (selected.getEmail() == null || selected.getEmail().isBlank()) {
            showAlert("Erreur", "Aucun email disponible pour ce candidat.");
            return;
        }
        String name = selected.getCandidateName() != null ? selected.getCandidateName() : "Candidat";
        String eventTitle = selectedEvent != null ? selectedEvent.getTitle() : "";
        String eventDate = selectedEvent != null && selectedEvent.getEventDate() != null
                ? selectedEvent.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
        String eventLocation = selectedEvent != null && selectedEvent.getLocation() != null
                ? selectedEvent.getLocation() : "";
        String eventType = selectedEvent != null && selectedEvent.getEventType() != null
                ? selectedEvent.getEventType() : "";
        new Thread(() -> {
            boolean sent = Services.EmailService.sendEventRegistrationConfirmation(
                    selected.getEmail(), name, eventTitle, eventDate, eventLocation, eventType);
            javafx.application.Platform.runLater(() ->
                showAlert(sent ? "Email envoyé" : "Erreur",
                          sent ? "Email de confirmation envoyé à " + selected.getEmail()
                               : "Échec de l'envoi de l'email."));
        }, "event-manual-email").start();
    }

    @FXML
    public void goToJobOffers() {
        showAlert("Navigation", "Navigation vers les Offres d'emploi (à implémenter).");
    }

    @FXML
    public void goToSettings() {
        showAlert("Navigation", "Navigation vers les Paramètres (à implémenter).");
    }

    private void updateEventFromForm(RecruitmentEvent event) {
        event.setTitle(titleField.getText());
        event.setDescription(descriptionField.getText());
        event.setEventType(typeCombo.getValue());
        event.setLocation(locationField.getText());
        if (dateField.getValue() != null) {
            event.setEventDate(dateField.getValue().atStartOfDay());
        }
        try {
            event.setCapacity(Integer.parseInt(capacityField.getText()));
        } catch (NumberFormatException e) {
            event.setCapacity(0);
        }
        if (meetLinkField != null) event.setMeetLink(meetLinkField.getText());
    }

    private void populateForm(RecruitmentEvent event) {
        titleField.setText(event.getTitle());
        descriptionField.setText(event.getDescription());
        typeCombo.setValue(event.getEventType());
        locationField.setText(event.getLocation());
        capacityField.setText(String.valueOf(event.getCapacity()));
        if (event.getEventDate() != null) dateField.setValue(event.getEventDate().toLocalDate());
        if (meetLinkField != null) {
            boolean isWebinaire = "WEBINAIRE".equals(event.getEventType());
            meetLinkField.setVisible(isWebinaire);
            meetLinkField.setManaged(isWebinaire);
            meetLinkField.setText(event.getMeetLink() != null ? event.getMeetLink() : "");
            if (meetLinkErrorLabel != null) {
                meetLinkErrorLabel.setVisible(isWebinaire);
                meetLinkErrorLabel.setManaged(isWebinaire);
            }
        }
    }

    private void clearForm() {
        titleField.clear();
        descriptionField.clear();
        typeCombo.getSelectionModel().clearSelection();
        typeCombo.setValue(null);
        locationField.clear();
        capacityField.clear();
        dateField.setValue(null);
        if (meetLinkField != null) {
            meetLinkField.clear();
            meetLinkField.setVisible(false);
            meetLinkField.setManaged(false);
        }
        if (titleErrorLabel != null) titleErrorLabel.setText("");
        if (typeErrorLabel != null) typeErrorLabel.setText("");
        if (locationErrorLabel != null) locationErrorLabel.setText("");
        if (dateErrorLabel != null) dateErrorLabel.setText("");
        if (capacityErrorLabel != null) capacityErrorLabel.setText("");
        if (descriptionErrorLabel != null) descriptionErrorLabel.setText("");
        if (meetLinkErrorLabel != null) { meetLinkErrorLabel.setText(""); meetLinkErrorLabel.setVisible(false); meetLinkErrorLabel.setManaged(false); }
    }

    private boolean validateForm() {
        boolean hasError = false;

        // Clear previous error messages
        titleErrorLabel.setText("");
        typeErrorLabel.setText("");
        locationErrorLabel.setText("");
        dateErrorLabel.setText("");
        capacityErrorLabel.setText("");
        descriptionErrorLabel.setText("");
        if (meetLinkErrorLabel != null) meetLinkErrorLabel.setText("");

        if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
            titleErrorLabel.setText("Minimum 3 caractères requis.");
            hasError = true;
        }

        if (typeCombo.getValue() == null) {
            typeErrorLabel.setText("Veuillez sélectionner un type.");
            hasError = true;
        }

        if (locationField.getText() == null || locationField.getText().trim().isEmpty()) {
            locationErrorLabel.setText("Le lieu est obligatoire.");
            hasError = true;
        }

        if (dateField.getValue() == null) {
            dateErrorLabel.setText("La date est obligatoire.");
            hasError = true;
        } else if (dateField.getValue().isBefore(java.time.LocalDate.now())) {
            dateErrorLabel.setText("La date ne peut pas être passée.");
            hasError = true;
        }

        try {
            int capacity = Integer.parseInt(capacityField.getText());
            if (capacity <= 0) {
                capacityErrorLabel.setText("Doit être un nombre positif.");
                hasError = true;
            }
        } catch (NumberFormatException e) {
            capacityErrorLabel.setText("Nombre invalide.");
            hasError = true;
        }

        if (descriptionField.getText() == null || descriptionField.getText().trim().length() < 10) {
            descriptionErrorLabel.setText("Minimum 10 caractères requis.");
            hasError = true;
        }

        // Validate meet link when WEBINAIRE is selected
        if ("WEBINAIRE".equals(typeCombo.getValue())) {
            String link = meetLinkField != null ? meetLinkField.getText() : null;
            if (link == null || link.trim().isEmpty()) {
                if (meetLinkErrorLabel != null) meetLinkErrorLabel.setText("Le lien de réunion est obligatoire pour un webinaire.");
                hasError = true;
            }
        }

        return !hasError;
    }

    @FXML
    private void handleDateValidation() {
        if (dateField.getValue() != null && dateField.getValue().isBefore(java.time.LocalDate.now())) {
            if (dateErrorLabel != null) {
                dateErrorLabel.setText("La date ne peut pas être passée.");
            }
        } else {
            if (dateErrorLabel != null) {
                dateErrorLabel.setText("");
            }
        }
        // Show/hide meet link field based on type
        if (typeCombo != null && meetLinkField != null) {
            boolean isWebinaire = "WEBINAIRE".equals(typeCombo.getValue());
            meetLinkField.setVisible(isWebinaire);
            meetLinkField.setManaged(isWebinaire);
        }
    }

    @FXML
    private void handlePickLocation() {
        Controllers.joboffers.LocationPickerController.pickLocation((lat, lng, address) -> {
            javafx.application.Platform.runLater(() -> {
                locationField.setText(address);
            });
        });
    }

    @FXML
    private void handleGenerateDescriptionAI() {
        String title = titleField.getText();
        if (title == null || title.trim().isEmpty()) {
            showAlert("Information manquante", "Veuillez remplir le titre pour générer une description.");
            return;
        }

        String type = typeCombo.getValue();
        String location = locationField.getText();
        String generatedDescription = generateLocalEventDescription(title, type, location);
        descriptionField.setText(generatedDescription);
    }

    private String generateLocalEventDescription(String title, String type, String location) {
        String t = title.toLowerCase();
        String loc = (location != null && !location.trim().isEmpty()) ? location : null;
        String locationPhrase = loc != null ? " à " + loc : "";

        // Priority 1: Use the event type from the combo box
        if ("WEBINAIRE".equals(type)) {
            return "Rejoignez-nous pour notre webinaire interactif : " + title + ". "
                 + "Cet événement en ligne sera l'occasion d'échanger avec nos experts, d'explorer de nouvelles thématiques "
                 + "et de découvrir nos opportunités depuis le confort de votre domicile. "
                 + "Connectez-vous et participez à des discussions enrichissantes avec des professionnels du secteur.";
        }
        if ("Job_Faire".equals(type)) {
            return "Ne manquez pas notre prochain salon de l'emploi : " + title
                 + (loc != null ? ", qui se tiendra" + locationPhrase : "") + " ! "
                 + "C'est une opportunité unique de rencontrer nos équipes RH en personne, de déposer votre CV "
                 + "et de discuter des postes actuellement ouverts. Venez découvrir notre culture d'entreprise "
                 + "et saisir les meilleures opportunités de carrière !";
        }
        if ("Interview day".equals(type)) {
            return "Inscrivez-vous à notre journée d'entretiens : " + title
                 + (loc != null ? ", qui aura lieu" + locationPhrase : "") + ". "
                 + "Pendant cet événement, vous aurez la chance de passer des entretiens avec nos managers, "
                 + "de démontrer vos compétences techniques et comportementales, et peut-être de décrocher votre futur poste ! "
                 + "Préparez votre CV et venez avec votre meilleure motivation.";
        }

        // Priority 2: Fallback to title-based detection
        if (t.contains("webinaire") || t.contains("webinar") || t.contains("ligne")) {
            return "Rejoignez-nous pour notre webinaire interactif dédié à : " + title + ". "
                 + "Cet événement en ligne sera l'occasion d'échanger avec nos experts et de découvrir nos opportunités.";
        }
        if (t.contains("hackathon") || t.contains("challenge") || t.contains("concours")) {
            return "Prêt(e) à relever le défi ? Participez à notre : " + title
                 + (loc != null ? locationPhrase : "") + ". "
                 + "Venez démontrer votre talent en équipe, résoudre des problèmes complexes et innover avec nous. "
                 + "Des prix exclusifs et des opportunités d'embauche seront à la clé !";
        }
        if (t.contains("atelier") || t.contains("workshop") || t.contains("formation")) {
            return "Développez vos compétences lors de notre : " + title
                 + (loc != null ? locationPhrase : "") + ". "
                 + "Nos experts animeront des sessions pratiques pour vous former sur les dernières technologies "
                 + "et méthodes de travail. Une excellente occasion d'apprendre et de réseauter.";
        }

        return "Nous avons le plaisir de vous annoncer notre prochain événement : " + title
             + (loc != null ? locationPhrase : "") + ". "
             + "Ce sera une excellente occasion de rencontrer nos équipes, d'en apprendre davantage sur notre secteur "
             + "et de découvrir les différentes opportunités que nous offrons. Venez nombreux !";
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * JavaFX workaround: setStyle() on a TableRow is often overridden by the
     * table's own CSS. Applying the style to each TableCell inside the row
     * ensures the color actually shows up regardless of the stylesheet.
     */
    private void applyCellStyle(String style, TableRow<?> row) {
        for (javafx.scene.Node node : row.getChildrenUnmodifiable()) {
            if (node instanceof TableCell) {
                node.setStyle(style);
            }
        }
    }
}

