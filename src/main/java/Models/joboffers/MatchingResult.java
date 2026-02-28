package Models.joboffers;

import Models.application.CandidateProfile;

/**
 * Résultat du matching entre un candidat et une offre d'emploi
 */
public class MatchingResult {
    private JobOffer jobOffer;
    private CandidateProfile candidate;
    private double overallScore;          // Score global (0-100)
    private double skillsScore;           // Score compétences (0-100)
    private double locationScore;         // Score localisation (0-100)
    private double contractTypeScore;     // Score type contrat (0-100)
    private double experienceScore;       // Score expérience (0-100)

    private int matchedSkillsCount;
    private int totalRequiredSkills;
    private double distanceKm;
    private String matchLevel;            // EXCELLENT, BON, MOYEN, FAIBLE

    public MatchingResult() {}

    public MatchingResult(JobOffer jobOffer, CandidateProfile candidate) {
        this.jobOffer = jobOffer;
        this.candidate = candidate;
    }

    /**
     * Calcule le niveau de matching basé sur le score global
     */
    public void calculateMatchLevel() {
        if (overallScore >= 85) {
            matchLevel = "EXCELLENT";
        } else if (overallScore >= 70) {
            matchLevel = "BON";
        } else if (overallScore >= 50) {
            matchLevel = "MOYEN";
        } else {
            matchLevel = "FAIBLE";
        }
    }

    /**
     * Retourne une couleur pour l'affichage du score
     */
    public String getScoreColor() {
        if (overallScore >= 85) return "#28a745";  // Vert
        if (overallScore >= 70) return "#17a2b8";  // Bleu
        if (overallScore >= 50) return "#ffc107";  // Jaune
        return "#dc3545";                          // Rouge
    }

    /**
     * Retourne une icône emoji pour le niveau
     */
    public String getLevelEmoji() {
        return switch (matchLevel) {
            case "EXCELLENT" -> "🌟";
            case "BON" -> "✅";
            case "MOYEN" -> "⚠️";
            default -> "❌";
        };
    }

    // Getters et Setters
    public JobOffer getJobOffer() { return jobOffer; }
    public void setJobOffer(JobOffer jobOffer) { this.jobOffer = jobOffer; }

    public CandidateProfile getCandidate() { return candidate; }
    public void setCandidate(CandidateProfile candidate) { this.candidate = candidate; }

    public double getOverallScore() { return overallScore; }
    public void setOverallScore(double score) {
        this.overallScore = score;
        calculateMatchLevel();
    }

    public double getSkillsScore() { return skillsScore; }
    public void setSkillsScore(double score) { this.skillsScore = score; }

    public double getLocationScore() { return locationScore; }
    public void setLocationScore(double score) { this.locationScore = score; }

    public double getContractTypeScore() { return contractTypeScore; }
    public void setContractTypeScore(double score) { this.contractTypeScore = score; }

    public double getExperienceScore() { return experienceScore; }
    public void setExperienceScore(double score) { this.experienceScore = score; }

    public int getMatchedSkillsCount() { return matchedSkillsCount; }
    public void setMatchedSkillsCount(int count) { this.matchedSkillsCount = count; }

    public int getTotalRequiredSkills() { return totalRequiredSkills; }
    public void setTotalRequiredSkills(int total) { this.totalRequiredSkills = total; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distance) { this.distanceKm = distance; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String level) { this.matchLevel = level; }

    /**
     * Formatte le score pour l'affichage
     */
    public String getFormattedScore() {
        return String.format("%.0f%%", overallScore);
    }

    /**
     * Retourne une description du matching
     */
    public String getMatchDescription() {
        return switch (matchLevel) {
            case "EXCELLENT" -> "Votre profil correspond parfaitement à cette offre !";
            case "BON" -> "Bonne correspondance avec cette offre.";
            case "MOYEN" -> "Correspondance partielle, quelques critères manquants.";
            default -> "Cette offre ne correspond pas bien à votre profil.";
        };
    }
}

