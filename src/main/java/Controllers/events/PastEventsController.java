package Controllers.events;

import Models.events.*;
import Services.events.EventRegistrationService;
import Services.events.RecruitmentEventService;
import Services.events.ReviewService;
import Utils.UserContext;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PastEventsController implements Initializable {

    // ── Left panel ──────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> typeFilter;
    @FXML private Label lblCount;
    @FXML private VBox eventsContainer;

    // ── Right panel ─────────────────────────────────────────────────────────
    @FXML private VBox  detailPlaceholder;
    @FXML private ScrollPane detailScroll;
    @FXML private Label detailTitle;
    @FXML private Label detailType;
    @FXML private Label detailDateLocation;
    @FXML private Label detailDescription;
    @FXML private Label detailStatusBadge;

    // Review input
    @FXML private Label reviewNotEligibleLabel;
    @FXML private Label reviewAlreadyDoneLabel;
    @FXML private VBox  reviewInputBox;
    @FXML private ToggleButton star1, star2, star3, star4, star5;
    @FXML private Label starValueLabel;
    @FXML private TextArea reviewCommentField;
    @FXML private Label reviewErrorLabel;
    @FXML private Button btnSubmitReview;

    // Reviews summary
    @FXML private VBox  reviewsSummaryPanel;
    @FXML private Label avgRatingLabel;
    @FXML private VBox  reviewsListBox;

    // ── Services ────────────────────────────────────────────────────────────
    private final RecruitmentEventService eventService       = new RecruitmentEventService();
    private final EventRegistrationService registrationService = new EventRegistrationService();
    private final ReviewService reviewService                = new ReviewService();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private List<RecruitmentEvent> allPastEvents = new ArrayList<>();
    private RecruitmentEvent selectedEvent = null;
    private long candidateId = -1;
    private int currentStarRating = 0;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Long uid = UserContext.getUserId();
        if (uid != null) candidateId = uid;

        typeFilter.getItems().addAll("Tous", "Job_Faire", "WEBINAIRE", "Interview day");
        typeFilter.setValue("Tous");

        loadPastEvents();
    }

    // ── Data loading ────────────────────────────────────────────────────────

    private void loadPastEvents() {
        try {
            List<RecruitmentEvent> all = eventService.getAll();
            allPastEvents = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();
            for (RecruitmentEvent ev : all) {
                if (ev.getEventDate() != null && ev.getEventDate().isBefore(now)) {
                    allPastEvents.add(ev);
                }
            }
        } catch (SQLException e) {
            allPastEvents = new ArrayList<>();
            System.err.println("Error loading past events: " + e.getMessage());
        }
        renderList(allPastEvents);
    }

    private void renderList(List<RecruitmentEvent> events) {
        eventsContainer.getChildren().clear();
        lblCount.setText(events.size() + " evenement(s)");

        if (events.isEmpty()) {
            Label empty = new Label("Aucun evenement passe trouve.");
            empty.setStyle("-fx-font-size:13px; -fx-text-fill:#94A3B8; -fx-padding:30;");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            eventsContainer.getChildren().add(empty);
            return;
        }

        for (RecruitmentEvent ev : events) {
            eventsContainer.getChildren().add(buildCard(ev));
        }
    }

    private VBox buildCard(RecruitmentEvent ev) {
        VBox card = new VBox(6);
        card.setStyle("-fx-background-color:white; -fx-background-radius:12;" +
                      "-fx-border-color:#E4EBF5; -fx-border-width:1; -fx-border-radius:12;" +
                      "-fx-padding:14 16; -fx-cursor:hand;");

        // Type badge + date row
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);

        Label typeBadge = new Label(ev.getEventType() != null ? ev.getEventType() : "");
        typeBadge.setStyle("-fx-background-color:#F1F5F9; -fx-text-fill:#475569;" +
                           "-fx-font-size:10px; -fx-font-weight:700;" +
                           "-fx-padding:2 8; -fx-background-radius:6;");

        Pane sp = new Pane(); HBox.setHgrow(sp, Priority.ALWAYS);

        Label dateLbl = new Label(ev.getEventDate() != null ? ev.getEventDate().format(FMT) : "");
        dateLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#94A3B8;");

        top.getChildren().addAll(typeBadge, sp, dateLbl);

        Label title = new Label(ev.getTitle());
        title.setStyle("-fx-font-size:14px; -fx-font-weight:700; -fx-text-fill:#1E293B;");
        title.setWrapText(true);

        // Rating badge — show avg if reviews exist
        try {
            double avg = reviewService.getAverageRating(ev.getId());
            if (avg > 0) {
                Label ratingBadge = new Label(String.format("%.1f / 5  ★", avg));
                ratingBadge.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:#D97706;" +
                                     "-fx-background-color:#FEF9C3; -fx-background-radius:20; -fx-padding:2 8;");
                card.getChildren().addAll(top, title, ratingBadge);
            } else {
                card.getChildren().addAll(top, title);
            }
        } catch (SQLException e) {
            card.getChildren().addAll(top, title);
        }

        // Hover
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle()
                .replace("-fx-background-color:white;", "-fx-background-color:#F8FAFF;")));
        card.setOnMouseExited(e -> {
            if (selectedEvent == null || selectedEvent.getId() != ev.getId())
                card.setStyle(card.getStyle()
                        .replace("-fx-background-color:#F8FAFF;", "-fx-background-color:white;")
                        .replace("-fx-border-color:#1565C0;", "-fx-border-color:#E4EBF5;"));
        });
        card.setOnMouseClicked(e -> selectEvent(ev, card));
        return card;
    }

    private void selectEvent(RecruitmentEvent ev, VBox clickedCard) {
        selectedEvent = ev;
        // Reset all card styles
        for (Node n : eventsContainer.getChildren()) {
            if (n instanceof VBox c) {
                c.setStyle(c.getStyle()
                        .replace("-fx-background-color:#EBF3FF;", "-fx-background-color:white;")
                        .replace("-fx-border-color:#1565C0;", "-fx-border-color:#E4EBF5;"));
            }
        }
        // Highlight selected
        clickedCard.setStyle(clickedCard.getStyle()
                .replace("-fx-background-color:white;", "-fx-background-color:#EBF3FF;")
                .replace("-fx-border-color:#E4EBF5;", "-fx-border-color:#1565C0;"));
        showDetail(ev);
    }

    private void showDetail(RecruitmentEvent ev) {
        detailPlaceholder.setVisible(false);
        detailPlaceholder.setManaged(false);
        detailScroll.setVisible(true);
        detailScroll.setManaged(true);

        detailTitle.setText(ev.getTitle());
        detailType.setText(ev.getEventType() != null ? ev.getEventType() : "");
        detailDateLocation.setText(
            (ev.getEventDate() != null ? ev.getEventDate().format(FMT) : "") +
            (ev.getLocation() != null ? "   •   " + ev.getLocation() : ""));
        detailDescription.setText(ev.getDescription() != null ? ev.getDescription() : "");

        // Show candidate's registration status badge
        try {
            AttendanceStatusEnum status = registrationService.getRegistrationStatus(ev.getId(), candidateId);
            if (status != null) {
                String badgeText;
                String badgeStyle;
                switch (status) {
                    case CONFIRMED:
                        badgeText = "Votre inscription etait CONFIRMEE";
                        badgeStyle = "-fx-background-color:#DCFCE7; -fx-text-fill:#166534;";
                        break;
                    case CANCELLED:
                        badgeText = "Vous avez annule votre inscription";
                        badgeStyle = "-fx-background-color:#FEE2E2; -fx-text-fill:#991B1B;";
                        break;
                    case REJECTED:
                        badgeText = "Votre inscription avait ete refusee";
                        badgeStyle = "-fx-background-color:#FFEDD5; -fx-text-fill:#9A3412;";
                        break;
                    default:
                        badgeText = "Inscription en attente (non traitee)";
                        badgeStyle = "-fx-background-color:#FEF9C3; -fx-text-fill:#854D0E;";
                        break;
                }
                detailStatusBadge.setText(badgeText);
                detailStatusBadge.setStyle(badgeStyle +
                        " -fx-font-size:12px; -fx-font-weight:700; -fx-background-radius:20; -fx-padding:6 14;");
                detailStatusBadge.setVisible(true);
                detailStatusBadge.setManaged(true);
            } else {
                detailStatusBadge.setVisible(false);
                detailStatusBadge.setManaged(false);
            }
        } catch (SQLException e) {
            detailStatusBadge.setVisible(false);
            detailStatusBadge.setManaged(false);
        }

        // Reset review panel
        resetReviewPanel();
        updateReviewPanel(ev);
        loadReviewsSummary(ev.getId());
    }

    // ── Fields for review editing ─────────────────────────────────────────────
    private EventReview existingReview = null;

    // ── Review panel ─────────────────────────────────────────────────────────

    private void resetReviewPanel() {
        reviewNotEligibleLabel.setVisible(false); reviewNotEligibleLabel.setManaged(false);
        reviewAlreadyDoneLabel.setVisible(false); reviewAlreadyDoneLabel.setManaged(false);
        reviewInputBox.setVisible(false);         reviewInputBox.setManaged(false);
        reviewsSummaryPanel.setVisible(false);    reviewsSummaryPanel.setManaged(false);
        if (reviewErrorLabel != null) { reviewErrorLabel.setVisible(false); reviewErrorLabel.setManaged(false); }
        currentStarRating = 0;
        setStars(0);
        if (reviewCommentField != null) reviewCommentField.clear();
        existingReview = null;
    }

    private void updateReviewPanel(RecruitmentEvent ev) {
        if (candidateId <= 0) {
            reviewNotEligibleLabel.setText("Connectez-vous pour laisser un avis.");
            reviewNotEligibleLabel.setVisible(true); reviewNotEligibleLabel.setManaged(true);
            return;
        }
        try {
            AttendanceStatusEnum status = registrationService.getRegistrationStatus(ev.getId(), candidateId);
            boolean wasConfirmed = status == AttendanceStatusEnum.CONFIRMED;

            if (!wasConfirmed) {
                String msg = (status == null)
                        ? "Vous n'etiez pas inscrit a cet evenement."
                        : "Seuls les participants confirmes peuvent noter l'evenement.";
                reviewNotEligibleLabel.setText(msg);
                reviewNotEligibleLabel.setVisible(true); reviewNotEligibleLabel.setManaged(true);
            } else {
                // Check if already reviewed
                List<EventReview> reviews = reviewService.getByEvent(ev.getId());
                EventReview myReview = reviews.stream()
                        .filter(r -> r.getCandidateId() == candidateId)
                        .findFirst().orElse(null);

                if (myReview != null) {
                    // Already reviewed — show edit/delete options
                    existingReview = myReview;
                    showEditReviewPanel(myReview);
                } else {
                    // Not yet reviewed — show submit form
                    reviewInputBox.setVisible(true); reviewInputBox.setManaged(true);
                    if (btnSubmitReview != null) {
                        btnSubmitReview.setText("Soumettre mon avis");
                        btnSubmitReview.setStyle("-fx-background-color:#D97706; -fx-text-fill:white;"
                                + "-fx-font-weight:700; -fx-background-radius:8; -fx-padding:9 22;");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Review eligibility check error: " + e.getMessage());
        }
    }

    private void showEditReviewPanel(EventReview review) {
        // Pre-fill the form with existing values
        currentStarRating = review.getRating();
        setStars(currentStarRating);
        if (reviewCommentField != null) reviewCommentField.setText(review.getComment() != null ? review.getComment() : "");

        reviewInputBox.setVisible(true); reviewInputBox.setManaged(true);

        if (btnSubmitReview != null) {
            btnSubmitReview.setText("💾  Mettre à jour mon avis");
            btnSubmitReview.setStyle("-fx-background-color:#1565C0; -fx-text-fill:white;"
                    + "-fx-font-weight:700; -fx-background-radius:8; -fx-padding:9 22;");
        }

        // Show a small info banner above the form
        reviewAlreadyDoneLabel.setText("✏  Vous avez déjà soumis un avis — vous pouvez le modifier ou le supprimer ci-dessous.");
        reviewAlreadyDoneLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#1565C0;"
                + "-fx-background-color:#E3F2FD; -fx-background-radius:8;"
                + "-fx-padding:10 14; -fx-border-color:#BBDEFB;"
                + "-fx-border-width:1; -fx-border-radius:8;");
        reviewAlreadyDoneLabel.setVisible(true); reviewAlreadyDoneLabel.setManaged(true);

        // Add a delete button dynamically if not already present
        if (reviewInputBox != null) {
            // Remove any existing delete button first
            reviewInputBox.getChildren().removeIf(node ->
                    node instanceof Button && "deleteReviewBtn".equals(node.getId()));

            Button deleteBtn = new Button("🗑  Supprimer mon avis");
            deleteBtn.setId("deleteReviewBtn");
            deleteBtn.setMaxWidth(Double.MAX_VALUE);
            deleteBtn.setStyle("-fx-background-color:#FFF0F0; -fx-text-fill:#E53935; -fx-font-size:12px;"
                    + "-fx-font-weight:600; -fx-padding:9 0; -fx-background-radius:8; -fx-cursor:hand;"
                    + "-fx-border-color:#FFCDD2; -fx-border-width:1; -fx-border-radius:8;");
            deleteBtn.setOnAction(e -> handleDeleteReview());
            reviewInputBox.getChildren().add(deleteBtn);
        }
    }

    private void loadReviewsSummary(long eventId) {
        try {
            List<EventReview> reviews = reviewService.getByEvent(eventId);
            if (reviews.isEmpty()) return;

            reviewsSummaryPanel.setVisible(true); reviewsSummaryPanel.setManaged(true);

            double avg = reviews.stream().mapToInt(EventReview::getRating).average().orElse(0);
            String stars = "★".repeat((int) Math.round(avg)) + "☆".repeat(5 - (int) Math.round(avg));
            avgRatingLabel.setText(String.format("%.1f / 5  %s  (%d avis)", avg, stars, reviews.size()));

            reviewsListBox.getChildren().clear();
            for (EventReview r : reviews) {
                VBox card = new VBox(4);
                card.setStyle("-fx-background-color:#F8FAFF; -fx-background-radius:10;" +
                              "-fx-border-color:#E4EBF5; -fx-border-width:1; -fx-border-radius:10;" +
                              "-fx-padding:10 14;");
                String name = (r.getCandidateName() != null && !r.getCandidateName().isBlank())
                        ? r.getCandidateName() : "Candidat";
                String ratingStars = "★".repeat(r.getRating()) + "☆".repeat(5 - r.getRating());
                String dateStr = r.getCreatedAt() != null
                        ? r.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";

                Label header = new Label(name + "   " + ratingStars + "   " + dateStr);
                header.setStyle("-fx-font-size:12px; -fx-font-weight:700; -fx-text-fill:#1E293B;");
                card.getChildren().add(header);

                if (r.getComment() != null && !r.getComment().isBlank()) {
                    Label comment = new Label(r.getComment());
                    comment.setWrapText(true);
                    comment.setStyle("-fx-font-size:12px; -fx-text-fill:#475569;");
                    card.getChildren().add(comment);
                }
                reviewsListBox.getChildren().add(card);
            }
        } catch (SQLException e) {
            System.err.println("Error loading reviews: " + e.getMessage());
        }
    }

    @FXML
    private void handleStarClick() {
        ToggleButton[] stars = {star1, star2, star3, star4, star5};
        int clicked = 0;
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] != null && stars[i].isSelected()) clicked = i + 1;
        }
        currentStarRating = clicked;
        setStars(clicked);
    }

    private void setStars(int count) {
        ToggleButton[] stars = {star1, star2, star3, star4, star5};
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == null) continue;
            boolean filled = i < count;
            stars[i].setSelected(filled);
            String color = filled ? "#F59E0B" : "#D1D5DB";
            stars[i].setStyle("-fx-font-size:28px; -fx-background-color:transparent;" +
                              "-fx-border-color:transparent; -fx-padding:0 2; -fx-cursor:hand;" +
                              "-fx-text-fill:" + color + ";");
        }
        if (starValueLabel != null)
            starValueLabel.setText(count > 0 ? count + " / 5" : "");
    }

    @FXML
    private void handleSubmitReview() {
        if (selectedEvent == null || candidateId <= 0) return;
        if (currentStarRating == 0) {
            if (reviewErrorLabel != null) {
                reviewErrorLabel.setText("Veuillez selectionner une note (1 a 5 etoiles).");
                reviewErrorLabel.setVisible(true); reviewErrorLabel.setManaged(true);
            }
            return;
        }
        try {
            String comment = reviewCommentField != null ? reviewCommentField.getText().trim() : "";

            if (existingReview != null) {
                // Update existing review via SQL directly through ReviewService
                updateExistingReview(existingReview.getId(), currentStarRating, comment);
            } else {
                // Create new review
                EventReview review = new EventReview(selectedEvent.getId(), candidateId, currentStarRating, comment);
                reviewService.add(review);
            }

            // Refresh
            existingReview = null;
            resetReviewPanel();
            updateReviewPanel(selectedEvent);
            loadReviewsSummary(selectedEvent.getId());
            renderList(allPastEvents);
        } catch (SQLException e) {
            if (reviewErrorLabel != null) {
                reviewErrorLabel.setText("Erreur : " + e.getMessage());
                reviewErrorLabel.setVisible(true); reviewErrorLabel.setManaged(true);
            }
        }
    }

    private void updateExistingReview(long reviewId, int rating, String comment) throws SQLException {
        String sql = "UPDATE event_review SET rating=?, comment=?, created_at=? WHERE id=?";
        try (java.sql.PreparedStatement ps = Utils.MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setInt(1, rating);
            ps.setString(2, comment);
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setLong(4, reviewId);
            ps.executeUpdate();
        }
    }

    private void handleDeleteReview() {
        if (selectedEvent == null || existingReview == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Supprimer l'avis");
        confirm.setHeaderText(null);
        confirm.setContentText("Voulez-vous vraiment supprimer votre avis ?");
        confirm.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String sql = "DELETE FROM event_review WHERE id=?";
                    try (java.sql.PreparedStatement ps = Utils.MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
                        ps.setLong(1, existingReview.getId());
                        ps.executeUpdate();
                    }
                    existingReview = null;
                    resetReviewPanel();
                    updateReviewPanel(selectedEvent);
                    loadReviewsSummary(selectedEvent.getId());
                    renderList(allPastEvents);
                } catch (SQLException e) {
                    if (reviewErrorLabel != null) {
                        reviewErrorLabel.setText("Erreur suppression : " + e.getMessage());
                        reviewErrorLabel.setVisible(true); reviewErrorLabel.setManaged(true);
                    }
                }
            }
        });
    }

    // ── Search / Filter ──────────────────────────────────────────────────────

    @FXML
    private void handleSearch() {
        String q = searchField.getText() != null ? searchField.getText().toLowerCase().trim() : "";
        String type = typeFilter.getValue();
        List<RecruitmentEvent> filtered = new ArrayList<>();
        for (RecruitmentEvent ev : allPastEvents) {
            boolean matchQ = q.isEmpty()
                    || (ev.getTitle() != null && ev.getTitle().toLowerCase().contains(q))
                    || (ev.getLocation() != null && ev.getLocation().toLowerCase().contains(q));
            boolean matchType = type == null || "Tous".equals(type) || type.equals(ev.getEventType());
            if (matchQ && matchType) filtered.add(ev);
        }
        renderList(filtered);
    }

    @FXML
    private void handleRefresh() {
        loadPastEvents();
        detailPlaceholder.setVisible(true);  detailPlaceholder.setManaged(true);
        detailScroll.setVisible(false);      detailScroll.setManaged(false);
        selectedEvent = null;
    }
}


