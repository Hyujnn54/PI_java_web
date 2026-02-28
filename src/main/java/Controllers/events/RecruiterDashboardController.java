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

    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TextField locationField;
    @FXML
    private DatePicker dateField;
    @FXML
    private TextField capacityField;
    @FXML
    private Label titleErrorLabel;
    @FXML
    private Label typeErrorLabel;
    @FXML
    private Label locationErrorLabel;
    @FXML
    private Label dateErrorLabel;
    @FXML
    private Label capacityErrorLabel;
    @FXML
    private Label descriptionErrorLabel;

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
        // Garantir que la base de données est à jour
        SchemaFixer.main(null);

        setupTable();
        setupAttendeesTable();
        setupComboBoxes();
        loadRecruiterData();
    }

    private void setupComboBoxes() {
        typeCombo.setItems(FXCollections.observableArrayList("Job_Faire", "WEBINAIRE", "Interview day"));
        registrationStatusCombo.setItems(FXCollections.observableArrayList(AttendanceStatusEnum.values()));
    }

    private void setupAttendeesTable() {
        eventTitleCol.setCellValueFactory(cellData -> {
            if (cellData.getValue().getEvent() != null) {
                return new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEvent().getTitle());
            }
            return new javafx.beans.property.SimpleStringProperty("N/A");
        });
        candLastNameCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLastName()));
        candFirstNameCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFirstName()));
        candEmailCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEmail()));
        statusCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getAttendanceStatus().name()));
        regDateCol.setCellValueFactory(new PropertyValueFactory<>("registeredAt"));

        attendeesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                registrationStatusCombo.setValue(newVal.getAttendanceStatus());
            }
        });
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
        } catch (SQLException e) {
            showAlert("Erreur Participants", e.getMessage());
        }
    }

    private void refreshAttendees(long eventId) {
        try {
            ObservableList<EventRegistration> list = FXCollections
                    .observableArrayList(registrationService.getByEvent(eventId));
            attendeesTable.setItems(list);
        } catch (SQLException e) {
            showAlert("Erreur Participants", e.getMessage());
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

                // Fetch User details for top bar
                try {
                    User user = userService.getById(currentRecruiter.getId());
                    if (user != null) {
                        userNameLabel.setText(user.getFirstName() + " " + user.getLastName());
                        userRoleLabel.setText("RECRUTEUR");
                        userRoleLabel.getStyleClass().add("badge-recruiter");
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
            
            populateForm(event);
            refreshAttendees(event.getId());
            
            // Store selected event for update/delete
            // We can use a property or just rely on populateForm and a local variable if needed
            // But let's add a field for convenience
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

    private void switchView(boolean isEvents) {
        eventsView.setVisible(isEvents);
        eventsView.setManaged(isEvents);
        interviewsView.setVisible(!isEvents);
        interviewsView.setManaged(!isEvents);

        // Update sidebar button styles
        eventsBtn.getStyleClass().remove("sidebar-button-active");
        eventsBtn.getStyleClass().remove("sidebar-button");
        interviewsBtn.getStyleClass().remove("sidebar-button-active");
        interviewsBtn.getStyleClass().remove("sidebar-button");

        if (isEvents) {
            eventsBtn.getStyleClass().add("sidebar-button-active");
            interviewsBtn.getStyleClass().add("sidebar-button");
        } else {
            eventsBtn.getStyleClass().add("sidebar-button");
            interviewsBtn.getStyleClass().add("sidebar-button-active");
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
            // Refresh the table to show updated status
            if (selectedEvent != null) {
                refreshAttendees(selectedEvent.getId());
            } else {
                refreshAllAttendees();
            }
        } catch (SQLException e) {
            showAlert("Erreur Mise à jour", e.getMessage());
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
    }

    private void populateForm(RecruitmentEvent event) {
        titleField.setText(event.getTitle());
        descriptionField.setText(event.getDescription());
        typeCombo.setValue(event.getEventType());
        locationField.setText(event.getLocation());
        capacityField.setText(String.valueOf(event.getCapacity()));
        if (event.getEventDate() != null) {
            dateField.setValue(event.getEventDate().toLocalDate());
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

        // Clear error labels
        titleErrorLabel.setText("");
        typeErrorLabel.setText("");
        locationErrorLabel.setText("");
        dateErrorLabel.setText("");
        capacityErrorLabel.setText("");
        descriptionErrorLabel.setText("");
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

        return !hasError;
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

