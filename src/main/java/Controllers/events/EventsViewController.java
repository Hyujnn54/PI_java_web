package Controllers.events;

import Models.events.*;
import Services.events.EventRegistrationService;
import Services.events.RecruitmentEventService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.animation.*;
import javafx.util.Duration;

import java.net.URL;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EventsViewController implements Initializable {

    @FXML private VBox eventsContainer;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label lblCount;
    @FXML private VBox detailPlaceholder;
    @FXML private VBox detailContent;
    @FXML private Label detailTitle;
    @FXML private Label detailType;
    @FXML private Label detailDescription;
    @FXML private Label detailLocation;
    @FXML private Label detailDate;
    @FXML private Label detailCapacity;
    @FXML private Label detailMeetLink;
    @FXML private HBox  detailMeetLinkRow;
    @FXML private Label lblRegistrationStatus;
    @FXML private Button btnApply;
    @FXML private Button btnCancel;
    @FXML private Button btnAnalyze;
    
    @FXML private VBox aiAnalysisContainer;
    @FXML private Label aiLoadingLabel;
    @FXML private VBox aiContentBox;
    @FXML private Label aiProsLabel;
    @FXML private Label aiConsLabel;

    private final RecruitmentEventService eventService = new RecruitmentEventService();
    private final EventRegistrationService registrationService = new EventRegistrationService();
    private final Services.events.CandidateService candidateService = new Services.events.CandidateService();

    // Cache pour l'IA
    private String lastAnalyzedEventId = null;

    private List<RecruitmentEvent> allEvents = new ArrayList<>();
    private RecruitmentEvent selectedEvent = null;
    private long currentCandidateId = -1;
    private boolean isRecruiter = false;
    private boolean isAdmin = false;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Determine role
        UserContext.Role role = UserContext.getRole();
        isRecruiter = (role == UserContext.Role.RECRUITER);
        isAdmin     = (role == UserContext.Role.ADMIN);
        Long uid = UserContext.getUserId();
        if (uid != null) currentCandidateId = uid;

        // Populate type filter
        typeFilter.getItems().addAll("Tous", "Job_Faire", "WEBINAIRE", "Interview day");
        typeFilter.setValue("Tous");

        loadEvents();
    }

    private void loadEvents() {
        try {
            allEvents = eventService.getAll();
        } catch (SQLException e) {
            allEvents = new ArrayList<>();
            System.err.println("Error loading events: " + e.getMessage());
        }
        renderEvents(allEvents);
    }

    private void renderEvents(List<RecruitmentEvent> events) {
        eventsContainer.getChildren().clear();
        lblCount.setText(events.size() + " événement(s)");

        for (int i = 0; i < events.size(); i++) {
            RecruitmentEvent ev = events.get(i);
            VBox card = buildEventCard(ev);

            // Stagger animation
            card.setOpacity(0);
            card.setTranslateY(10);
            Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(i * 60),
                    new KeyValue(card.opacityProperty(), 0),
                    new KeyValue(card.translateYProperty(), 10)),
                new KeyFrame(Duration.millis(i * 60 + 220),
                    new KeyValue(card.opacityProperty(), 1),
                    new KeyValue(card.translateYProperty(), 0))
            );
            tl.play();

            eventsContainer.getChildren().add(card);
        }

        if (events.isEmpty()) {
            Label empty = new Label("Aucun événement trouvé");
            empty.setStyle("-fx-font-size:14px; -fx-text-fill:#8FA3B8; -fx-padding:40;");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            eventsContainer.getChildren().add(empty);
        }
    }

    private VBox buildEventCard(RecruitmentEvent ev) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14;" +
                      "-fx-border-color: #E4EBF5; -fx-border-width: 1; -fx-border-radius: 14;" +
                      "-fx-padding: 18 20; -fx-cursor: hand;" +
                      "-fx-effect: dropshadow(gaussian, rgba(100,150,220,0.06), 8,0,0,2);");

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(ev.getEventType() != null ? ev.getEventType() : "");
        typeBadge.setStyle("-fx-background-color: #EBF3FF; -fx-text-fill: #1565C0;" +
                           "-fx-font-size: 10px; -fx-font-weight: 700;" +
                           "-fx-padding: 3 8; -fx-background-radius: 6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLbl = new Label(ev.getEventDate() != null ? ev.getEventDate().format(FMT) : "");
        dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #8FA3B8;");

        topRow.getChildren().addAll(typeBadge, spacer, dateLbl);

        Label titleLbl = new Label(ev.getTitle());
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #1E293B;");
        titleLbl.setWrapText(true);

        Label descLbl = new Label(ev.getDescription() != null ? ev.getDescription() : "");
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");
        descLbl.setWrapText(true);
        descLbl.setMaxHeight(36);

        HBox infoRow = new HBox(16);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        Label locLbl = new Label("📍 " + (ev.getLocation() != null ? ev.getLocation() : ""));
        locLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        Label capLbl = new Label("👥 " + ev.getCapacity() + " places");
        capLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #475569;");
        infoRow.getChildren().addAll(locLbl, capLbl);

        card.getChildren().addAll(topRow, titleLbl, descLbl, infoRow);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace(
                "-fx-background-color: white;", "-fx-background-color: #F7FAFF;")));
        card.setOnMouseExited(e -> {
            if (selectedEvent == null || selectedEvent.getId() != ev.getId()) {
                card.setStyle(card.getStyle().replace(
                        "-fx-background-color: #F7FAFF;", "-fx-background-color: white;")
                        .replace("-fx-border-color: #1565C0;", "-fx-border-color: #E4EBF5;"));
            }
        });

        card.setOnMouseClicked(e -> selectEvent(ev, card));
        return card;
    }

    private void selectEvent(RecruitmentEvent ev, VBox card) {
        selectedEvent = ev;

        // Reset all cards style
        for (Node n : eventsContainer.getChildren()) {
            if (n instanceof VBox c) {
                String s = c.getStyle();
                s = s.replace("-fx-background-color: #EBF3FF;", "-fx-background-color: white;");
                s = s.replace("-fx-border-color: #1565C0;", "-fx-border-color: #E4EBF5;");
                c.setStyle(s);
            }
        }
        // Highlight selected
        String s = card.getStyle();
        s = s.replace("-fx-background-color: white;", "-fx-background-color: #EBF3FF;");
        s = s.replace("-fx-border-color: #E4EBF5;", "-fx-border-color: #1565C0;");
        card.setStyle(s);

        // Show detail panel
        showDetail(ev);
    }

    private void showDetail(RecruitmentEvent ev) {
        detailPlaceholder.setVisible(false);
        detailPlaceholder.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);

        // Reset AI analysis panel
        if (aiAnalysisContainer != null) {
            aiAnalysisContainer.setVisible(false);
            aiAnalysisContainer.setManaged(false);
            aiProsLabel.setText("");
            aiConsLabel.setText("");
            lastAnalyzedEventId = null;
        }

        detailTitle.setText(ev.getTitle());
        detailType.setText(ev.getEventType() != null ? ev.getEventType() : "");
        detailDescription.setText(ev.getDescription() != null ? ev.getDescription() : "");
        detailLocation.setText(ev.getLocation() != null ? ev.getLocation() : "");
        detailDate.setText(ev.getEventDate() != null ? ev.getEventDate().format(FMT) : "");
        detailCapacity.setText(ev.getCapacity() + " places disponibles");

        // Meet link — NEVER show to candidates; they only receive it via email on CONFIRMED status
        if (detailMeetLinkRow != null) {
            detailMeetLinkRow.setVisible(false);
            detailMeetLinkRow.setManaged(false);
        }

        // Hide status message
        lblRegistrationStatus.setVisible(false);
        lblRegistrationStatus.setManaged(false);

        if (isAdmin) {
            // Admin: read-only view
            btnApply.setVisible(false); btnApply.setManaged(false);
            btnCancel.setVisible(false); btnCancel.setManaged(false);
            if(btnAnalyze != null) { btnAnalyze.setVisible(false); btnAnalyze.setManaged(false); }
        } else if (isRecruiter) {
            // Recruiter: also read-only in main shell view
            btnApply.setVisible(false); btnApply.setManaged(false);
            btnCancel.setVisible(false); btnCancel.setManaged(false);
            if(btnAnalyze != null) { btnAnalyze.setVisible(false); btnAnalyze.setManaged(false); }
        } else {
            // Candidate: check if already registered
            btnApply.setVisible(true); btnApply.setManaged(true);
            if(btnAnalyze != null) { btnAnalyze.setVisible(true); btnAnalyze.setManaged(true); }
            if (currentCandidateId > 0) {
                try {
                    boolean already = registrationService.isAlreadyRegistered(ev.getId(), currentCandidateId);
                    if (already) {
                        btnApply.setDisable(true);
                        btnApply.setText("✅  Déjà inscrit");
                        btnApply.setStyle(btnApply.getStyle().replace(
                                "-fx-background-color: #1565C0;", "-fx-background-color: #9CA3AF;"));
                        btnCancel.setVisible(true); btnCancel.setManaged(true);
                    } else {
                        btnApply.setDisable(false);
                        btnApply.setText("✅  S'inscrire");
                        btnApply.setStyle(btnApply.getStyle().replace(
                                "-fx-background-color: #9CA3AF;", "-fx-background-color: #1565C0;"));
                        btnCancel.setVisible(false); btnCancel.setManaged(false);
                    }
                } catch (SQLException e) {
                    System.err.println("Error checking registration: " + e.getMessage());
                }
            }
        }
    }

    @FXML
    private void handleRefresh() {
        loadEvents();
        detailPlaceholder.setVisible(true); detailPlaceholder.setManaged(true);
        detailContent.setVisible(false); detailContent.setManaged(false);
        lblRegistrationStatus.setVisible(false);
        lblRegistrationStatus.setManaged(false);

        // Reset AI
        if (aiAnalysisContainer != null) {
            aiAnalysisContainer.setVisible(false);
            aiAnalysisContainer.setManaged(false);
            lastAnalyzedEventId = null;
        }

        selectedEvent = null;

        // Reset sidebar selection
        for (Node n : eventsContainer.getChildren()) {
            if (n instanceof VBox c) {
                String style = c.getStyle();
                style = style.replace("-fx-background-color: #EBF3FF;", "-fx-background-color: white;");
                style = style.replace("-fx-border-color: #1565C0;", "-fx-border-color: #E4EBF5;");
                c.setStyle(style);
            }
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleFilter() {
        applyFilters();
    }

    private void applyFilters() {
        String search = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        String type = typeFilter.getValue();

        List<RecruitmentEvent> filtered = new ArrayList<>();
        for (RecruitmentEvent ev : allEvents) {
            boolean matchSearch = search.isEmpty()
                    || (ev.getTitle() != null && ev.getTitle().toLowerCase().contains(search))
                    || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(search))
                    || (ev.getLocation() != null && ev.getLocation().toLowerCase().contains(search));
            boolean matchType = type == null || type.equals("Tous")
                    || type.equals(ev.getEventType());
            if (matchSearch && matchType) filtered.add(ev);
        }
        renderEvents(filtered);
    }

    @FXML
    private void handleApply() {
        if (selectedEvent == null || currentCandidateId < 0) return;
        try {
            EventRegistration reg = new EventRegistration();
            reg.setEventId(selectedEvent.getId());
            reg.setCandidateId(currentCandidateId);
            registrationService.apply(reg);

            lblRegistrationStatus.setText("✅  Inscription confirmée pour : " + selectedEvent.getTitle());
            lblRegistrationStatus.setStyle("-fx-background-color: #D4EDDA; -fx-text-fill: #155724;" +
                    "-fx-font-size:12px; -fx-padding:10 14; -fx-background-radius:8; -fx-border-radius:8;");
            lblRegistrationStatus.setVisible(true); lblRegistrationStatus.setManaged(true);

            btnApply.setDisable(true);
            btnApply.setText("✅  Déjà inscrit");
            btnCancel.setVisible(true); btnCancel.setManaged(true);

            // Send confirmation email in background
            new Thread(() -> {
                try {
                    Services.events.UserService.UserInfo info = Services.events.UserService.getUserInfo(currentCandidateId);
                    if (info != null && info.email() != null && !info.email().isBlank()) {
                        String name = ((info.firstName() != null ? info.firstName() : "") + " " +
                                       (info.lastName()  != null ? info.lastName()  : "")).trim();
                        String date = selectedEvent.getEventDate() != null
                                ? selectedEvent.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
                        Services.EmailService.sendEventRegistrationConfirmation(
                                info.email(), name, selectedEvent.getTitle(),
                                date,
                                selectedEvent.getLocation() != null ? selectedEvent.getLocation() : "",
                                selectedEvent.getEventType() != null ? selectedEvent.getEventType() : "");
                    }
                } catch (Exception ex) {
                    System.err.println("[EventsView] Email error: " + ex.getMessage());
                }
            }, "event-email").start();

        } catch (java.sql.SQLException e) {
            lblRegistrationStatus.setText("⚠️  " + e.getMessage());
            lblRegistrationStatus.setStyle("-fx-background-color: #FFF3CD; -fx-text-fill: #856404;" +
                    "-fx-font-size:12px; -fx-padding:10 14; -fx-background-radius:8; -fx-border-radius:8;");
            lblRegistrationStatus.setVisible(true); lblRegistrationStatus.setManaged(true);
        }
    }

    @FXML
    private void handleCancelRegistration() {
        if (selectedEvent == null || currentCandidateId < 0) return;
        try {
            // Find the registration id and delete it
            List<EventRegistration> regs = registrationService.getByCandidate(currentCandidateId);
            for (EventRegistration r : regs) {
                if (r.getEventId() == selectedEvent.getId()) {
                    registrationService.delete(r.getId());
                    break;
                }
            }
            lblRegistrationStatus.setText("❌  Inscription annulée.");
            lblRegistrationStatus.setStyle("-fx-background-color: #F8D7DA; -fx-text-fill: #721C24;" +
                    "-fx-font-size:12px; -fx-padding:10 14; -fx-background-radius:8; -fx-border-radius:8;");
            lblRegistrationStatus.setVisible(true); lblRegistrationStatus.setManaged(true);

            btnApply.setDisable(false);
            btnApply.setText("✅  S'inscrire");
            btnCancel.setVisible(false); btnCancel.setManaged(false);

        } catch (SQLException e) {
            lblRegistrationStatus.setText("⚠️  Erreur : " + e.getMessage());
            lblRegistrationStatus.setStyle("-fx-background-color: #FFF3CD; -fx-text-fill: #856404;" +
                    "-fx-font-size:12px; -fx-padding:10 14; -fx-background-radius:8; -fx-border-radius:8;");
            lblRegistrationStatus.setVisible(true); lblRegistrationStatus.setManaged(true);
        }
    }

    @FXML
    private void handleAIAnalysis() {
        if (selectedEvent == null || currentCandidateId < 0) return;

        // Prevent useless repeated calls
        if (String.valueOf(selectedEvent.getId()).equals(lastAnalyzedEventId)) {
            aiAnalysisContainer.setVisible(true);
            aiAnalysisContainer.setManaged(true);
            return;
        }

        aiAnalysisContainer.setVisible(true);
        aiAnalysisContainer.setManaged(true);
        aiContentBox.setVisible(false);
        aiContentBox.setManaged(false);
        aiLoadingLabel.setVisible(true);
        aiLoadingLabel.setManaged(true);
        btnAnalyze.setDisable(true);

        // Use a background thread for the API calls
        new Thread(() -> {
            try {
                // Fetch candidate profile
                Models.events.Candidate candidate = candidateService.getByUserId(currentCandidateId);
                
                String prompt = "Tu es un assistant IA expert en conseil de carrière.\n" +
                        "Un candidat souhaite participer à un événement de recrutement.\n\n" +
                        "### PROFIL DU CANDIDAT :\n" + candidateProfileSummary(candidate) + "\n\n" +
                        "### DÉTAILS DE L'ÉVÉNEMENT :\n" +
                        "Titre : " + selectedEvent.getTitle() + "\n" +
                        "Type : " + selectedEvent.getEventType() + "\n" +
                        "Lieu : " + (selectedEvent.getLocation() == null || selectedEvent.getLocation().isBlank() ? "En ligne / À définir" : selectedEvent.getLocation()) + "\n" +
                        "Description : " + selectedEvent.getDescription() + "\n\n" +
                        "### TÂCHE :\n" +
                        "Analyse la pertinence de cet événement pour ce candidat et donne EXACTEMENT :\n" +
                        "1) 3 points forts (Avantages) courts et percutants.\n" +
                        "2) 3 points de vigilance (Inconvénients ou défis) courts et objectifs.\n" +
                        "Sépare les sections par le mot-clé EXACT '###INCONVENIENTS###'.\n" +
                        "Ta réponse doit être uniquement au format suivant (pas d'intro ni de conclusion) :\n" +
                        "- [Avantage 1]\n- [Avantage 2]\n- [Avantage 3]\n###INCONVENIENTS###\n- [Inconvénient 1]\n- [Inconvénient 2]\n- [Inconvénient 3]";

                // 1. Try Gemini
                String aiResponse = null;
                try {
                    aiResponse = callGeminiAPI(prompt);
                } catch (Exception e) {
                    System.err.println("Gemini Analysis failed: " + e.getMessage());
                }

                // 2. Try Groq if Gemini failed
                if (aiResponse == null || aiResponse.isBlank()) {
                    try {
                        aiResponse = callGroqAPI(prompt);
                    } catch (Exception e) {
                        System.err.println("Groq Analysis failed: " + e.getMessage());
                    }
                }

                final String finalRes = aiResponse;
                
                javafx.application.Platform.runLater(() -> {
                    btnAnalyze.setDisable(false);
                    aiLoadingLabel.setVisible(false);
                    aiLoadingLabel.setManaged(false);
                    aiContentBox.setVisible(true);
                    aiContentBox.setManaged(true);

                    if (finalRes == null || finalRes.isBlank() || !finalRes.contains("###INCONVENIENTS###")) {
                        aiProsLabel.setText("- Impossible de générer l'analyse pour le moment.");
                        aiConsLabel.setText("- Service IA temporairement indisponible.");
                    } else {
                        String[] parts = finalRes.split("###INCONVENIENTS###");
                        aiProsLabel.setText(parts[0].trim());
                        aiConsLabel.setText(parts.length > 1 ? parts[1].trim() : "- Aucun");
                        lastAnalyzedEventId = String.valueOf(selectedEvent.getId());
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    btnAnalyze.setDisable(false);
                    aiLoadingLabel.setVisible(false);
                    aiLoadingLabel.setManaged(false);
                    aiContentBox.setVisible(true);
                    aiContentBox.setManaged(true);
                    aiProsLabel.setText("- Erreur lors de l'analyse.");
                    aiConsLabel.setText("- Veuillez réessayer plus tard.");
                });
            }
        }).start();
    }

    private String candidateProfileSummary(Models.events.Candidate candidate) {
        if (candidate == null) return "Profil du candidat non renseigné au complet.";
        return String.format("- Localisation : %s\n- Niveau d'étude : %s\n- Années d'expérience : %d",
                candidate.getLocation() != null ? candidate.getLocation() : "Non spécifié",
                candidate.getEducationLevel() != null ? candidate.getEducationLevel() : "Non spécifié",
                candidate.getExperienceYears());
    }

    private String callGeminiAPI(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=AIzaSyA40pYJkW9p7QYQerVUv_rmS4pNFo1T46o";
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod("POST"); c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(60000);
        String body = "{\"contents\":[{\"parts\":[{\"text\":\"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]}]," +
                "\"generationConfig\":{\"maxOutputTokens\":800,\"temperature\":0.7}}";
        c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (c.getResponseCode() != 200) throw new Exception("Gemini HTTP " + c.getResponseCode());
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line; while ((line = br.readLine()) != null) sb.append(line);
        }
        String json = sb.toString();
        int s = json.indexOf("\"text\":"); if (s < 0) return null;
        s = json.indexOf("\"", s + 7) + 1; int e = s;
        for (int i = s; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '\\' && i + 1 < json.length()) { i++; continue; }
            if (ch == '"') { e = i; break; }
        }
        return e > s ? json.substring(s, e).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim() : null;
    }

    private String callGroqAPI(String prompt) throws Exception {
        String[] MODELS = {"llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b-32768"};
        String GROQ_KEY = "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";
        String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
        for (String model : MODELS) {
            try {
                java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(GROQ_URL).openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Authorization", "Bearer " + GROQ_KEY);
                c.setDoOutput(true); c.setConnectTimeout(15000); c.setReadTimeout(30000);
                String body = "{\"model\":\"" + model + "\",\"messages\":[" +
                        "{\"role\":\"system\",\"content\":\"You are an expert career advisor.\"}," +
                        "{\"role\":\"user\",\"content\":\"" + prompt.replace("\"", "\\\"").replace("\n", "\\n") + "\"}]," +
                        "\"max_tokens\":600,\"temperature\":0.7}";
                c.getOutputStream().write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                if (c.getResponseCode() != 200) continue;
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(c.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                    String line; while ((line = br.readLine()) != null) sb.append(line);
                }
                String json = sb.toString();
                int s = json.indexOf("\"content\":"); if (s < 0) continue;
                s = json.indexOf("\"", s + 10) + 1;
                int e = json.indexOf("\"", s);
                while (e > 0 && json.charAt(e - 1) == '\\') e = json.indexOf("\"", e + 1);
                if (s > 0 && e > s) {
                    String content = json.substring(s, e)
                            .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\").trim();
                    if (!content.isBlank()) return content;
                }
            } catch (Exception ex) {
                System.err.println("Groq model " + model + " failed: " + ex.getMessage());
            }
        }
        throw new Exception("All Groq models failed");
    }
}

