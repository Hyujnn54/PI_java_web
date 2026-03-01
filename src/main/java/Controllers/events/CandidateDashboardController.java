package Controllers.events;

import Models.events.EventCandidate;
import Models.events.EventRegistration;
import Models.events.RecruitmentEvent;
import Models.events.EventUser;
import Models.events.RoleEnum;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.Node;
import javafx.geometry.Pos;
import Services.events.CandidateService;
import Services.events.EventRegistrationService;
import Services.events.RecruitmentEventService;
import Services.events.UserService;
import Utils.SchemaFixer;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ResourceBundle;

public class CandidateDashboardController implements Initializable {

    @FXML
    private VBox eventsView;
    @FXML
    private VBox registrationsView;
    @FXML
    private HBox eventsHBox;

    @FXML
    private TableView<EventRegistration> myRegistrationsTable;
    @FXML
    private TableColumn<EventRegistration, String> regEventTitleCol;
    @FXML
    private TableColumn<EventRegistration, LocalDateTime> regDateCol;
    @FXML
    private TableColumn<EventRegistration, String> regStatusCol;

    @FXML
    private ComboBox<String> statusFilterCombo;

    @FXML
    private Button eventsBtn;
    @FXML
    private Button interviewsBtn;

    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;

    private RecruitmentEventService eventService;
    private EventRegistrationService registrationService;
    private CandidateService candidateService;
    private UserService userService;
    private EventCandidate currentCandidate;
    private java.util.List<String> recommendedTypes = new java.util.ArrayList<>();

    public CandidateDashboardController() {
        eventService = new RecruitmentEventService();
        registrationService = new EventRegistrationService();
        candidateService = new CandidateService();
        userService = new UserService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SchemaFixer.main(null);

        // Ensure only eventsView is shown at startup
        if (eventsView != null)         { eventsView.setVisible(true);  eventsView.setManaged(true); }
        if (registrationsView != null)  { registrationsView.setVisible(false); registrationsView.setManaged(false); }

        if (myRegistrationsTable != null) setupRegistrationsTable();
        loadCandidateData();
    }

    private void setupEventsTable() {
        // No longer using TableView setup
    }

    private void setupRegistrationsTable() {
        if (regEventTitleCol != null) regEventTitleCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEvent() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvent().getTitle());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        if (regDateCol != null) regDateCol.setCellValueFactory(new PropertyValueFactory<>("registeredAt"));
        if (regStatusCol != null) regStatusCol.setCellValueFactory(new PropertyValueFactory<>("attendanceStatus"));
        
