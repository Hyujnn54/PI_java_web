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

    private final RecruitmentEventService eventService = new RecruitmentEventService();
    private final EventRegistrationService registrationService = new EventRegistrationService();

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
        } else if (isRecruiter) {
            // Recruiter: also read-only in main shell view
            btnApply.setVisible(false); btnApply.setManaged(false);
            btnCancel.setVisible(false); btnCancel.setManaged(false);
        } else {
            // Candidate: check if already registered
            btnApply.setVisible(true); btnApply.setManaged(true);
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
        selectedEvent = null;
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
}

