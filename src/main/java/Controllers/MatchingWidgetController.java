package Controllers;

import Models.*;
import Models.CandidateProfile.CandidateSkill;
import Services.MatchingService;
import Services.NominatimMapService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget pour afficher et configurer le profil candidat et le score de matching
 */
public class MatchingWidgetController {

    private MatchingService matchingService;
    private CandidateProfile candidateProfile;
    private Runnable onProfileUpdated;

    public MatchingWidgetController() {
        this.matchingService = new MatchingService();
        // Créer un profil candidat par défaut (à personnaliser)
        this.candidateProfile = new CandidateProfile();
    }

    /**
     * Définit le callback appelé quand le profil est mis à jour
     */
    public void setOnProfileUpdated(Runnable callback) {
        this.onProfileUpdated = callback;
    }

    /**
     * Retourne le profil candidat actuel
     */
    public CandidateProfile getCandidateProfile() {
        return candidateProfile;
    }

    /**
     * Définit le profil candidat
     */
    public void setCandidateProfile(CandidateProfile profile) {
        this.candidateProfile = profile;
    }

    /**
     * Calcule le score de matching pour une offre
     */
    public MatchingResult calculateMatch(JobOffer offer) {
        return matchingService.calculateMatch(candidateProfile, offer);
    }

    /**
     * Crée le widget d'affichage du score de matching
     */
    public VBox createMatchingScoreWidget(MatchingResult result) {
        VBox widget = new VBox(12);
        widget.setStyle("-fx-background-color: linear-gradient(to right, " + result.getScoreColor() + "15, white); " +
                       "-fx-background-radius: 12; -fx-padding: 18; " +
                       "-fx-border-color: " + result.getScoreColor() + "; -fx-border-radius: 12; -fx-border-width: 2;");

        // En-tête avec score principal
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        // Cercle de score
        StackPane scoreCircle = createScoreCircle(result.getOverallScore(), result.getScoreColor());

        // Informations textuelles
        VBox textInfo = new VBox(4);

        Label matchLabel = new Label(result.getLevelEmoji() + " " + result.getMatchLevel());
        matchLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: " + result.getScoreColor() + ";");

        Label descLabel = new Label(result.getMatchDescription());
        descLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #666;");
        descLabel.setWrapText(true);

        textInfo.getChildren().addAll(matchLabel, descLabel);
        HBox.setHgrow(textInfo, Priority.ALWAYS);

        header.getChildren().addAll(scoreCircle, textInfo);
        widget.getChildren().add(header);

        // Séparateur
        Separator sep = new Separator();
        widget.getChildren().add(sep);

        // Détails des scores par critère
        VBox detailsBox = new VBox(8);
        detailsBox.getChildren().add(createScoreBar("🎯 Compétences", result.getSkillsScore(), "#5BA3F5"));
        detailsBox.getChildren().add(createScoreBar("📍 Localisation", result.getLocationScore(), "#28a745"));
        detailsBox.getChildren().add(createScoreBar("💼 Type de contrat", result.getContractTypeScore(), "#ffc107"));
        detailsBox.getChildren().add(createScoreBar("📊 Expérience", result.getExperienceScore(), "#17a2b8"));

        widget.getChildren().add(detailsBox);

        return widget;
    }

    /**
     * Crée un cercle avec le pourcentage de score
     */
    private StackPane createScoreCircle(double score, String color) {
        StackPane stack = new StackPane();
        stack.setPrefSize(70, 70);

        // Cercle de fond
        Circle bgCircle = new Circle(32);
        bgCircle.setFill(Color.web("#f0f0f0"));
        bgCircle.setStroke(Color.web("#e0e0e0"));
        bgCircle.setStrokeWidth(3);

        // Arc de progression
        Arc progressArc = new Arc(0, 0, 32, 32, 90, -score * 3.6);
        progressArc.setType(ArcType.OPEN);
        progressArc.setFill(Color.TRANSPARENT);
        progressArc.setStroke(Color.web(color));
        progressArc.setStrokeWidth(5);

        // Texte du pourcentage
        Label scoreLabel = new Label(String.format("%.0f%%", score));
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: " + color + ";");

        stack.getChildren().addAll(bgCircle, progressArc, scoreLabel);

        return stack;
    }

    /**
     * Crée une barre de progression pour un critère
     */
    private HBox createScoreBar(String label, double score, String color) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057; -fx-min-width: 130;");

