package Controllers;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import Utils.MyDatabase;

import java.sql.*;
import java.util.Locale;
import Utils.PasswordUtil;

public class AdminDashboardController {

    // ===== Cards =====
    @FXML private Label lblTotalUsers;
    @FXML private Label lblRecruiters;
    @FXML private Label lblCandidates;
    @FXML private Label lblAdmins;

    // ===== Charts =====
    @FXML private PieChart pieChart;
    @FXML private BarChart<String, Number> barChart;

    // ===== Management =====
    @FXML private Label lblRecordCount;
    @FXML private Label statusLabel;

    @FXML private TextField searchField;

    // ✅ CHANGED: filter by String type (no Role enum)
    @FXML private ComboBox<String> roleFilter;
    @FXML private Button btnAddUser;

    @FXML private TableView<UserRow> userTable;
    @FXML private TableColumn<UserRow, String> colName;
    @FXML private TableColumn<UserRow, String> colEmail;
    @FXML private TableColumn<UserRow, String> colRole;
    @FXML private TableColumn<UserRow, String> colPhone;
    @FXML private TableColumn<UserRow, String> colActive;

    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private final ObservableList<UserRow> rows = FXCollections.observableArrayList();
    private boolean bigAdmin = false;


    private Connection cnx() {
        return MyDatabase.getInstance().getConnection();
    }

    // ===== Table row model =====
    public static class UserRow {
        private final LongProperty id = new SimpleLongProperty();
        private final StringProperty firstName = new SimpleStringProperty();
        private final StringProperty lastName = new SimpleStringProperty();
        private final StringProperty email = new SimpleStringProperty();
        private final StringProperty type = new SimpleStringProperty(); // ADMIN / RECRUITER / CANDIDATE / USER
        private final StringProperty phone = new SimpleStringProperty();
        private final BooleanProperty active = new SimpleBooleanProperty();


        public UserRow(long id, String firstName, String lastName, String email, String type, String phone, boolean active) {
            this.id.set(id);
            this.firstName.set(firstName == null ? "" : firstName);
            this.lastName.set(lastName == null ? "" : lastName);
            this.email.set(email == null ? "" : email);
            this.type.set(type == null ? "USER" : type);
            this.phone.set(phone == null ? "" : phone);
            this.active.set(active);
        }

        public long getId() { return id.get(); }
        public String getFirstName() { return firstName.get(); }
        public String getLastName() { return lastName.get(); }
        public String getName() { return (getFirstName() + " " + getLastName()).trim(); }
        public String getEmail() { return email.get(); }
        public String getType() { return type.get(); }
        public String getPhone() { return phone.get(); }
        public boolean isActive() { return active.get(); }
    }

