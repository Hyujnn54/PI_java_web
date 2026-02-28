package Services.joboffers;

import Models.application.CandidateProfile;
import Models.joboffers.JobOffer;
import Models.joboffers.JobOfferRecommendation;
import Models.joboffers.MatchingResult;
import Models.joboffers.OfferSkill;
import Services.application.ApplicationService;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class RecommendationService {

    private final OfferSkillService offerSkillService;
    private final MatchingService matchingService;

    public RecommendationService() {
        this.offerSkillService = new OfferSkillService();
        this.matchingService = new MatchingService();
    }

    /**
     * Analyze a job offer and generate dynamic recommendations based on applications and offer content.
     */
    public JobOfferRecommendation analyzeOffer(JobOffer offer) {
        JobOfferRecommendation recommendation = new JobOfferRecommendation();
        
        // 1. Calculate Applications Metrics
        List<ApplicationService.ApplicationRow> applications = ApplicationService.getByOfferId(offer.getId());
        int appCount = applications.size();
        recommendation.setApplicationCount(appCount);

        double totalScore = 0.0;
        int candidatesScored = 0;

        for (ApplicationService.ApplicationRow app : applications) {
            // Reconstruct candidate profile from the application info
            CandidateProfile candidate = new CandidateProfile(app.candidateId(), app.candidateName(), null);
            // We use the application's candidate ID to match against the offer.
            // Note: Since candidate skills aren't easily fetchable from ApplicationRow alone in this context,
            // the score might rely on the MatchingService implementation. 
            // We assume MatchingService fetches candidate skills if needed.

            try {
                MatchingResult result = matchingService.calculateMatch(candidate, offer);
                totalScore += result.getOverallScore();
                candidatesScored++;
            } catch (Exception e) {
                System.err.println("Could not calculate score for candidate " + app.candidateId());
            }
        }

        double avgScore = candidatesScored > 0 ? (totalScore / candidatesScored) : 0.0;
        recommendation.setAverageScore(avgScore);

        // 2. Identify "Unattractive" Offers
        boolean isUnattractive = false;
        long daysSinceCreation = ChronoUnit.DAYS.between(offer.getCreatedAt(), LocalDateTime.now());

        if (avgScore < 40.0 && candidatesScored > 0) {
            isUnattractive = true;
            recommendation.addRecommendation("Score moyen très faible (< 40%). Revoyez vos exigences à la baisse ou clarifiez l'offre.");
        }
        
        if (appCount < 3 && daysSinceCreation >= 7) {
            isUnattractive = true;
            recommendation.addRecommendation("Faible attractivité (moins de 3 candidatures en 7+ jours). Envisagez de sponsoriser l'offre ou d'améliorer les conditions.");
        }
        recommendation.setUnattractive(isUnattractive);

        // 3. Content Analysis
        if (offer.getDescription() == null || offer.getDescription().trim().length() < 100) {
            recommendation.addRecommendation("Description trop courte. Ajoutez les missions, responsabilités et avantages.");
        }

        if (offer.getLocation() == null || offer.getLocation().trim().isEmpty() || offer.getLocation().equalsIgnoreCase("Non specifie")) {
            recommendation.addRecommendation("Localisation trop vague ou vide. Précisez la ville ou indiquez 'Télétravail'.");
        }

        try {
            List<OfferSkill> skills = offerSkillService.getSkillsByOfferId(offer.getId());
            if (skills.isEmpty()) {
                recommendation.addRecommendation("Aucune compétence requise définie. Ajoutez des compétences clés (SQL, Java, etc.) pour cibler les candidats.");
            } else if (skills.size() < 3) {
                recommendation.addRecommendation("Peu de compétences définies. Citez au moins 3 compétences pour améliorer le matching.");
            }
        } catch (SQLException e) {
             System.err.println("Could not fetch skills for offer " + offer.getId());
        }

        if (offer.getDeadline() != null) {
            long daysToDeadline = ChronoUnit.DAYS.between(LocalDateTime.now(), offer.getDeadline());
            if (daysToDeadline >= 0 && daysToDeadline < 3) {
                recommendation.addRecommendation("Deadline très proche (moins de 3 jours). Prolongez le délai si vous cherchez encore des candidats.");
            } else if (daysToDeadline < 0) {
                 recommendation.addRecommendation("La deadline est dépassée. Pensez à fermer l'offre ou à la prolonger.");
            }
        }

        // Conversion rate text (No views column in DB, so we simulate a message if needed)
        // recommendation.addRecommendation("Note : Pour améliorer le taux de conversion, ajoutez une indication de salaire.");

        if (recommendation.getRecommendations().isEmpty()) {
             recommendation.addRecommendation("L'offre est bien rédigée et optimisée. Continuez ainsi !");
        }

        return recommendation;
    }
}