        if (statusFilterCombo != null) {
            ObservableList<String> filterOptions = FXCollections.observableArrayList(
                "Tous les statuts",
                Models.events.AttendanceStatusEnum.PENDING.name(),
                Models.events.AttendanceStatusEnum.REGISTERED.name(),
                Models.events.AttendanceStatusEnum.CONFIRMED.name(),
                Models.events.AttendanceStatusEnum.ATTENDED.name(),
                Models.events.AttendanceStatusEnum.ABSENT.name(),
                Models.events.AttendanceStatusEnum.CANCELLED.name()
            );
            statusFilterCombo.setItems(filterOptions);
            statusFilterCombo.setValue("Tous les statuts");
            statusFilterCombo.setOnAction(e -> refreshRegistrations());
        }
    }

    private void loadCandidateData() {
        try {
            // Use the actual logged-in user ID from UserContext
            Long contextId = Utils.UserContext.getUserId();
            long lookupId = (contextId != null) ? contextId : 5L;
            currentCandidate = candidateService.getByUserId(lookupId);
            if (currentCandidate == null) {
                java.util.List<EventCandidate> all = candidateService.getAll();
                if (!all.isEmpty()) {
                    currentCandidate = all.get(0);
                }
            }
            if (currentCandidate == null) {
                String testEmail = "candidat.test@talentbridge.com";
                EventUser testUser = null;
                for (EventUser u : userService.getAll()) {
                    if (testEmail.equals(u.getEmail())) {
                        testUser = u;
                        break;
                    }
                }
                if (testUser != null) {
                    testUser.setFirstName("Rayen");
                    testUser.setLastName("EventCandidate");
                    userService.update(testUser);
                }
                if (testUser == null) {
                    testUser = new EventUser();
                    testUser.setEmail(testEmail);
                    testUser.setPassword("password123");
                    testUser.setFirstName("Rayen");
                    testUser.setLastName("EventCandidate");
                    testUser.setRole(RoleEnum.CANDIDATE);
                    testUser.setActive(true);
                    userService.add(testUser);
                }
                currentCandidate = new EventCandidate();
                currentCandidate.setId(testUser.getId());
                currentCandidate.setLocation("Tunis");
                currentCandidate.setEducationLevel("Master");
                currentCandidate.setExperienceYears(2);
                candidateService.add(currentCandidate);
            }
            if (currentCandidate != null) {
                refreshEvents();
                refreshRegistrations();

                // Fetch EventUser details for top bar (only present in standalone dashboard, not embedded view)
                try {
                    EventUser user = userService.getById(currentCandidate.getId());
                    if (user != null) {
                        if (userNameLabel != null) userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                        if (userRoleLabel != null) {
                            userRoleLabel.setText("CANDIDAT");
                            userRoleLabel.getStyleClass().add("badge-candidate");
                        }
                    }
                } catch (SQLException e) {
                    System.err.println("Error loading user name: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            showAlert("Erreur Initialisation", "Impossible de préparer le profil candidat test : " + e.getMessage());
        }
    }

    private void refreshEvents() {
        try {
            java.util.List<RecruitmentEvent> events = eventService.getAll();
            eventsHBox.getChildren().clear();

            // Load recommended types for the current candidate
            recommendedTypes.clear();
            if (currentCandidate != null) {
                try {
                    recommendedTypes = registrationService.getEventTypesForCandidate(currentCandidate.getId());
                } catch (SQLException ex) {
                    System.err.println("Could not load recommended types: " + ex.getMessage());
                }
            }

            // Sort: recommended events first
            if (!recommendedTypes.isEmpty()) {
                events.sort((a, b) -> {
                    boolean aRec = recommendedTypes.contains(a.getEventType());
                    boolean bRec = recommendedTypes.contains(b.getEventType());
                    if (aRec && !bRec) return -1;
                    if (!aRec && bRec) return 1;
                    return 0;
                });
            }

            for (RecruitmentEvent event : events) {
                eventsHBox.getChildren().add(createEventCard(event));
            }
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private Node createEventCard(RecruitmentEvent event) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(200);
        card.setMinWidth(200);
        card.setMaxWidth(220);
        card.setMinHeight(220);

        // Check popularity for orange border
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
            "-fx-padding: 16;" +
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
            default              -> "#FFF7ED;-fx-text-fill:#C2410C";
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

        // Recommendation badge — match candidate's past registrations
        boolean isRecommended = recommendedTypes.contains(event.getEventType());
        if (isRecommended) {
            Label recBadge = new Label("\u2B50 Recommandé pour vous");
            recBadge.setStyle(
                "-fx-background-color: #EBF3FF; -fx-text-fill: #1565C0;" +
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-padding: 4 8; -fx-background-radius: 12;"
            );
            card.getChildren().add(0, recBadge);
        }

        // Popular badge
        if (isPopular) {
            Label popularBadge = new Label("\uD83D\uDD25 Populaire");
            popularBadge.setStyle(
                "-fx-background-color: #FFF3CD; -fx-text-fill: #F97316;" +
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-padding: 4 8; -fx-background-radius: 12;"
            );
            card.getChildren().add(isRecommended ? 1 : 0, popularBadge);
        }

        // Hover & selection
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
            eventsHBox.getChildren().forEach(node -> node.getStyleClass().remove("event-card-selected"));
            card.setStyle(defaultStyle +
                "-fx-border-color: #1565C0; -fx-border-width: 2;" +
                "-fx-effect: dropshadow(gaussian, rgba(21,101,192,0.25), 16, 0, 0, 5);"
            );
            card.getStyleClass().add("event-card-selected");
            selectedEvent = event;
        });

        return card;
    }

    private RecruitmentEvent selectedEvent;

    private void refreshRegistrations() {
        if (currentCandidate == null || myRegistrationsTable == null)
            return;
        try {
            java.util.List<EventRegistration> allRegs = registrationService.getByCandidate(currentCandidate.getId());
            
            // Apply filter
            String selectedFilter = (statusFilterCombo != null) ? statusFilterCombo.getValue() : "Tous les statuts";
            if (selectedFilter != null && !"Tous les statuts".equals(selectedFilter)) {
                allRegs = allRegs.stream()
                        .filter(reg -> reg.getAttendanceStatus() != null && 
                                       reg.getAttendanceStatus().name().equals(selectedFilter))
                        .collect(java.util.stream.Collectors.toList());
            }
            
            ObservableList<EventRegistration> list = FXCollections.observableArrayList(allRegs);
            myRegistrationsTable.setItems(list);
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleApply() {
        if (selectedEvent == null) {
            showAlert("Info", "Veuillez sélectionner un événement en cliquant sur sa carte.");
            return;
        }
        openRegistrationModal(selectedEvent);
    }

    private void openRegistrationModal(RecruitmentEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/GUI/registration_modal.fxml"));
            javafx.scene.Parent root = loader.load();

            RegistrationModalController controller = loader.getController();
            controller.setEvent(event);
            controller.setCandidate(currentCandidate);
            controller.setParentController(this);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Inscription : " + event.getTitle());
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

        } catch (java.io.IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir le formulaire d'inscription : " + e.getMessage());
        }
    }

    public void onRegistrationSuccess() {
        refreshRegistrations();
    }

    @FXML
    private void handleCancelRegistration() {
        EventRegistration selected = myRegistrationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Info", "Sélectionnez une inscription.");
            return;
        }
        try {
            registrationService.delete(selected.getId());
            showAlert("Succès", "Inscription annulée.");
            refreshRegistrations();
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    public void goToRecruiterView() {
        try {
            javafx.stage.Stage stage = (javafx.stage.Stage) eventsHBox.getScene().getWindow();
            javafx.scene.Parent root = javafx.fxml.FXMLLoader
                    .load(getClass().getResource("/GUI/recruiter_dashboard.fxml"));
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            showAlert("Erreur Navigation", "Impossible de charger le dashboard recruteur.");
        }
    }

    @FXML
    public void goToInterviews() {
        switchTab(1);
        refreshRegistrations();
    }

    @FXML
    public void goToEvents() {
        switchTab(0);
        refreshEvents();
    }

    private void switchTab(int index) {
        if (eventsView == null || registrationsView == null)
            return;

        eventsView.setVisible(index == 0);
        eventsView.setManaged(index == 0);
        registrationsView.setVisible(index == 1);
        registrationsView.setManaged(index == 1);

        if (eventsBtn == null || interviewsBtn == null)
            return;
        eventsBtn.getStyleClass().remove("sidebar-button-active");
        eventsBtn.getStyleClass().remove("sidebar-button");
        interviewsBtn.getStyleClass().remove("sidebar-button-active");
        interviewsBtn.getStyleClass().remove("sidebar-button");

        if (index == 0) {
            eventsBtn.getStyleClass().add("sidebar-button-active");
            interviewsBtn.getStyleClass().add("sidebar-button");
        } else {
            eventsBtn.getStyleClass().add("sidebar-button");
            interviewsBtn.getStyleClass().add("sidebar-button-active");
        }
    }

    @FXML
    public void goToJobOffers() {
        showAlert("Navigation", "Navigation vers les Offres d'emploi (à implémenter).");
    }

    @FXML
    public void goToSettings() {
        showAlert("Navigation", "Navigation vers les Paramètres (à implémenter).");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
