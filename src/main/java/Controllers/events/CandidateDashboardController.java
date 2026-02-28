package Controllers.events;

import Models.events.Candidate;
import Models.events.EventRegistration;
import Models.events.RecruitmentEvent;
import Models.events.User;
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
    private Candidate currentCandidate;

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
    }

    private void loadCandidateData() {
        try {
            // Use the actual logged-in user ID from UserContext
            Long contextId = Utils.UserContext.getUserId();
            long lookupId = (contextId != null) ? contextId : 5L;
            currentCandidate = candidateService.getByUserId(lookupId);
            if (currentCandidate == null) {
                java.util.List<Candidate> all = candidateService.getAll();
                if (!all.isEmpty()) {
                    currentCandidate = all.get(0);
                }
            }
            if (currentCandidate == null) {
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
            }
            if (currentCandidate != null) {
                refreshEvents();
                refreshRegistrations();

                // Fetch User details for top bar (only present in standalone dashboard, not embedded view)
                try {
                    User user = userService.getById(currentCandidate.getId());
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
            
            for (RecruitmentEvent event : events) {
                eventsHBox.getChildren().add(createEventCard(event));
            }
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    private Node createEventCard(RecruitmentEvent event) {
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
        
        card.getChildren().addAll(titleLabel, typeLabel, descriptionLabel, infoBox);
        
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