    @FXML
    public void initialize() {

        // Bind table columns
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        colEmail.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        colRole.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getType()));
        colPhone.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        colActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Inactive"));

        userTable.setItems(rows);

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            boolean has = newV != null;
            btnEdit.setDisable(!has);
            btnDelete.setDisable(!has);
        });

        // filter values
        roleFilter.setItems(FXCollections.observableArrayList("ALL", "ADMIN", "RECRUITER", "CANDIDATE", "USER"));
        roleFilter.setValue("ALL");

        refreshAll();

        if (Utils.Session.getCurrentUser() instanceof Models.Admin admin) {

            String area = admin.getAssignedArea();

            bigAdmin = area != null && area.trim().equalsIgnoreCase("SUPER ADMIN");
            applyPermissions();
        }
    }

    // ===== UI handlers =====
    @FXML public void handleRefresh(ActionEvent e) { refreshAll(); }

    @FXML
    public void handleSearch(ActionEvent e) {
        String type = roleFilter.getValue();
        loadUsers(searchField.getText(), type);
        updateCardsAndCharts();
        statusLabel.setText("Search applied ✅");
    }

    @FXML
    public void handleShowAll(ActionEvent e) {
        roleFilter.setValue("ALL");
        loadUsers(searchField.getText(), "ALL");
        updateCardsAndCharts();
    }

    @FXML
    public void handleShowRecruiters(ActionEvent e) {
        roleFilter.setValue("RECRUITER");
        loadUsers(searchField.getText(), "RECRUITER");
        updateCardsAndCharts();
    }

    @FXML
    public void handleShowCandidates(ActionEvent e) {
        roleFilter.setValue("CANDIDATE");
        loadUsers(searchField.getText(), "CANDIDATE");
        updateCardsAndCharts();
    }

    @FXML
    public void handleShowAdmins(ActionEvent e) {
        roleFilter.setValue("ADMIN");
        loadUsers(searchField.getText(), "ADMIN");
        updateCardsAndCharts();
    }

    // ===== Add Admin user (since your UI forces ADMIN) =====
    @FXML
    public void handleAddUser(ActionEvent e) {
        if (!bigAdmin) {
            new Alert(Alert.AlertType.WARNING,
                    "Access denied. Only SUPER ADMIN can add other admins.")
                    .showAndWait();
        }
        UserForm form = showUserDialog("Add Admin", null);
        if (form == null) return;

        try {
            cnx().setAutoCommit(false);

            long newId;
            // 1) users
            String sqlUser = "INSERT INTO users (email, password, first_name, last_name, phone, is_active) VALUES (?,?,?,?,?,?)";
            try (PreparedStatement ps = cnx().prepareStatement(sqlUser, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, form.email);
                ps.setString(2, PasswordUtil.hash(form.password));                ps.setString(3, form.firstName);
                ps.setString(4, form.lastName);
                ps.setString(5, form.phone);
                ps.setInt(6, form.active ? 1 : 0);
                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) throw new SQLException("No generated id.");
                    newId = rs.getLong(1);
                }
            }

            // 2) admin table (optional assigned area)
            try (PreparedStatement ps = cnx().prepareStatement("INSERT INTO admin (id, assigned_area) VALUES (?,?)")) {
                ps.setLong(1, newId);
                ps.setString(2, form.assignedArea);
                ps.executeUpdate();
            }

            cnx().commit();
            cnx().setAutoCommit(true);

            refreshAll();
            statusLabel.setText("Admin added ✅");

        } catch (Exception ex) {
            try { cnx().rollback(); cnx().setAutoCommit(true); } catch (Exception ignore) {}
            ex.printStackTrace();
            statusLabel.setText("Add failed: " + ex.getMessage());
        }
    }

    @FXML
    public void handleEdit(ActionEvent e) {
        if (!bigAdmin) {
            new Alert(Alert.AlertType.WARNING,
                    "Access denied. You cannot modify users.")
                    .showAndWait();
            return;
        }
        UserRow selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!"ADMIN".equalsIgnoreCase(selected.getType())) {
            new Alert(Alert.AlertType.WARNING, "This screen edits only ADMIN users in your current implementation.").showAndWait();
            return;
        }

        UserForm form = showUserDialog("Update Admin", selected);
        if (form == null) return;

        try {
            cnx().setAutoCommit(false);

            boolean changePassword = form.password != null && !form.password.isBlank();

            String sqlUser = changePassword
                    ? "UPDATE users SET email=?, first_name=?, last_name=?, phone=?, is_active=?, password=? WHERE id=?"
                    : "UPDATE users SET email=?, first_name=?, last_name=?, phone=?, is_active=? WHERE id=?";

            try (PreparedStatement ps = cnx().prepareStatement(sqlUser)) {
                int i = 1;
                ps.setString(i++, form.email);
                ps.setString(i++, form.firstName);
                ps.setString(i++, form.lastName);
                ps.setString(i++, form.phone);
                ps.setInt(i++, form.active ? 1 : 0);
                if (changePassword) ps.setString(i++, PasswordUtil.hash(form.password));                ps.setLong(i, selected.getId());
                ps.executeUpdate();
            }

            // admin table
            try (PreparedStatement ps = cnx().prepareStatement("UPDATE admin SET assigned_area=? WHERE id=?")) {
                ps.setString(1, form.assignedArea);
                ps.setLong(2, selected.getId());
                ps.executeUpdate();
            }

            cnx().commit();
            cnx().setAutoCommit(true);

            refreshAll();
            statusLabel.setText("Admin updated ✅");

        } catch (Exception ex) {
            try { cnx().rollback(); cnx().setAutoCommit(true); } catch (Exception ignore) {}
            ex.printStackTrace();
            statusLabel.setText("Update failed: " + ex.getMessage());
        }
    }

    @FXML
    public void handleDelete(ActionEvent e) {
        if (!bigAdmin) {
            new Alert(Alert.AlertType.WARNING,
                    "Access denied. You cannot delete users.")
                    .showAndWait();
            return;
        }
        UserRow selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete this user permanently?");
        confirm.setContentText(selected.getEmail());

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try (PreparedStatement ps = cnx().prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setLong(1, selected.getId());
            int affected = ps.executeUpdate();
            if (affected == 0) {
                statusLabel.setText("Nothing deleted (user not found).");
                return;
            }

            refreshAll();
            statusLabel.setText("User deleted ✅");

        } catch (SQLException ex) {
            ex.printStackTrace();
            statusLabel.setText("Delete failed: " + ex.getMessage());
        }
    }

    // ===== Data loading =====
    private void refreshAll() {
        loadUsers(null, "ALL");
        updateCardsAndCharts();
        statusLabel.setText("Loaded ✅");
    }

    private void loadUsers(String search, String typeFilter) {
        rows.clear();

        boolean hasSearch = search != null && !search.trim().isEmpty();
        boolean filterType = typeFilter != null && !"ALL".equalsIgnoreCase(typeFilter);

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT u.id, u.first_name, u.last_name, u.email, u.phone, u.is_active,
                   CASE
                     WHEN a.id IS NOT NULL THEN 'ADMIN'
                     WHEN r.id IS NOT NULL THEN 'RECRUITER'
                     WHEN c.id IS NOT NULL THEN 'CANDIDATE'
                     ELSE 'USER'
                   END AS type
            FROM users u
            LEFT JOIN admin a ON a.id = u.id
            LEFT JOIN recruiter r ON r.id = u.id
            LEFT JOIN candidate c ON c.id = u.id
            WHERE 1=1
        """);

        if (hasSearch) {
            sql.append(" AND (LOWER(u.first_name) LIKE ? OR LOWER(u.last_name) LIKE ? OR LOWER(u.email) LIKE ?)");
        }
        if (filterType) {
            sql.append("""
                AND (
                  (?='ADMIN' AND a.id IS NOT NULL)
                  OR (?='RECRUITER' AND r.id IS NOT NULL)
                  OR (?='CANDIDATE' AND c.id IS NOT NULL)
                  OR (?='USER' AND a.id IS NULL AND r.id IS NULL AND c.id IS NULL)
                )
            """);
        }

        sql.append(" ORDER BY u.id DESC");

        try (PreparedStatement ps = cnx().prepareStatement(sql.toString())) {
            int idx = 1;

            if (hasSearch) {
                String s = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                ps.setString(idx++, s);
                ps.setString(idx++, s);
                ps.setString(idx++, s);
            }

            if (filterType) {
                ps.setString(idx++, typeFilter.toUpperCase(Locale.ROOT));
                ps.setString(idx++, typeFilter.toUpperCase(Locale.ROOT));
                ps.setString(idx++, typeFilter.toUpperCase(Locale.ROOT));
                ps.setString(idx++, typeFilter.toUpperCase(Locale.ROOT));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new UserRow(
                            rs.getLong("id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("email"),
                            rs.getString("type"),
                            rs.getString("phone"),
                            rs.getInt("is_active") == 1
                    ));
                }
            }

            lblRecordCount.setText("Records: " + rows.size());

        } catch (Exception ex) {
            ex.printStackTrace();
            statusLabel.setText("Load failed: " + ex.getMessage());
        }
    }

    private void updateCardsAndCharts() {
        int total = 0, recruiters = 0, candidates = 0, admins = 0;
        int active = 0, inactive = 0;

        for (UserRow u : rows) {
            total++;
            String t = u.getType() == null ? "" : u.getType().trim().toUpperCase(Locale.ROOT);
            if (t.equals("RECRUITER")) recruiters++;
            else if (t.equals("CANDIDATE")) candidates++;
            else if (t.equals("ADMIN")) admins++;

            if (u.isActive()) active++; else inactive++;
        }

        lblTotalUsers.setText(String.valueOf(total));
        lblRecruiters.setText(String.valueOf(recruiters));
        lblCandidates.setText(String.valueOf(candidates));
        lblAdmins.setText(String.valueOf(admins));

        pieChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Recruiters", recruiters),
                new PieChart.Data("Candidates", candidates),
                new PieChart.Data("Admins", admins)
        ));

        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Users");
        series.getData().add(new XYChart.Data<>("Active", active));
        series.getData().add(new XYChart.Data<>("Inactive", inactive));
        barChart.getData().add(series);
    }

    // ===== Dialog form (ADMIN only) =====
    private static class UserForm {
        String firstName;
        String lastName;
        String email;
        String password; // required on add; optional on update
        String phone;
        boolean active;

        // admin extra
        String assignedArea;
    }

    private UserForm showUserDialog(String title, UserRow existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tfFirst = new TextField(existing == null ? "" : existing.getFirstName());
        TextField tfLast  = new TextField(existing == null ? "" : existing.getLastName());
        TextField tfEmail = new TextField(existing == null ? "" : existing.getEmail());
        PasswordField pfPass = new PasswordField();
        pfPass.setPromptText(existing == null ? "Password (required)" : "Leave blank to keep password");
        TextField tfPhone = new TextField(existing == null ? "" : existing.getPhone());

        ComboBox<String> cbAdminType = new ComboBox<>(
                FXCollections.observableArrayList("NORMAL ADMIN", "SUPER ADMIN")
        );
        cbAdminType.setValue("NORMAL ADMIN");
        if (existing != null) {
            String current = getAdminTypeFromDb(existing.getId()); // fetch assigned_area
            if (current != null && !current.isBlank()) {
                cbAdminType.setValue(current.trim().toUpperCase(Locale.ROOT).contains("SUPER") ? "SUPER ADMIN" : "NORMAL ADMIN");
            }
        }// default

        CheckBox cbActive = new CheckBox("Active");
        cbActive.setSelected(existing == null || existing.isActive());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.addRow(0, new Label("First name:"), tfFirst);
        grid.addRow(1, new Label("Last name:"), tfLast);
        grid.addRow(2, new Label("Email:"), tfEmail);
        grid.addRow(3, new Label("Password:"), pfPass);
        grid.addRow(4, new Label("Phone:"), tfPhone);
        grid.addRow(5, new Label("Admin type:"), cbAdminType);
        grid.addRow(6, new Label(""), cbActive);

        dialog.getDialogPane().setContent(grid);

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(ActionEvent.ACTION, ev -> {
            if (tfFirst.getText().trim().isEmpty()
                    || tfLast.getText().trim().isEmpty()
                    || tfEmail.getText().trim().isEmpty()) {
                ev.consume();
                new Alert(Alert.AlertType.WARNING, "First name, Last name, Email are required.").showAndWait();
                return;
            }
            if (existing == null && pfPass.getText().trim().isEmpty()) {
                ev.consume();
                new Alert(Alert.AlertType.WARNING, "Password is required for new users.").showAndWait();
            }
        });

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return null;

        UserForm form = new UserForm();
        form.firstName = tfFirst.getText().trim();
        form.lastName  = tfLast.getText().trim();
        form.email     = tfEmail.getText().trim();
        form.password  = pfPass.getText(); // can be blank on update
        form.phone     = tfPhone.getText().trim();
        form.active    = cbActive.isSelected();
        String choice = cbAdminType.getValue();     // "NORMAL ADMIN" or "SUPER ADMIN"
        form.assignedArea = choice;                 // ✅ store exactly in DB
        return form;
    }

    private void applyPermissions() {

        if (!bigAdmin) {

            // Hide Add Admin button
            // ⚠️ change fx:id if different in FXML
            if (btnAddUser != null) {
                btnAddUser.setVisible(false);
                btnAddUser.setManaged(false);
            }

            // Disable edit & delete
            btnEdit.setDisable(true);
            btnDelete.setDisable(true);

            statusLabel.setText("Normal Admin mode: limited permissions");
        }
    }

    private String getAdminTypeFromDb(long id) {
        try (PreparedStatement ps = cnx().prepareStatement("SELECT assigned_area FROM admin WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return rs.getString("assigned_area");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}