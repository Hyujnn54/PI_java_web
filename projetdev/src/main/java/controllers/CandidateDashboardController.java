package controllers;

import entities.Candidate;
import entities.EventRegistration;
import entities.RecruitmentEvent;
import entities.User;
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
import services.CandidateService;
import services.EventRegistrationService;
import services.RecruitmentEventService;
import services.UserService;
import entities.RoleEnum;
import utils.SchemaFixer;

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
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilterCombo;
    @FXML
    private VBox recommendationBox;
    @FXML
    private HBox recommendedHBox;

    @FXML
    private TableView<EventRegistration> myRegistrationsTable;
    @FXML
    private TableColumn<EventRegistration, String> regEventTitleCol;
    @FXML
    private TableColumn<EventRegistration, LocalDateTime> regDateCol;
    @FXML
    private TableColumn<EventRegistration, String> regStatusCol;

    @FXML
    private Button eventsBtn;
    @FXML
    private Button interviewsBtn;

    @FXML
    private Label userNameLabel;
    @FXML
    private Label userRoleLabel;

    @FXML
    private Label totalRegLabel;
    @FXML
    private Label confirmedRegLabel;
    @FXML
    private Label pendingRegLabel;
    @FXML
    private Label nextEventLabel;

    private RecruitmentEventService eventService;
    private EventRegistrationService registrationService;
    private CandidateService candidateService;
    private UserService userService;
    private Candidate currentCandidate;
    private java.util.List<RecruitmentEvent> allEvents = new java.util.ArrayList<>();

    public CandidateDashboardController() {
        eventService = new RecruitmentEventService();
        registrationService = new EventRegistrationService();
        candidateService = new CandidateService();
        userService = new UserService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SchemaFixer.main(null);
        setupEventsTable();
        setupRegistrationsTable();
        loadCandidateData();
        setupSearch();
    }

    private void setupSearch() {
        typeFilterCombo.setItems(FXCollections.observableArrayList("Tous", "Job_Faire", "WEBINAIRE", "Interview day"));
        typeFilterCombo.setValue("Tous");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterEvents();
        });
        typeFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            filterEvents();
        });
    }

    private void filterEvents() {
        String searchText = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String selectedType = typeFilterCombo.getValue();
        
        eventsHBox.getChildren().clear();
        for (RecruitmentEvent event : allEvents) {
            boolean matchesTitle = searchText.isEmpty() || event.getTitle().toLowerCase().contains(searchText);
            boolean matchesType = selectedType == null || selectedType.equals("Tous") || event.getEventType().equals(selectedType);
            
            if (matchesTitle && matchesType) {
                eventsHBox.getChildren().add(createEventCard(event));
            }
        }
    }

    private void setupEventsTable() {
        // No longer using TableView setup
    }

    private void setupRegistrationsTable() {
        regEventTitleCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEvent() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvent().getTitle());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        regDateCol.setCellValueFactory(new PropertyValueFactory<>("registeredAt"));
        regStatusCol.setCellValueFactory(new PropertyValueFactory<>("attendanceStatus"));

        // Row factory for status-based coloring
        myRegistrationsTable.setRowFactory(tv -> new TableRow<EventRegistration>() {
            @Override
            protected void updateItem(EventRegistration item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    getStyleClass().removeAll("confirmed-row", "cancelled-row");
                } else {
                    String status = item.getAttendanceStatus() != null ? item.getAttendanceStatus().toString() : "";
                    getStyleClass().removeAll("confirmed-row", "cancelled-row");
                    if ("CONFIRMED".equals(status)) {
                        getStyleClass().add("confirmed-row");
                    } else if ("CANCELLED".equals(status)) {
                        getStyleClass().add("cancelled-row");
                    }
                }
            }
        });
    }

    private void loadCandidateData() {
        try {
            currentCandidate = candidateService.getByUserId(5);
            if (currentCandidate == null) {
                java.util.List<entities.Candidate> all = candidateService.getAll();
                if (!all.isEmpty()) {
                    currentCandidate = all.get(0);
                }
            }
            if (currentCandidate == null) {
                UserService userService = new UserService();
                String testEmail = "candidat.test@talentbridge.com";
                User testUser = null;
                for (User u : userService.getAll()) {
                    if (testEmail.equals(u.getEmail())) {
                        testUser = u;
                        break;
                    }
                }
                if (testUser != null) {
                    testUser.setFirstName("Rayen");
                    testUser.setLastName("Candidate");
                    userService.update(testUser);
                }
                if (testUser == null) {
                    testUser = new User();
                    testUser.setEmail(testEmail);
                    testUser.setPassword("password123");
                    testUser.setFirstName("Rayen");
                    testUser.setLastName("Candidate");
                    testUser.setRole(RoleEnum.CANDIDATE);
                    testUser.setActive(true);
                    userService.add(testUser);
                }
                currentCandidate = new Candidate();
                currentCandidate.setId(testUser.getId());
                currentCandidate.setLocation("Tunis");
                currentCandidate.setEducationLevel("Master");
                currentCandidate.setExperienceYears(2);
                candidateService.add(currentCandidate);
                
                // Store in SessionManager for other controllers
                utils.SessionManager.setCurrentUser(testUser);
            }
            if (currentCandidate != null) {
                // Ensure SessionManager is populated even if we found existing candidate
                User loggedUser = userService.getById(currentCandidate.getId());
                utils.SessionManager.setCurrentUser(loggedUser);
                
                refreshEvents();
                refreshRegistrations();
                refreshRecommendations();

                // Fetch User details for top bar
                try {
                    entities.User user = userService.getById(currentCandidate.getId());
                    if (user != null) {
                        userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                        userRoleLabel.setText("CANDIDAT");
                        userRoleLabel.getStyleClass().add("badge-candidate");
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
            allEvents = eventService.getAll();
            filterEvents();
            refreshRecommendations(); // Also refresh recommendations when events are loaded
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private void refreshRecommendations() {
        if (currentCandidate == null || allEvents == null || allEvents.isEmpty()) return;

        try {
            java.util.List<EventRegistration> history = registrationService.getByCandidate(currentCandidate.getId());
            if (history.isEmpty()) {
                recommendationBox.setVisible(false);
                recommendationBox.setManaged(false);
                return;
            }

            // Simple recommendation: Find events of the same types the user registered for
            java.util.Set<String> registeredTypes = new java.util.HashSet<>();
            java.util.Set<Long> registeredEventIds = new java.util.HashSet<>();
            
            for (EventRegistration reg : history) {
                if (reg.getEvent() != null) {
                    registeredTypes.add(reg.getEvent().getEventType());
                    registeredEventIds.add(reg.getEventId());
                }
            }

            recommendedHBox.getChildren().clear();
            boolean foundAny = false;

            for (RecruitmentEvent event : allEvents) {
                // Recommend if same type AND not already registered
                if (registeredTypes.contains(event.getEventType()) && !registeredEventIds.contains(event.getId())) {
                    Node card = createEventCard(event);
                    // Add a small styling or badge to distinguish in recommendation if needed
                    card.getStyleClass().add("recommended-card");
                    recommendedHBox.getChildren().add(card);
                    foundAny = true;
                }
            }

            recommendationBox.setVisible(foundAny);
            recommendationBox.setManaged(foundAny);

        } catch (SQLException e) {
            System.err.println("Error loading recommendations: " + e.getMessage());
        }
    }

    private javafx.scene.Node createEventCard(RecruitmentEvent event) {
        VBox card = new VBox(15);
        card.getStyleClass().add("event-card");
        card.setAlignment(Pos.TOP_LEFT);
        
        Label titleLabel = new Label(event.getTitle());
        titleLabel.getStyleClass().add("event-card-title");
        
        Label typeLabel = new Label(event.getEventType());
        typeLabel.getStyleClass().add("event-card-type");
        
        Label descriptionLabel = new Label(event.getDescription());
        descriptionLabel.getStyleClass().add("event-card-description");
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxHeight(60);
        
        VBox infoBox = new VBox(8);
        infoBox.getStyleClass().add("event-card-info-box");
        
        Label locationLabel = new Label("📍 " + event.getLocation());
        locationLabel.getStyleClass().add("event-card-info");
        
        Label dateLabel = new Label("📅 " + (event.getEventDate() != null ? event.getEventDate().toString().replace("T", " ") : "N/A"));
        dateLabel.getStyleClass().add("event-card-info");
        
        Label capacityLabel = new Label("👥 " + event.getCapacity() + " places");
        capacityLabel.getStyleClass().add("event-card-info");
        
        infoBox.getChildren().addAll(locationLabel, dateLabel, capacityLabel);

        // Badge Populaire Logic
        try {
            if (eventService.isEventPopular(event.getId())) {
                Label popularBadge = new Label("🔥 Populaire");
                popularBadge.getStyleClass().add("badge-popular");
                card.getChildren().add(popularBadge);
            }
        } catch (SQLException e) {
            System.err.println("Error checking popularity: " + e.getMessage());
        }
        
        card.getChildren().addAll(titleLabel, typeLabel, descriptionLabel, infoBox);
        
        // Tooltips for full text
        Tooltip titleTooltip = new Tooltip(event.getTitle());
        titleTooltip.setWrapText(true);
        titleTooltip.setMaxWidth(300);
        titleLabel.setTooltip(titleTooltip);
        
        Tooltip descTooltip = new Tooltip(event.getDescription());
        descTooltip.setWrapText(true);
        descTooltip.setMaxWidth(400);
        descriptionLabel.setTooltip(descTooltip);
        
        // Voir Détails Button
        Button detailsBtn = new Button("Voir Détails");
        detailsBtn.getStyleClass().add("btn-secondary");
        detailsBtn.setStyle("-fx-font-size: 10px; -fx-padding: 4 8;");
        detailsBtn.setOnAction(e -> openEventDetailsModal(event));
        
        HBox actionBox = new HBox(10, detailsBtn);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().add(actionBox);
        
        card.setOnMouseClicked(e -> {
            // Remove previous selection styling
            eventsHBox.getChildren().forEach(node -> node.getStyleClass().remove("event-card-selected"));
            // Add selection styling
            card.getStyleClass().add("event-card-selected");
            
            selectedEvent = event;
        });
        
        return card;
    }

    private RecruitmentEvent selectedEvent;

    private void refreshRegistrations() {
        if (currentCandidate == null)
            return;
        try {
            ObservableList<EventRegistration> list = FXCollections
                    .observableArrayList(registrationService.getByCandidate(currentCandidate.getId()));
            myRegistrationsTable.setItems(list);
            updateStatistics();
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

    private void openEventDetailsModal(RecruitmentEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/GUI/event_details_modal.fxml"));
            javafx.scene.Parent root = loader.load();

            EventDetailsModalController controller = loader.getController();
            controller.setEvent(event);
            controller.setCandidate(currentCandidate);
            controller.setParentController(this);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setTitle("Détails : " + event.getTitle());
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

        } catch (java.io.IOException e) {
            showAlert("Erreur", "Impossible d'ouvrir les détails de l'événement : " + e.getMessage());
        }
    }

    public void handleApplyFromDetails(RecruitmentEvent event) {
        openRegistrationModal(event);
    }

    public void onRegistrationSuccess() {
        refreshRegistrations();
        refreshRecommendations();
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

    private void updateStatistics() {
        ObservableList<EventRegistration> registrations = myRegistrationsTable.getItems();
        if (registrations == null) return;

        long total = registrations.size();
        long confirmed = registrations.stream()
                .filter(r -> r.getAttendanceStatus() != null && "CONFIRMED".equals(r.getAttendanceStatus().toString()))
                .count();
        long pending = registrations.stream()
                .filter(r -> r.getAttendanceStatus() != null && ("PENDING".equals(r.getAttendanceStatus().toString()) || "REGISTERED".equals(r.getAttendanceStatus().toString())))
                .count();

        totalRegLabel.setText(String.valueOf(total));
        confirmedRegLabel.setText(String.valueOf(confirmed));
        pendingRegLabel.setText(String.valueOf(pending));

        // Find next upcoming confirmed event
        EventRegistration next = registrations.stream()
                .filter(r -> r.getAttendanceStatus() != null && "CONFIRMED".equals(r.getAttendanceStatus().toString()))
                .filter(r -> r.getEvent() != null && r.getEvent().getEventDate() != null)
                .filter(r -> r.getEvent().getEventDate().isAfter(LocalDateTime.now()))
                .min(java.util.Comparator.comparing(r -> r.getEvent().getEventDate()))
                .orElse(null);

        if (next != null) {
            String title = next.getEvent().getTitle();
            if (title.length() > 15) title = title.substring(0, 12) + "...";
            nextEventLabel.setText(next.getEvent().getEventDate().toLocalDate().toString() + " : " + title);
        } else {
            nextEventLabel.setText("Aucun à venir");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
