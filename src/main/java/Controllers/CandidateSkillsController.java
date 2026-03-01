package Controllers;

import Models.Candidate;
import Models.CandidateSkill;
import Models.SkillLevel;
import Services.CandidateSkillService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import Utils.Session;

import java.util.ArrayList;
import java.util.List;

import Services.EscoSkillApiService;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.util.Duration;

public class CandidateSkillsController {

    @FXML
    private VBox rowsBox;
    @FXML
    private Label lblStatus;

    private final CandidateSkillService service = new CandidateSkillService();
    private final EscoSkillApiService esco = new EscoSkillApiService();
    // each row = (TextField + ComboBox + remove button)
    private static class SkillRowUI {
        HBox root;
        TextField tfName;
        ComboBox<SkillLevel> cbLevel;
        Button btnRemove;
    }

    private final List<SkillRowUI> rowUIs = new ArrayList<>();

    @FXML
    public void initialize() {
        try {
            // ✅ IMPORTANT: only candidates can use this screen
            if (!(Session.getCurrentUser() instanceof Candidate)) {
                lblStatus.setText("Only candidates can manage skills.");
                rowsBox.setDisable(true);
                return;
            }

            long candidateId = Session.getUserId();
            List<CandidateSkill> existing = service.getByCandidate(candidateId);

            if (existing.isEmpty()) {
                // show 2 empty rows by default
                addRow(null, SkillLevel.BEGINNER);
                addRow(null, SkillLevel.BEGINNER);
                lblStatus.setText("Add your skills then Save ✅");
            } else {
                // pre-fill rows
                for (CandidateSkill s : existing) {
                    addRow(s.getSkillName(), s.getLevel());
                }
                lblStatus.setText("Loaded " + existing.size() + " skill(s) ✅");
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Load failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddRow() {
        // block non-candidates
        if (!(Session.getCurrentUser() instanceof Candidate)) {
            lblStatus.setText("Only candidates can add skills.");
            return;
        }
        addRow(null, SkillLevel.BEGINNER);
    }

    private void addRow(String skillName, SkillLevel level) {
        SkillRowUI ui = new SkillRowUI();
        ui.root = new HBox(10);

        ui.tfName = new TextField();
        ui.tfName.setPromptText("Skill name");
        ui.tfName.setPrefWidth(220);
        if (skillName != null) ui.tfName.setText(skillName);
        attachSkillAutocomplete(ui);
        ui.cbLevel = new ComboBox<>(FXCollections.observableArrayList(SkillLevel.values()));
        ui.cbLevel.setValue(level != null ? level : SkillLevel.BEGINNER);
        ui.cbLevel.setPrefWidth(170);

        ui.btnRemove = new Button("✖");
        ui.btnRemove.setOnAction(e -> removeRow(ui));
        ui.btnRemove.setStyle("-fx-background-color: transparent; -fx-text-fill: #dc3545; -fx-font-weight: bold;");

        ui.root.getChildren().addAll(ui.tfName, ui.cbLevel, ui.btnRemove);

        rowUIs.add(ui);
        rowsBox.getChildren().add(ui.root);
    }

    private void removeRow(SkillRowUI ui) {
        // block non-candidates
        if (!(Session.getCurrentUser() instanceof Candidate)) {
            lblStatus.setText("Only candidates can remove skills.");
            return;
        }

        rowsBox.getChildren().remove(ui.root);
        rowUIs.remove(ui);

        // if user deletes all rows accidentally, keep at least 1 row
        if (rowUIs.isEmpty()) {
            addRow(null, SkillLevel.BEGINNER);
        }
    }

    @FXML
    private void handleSave() {
        try {
            // ✅ IMPORTANT: only candidates can save
            if (!(Session.getCurrentUser() instanceof Candidate)) {
                lblStatus.setText("Only candidates can save skills.");
                return;
            }

            long candidateId = Session.getUserId();

            List<CandidateSkill> toSave = new ArrayList<>();
            for (SkillRowUI ui : rowUIs) {
                String name = ui.tfName.getText() == null ? "" : ui.tfName.getText().trim();
                SkillLevel level = ui.cbLevel.getValue();

                // ignore empty lines
                if (name.isEmpty()) continue;

                CandidateSkill s = new CandidateSkill(null, candidateId, name, level);
                toSave.add(s);
            }

            if (toSave.isEmpty()) {
                lblStatus.setText("Add at least 1 skill before saving.");
                return;
            }

            // Replace-all strategy
            service.replaceAll(candidateId, toSave);
            lblStatus.setText("Saved " + toSave.size() + " skill(s) ✅");

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Save failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteAll() {
        try {
            // ✅ IMPORTANT: only candidates can delete
            if (!(Session.getCurrentUser() instanceof Candidate)) {
                lblStatus.setText("Only candidates can delete skills.");
                return;
            }

            long candidateId = Session.getUserId();
            service.deleteAllForCandidate(candidateId);

            rowsBox.getChildren().clear();
            rowUIs.clear();

            // keep 2 empty rows again
            addRow(null, SkillLevel.BEGINNER);
            addRow(null, SkillLevel.BEGINNER);

            lblStatus.setText("All skills deleted ✅");
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Delete failed: " + e.getMessage());
        }
    }

    private void attachSkillAutocomplete(SkillRowUI ui) {
        ContextMenu menu = new ContextMenu();
        PauseTransition debounce = new PauseTransition(Duration.millis(250));

        ui.tfName.textProperty().addListener((obs, oldV, newV) -> {
            if (newV == null || newV.trim().length() < 2) {
                menu.hide();
                return;
            }
            debounce.stop();
            debounce.setOnFinished(e -> fetchSuggestions(ui, newV, menu));
            debounce.playFromStart();
        });

        ui.tfName.focusedProperty().addListener((obs, was, is) -> {
            if (!is) menu.hide();
        });
    }

    private void fetchSuggestions(SkillRowUI ui, String text, ContextMenu menu) {
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception {
                // change "en" to "fr" if you want
                return esco.suggestSkills(text, "en", 8);
            }
        };

        task.setOnSucceeded(e -> {
            List<String> suggestions = task.getValue();
            menu.getItems().clear();

            if (suggestions == null || suggestions.isEmpty()) {
                menu.hide();
                return;
            }

            for (String s : suggestions) {
                MenuItem item = new MenuItem(s);
                item.setOnAction(ev -> {
                    ui.tfName.setText(s);
                    ui.tfName.positionCaret(s.length());
                    menu.hide();
                });
                menu.getItems().add(item);
            }

            if (!menu.isShowing()) {
                menu.show(ui.tfName, Side.BOTTOM, 0, 0);
            }
        });

        task.setOnFailed(e -> {
            menu.hide();
            System.out.println("ESCO error: " + task.getException().getMessage());
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }
}