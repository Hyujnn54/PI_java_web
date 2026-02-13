package Controllers;

import Services.UserManagementService;
import Utils.UserContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.Optional;

/**
 * Controller for the Admin Dashboard.
 * Manages CRUD operations for all users and displays statistics charts.
 */
public class AdminDashboardController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilter;
    @FXML private Button btnSearch;
    @FXML private Button btnRefresh;

    @FXML private Button btnAllUsers;
    @FXML private Button btnRecruiters;
    @FXML private Button btnCandidates;
    @FXML private Button btnAdmins;

    @FXML private TableView<UserTableRow> userTable;
    @FXML private TableColumn<UserTableRow, String> colName;
    @FXML private TableColumn<UserTableRow, String> colEmail;
    @FXML private TableColumn<UserTableRow, String> colRole;
    @FXML private TableColumn<UserTableRow, String> colPhone;
    @FXML private TableColumn<UserTableRow, Boolean> colActive;

    @FXML private Button btnAddUser;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;
    @FXML private Label lblRecordCount;
    @FXML private Label statusLabel;

    // Statistics Labels
    @FXML private Label lblTotalUsers;
    @FXML private Label lblRecruiters;
    @FXML private Label lblCandidates;
    @FXML private Label lblAdmins;

    // Charts
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;

    private UserContext.Role currentFilter = null;
    private UserManagementService.UserRow selectedUser = null;

    /**
     * Simple table row class for displaying user data
     */
    public static class UserTableRow {
        private final Long id;
        private final String name;
        private final String email;
        private final String role;
        private final String phone;
        private final Boolean active;

        public UserTableRow(UserManagementService.UserRow user) {
            this.id = user.id();
            this.name = user.firstName() + " " + user.lastName();
            this.email = user.email();
            this.role = user.role();
            this.phone = user.phone();
            this.active = user.isActive();
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getPhone() { return phone; }
        public Boolean getActive() { return active; }
    }

    @FXML
    private void initialize() {
        setupTableColumns();
        setupEventHandlers();
        setupComboBoxes();
        loadAllUsers();
        updateStatistics();
    }

    /**
     * Setup ComboBox items
     */
    private void setupComboBoxes() {
        if (roleFilter != null) {
            roleFilter.setItems(FXCollections.observableArrayList("All", "RECRUITER", "CANDIDATE", "ADMIN"));
            roleFilter.setValue("All");
        }
    }

    /**
     * Setup table columns with cell value factories
     */
    private void setupTableColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colActive.setCellValueFactory(new PropertyValueFactory<>("active"));
    }

    /**
     * Setup event handlers for buttons and table selection
     */
    private void setupEventHandlers() {
        btnAllUsers.setOnAction(e -> loadUsers(null));
        btnRecruiters.setOnAction(e -> loadUsers(UserContext.Role.RECRUITER));
        btnCandidates.setOnAction(e -> loadUsers(UserContext.Role.CANDIDATE));
        btnAdmins.setOnAction(e -> loadUsers(UserContext.Role.ADMIN));

        btnSearch.setOnAction(e -> handleSearch());
        btnRefresh.setOnAction(e -> handleRefresh());
        btnAddUser.setOnAction(e -> handleAddUser());
        btnEdit.setOnAction(e -> handleEdit());
        btnDelete.setOnAction(e -> handleDelete());

        // Table selection listener
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedUser = UserManagementService.getUserById(newVal.getId());
                btnEdit.setDisable(false);
                btnDelete.setDisable(false);
                statusLabel.setText("Selected: " + newVal.getName());
            } else {
                selectedUser = null;
                btnEdit.setDisable(true);
                btnDelete.setDisable(true);
                statusLabel.setText("");
            }
        });
    }

    /**
     * Load all users into the table
     */
    private void loadAllUsers() {
        loadUsers(null);
    }

    /**
     * Load users filtered by role
     */
    private void loadUsers(UserContext.Role role) {
        currentFilter = role;
        updateButtonHighlight(role);

        List<UserManagementService.UserRow> userRows;
        if (role == null) {
            userRows = UserManagementService.getAllUsers();
        } else {
            userRows = UserManagementService.getUsersByRole(role);
        }

        ObservableList<UserTableRow> tableData = FXCollections.observableArrayList();
        for (UserManagementService.UserRow user : userRows) {
            tableData.add(new UserTableRow(user));
        }

        userTable.setItems(tableData);
        lblRecordCount.setText("Records: " + tableData.size());
        updateStatistics();
    }

    /**
     * Update highlight on filter buttons
     */
    private void updateButtonHighlight(UserContext.Role role) {
        btnAllUsers.setStyle("");
        btnRecruiters.setStyle("");
        btnCandidates.setStyle("");
        btnAdmins.setStyle("");

        if (role == null) {
            btnAllUsers.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white;");
        } else if (role == UserContext.Role.RECRUITER) {
            btnRecruiters.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white;");
        } else if (role == UserContext.Role.CANDIDATE) {
            btnCandidates.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white;");
        } else if (role == UserContext.Role.ADMIN) {
            btnAdmins.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white;");
        }
    }

    /**
     * Update statistics labels and charts
     */
    private void updateStatistics() {
        List<UserManagementService.UserRow> allUsers = UserManagementService.getAllUsers();
        List<UserManagementService.UserRow> recruiters = UserManagementService.getUsersByRole(UserContext.Role.RECRUITER);
        List<UserManagementService.UserRow> candidates = UserManagementService.getUsersByRole(UserContext.Role.CANDIDATE);
        List<UserManagementService.UserRow> admins = UserManagementService.getUsersByRole(UserContext.Role.ADMIN);

        // Update statistics labels
        if (lblTotalUsers != null) lblTotalUsers.setText(String.valueOf(allUsers.size()));
        if (lblRecruiters != null) lblRecruiters.setText(String.valueOf(recruiters.size()));
        if (lblCandidates != null) lblCandidates.setText(String.valueOf(candidates.size()));
        if (lblAdmins != null) lblAdmins.setText(String.valueOf(admins.size()));

        // Update pie chart - User Distribution by Role
        updatePieChart(recruiters.size(), candidates.size(), admins.size());

        // Update bar chart - Active vs Inactive
        updateBarChart(allUsers);
    }

    /**
     * Update pie chart with user distribution
     */
    private void updatePieChart(int recruiterCount, int candidateCount, int adminCount) {
        if (pieChart == null) return;

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        if (recruiterCount > 0) {
            pieData.add(new PieChart.Data("Recruiters", recruiterCount));
        }
        if (candidateCount > 0) {
            pieData.add(new PieChart.Data("Candidates", candidateCount));
        }
        if (adminCount > 0) {
            pieData.add(new PieChart.Data("Admins", adminCount));
        }

        if (pieData.isEmpty()) {
            pieData.add(new PieChart.Data("No Data", 1));
        }

        pieChart.setData(pieData);
    }

    /**
     * Update bar chart with active vs inactive users
     */
    private void updateBarChart(List<UserManagementService.UserRow> users) {
        if (barChart == null) return;

        int activeCount = 0;
        int inactiveCount = 0;

        for (UserManagementService.UserRow user : users) {
            if (user.isActive()) {
                activeCount++;
            } else {
                inactiveCount++;
            }
        }

        barChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("User Status");
        series.getData().add(new XYChart.Data<>("Active", activeCount));
        series.getData().add(new XYChart.Data<>("Inactive", inactiveCount));

        barChart.getData().add(series);
    }

    /**
     * Handle search functionality
     */
    @FXML
    private void handleSearch() {
        String searchQuery = searchField.getText().trim();
        if (searchQuery.isEmpty()) {
            loadUsers(currentFilter);
            return;
        }

        UserContext.Role roleFilterValue = switch (this.roleFilter.getValue()) {
            case "RECRUITER" -> UserContext.Role.RECRUITER;
            case "CANDIDATE" -> UserContext.Role.CANDIDATE;
            case "ADMIN" -> UserContext.Role.ADMIN;
            default -> null;
        };

        List<UserManagementService.UserRow> userRows = UserManagementService.searchUsers(searchQuery, roleFilterValue);
        ObservableList<UserTableRow> tableData = FXCollections.observableArrayList();
        for (UserManagementService.UserRow user : userRows) {
            tableData.add(new UserTableRow(user));
        }

        userTable.setItems(tableData);
        lblRecordCount.setText("Records: " + tableData.size());
        statusLabel.setText("Search returned " + tableData.size() + " user(s)");
    }

    /**
     * Handle refresh button
     */
    @FXML
    private void handleRefresh() {
        searchField.clear();
        if (roleFilter != null) {
            roleFilter.setValue("All");
        }
        loadAllUsers();
        userTable.getSelectionModel().clearSelection();
        selectedUser = null;
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        statusLabel.setText("Data refreshed");
    }

    /**
     * Handle add user button - Opens a dialog
     */
    @FXML
    private void handleAddUser() {
        showAddUserDialog();
    }

    /**
     * Handle edit button - Opens a dialog
     */
    @FXML
    private void handleEdit() {
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to edit");
            return;
        }
        showEditUserDialog(selectedUser);
    }

    /**
     * Handle delete button
     */
    @FXML
    private void handleDelete() {
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a user to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete User?");
        confirm.setContentText("Are you sure you want to delete " + selectedUser.firstName() + " " + selectedUser.lastName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = UserManagementService.deleteUser(selectedUser.id());
            if (success) {
                statusLabel.setText("✅ User deactivated successfully");
                loadUsers(currentFilter);
                userTable.getSelectionModel().clearSelection();
                selectedUser = null;
                btnEdit.setDisable(true);
                btnDelete.setDisable(true);
            } else {
                statusLabel.setText("❌ Failed to delete user");
            }
        }
    }

    /**
     * Show dialog for adding new user
     */
    private void showAddUserDialog() {
        Dialog<UserData> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Enter user details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField firstName = new TextField();
        firstName.setPromptText("First Name");
        TextField lastName = new TextField();
        lastName.setPromptText("Last Name");
        TextField email = new TextField();
        email.setPromptText("Email");
        TextField phone = new TextField();
        phone.setPromptText("Phone");
        ComboBox<String> role = new ComboBox<>();
        role.setItems(FXCollections.observableArrayList("RECRUITER", "CANDIDATE", "ADMIN"));
        role.setValue("CANDIDATE");

        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstName, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastName, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(email, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phone, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(role, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new UserData(firstName.getText(), lastName.getText(), email.getText(), phone.getText(), role.getValue());
            }
            return null;
        });

        Optional<UserData> result = dialog.showAndWait();
        if (result.isPresent()) {
            UserData userData = result.get();
            if (userData.firstName.isEmpty() || userData.lastName.isEmpty() || userData.email.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "First name, last name, and email are required");
                return;
            }

            Long userId = UserManagementService.createUser(userData.firstName, userData.lastName, userData.email, userData.phone, "password123", userData.role, true);
            if (userId != null) {
                statusLabel.setText("✅ User created successfully");
                loadUsers(currentFilter);
            } else {
                statusLabel.setText("❌ Failed to create user");
            }
        }
    }

    /**
     * Show dialog for editing user
     */
    private void showEditUserDialog(UserManagementService.UserRow user) {
        Dialog<UserData> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit user details");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField firstName = new TextField(user.firstName());
        TextField lastName = new TextField(user.lastName());
        TextField email = new TextField(user.email());
        TextField phone = new TextField(user.phone());
        ComboBox<String> role = new ComboBox<>();
        role.setItems(FXCollections.observableArrayList("RECRUITER", "CANDIDATE", "ADMIN"));
        role.setValue(user.role());

        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstName, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastName, 1, 1);
        grid.add(new Label("Email:"), 0, 2);
        grid.add(email, 1, 2);
        grid.add(new Label("Phone:"), 0, 3);
        grid.add(phone, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(role, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new UserData(firstName.getText(), lastName.getText(), email.getText(), phone.getText(), role.getValue());
            }
            return null;
        });

        Optional<UserData> result = dialog.showAndWait();
        if (result.isPresent()) {
            UserData userData = result.get();
            if (userData.firstName.isEmpty() || userData.lastName.isEmpty() || userData.email.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation Error", "First name, last name, and email are required");
                return;
            }

            boolean success = UserManagementService.updateUser(user.id(), userData.firstName, userData.lastName, userData.email, userData.phone, userData.role, true);
            if (success) {
                statusLabel.setText("✅ User updated successfully");
                loadUsers(currentFilter);
                userTable.getSelectionModel().clearSelection();
                selectedUser = null;
                btnEdit.setDisable(true);
                btnDelete.setDisable(true);
            } else {
                statusLabel.setText("❌ Failed to update user");
            }
        }
    }

    /**
     * Show alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class for user data in dialogs
     */
    private static class UserData {
        String firstName;
        String lastName;
        String email;
        String phone;
        String role;

        UserData(String firstName, String lastName, String email, String phone, String role) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
            this.phone = phone;
            this.role = role;
        }
    }

    /**
     * Called when role is set via MainShellController
     */
    public void setUserRole(String role) {
        // Admin dashboard is only for admin users
    }
}





