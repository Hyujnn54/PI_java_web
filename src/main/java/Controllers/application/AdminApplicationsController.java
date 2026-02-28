package Controllers.application;

import Services.application.ApplicationService;
import Services.application.ApplicationStatusHistoryService;
import Services.application.GrokAIService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.awt.Desktop;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AdminApplicationsController {

    @FXML private VBox applicationListContainer;
    @FXML private VBox detailContainer;
    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cbSearchCriteria;
    @FXML private ComboBox<String> cbStatusFilter;
    @FXML private CheckBox chkShowArchived;
    @FXML private Label lblTotalCount;

    private ApplicationService.ApplicationRow selectedApplication;
    private List<ApplicationService.ApplicationRow> allApplications;

    @FXML
    public void initialize() {
        cbSearchCriteria.getItems().addAll("Nom du candidat", "Email", "Titre de l'offre", "Statut");
        cbSearchCriteria.setPromptText("Chercher par...");

        cbStatusFilter.getItems().addAll("Tous", "SUBMITTED", "IN_REVIEW", "SHORTLISTED", "INTERVIEW", "HIRED", "REJECTED");
        cbStatusFilter.setValue("Tous");
        cbStatusFilter.setOnAction(e -> applyFilters());

        chkShowArchived.setOnAction(e -> applyFilters());

        if (txtSearch != null) {
            txtSearch.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ENTER) applyFilters();
            });
        }

        loadAll();
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private void loadAll() {
        allApplications = ApplicationService.getAll();
        applyFilters();
    }

    private void applyFilters() {
        if (allApplications == null) return;

        List<ApplicationService.ApplicationRow> filtered = allApplications;

        // Archived filter
        boolean showArchived = chkShowArchived != null && chkShowArchived.isSelected();
        if (!showArchived) {
            filtered = filtered.stream().filter(a -> !a.isArchived()).collect(Collectors.toList());
        }

        // Status filter
        String statusFilter = cbStatusFilter != null ? cbStatusFilter.getValue() : "Tous";
        if (statusFilter != null && !"Tous".equals(statusFilter)) {
            filtered = filtered.stream()
                    .filter(a -> statusFilter.equals(a.currentStatus()))
                    .collect(Collectors.toList());
        }

        // Search filter
        String searchText = txtSearch != null ? txtSearch.getText().trim() : "";
        String criteria   = cbSearchCriteria != null ? cbSearchCriteria.getValue() : null;
        if (!searchText.isEmpty() && criteria != null) {
            String lower = searchText.toLowerCase();
            filtered = filtered.stream().filter(a -> switch (criteria) {
                case "Nom du candidat" -> a.candidateName() != null && a.candidateName().toLowerCase().contains(lower);
                case "Email"           -> a.candidateEmail() != null && a.candidateEmail().toLowerCase().contains(lower);
                case "Titre de l'offre"-> a.jobTitle() != null && a.jobTitle().toLowerCase().contains(lower);
                case "Statut"          -> a.currentStatus() != null && a.currentStatus().toLowerCase().contains(lower);
                default                -> true;
            }).collect(Collectors.toList());
        }

        renderList(filtered);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    private void renderList(List<ApplicationService.ApplicationRow> apps) {
        if (applicationListContainer == null) return;
        applicationListContainer.getChildren().clear();
        selectedApplication = null;

        if (lblTotalCount != null) {
            lblTotalCount.setText(apps.size() + " candidature(s) trouvée(s)");
        }

        if (apps.isEmpty()) {
            Label empty = new Label("Aucune candidature trouvée.");
            empty.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 13px; -fx-padding: 20;");
            applicationListContainer.getChildren().add(empty);
            if (detailContainer != null) {
                detailContainer.getChildren().clear();
                Label lbl = new Label("← Sélectionnez une candidature");
                lbl.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 15px; -fx-padding: 40 0 0 20;");
                detailContainer.getChildren().add(lbl);
            }
            return;
        }

        boolean first = true;
        for (ApplicationService.ApplicationRow app : apps) {
            VBox card = buildCard(app);
            applicationListContainer.getChildren().add(card);
            if (first) { selectApplication(app, card); first = false; }
        }
    }

    private VBox buildCard(ApplicationService.ApplicationRow app) {
        String normalStyle = "-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 10; "
                + "-fx-padding: 12 14; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);";
        String hoverStyle  = "-fx-background-color: #F7FBFF; -fx-background-radius: 10; "
                + "-fx-border-color: #5BA3F5; -fx-border-width: 1.5; -fx-border-radius: 10; "
                + "-fx-padding: 12 14; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.18),10,0,0,3);";

        VBox card = new VBox(4);
        card.setStyle(normalStyle);
        card.setUserData(app);
        card.setOnMouseEntered(e -> { if (!card.getStyleClass().contains("admin-card-selected")) card.setStyle(hoverStyle); });
        card.setOnMouseExited (e -> { if (!card.getStyleClass().contains("admin-card-selected")) card.setStyle(normalStyle); });

        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        // Avatar
        Label avatar = new Label(getInitials(app.candidateName()));
        avatar.setStyle("-fx-background-color: #EBF3FE; -fx-text-fill: #5BA3F5; "
                + "-fx-font-weight: 700; -fx-font-size: 12px; -fx-alignment: center; "
                + "-fx-min-width: 36; -fx-max-width: 36; -fx-min-height: 36; -fx-max-height: 36; "
                + "-fx-background-radius: 18;");

        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(app.candidateName() != null && !app.candidateName().isBlank()
                ? app.candidateName() : "Candidat #" + app.id());
        nameLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        Label offerLabel = new Label(app.jobTitle() != null ? app.jobTitle() : "—");
        offerLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");

        HBox badges = new HBox(6);
        badges.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = new Label(translateStatus(app.currentStatus()));
        statusBadge.setStyle("-fx-padding: 2 8; -fx-background-radius: 20; -fx-font-size: 10px; -fx-font-weight: 600; "
                + statusStyle(app.currentStatus()));
        badges.getChildren().add(statusBadge);

        if (app.isArchived()) {
            Label archBadge = new Label("ARCHIVÉ");
            archBadge.setStyle("-fx-padding: 2 8; -fx-background-color: #6c757d; -fx-text-fill: white; "
                    + "-fx-background-radius: 20; -fx-font-size: 10px;");
            badges.getChildren().add(archBadge);
        }

        Label dateLabel = new Label(app.appliedAt() != null
                ? app.appliedAt().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "");
        dateLabel.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 10px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        info.getChildren().addAll(nameLabel, offerLabel, new HBox(spacer, badges, dateLabel) {{ setAlignment(Pos.CENTER_LEFT); }});
        row.getChildren().addAll(avatar, info);
        card.getChildren().add(row);

        card.setOnMouseClicked(e -> selectApplication(app, card));
        return card;
    }

    private void selectApplication(ApplicationService.ApplicationRow app, VBox card) {
        String normalStyle = "-fx-background-color: white; -fx-background-radius: 10; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 10; "
                + "-fx-padding: 12 14; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);";
        String selectedStyle = "-fx-background-color: #EBF3FE; -fx-background-radius: 10; "
                + "-fx-border-color: #5BA3F5; -fx-border-width: 2; -fx-border-radius: 10; "
                + "-fx-padding: 12 14; -fx-cursor: hand; "
                + "-fx-effect: dropshadow(gaussian,rgba(91,163,245,0.25),12,0,0,3);";

        applicationListContainer.getChildren().forEach(n -> {
            if (n instanceof VBox v) {
                v.getStyleClass().remove("admin-card-selected");
                v.setStyle(normalStyle);
            }
        });
        card.getStyleClass().add("admin-card-selected");
        card.setStyle(selectedStyle);
        selectedApplication = app;
        showDetail(app);
    }

    // -------------------------------------------------------------------------
    // Detail panel
    // -------------------------------------------------------------------------

    private void showDetail(ApplicationService.ApplicationRow app) {
        if (detailContainer == null) return;
        detailContainer.getChildren().clear();

        // ── Header card ──────────────────────────────────────────────────────
        VBox headerBox = new VBox(10);
        headerBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                + "-fx-padding: 20; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.05),8,0,0,2);");

        HBox nameRow = new HBox(14);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label avatar = new Label(getInitials(app.candidateName()));
        avatar.setStyle("-fx-background-color: #EBF3FE; -fx-text-fill: #5BA3F5; "
                + "-fx-font-weight: 700; -fx-font-size: 18px; -fx-alignment: center; "
                + "-fx-min-width: 54; -fx-max-width: 54; -fx-min-height: 54; -fx-max-height: 54; "
                + "-fx-background-radius: 27;");
        VBox nameBox = new VBox(4);
        Label nameLabel = new Label(app.candidateName() != null && !app.candidateName().isBlank()
                ? app.candidateName() : "Candidat #" + app.id());
        nameLabel.setStyle("-fx-font-weight: 700; -fx-font-size: 18px; -fx-text-fill: #2c3e50;");
        Label offerLabel = new Label(app.jobTitle() != null ? app.jobTitle() : "—");
        offerLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        nameBox.getChildren().addAll(nameLabel, offerLabel);
        nameRow.getChildren().addAll(avatar, nameBox);

        HBox infoGrid = new HBox(30);
        infoGrid.setStyle("-fx-padding: 8 0 0 0;");
        infoGrid.getChildren().addAll(
            detailItem("📧 Email",   app.candidateEmail() != null ? app.candidateEmail() : "N/A"),
            detailItem("📞 Tél.",    app.phone()          != null ? app.phone()          : "N/A"),
            detailItem("📅 Posté",   app.appliedAt()      != null
                    ? app.appliedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "N/A"),
            detailItem("🆔 ID",      "#" + app.id())
        );

        Label statusBadge = new Label(translateStatus(app.currentStatus()));
        statusBadge.setStyle("-fx-padding: 5 14; -fx-background-radius: 20; -fx-font-weight: 700; -fx-font-size: 12px; "
                + statusStyle(app.currentStatus()));

        headerBox.getChildren().addAll(nameRow, infoGrid, statusBadge);
        detailContainer.getChildren().add(headerBox);

        // ── Admin Actions ────────────────────────────────────────────────────
        VBox actionsBox = new VBox(10);
        actionsBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                + "-fx-padding: 16; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");

        Label actionsTitle = new Label("⚙ Actions Admin");
        actionsTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        // Status change row
        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label statusLbl = new Label("Changer le statut :");
        statusLbl.setStyle("-fx-font-weight: 600; -fx-font-size: 13px;");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        statusCombo.setValue(app.currentStatus());
        statusCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : translateStatus(item)); }
        });
        statusCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty); setText(empty || item == null ? null : translateStatus(item)); }
        });
        statusCombo.setPrefWidth(220);

        Button btnUpdateStatus = new Button("✔ Mettre à jour");
        btnUpdateStatus.setStyle("-fx-padding: 8 16; -fx-background-color: #28a745; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-background-radius: 6; -fx-font-weight: 600;");
        btnUpdateStatus.setOnAction(e -> {
            String newStatus = statusCombo.getValue();
            if (newStatus != null) {
                ApplicationService.updateStatus(app.id(), newStatus,
                        UserContext.getAdminId(), "Statut mis à jour par l'admin");
                showAlert("Succès", "Statut mis à jour : " + translateStatus(newStatus), Alert.AlertType.INFORMATION);
                refreshAfterAction();
            }
        });
        statusRow.getChildren().addAll(statusLbl, statusCombo, btnUpdateStatus);

        // Archive / Delete row
        HBox archiveDeleteRow = new HBox(10);
        archiveDeleteRow.setAlignment(Pos.CENTER_LEFT);

        Button btnArchive = new Button(app.isArchived() ? "📤 Désarchiver" : "📥 Archiver");
        btnArchive.setStyle("-fx-padding: 8 16; -fx-background-color: #6c757d; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-background-radius: 6; -fx-font-weight: 600;");
        btnArchive.setOnAction(e -> {
            boolean toArchive = !app.isArchived();
            ApplicationService.setArchived(app.id(), toArchive, UserContext.getAdminId());
            showAlert("Succès",
                    toArchive ? "Candidature archivée." : "Candidature désarchivée.",
                    Alert.AlertType.INFORMATION);
            refreshAfterAction();
        });

        Button btnDelete = new Button("🗑 Supprimer définitivement");
        btnDelete.setStyle("-fx-padding: 8 16; -fx-background-color: #dc3545; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-background-radius: 6; -fx-font-weight: 600;");
        btnDelete.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Supprimer définitivement cette candidature ? Cette action est irréversible.",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirmer la suppression");
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.YES) {
                    try {
                        ApplicationService.delete(app.id());
                        showAlert("Succès", "Candidature supprimée.", Alert.AlertType.INFORMATION);
                        refreshAfterAction();
                    } catch (Exception ex) {
                        showAlert("Erreur", "Erreur lors de la suppression : " + ex.getMessage(), Alert.AlertType.ERROR);
                    }
                }
            });
        });

        archiveDeleteRow.getChildren().addAll(btnArchive, btnDelete);
        actionsBox.getChildren().addAll(actionsTitle, statusRow, archiveDeleteRow);
        detailContainer.getChildren().add(actionsBox);

        // ── Cover Letter ─────────────────────────────────────────────────────
        if (app.coverLetter() != null && !app.coverLetter().isBlank()) {
            VBox clBox = new VBox(8);
            clBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                    + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                    + "-fx-padding: 16; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");

            HBox clHeader = new HBox(10);
            clHeader.setAlignment(Pos.CENTER_LEFT);
            Label clTitle = new Label("Lettre de motivation :");
            clTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

            final String originalText = app.coverLetter();
            TextArea clArea = new TextArea(originalText);
            clArea.setEditable(false);
            clArea.setWrapText(true);
            clArea.setPrefRowCount(6);
            clArea.setStyle("-fx-control-inner-background: #f8f9fa; -fx-font-size: 12px;");

            Button btnTranslate = new Button("🌐 Traduire");
            btnTranslate.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; "
                    + "-fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 4;");

            Button btnOriginal = new Button("🔄 Original");
            btnOriginal.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; "
                    + "-fx-font-size: 11px; -fx-padding: 4 10; -fx-cursor: hand; -fx-background-radius: 4;");
            btnOriginal.setDisable(true);

            btnOriginal.setOnAction(e -> {
                clArea.setText(originalText);
                clTitle.setText("Lettre de motivation :");
                btnOriginal.setDisable(true);
            });

            btnTranslate.setOnAction(e -> {
                ChoiceDialog<String> dlg = new ChoiceDialog<>("Français", "Français", "Anglais", "Arabe");
                dlg.setTitle("Traduire");
                dlg.setHeaderText("Sélectionnez la langue cible");
                dlg.setContentText("Langue :");
                dlg.showAndWait().ifPresent(lang -> {
                    String apiLang = switch (lang) {
                        case "Français" -> "French";
                        case "Anglais"  -> "English";
                        case "Arabe"    -> "Arabic";
                        default         -> lang;
                    };
                    btnTranslate.setText("⏳ ...");
                    btnTranslate.setDisable(true);
                    javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
                        @Override protected String call() {
                            return GrokAIService.translateCoverLetter(originalText, apiLang);
                        }
                    };
                    task.setOnSucceeded(ev -> {
                        String translated = task.getValue();
                        if (translated != null && !translated.isEmpty()) {
                            clArea.setText(translated);
                            clTitle.setText("Lettre de motivation (" + lang + ") :");
                            btnOriginal.setDisable(false);
                        }
                        btnTranslate.setText("🌐 Traduire");
                        btnTranslate.setDisable(false);
                    });
                    task.setOnFailed(ev -> {
                        btnTranslate.setText("🌐 Traduire");
                        btnTranslate.setDisable(false);
                        showAlert("Erreur", "La traduction a échoué.", Alert.AlertType.ERROR);
                    });
                    new Thread(task).start();
                });
            });

            clHeader.getChildren().addAll(clTitle, btnTranslate, btnOriginal);
            clBox.getChildren().addAll(clHeader, clArea);
            detailContainer.getChildren().add(clBox);
        }

        // ── CV Download ──────────────────────────────────────────────────────
        if (app.cvPath() != null && !app.cvPath().isBlank()) {
            HBox cvRow = new HBox(10);
            cvRow.setAlignment(Pos.CENTER_LEFT);
            cvRow.setStyle("-fx-background-color: white; -fx-background-radius: 10; "
                    + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 10; "
                    + "-fx-padding: 12 16;");
            Label cvLbl = new Label("📄 CV : " + app.cvPath());
            cvLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 12px;");
            HBox.setHgrow(cvLbl, Priority.ALWAYS);
            Button btnDl = new Button("📥 Télécharger");
            btnDl.setStyle("-fx-padding: 5 12; -fx-background-color: #28a745; -fx-text-fill: white; "
                    + "-fx-cursor: hand; -fx-background-radius: 6;");
            btnDl.setOnAction(e -> downloadCV(app));
            cvRow.getChildren().addAll(cvLbl, btnDl);
            detailContainer.getChildren().add(cvRow);
        }

        // ── Status History ───────────────────────────────────────────────────
        VBox historyBox = new VBox(10);
        historyBox.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e9ecef; -fx-border-width: 1; -fx-border-radius: 12; "
                + "-fx-padding: 16; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.04),6,0,0,2);");

        HBox historyHeader = new HBox(10);
        historyHeader.setAlignment(Pos.CENTER_LEFT);
        Label histTitle = new Label("📜 Historique des statuts");
        histTitle.setStyle("-fx-font-weight: 700; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        Button btnAddHistory = new Button("+ Ajouter");
        btnAddHistory.setStyle("-fx-padding: 4 10; -fx-background-color: #5BA3F5; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-background-radius: 6; -fx-font-size: 11px;");
        btnAddHistory.setOnAction(e -> showAddHistoryDialog(app));

        Region histSpacer = new Region(); HBox.setHgrow(histSpacer, Priority.ALWAYS);
        historyHeader.getChildren().addAll(histTitle, histSpacer, btnAddHistory);
        historyBox.getChildren().add(historyHeader);

        List<ApplicationStatusHistoryService.StatusHistoryRow> history =
                ApplicationStatusHistoryService.getByApplicationId(app.id());

        if (history.isEmpty()) {
            Label noHist = new Label("Aucun historique disponible.");
            noHist.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 12px;");
            historyBox.getChildren().add(noHist);
        } else {
            for (ApplicationStatusHistoryService.StatusHistoryRow record : history) {
                historyBox.getChildren().add(buildHistoryItem(record));
            }
        }

        detailContainer.getChildren().add(historyBox);
    }

    // ── History item card ────────────────────────────────────────────────────

    private VBox buildHistoryItem(ApplicationStatusHistoryService.StatusHistoryRow record) {
        VBox item = new VBox(4);
        item.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; "
                + "-fx-border-color: #dee2e6; -fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 10 12;");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label statusLbl = new Label(translateStatus(record.status()));
        statusLbl.setStyle("-fx-font-weight: 700; -fx-font-size: 12px; -fx-padding: 2 8; "
                + "-fx-background-radius: 20; " + statusStyle(record.status()));

        Label dateLbl = new Label(record.changedAt() != null
                ? record.changedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A");
        dateLbl.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 11px;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnEdit = new Button("✏");
        btnEdit.setStyle("-fx-padding: 3 7; -fx-background-color: #5BA3F5; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px;");
        btnEdit.setOnAction(e -> showEditHistoryDialog(record));

        Button btnDel = new Button("🗑");
        btnDel.setStyle("-fx-padding: 3 7; -fx-background-color: #dc3545; -fx-text-fill: white; "
                + "-fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px;");
        btnDel.setOnAction(e -> confirmDeleteHistory(record));

        topRow.getChildren().addAll(statusLbl, dateLbl, sp, btnEdit, btnDel);
        item.getChildren().add(topRow);

        if (record.note() != null && !record.note().isBlank()) {
            Label noteLbl = new Label("📝 " + record.note());
            noteLbl.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
            noteLbl.setWrapText(true);
            item.getChildren().add(noteLbl);
        }
        return item;
    }

    // -------------------------------------------------------------------------
    // History CRUD dialogs
    // -------------------------------------------------------------------------

    private void showAddHistoryDialog(ApplicationService.ApplicationRow app) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter un historique");
        dialog.setHeaderText("Ajouter une entrée de statut pour #" + app.id());

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        statusCombo.setPromptText("Sélectionner un statut");
        statusCombo.setPrefWidth(250);

        TextArea noteArea = new TextArea();
        noteArea.setPromptText("Note (optionnel)");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);

        content.getChildren().addAll(
                new Label("Statut :"), statusCombo,
                new Label("Note :"), noteArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String status = statusCombo.getValue();
                if (status == null) { showAlert("Avertissement", "Veuillez sélectionner un statut.", Alert.AlertType.WARNING); return; }
                ApplicationStatusHistoryService.addStatusHistory(
                        app.id(), status, UserContext.getAdminId(), noteArea.getText().trim());
                // Optionally sync application status
                ApplicationService.updateStatus(app.id(), status);
                showAlert("Succès", "Historique ajouté.", Alert.AlertType.INFORMATION);
                refreshAfterAction();
            }
        });
    }

    private void showEditHistoryDialog(ApplicationStatusHistoryService.StatusHistoryRow record) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier l'historique");
        dialog.setHeaderText("Modifier l'entrée #" + record.id());

        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("SUBMITTED", "IN_REVIEW", "SHORTLISTED", "REJECTED", "INTERVIEW", "HIRED");
        statusCombo.setValue(record.status());
        statusCombo.setPrefWidth(250);

        TextArea noteArea = new TextArea(record.note() != null ? record.note() : "");
        noteArea.setPromptText("Note (optionnel)");
        noteArea.setPrefRowCount(3);
        noteArea.setWrapText(true);

        content.getChildren().addAll(
                new Label("Statut :"), statusCombo,
                new Label("Note :"), noteArea
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String newStatus = statusCombo.getValue();
                String newNote   = noteArea.getText().trim();
                if (newStatus == null) { showAlert("Avertissement", "Veuillez sélectionner un statut.", Alert.AlertType.WARNING); return; }
                ApplicationStatusHistoryService.updateStatusHistory(record.id(), newStatus, newNote);
                showAlert("Succès", "Historique mis à jour.", Alert.AlertType.INFORMATION);
                refreshAfterAction();
            }
        });
    }

    private void confirmDeleteHistory(ApplicationStatusHistoryService.StatusHistoryRow record) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Supprimer cette entrée d'historique ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmer");
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                ApplicationStatusHistoryService.deleteStatusHistory(record.id());
                showAlert("Succès", "Entrée supprimée.", Alert.AlertType.INFORMATION);
                refreshAfterAction();
            }
        });
    }

    // -------------------------------------------------------------------------
    // FXML action handlers
    // -------------------------------------------------------------------------

    @FXML
    private void handleSearch() { applyFilters(); }

    @FXML
    private void handleClear() {
        if (txtSearch != null) txtSearch.clear();
        if (cbSearchCriteria != null) cbSearchCriteria.getSelectionModel().clearSelection();
        if (cbStatusFilter != null) cbStatusFilter.setValue("Tous");
        if (chkShowArchived != null) chkShowArchived.setSelected(false);
        loadAll();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void refreshAfterAction() {
        Long selectedId = selectedApplication != null ? selectedApplication.id() : null;
        loadAll();
        // Re-select the same application if it still exists
        if (selectedId != null) {
            for (javafx.scene.Node node : applicationListContainer.getChildren()) {
                if (node instanceof VBox card && card.getUserData() instanceof ApplicationService.ApplicationRow row
                        && row.id().equals(selectedId)) {
                    selectApplication(ApplicationService.getById(selectedId), card);
                    break;
                }
            }
        }
    }

    private void downloadCV(ApplicationService.ApplicationRow app) {
        try {
            java.io.File f = ApplicationService.downloadPDF(app.id());
            if (f != null && f.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
            }
        } catch (Exception ex) {
            showAlert("Erreur", "Impossible d'ouvrir le CV : " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private VBox detailItem(String label, String value) {
        VBox box = new VBox(3);
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d; -fx-font-weight: 600;");
        Label val = new Label(value);
        val.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
        val.setWrapText(true);
        box.getChildren().addAll(lbl, val);
        return box;
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }

    private String translateStatus(String status) {
        if (status == null) return "Soumise";
        return switch (status.toUpperCase()) {
            case "SUBMITTED"   -> "Soumise";
            case "IN_REVIEW"   -> "En révision";
            case "SHORTLISTED" -> "Présélectionné(e)";
            case "INTERVIEW"   -> "Entretien planifié";
            case "HIRED"       -> "Embauché(e)";
            case "REJECTED"    -> "Rejetée";
            case "ARCHIVED"    -> "Archivée";
            case "UNARCHIVED"  -> "Désarchivée";
            default -> status;
        };
    }

    private String statusStyle(String status) {
        if (status == null) return "-fx-background-color: #e9ecef; -fx-text-fill: #6c757d;";
        return switch (status.toUpperCase()) {
            case "SUBMITTED"   -> "-fx-background-color: #E3F2FD; -fx-text-fill: #1565C0;";
            case "IN_REVIEW"   -> "-fx-background-color: #FFF8E1; -fx-text-fill: #E65100;";
            case "SHORTLISTED" -> "-fx-background-color: #E8F5E9; -fx-text-fill: #2E7D32;";
            case "INTERVIEW"   -> "-fx-background-color: #EDE7F6; -fx-text-fill: #4527A0;";
            case "HIRED"       -> "-fx-background-color: #E0F2F1; -fx-text-fill: #00695C;";
            case "REJECTED"    -> "-fx-background-color: #FFEBEE; -fx-text-fill: #B71C1C;";
            default            -> "-fx-background-color: #e9ecef; -fx-text-fill: #6c757d;";
        };
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}








