package Controllers.events;

import Models.events.*;
import Services.events.*;
import Utils.SceneManager;
import Utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller simple pour le dashboard Admin
 * Affiche toutes les données en lecture seule
 */
public class AdminDashboardController implements Initializable {

    @FXML private TableView<User> usersTable;
    @FXML private TableView<Candidate> candidatesTable;
    @FXML private TableView<Recruiter> recruitersTable;
    @FXML private TableView<EventRegistration> registrationsTable;

    private UserService userService;
    private CandidateService candidateService;
    private RecruiterService recruiterService;
    private EventRegistrationService registrationService;

    public AdminDashboardController() {
        userService = new UserService();
        candidateService = new CandidateService();
        recruiterService = new RecruiterService();
        registrationService = new EventRegistrationService();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUsersTable();
        setupCandidatesTable();
        setupRecruitersTable();
        setupRegistrationsTable();
        loadAllData();
    }

    private void setupUsersTable() {
        TableColumn<User, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        TableColumn<User, String> roleCol = new TableColumn<>("Rôle");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        usersTable.getColumns().addAll(idCol, emailCol, roleCol);
    }

    private void setupCandidatesTable() {
        TableColumn<Candidate, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<Candidate, String> locCol = new TableColumn<>("Lieu");
        locCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        
        TableColumn<Candidate, Integer> expCol = new TableColumn<>("Expérience");
        expCol.setCellValueFactory(new PropertyValueFactory<>("experienceYears"));

        candidatesTable.getColumns().addAll(idCol, locCol, expCol);
    }

    private void setupRecruitersTable() {
        TableColumn<Recruiter, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<Recruiter, String> companyCol = new TableColumn<>("Entreprise");
        companyCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));

        recruitersTable.getColumns().addAll(idCol, companyCol);
    }


    private void setupRegistrationsTable() {
        TableColumn<EventRegistration, Long> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        TableColumn<EventRegistration, Long> eventCol = new TableColumn<>("Event ID");
        eventCol.setCellValueFactory(new PropertyValueFactory<>("eventId"));
        
        TableColumn<EventRegistration, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("attendanceStatus"));

        registrationsTable.getColumns().addAll(idCol, eventCol, statusCol);
    }

    private void loadAllData() {
        try {
            usersTable.setItems(FXCollections.observableArrayList(userService.getAll()));
            candidatesTable.setItems(FXCollections.observableArrayList(candidateService.getAll()));
            recruitersTable.setItems(FXCollections.observableArrayList(recruiterService.getAll()));
            registrationsTable.setItems(FXCollections.observableArrayList(registrationService.getAll()));
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Erreur lors du chargement des données");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        Stage stage = (Stage) usersTable.getScene().getWindow();
        SceneManager.switchScene(stage, "/GUI/login.fxml", "Login");
    }
}