        // Barre de progression
        StackPane progressBar = new StackPane();
        progressBar.setPrefHeight(8);
        progressBar.setMinWidth(150);
        progressBar.setMaxWidth(200);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        Region bgBar = new Region();
        bgBar.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 4;");
        bgBar.prefWidthProperty().bind(progressBar.widthProperty());

        Region fillBar = new Region();
        fillBar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");
        fillBar.prefWidthProperty().bind(progressBar.widthProperty().multiply(score / 100));
        fillBar.setMaxWidth(Region.USE_PREF_SIZE);

        progressBar.getChildren().addAll(bgBar, fillBar);
        StackPane.setAlignment(fillBar, Pos.CENTER_LEFT);

        Label valueLabel = new Label(String.format("%.0f%%", score));
        valueLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " + color + "; -fx-min-width: 40;");

        bar.getChildren().addAll(nameLabel, progressBar, valueLabel);

        return bar;
    }

    /**
     * Affiche l'éditeur de profil candidat
     */
    public void showProfileEditor() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("📝 Configurer mon profil");
        dialog.setMinWidth(600);
        dialog.setMinHeight(700);

        VBox root = new VBox(20);
        root.setStyle("-fx-background-color: #f5f6f8; -fx-padding: 25;");

        // Titre
        Label title = new Label("🎯 Configurez votre profil pour le matching");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        Label subtitle = new Label("Ces informations seront utilisées pour calculer votre compatibilité avec les offres");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6c757d;");
        subtitle.setWrapText(true);

        root.getChildren().addAll(title, subtitle);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox formContent = new VBox(20);
        formContent.setStyle("-fx-padding: 10 5;");

        // Section Localisation
        VBox locationSection = createFormSection("📍 Localisation");
        TextField txtLocation = new TextField(candidateProfile.getLocation());
        txtLocation.setPromptText("Votre ville (ex: Tunis, Paris...)");
        txtLocation.setStyle("-fx-padding: 10; -fx-font-size: 14px;");
        locationSection.getChildren().add(txtLocation);
        formContent.getChildren().add(locationSection);

        // Section Type de contrat préféré
        VBox contractSection = createFormSection("💼 Types de contrat souhaités");
        FlowPane contractFlow = new FlowPane(10, 10);
        List<CheckBox> contractCheckboxes = new ArrayList<>();
        for (ContractType type : ContractType.values()) {
            CheckBox cb = new CheckBox(formatContractType(type));
            cb.setUserData(type);
            cb.setSelected(candidateProfile.getPreferredContractTypes().contains(type));
            cb.setStyle("-fx-font-size: 13px;");
            contractCheckboxes.add(cb);
            contractFlow.getChildren().add(cb);
        }
        contractSection.getChildren().add(contractFlow);
        formContent.getChildren().add(contractSection);

        // Section Expérience
        VBox experienceSection = createFormSection("📊 Années d'expérience");
        Spinner<Integer> spinnerExperience = new Spinner<>(0, 30, candidateProfile.getYearsOfExperience());
        spinnerExperience.setEditable(true);
        spinnerExperience.setStyle("-fx-font-size: 14px;");
        experienceSection.getChildren().add(spinnerExperience);
        formContent.getChildren().add(experienceSection);

        // Section Compétences
        VBox skillsSection = createFormSection("🎯 Vos compétences");
        VBox skillsContainer = new VBox(10);

        // Ajouter les compétences existantes
        for (CandidateSkill skill : candidateProfile.getSkills()) {
            HBox skillRow = createSkillRow(skill.getSkillName(), skill.getLevel(), skillsContainer);
            skillsContainer.getChildren().add(skillRow);
        }

        // Bouton pour ajouter une compétence
        Button btnAddSkill = new Button("+ Ajouter une compétence");
        btnAddSkill.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-padding: 8 15; " +
                           "-fx-background-radius: 6; -fx-cursor: hand;");
        btnAddSkill.setOnAction(e -> {
            HBox newSkillRow = createSkillRow("", SkillLevel.INTERMEDIATE, skillsContainer);
            skillsContainer.getChildren().add(skillsContainer.getChildren().size() - 1, newSkillRow);
        });

        skillsContainer.getChildren().add(btnAddSkill);
        skillsSection.getChildren().add(skillsContainer);
        formContent.getChildren().add(skillsSection);

        scrollPane.setContent(formContent);
        root.getChildren().add(scrollPane);

        // Boutons d'action
        HBox buttonBar = new HBox(15);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Annuler");
        btnCancel.setStyle("-fx-background-color: #e0e0e0; -fx-text-fill: #333; -fx-padding: 12 25; " +
                          "-fx-background-radius: 6; -fx-cursor: hand;");
        btnCancel.setOnAction(e -> dialog.close());

        Button btnSave = new Button("✓ Enregistrer mon profil");
        btnSave.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; -fx-font-weight: 700; " +
                        "-fx-padding: 12 25; -fx-background-radius: 6; -fx-cursor: hand;");
        btnSave.setOnAction(e -> {
            // Sauvegarder les données
            candidateProfile.setLocation(txtLocation.getText().trim());

            // Géocoder la localisation
            if (!txtLocation.getText().trim().isEmpty()) {
                NominatimMapService mapService = new NominatimMapService();
                NominatimMapService.GeoLocation geo = mapService.geocode(txtLocation.getText().trim());
                if (geo != null) {
                    candidateProfile.setLatitude(geo.getLatitude());
                    candidateProfile.setLongitude(geo.getLongitude());
                }
            }

            // Types de contrat
            candidateProfile.getPreferredContractTypes().clear();
            for (CheckBox cb : contractCheckboxes) {
                if (cb.isSelected()) {
                    candidateProfile.addPreferredContractType((ContractType) cb.getUserData());
                }
            }

            // Expérience
            candidateProfile.setYearsOfExperience(spinnerExperience.getValue());

            // Compétences
            candidateProfile.getSkills().clear();
            for (int i = 0; i < skillsContainer.getChildren().size() - 1; i++) {
                if (skillsContainer.getChildren().get(i) instanceof HBox skillRow) {
                    TextField nameField = (TextField) skillRow.getChildren().get(0);
                    @SuppressWarnings("unchecked")
                    ComboBox<SkillLevel> levelCombo = (ComboBox<SkillLevel>) skillRow.getChildren().get(1);

                    String skillName = nameField.getText().trim();
                    if (!skillName.isEmpty()) {
                        candidateProfile.addSkill(skillName, levelCombo.getValue());
                    }
                }
            }

            // Notifier que le profil a été mis à jour
            if (onProfileUpdated != null) {
                onProfileUpdated.run();
            }

            dialog.close();
        });

        buttonBar.getChildren().addAll(btnCancel, btnSave);
        root.getChildren().add(buttonBar);

        Scene scene = new Scene(root, 600, 700);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    /**
     * Crée une section de formulaire
     */
    private VBox createFormSection(String title) {
        VBox section = new VBox(10);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 15;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        section.getChildren().add(titleLabel);
        return section;
    }

    /**
     * Crée une ligne pour une compétence
     */
    private HBox createSkillRow(String skillName, SkillLevel level, VBox container) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        TextField nameField = new TextField(skillName);
        nameField.setPromptText("Nom de la compétence");
        nameField.setStyle("-fx-padding: 8; -fx-font-size: 13px;");
        HBox.setHgrow(nameField, Priority.ALWAYS);

        ComboBox<SkillLevel> levelCombo = new ComboBox<>();
        levelCombo.getItems().addAll(SkillLevel.values());
        levelCombo.setValue(level);
        levelCombo.setStyle("-fx-font-size: 13px;");
        levelCombo.setPrefWidth(130);

        Button btnRemove = new Button("✕");
        btnRemove.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-padding: 5 10; " +
                          "-fx-background-radius: 4; -fx-cursor: hand;");
        btnRemove.setOnAction(e -> container.getChildren().remove(row));

        row.getChildren().addAll(nameField, levelCombo, btnRemove);
        return row;
    }

    /**
     * Formate le type de contrat
     */
    private String formatContractType(ContractType type) {
        return switch (type) {
            case CDI -> "CDI";
            case CDD -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE -> "Freelance";
            case PART_TIME -> "Temps Partiel";
            case FULL_TIME -> "Temps Plein";
        };
    }

    /**
     * Crée un badge de score compact pour les cartes
     */
    public HBox createCompactScoreBadge(double score) {
        HBox badge = new HBox(5);
        badge.setAlignment(Pos.CENTER);
        badge.setStyle("-fx-background-color: " + getScoreColor(score) + "; -fx-background-radius: 12; -fx-padding: 4 10;");

        Label scoreLabel = new Label(String.format("%.0f%%", score));
        scoreLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: white;");

        badge.getChildren().add(scoreLabel);
        return badge;
    }

    private String getScoreColor(double score) {
        if (score >= 85) return "#28a745";
        if (score >= 70) return "#17a2b8";
        if (score >= 50) return "#ffc107";
        return "#dc3545";
    }
}


