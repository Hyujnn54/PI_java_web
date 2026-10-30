package controllers;

import entities.RecruitmentEvent;
import entities.Candidate;
import entities.EventRegistration;
import entities.EventReview;
import entities.User;
import entities.AttendanceStatusEnum;
import services.ReviewService;
import services.UserService;
import services.EventRegistrationService;
import utils.AIService;
import utils.SessionManager;
import utils.NotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import org.controlsfx.control.Rating;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class EventDetailsModalController {

    @FXML
    private Label modalTitleLabel;
    @FXML
    private Label modalTypeLabel;
    @FXML
    private Label modalDescriptionLabel;
    @FXML
    private Label modalLocationLabel;
    @FXML
    private Label modalDateLabel;
    @FXML
    private Label modalCapacityLabel;
    @FXML
    private Label aiInsightLabel;
    @FXML
    private Button aiInsightBtn;
    @FXML
    private VBox reviewInputSection;
    @FXML
    private VBox notEligibleBox;
    @FXML
    private Label eligibilityMsgLabel;
    @FXML
    private Rating eventRating;
    @FXML
    private TextArea reviewCommentField;
    @FXML
    private Button submitReviewBtn;
    @FXML
    private VBox reviewSuccessBox;
    
    // Ratings Summary Dashboard bindings
    @FXML
    private VBox ratingsSummaryBox;
    @FXML
    private Label avgRatingLabel;
    @FXML
    private Rating summaryRatingStars;
    @FXML
    private Label totalReviewsLabel;
    @FXML
    private ProgressBar bar5;
    @FXML
    private ProgressBar bar4;
    @FXML
    private ProgressBar bar3;
    @FXML
    private ProgressBar bar2;
    @FXML
    private ProgressBar bar1;
    @FXML
    private VBox reviewsListContainer;

    private RecruitmentEvent event;
    private CandidateDashboardController parentController;
    private Candidate currentCandidate;
    private ReviewService reviewService = new ReviewService();
    private UserService userService = new UserService();
    private EventRegistrationService registrationService = new EventRegistrationService();

    public void setCandidate(Candidate candidate) {
        this.currentCandidate = candidate;
        checkReviewEligibility();
    }

    public void setEvent(RecruitmentEvent event) {
        this.event = event;
        modalTitleLabel.setText(event.getTitle());
        modalTypeLabel.setText(event.getEventType());
        modalDescriptionLabel.setText(event.getDescription());
        modalLocationLabel.setText(event.getLocation());
        modalDateLabel.setText(event.getEventDate() != null ? event.getEventDate().toString().replace("T", " ") : "N/A");
        modalCapacityLabel.setText(event.getCapacity() + " places");
        
        loadRatingsSummary();
        checkReviewEligibility();
    }

    public void setParentController(CandidateDashboardController parentController) {
        this.parentController = parentController;
    }

    @FXML
    private void handleClose() {
        ((Stage) modalTitleLabel.getScene().getWindow()).close();
    }

    @FXML
    private void handleApplyFromDetails() {
        handleClose();
        if (parentController != null) {
            parentController.handleApplyFromDetails(event);
        }
    }

    @FXML
    private void handleGenerateInsight() {
        if (event == null) return;
        aiInsightBtn.setDisable(true);
        aiInsightBtn.setText("⏳ Analyse...");
        aiInsightLabel.setText("L'IA analyse cet événement pour vous...");

        String profile = "Candidat Talent Bridge (Niveau Master)";
        AIService.generateCareerInsight(event.getTitle(), event.getDescription(), profile, new AIService.AICallback() {
            @Override
            public void onSuccess(String result) {
                Platform.runLater(() -> {
                    aiInsightLabel.setText(result);
                    aiInsightBtn.setDisable(false);
                    aiInsightBtn.setText("✨ Analyse IA");
                });
            }
            @Override
            public void onFailure(Throwable t) {
                Platform.runLater(() -> {
                    aiInsightLabel.setText("Désolé, l'IA n'a pas pu générer d'analyse pour le moment.");
                    aiInsightBtn.setDisable(false);
                    aiInsightBtn.setText("✨ Analyse IA");
                });
            }
        });
    }

    private void checkReviewEligibility() {
        User currentUser = SessionManager.getCurrentUser();

        // Default: hide all
        reviewInputSection.setVisible(false);
        reviewInputSection.setManaged(false);
        notEligibleBox.setVisible(false);
        notEligibleBox.setManaged(false);
        reviewSuccessBox.setVisible(false);
        reviewSuccessBox.setManaged(false);
        
        // Ensure summary stars are read-only
        summaryRatingStars.setMouseTransparent(true);
        summaryRatingStars.setFocusTraversable(false);

        if (currentUser == null || event == null) {
            notEligibleBox.setVisible(true);
            notEligibleBox.setManaged(true);
            eligibilityMsgLabel.setText("Connectez-vous pour laisser un avis.");
            return;
        }

        try {
            boolean isPast = event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now());
            
            // Check registrations for the LOGGED-IN user
            long userId = currentUser.getId();
            List<EventRegistration> userRegistrations = registrationService.getByCandidate(userId);
            
            boolean isConfirmed = userRegistrations.stream()
                    .filter(r -> r.getEventId() == event.getId())
                    .anyMatch(r -> r.getAttendanceStatus() == AttendanceStatusEnum.CONFIRMED || 
                                 r.getAttendanceStatus() == AttendanceStatusEnum.ATTENDED);

            boolean hasReviewed = reviewService.hasCandidateReviewed(event.getId(), userId);

            // DEBUG/TEST MODE: Disabled for production
            boolean forceShowForTest = false; 

            if (forceShowForTest) {
                reviewInputSection.setVisible(true);
                reviewInputSection.setManaged(true);
            } else if (!isPast) {
                notEligibleBox.setVisible(true);
                notEligibleBox.setManaged(true);
                String dateStr = event.getEventDate() != null ? event.getEventDate().toLocalDate().toString() : "N/A";
                eligibilityMsgLabel.setText("Note et feedback seront disponibles dès que l'événement sera passé (après le " + dateStr + ").");
            } else if (!isConfirmed) {
                notEligibleBox.setVisible(true);
                notEligibleBox.setManaged(true);
                eligibilityMsgLabel.setText("Seuls les participants confirmés peuvent noter cet événement.");
            } else if (hasReviewed) {
                reviewSuccessBox.setVisible(true);
                reviewSuccessBox.setManaged(true);
            } else {
                reviewInputSection.setVisible(true);
                reviewInputSection.setManaged(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRatingsSummary() {
        if (event == null) return;
        try {
            List<EventReview> reviews = reviewService.getByEvent(event.getId());
            int count = reviews.size();
            
            if (count == 0) {
                avgRatingLabel.setText("0.0");
                summaryRatingStars.setRating(0);
                totalReviewsLabel.setText("0 reviews");
                bar5.setProgress(0); bar4.setProgress(0); bar3.setProgress(0); bar2.setProgress(0); bar1.setProgress(0);
                return;
            }
            
            double sum = 0;
            int c5 = 0, c4 = 0, c3 = 0, c2 = 0, c1 = 0;
            for (EventReview r : reviews) {
                sum += r.getRating();
                switch (r.getRating()) {
                    case 5: c5++; break;
                    case 4: c4++; break;
                    case 3: c3++; break;
                    case 2: c2++; break;
                    case 1: c1++; break;
                }
            }
            
            double avg = sum / count;
            avgRatingLabel.setText(String.format("%.1f", avg));
            summaryRatingStars.setRating(avg);
            totalReviewsLabel.setText(String.format("%,d reviews", count));
            
            bar5.setProgress((double) c5 / count);
            bar4.setProgress((double) c4 / count);
            bar3.setProgress((double) c3 / count);
            bar2.setProgress((double) c2 / count);
            bar1.setProgress((double) c1 / count);

            // Fill individual reviews list
            reviewsListContainer.getChildren().clear();
            for (EventReview r : reviews) {
                reviewsListContainer.getChildren().add(createReviewCard(r));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private VBox createReviewCard(EventReview review) {
        VBox card = new VBox(10);
        card.getStyleClass().add("review-card");
        card.setPadding(new Insets(15));
        
        // Header (Name + Stars)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label("Utilisateur");
        try {
            User user = userService.getById(review.getCandidateId());
            if (user != null) {
                nameLabel.setText(user.getFirstName() + " " + user.getLastName());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
        
        Rating smallStars = new Rating(5);
        smallStars.setRating(review.getRating());
        smallStars.setPartialRating(false);
        smallStars.setUpdateOnHover(false);
        smallStars.setMouseTransparent(true);
        smallStars.setPrefWidth(120);
        smallStars.setMaxHeight(20);
        // We'll style these specific stars in CSS to be smaller
        smallStars.getStyleClass().add("review-rating-stars");
        
        header.getChildren().addAll(nameLabel, smallStars);
        
        // Date
        String dateStr = review.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
        Label dateLabel = new Label(dateStr);
        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        
        // Comment
        Label commentLabel = new Label(review.getComment());
        commentLabel.setWrapText(true);
        commentLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px; -fx-line-spacing: 1.4;");
        
        card.getChildren().addAll(header, dateLabel, commentLabel);
        return card;
    }

    @FXML
    private void handleSubmitReview() {
        int rating = (int) eventRating.getRating();
        String comment = reviewCommentField.getText();

        if (rating == 0) {
            NotificationService.showError("Erreur", "Veuillez donner une note avec les étoiles.");
            return;
        }

        try {
            long candidateId = (currentCandidate != null) ? currentCandidate.getId() : SessionManager.getCurrentUserId();
            EventReview review = new EventReview(event.getId(), candidateId, rating, comment);
            reviewService.add(review);
            
            reviewInputSection.setVisible(false);
            reviewInputSection.setManaged(false);
            reviewSuccessBox.setVisible(true);
            reviewSuccessBox.setManaged(true);
            
            loadRatingsSummary();
            NotificationService.showSuccess("Merci", "Votre avis a été enregistré avec succès.");
        } catch (SQLException e) {
            NotificationService.showError("Erreur", "Impossible d'enregistrer votre avis : " + e.getMessage());
        }
    }
}
