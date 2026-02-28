package Services.joboffers;

import Models.application.CandidateProfile;
import Models.application.CandidateProfile.CandidateSkill;
import Models.joboffers.JobOffer;
import Models.joboffers.MatchingResult;
import Models.joboffers.OfferSkill;

import java.util.ArrayList;
import java.util.List;

/**
 * Service de calcul du score de matching entre candidats et offres d'emploi
 *
 * Formule: Score = (Compétences × 40%) + (Localisation × 25%) + (Type Contrat × 20%) + (Expérience × 15%)
 */
public class MatchingService {

    // Poids des différents critères (total = 100%)
    private static final double SKILLS_WEIGHT = 0.40;      // 40%
    private static final double LOCATION_WEIGHT = 0.25;    // 25%
    private static final double CONTRACT_WEIGHT = 0.20;    // 20%
    private static final double EXPERIENCE_WEIGHT = 0.15;  // 15%

    // Seuils de distance (en km)
    private static final double DISTANCE_EXCELLENT = 10;   // < 10 km = 100%
    private static final double DISTANCE_GOOD = 30;        // < 30 km = 80%
    private static final double DISTANCE_MEDIUM = 50;      // < 50 km = 60%
    private static final double DISTANCE_FAR = 100;        // < 100 km = 40%

    public MatchingService() {
        // OfferSkillService is instantiated fresh per call to avoid stale connections
    }

    private OfferSkillService offerSkillService() {
        return new OfferSkillService();
    }

    /**
     * Calcule le score de matching entre un candidat et une offre
     */
    public MatchingResult calculateMatch(CandidateProfile candidate, JobOffer offer) {
        MatchingResult result = new MatchingResult(offer, candidate);

        try {
            // 1. Calculer le score des compétences
            List<OfferSkill> requiredSkills = offerSkillService().getSkillsByOfferId(offer.getId());
            double skillsScore = calculateSkillsScore(candidate.getSkills(), requiredSkills, result);
            result.setSkillsScore(skillsScore);
            result.setTotalRequiredSkills(requiredSkills.size());

            // 2. Calculer le score de localisation
            double locationScore = calculateLocationScore(candidate, offer);
            result.setLocationScore(locationScore);

            // 3. Calculer le score du type de contrat
            double contractScore = calculateContractTypeScore(candidate.getPreferredContractTypes(), offer.getContractType());
            result.setContractTypeScore(contractScore);

            // 4. Calculer le score d'expérience (basé sur les compétences)
            double experienceScore = calculateExperienceScore(candidate, requiredSkills);
            result.setExperienceScore(experienceScore);

            // 5. Calculer le score global pondéré
            double overallScore = (skillsScore * SKILLS_WEIGHT) +
                                 (locationScore * LOCATION_WEIGHT) +
                                 (contractScore * CONTRACT_WEIGHT) +
                                 (experienceScore * EXPERIENCE_WEIGHT);

            result.setOverallScore(overallScore);

            // Generate detailed explanations
            result.setScoreFormula(String.format("Score = (Comp. %.0f × 40%%) + (Loc. %.0f × 25%%) + (Contrat %.0f × 20%%) + (Exp. %.0f × 15%%) = %.0f%%",
                skillsScore, locationScore, contractScore, experienceScore, overallScore));
            
            result.setTextualExplanation(generateExplanation(result));

        } catch (Exception e) {
            System.err.println("Erreur calcul matching: " + e.getMessage());
            result.setOverallScore(0);
        }

        return result;
    }

    /**
     * Calcule le score de matching pour toutes les offres
     */
    public List<MatchingResult> calculateMatchForAllOffers(CandidateProfile candidate, List<JobOffer> offers) {
        List<MatchingResult> results = new ArrayList<>();

        for (JobOffer offer : offers) {
            MatchingResult result = calculateMatch(candidate, offer);
            results.add(result);
        }

        // Trier par score décroissant
        results.sort((a, b) -> Double.compare(b.getOverallScore(), a.getOverallScore()));

        return results;
    }

    /**
     * Calcule le score des compétences
     */
    private double calculateSkillsScore(List<CandidateSkill> candidateSkills, List<OfferSkill> requiredSkills, MatchingResult result) {
        if (requiredSkills.isEmpty()) {
            return 100; // Pas de compétences requises = 100%
        }

        int matchedCount = 0;
        double totalScore = 0;

        for (OfferSkill required : requiredSkills) {
            CandidateSkill matched = findMatchingSkill(candidateSkills, required.getSkillName());

            if (matched != null) {
                matchedCount++;
                // Bonus si le niveau est égal ou supérieur
                double skillScore = calculateSkillLevelScore(matched.getLevel(), required.getLevelRequired());
                totalScore += skillScore;
                
                if (skillScore == 100.0) {
                    result.getMatchingSkills().add(required.getSkillName() + " (Niveau OK)");
                } else {
                    result.getPartialSkills().add(required.getSkillName() + " (Candidat: " + matched.getLevel().name() + " | Requis: " + required.getLevelRequired().name() + ")");
                }
            } else {
                result.getMissingSkills().add(required.getSkillName());
            }
        }

        // Score = (compétences matchées / compétences requises) * moyenne des scores de niveau
        if (matchedCount == 0) {
            return 0;
        }

        double averageLevelScore = totalScore / matchedCount;
        double matchRatio = (double) matchedCount / requiredSkills.size();

        return matchRatio * averageLevelScore;
    }

    /**
     * Trouve une compétence correspondante dans la liste du candidat
     */
    private CandidateSkill findMatchingSkill(List<CandidateSkill> candidateSkills, String requiredSkillName) {
        String normalizedRequired = normalizeSkillName(requiredSkillName);

        for (CandidateSkill skill : candidateSkills) {
            String normalizedCandidate = normalizeSkillName(skill.getSkillName());

            // Correspondance exacte ou partielle
            if (normalizedCandidate.equals(normalizedRequired) ||
                normalizedCandidate.contains(normalizedRequired) ||
                normalizedRequired.contains(normalizedCandidate)) {
                return skill;
            }
        }
        return null;
    }

    /**
     * Normalise le nom d'une compétence pour la comparaison
     */
    private String normalizeSkillName(String skillName) {
        return skillName.toLowerCase()
                       .trim()
                       .replaceAll("[^a-z0-9+#]", "");
    }

    /**
     * Calcule le score basé sur le niveau de compétence
     */
    private double calculateSkillLevelScore(Models.joboffers.SkillLevel candidateLevel, Models.joboffers.SkillLevel requiredLevel) {
        int candidateValue = getLevelValue(candidateLevel);
        int requiredValue = getLevelValue(requiredLevel);

        if (candidateValue >= requiredValue) {
            return 100;
        } else if (candidateValue == requiredValue - 1) {
            return 70;
        } else {
            return 30;
        }
    }

    private int getLevelValue(Models.joboffers.SkillLevel level) {
        return switch (level) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
        };
    }

    /**
     * Calcule le score de localisation basé sur la distance
     */
    private double calculateLocationScore(CandidateProfile candidate, JobOffer offer) {
        // Si l'offre est en remote, score parfait
        if (offer.getLocation() != null &&
            offer.getLocation().toLowerCase().contains("remote")) {
            return 100;
        }

        // Si les coordonnées sont disponibles, calculer la distance
        if (candidate.hasCoordinates() && offer.hasCoordinates()) {
            double distance = calculateDistance(
                candidate.getLatitude(), candidate.getLongitude(),
                offer.getLatitude(), offer.getLongitude()
            );

            if (distance <= DISTANCE_EXCELLENT) return 100;
            if (distance <= DISTANCE_GOOD) return 80;
            if (distance <= DISTANCE_MEDIUM) return 60;
            if (distance <= DISTANCE_FAR) return 40;
            return 20;
        }

        // Comparaison par nom de ville
        if (candidate.getLocation() != null && offer.getLocation() != null) {
            String candidateCity = normalizeLocation(candidate.getLocation());
            String offerCity = normalizeLocation(offer.getLocation());

            if (candidateCity.equals(offerCity)) {
                return 100; // Même ville
            } else if (candidateCity.contains(offerCity) || offerCity.contains(candidateCity)) {
                return 80;  // Ville proche/région
            }
        }

        return 50; // Valeur par défaut si comparaison impossible
    }

    private String normalizeLocation(String location) {
        return location.toLowerCase()
                      .trim()
                      .replaceAll("[^a-z]", "");
    }

    /**
     * Calcule la distance entre deux points (formule Haversine)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Rayon de la Terre en km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Calcule le score du type de contrat
     */
    private double calculateContractTypeScore(List<Models.joboffers.ContractType> preferredTypes, Models.joboffers.ContractType offerType) {
        if (preferredTypes == null || preferredTypes.isEmpty()) {
            return 80; // Pas de préférence = score moyen+
        }

        if (preferredTypes.contains(offerType)) {
            return 100; // Correspondance parfaite
        }

        // Correspondances partielles
        for (Models.joboffers.ContractType preferred : preferredTypes) {
            if (areContractTypesCompatible(preferred, offerType)) {
                return 70;
            }
        }

        return 30; // Pas de correspondance
    }

    /**
     * Vérifie si deux types de contrat sont compatibles
     */
    private boolean areContractTypesCompatible(Models.joboffers.ContractType type1, Models.joboffers.ContractType type2) {
        // CDI et FULL_TIME sont compatibles
        if ((type1 == Models.joboffers.ContractType.CDI && type2 == Models.joboffers.ContractType.FULL_TIME) ||
            (type1 == Models.joboffers.ContractType.FULL_TIME && type2 == Models.joboffers.ContractType.CDI)) {
            return true;
        }
        // CDD et PART_TIME peuvent être compatibles
        if ((type1 == Models.joboffers.ContractType.CDD && type2 == Models.joboffers.ContractType.PART_TIME) ||
            (type1 == Models.joboffers.ContractType.PART_TIME && type2 == Models.joboffers.ContractType.CDD)) {
            return true;
        }
        return false;
    }

    /**
     * Calcule le score d'expérience
     */
    private double calculateExperienceScore(CandidateProfile candidate, List<OfferSkill> requiredSkills) {
        int candidateExperience = candidate.getYearsOfExperience();

        // Estimer l'expérience requise basée sur les niveaux des compétences
        int estimatedRequired = estimateRequiredExperience(requiredSkills);

        if (candidateExperience >= estimatedRequired) {
            return 100;
        } else if (candidateExperience >= estimatedRequired - 1) {
            return 80;
        } else if (candidateExperience >= estimatedRequired - 2) {
            return 60;
        } else {
            return 40;
        }
    }

    /**
     * Estime l'expérience requise basée sur les niveaux de compétences
     */
    private int estimateRequiredExperience(List<OfferSkill> skills) {
        if (skills.isEmpty()) return 0;

        int maxLevel = 0;
        for (OfferSkill skill : skills) {
            int level = getLevelValue(skill.getLevelRequired());
            if (level > maxLevel) {
                maxLevel = level;
            }
        }

        // Estimation: BEGINNER=0-1, INTERMEDIATE=2-3, ADVANCED=4+
        return switch (maxLevel) {
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 4;
            default -> 0;
        };
    }

    /**
     * Retourne les offres les mieux matchées pour un candidat
     */
    public List<MatchingResult> getTopMatches(CandidateProfile candidate, List<JobOffer> offers, int limit) {
        List<MatchingResult> allResults = calculateMatchForAllOffers(candidate, offers);

        if (allResults.size() <= limit) {
            return allResults;
        }

        return allResults.subList(0, limit);
    }

    /**
     * Retourne uniquement les offres avec un score minimum
     */
    public List<MatchingResult> getMatchesAboveThreshold(CandidateProfile candidate, List<JobOffer> offers, double minScore) {
        List<MatchingResult> allResults = calculateMatchForAllOffers(candidate, offers);

        return allResults.stream()
                        .filter(r -> r.getOverallScore() >= minScore)
                        .toList();
    }

    /**
     * Génère une explication textuelle du score de matching
     */
    private String generateExplanation(MatchingResult result) {
        StringBuilder explanation = new StringBuilder();
        
        if (result.getOverallScore() >= 85) {
            explanation.append("Excellent profil pour cette offre ! ");
        } else if (result.getOverallScore() >= 70) {
            explanation.append("Bon profil, mais quelques ajustements ou compétences supplémentaires seraient bénéfiques. ");
        } else if (result.getOverallScore() >= 50) {
            explanation.append("Profil moyen, certains critères importants ne sont pas remplis. ");
        } else {
            explanation.append("Le profil ne correspond pas aux attentes principales de l'offre. ");
        }
        
        if (!result.getMissingSkills().isEmpty()) {
            explanation.append("Il manque ").append(result.getMissingSkills().size()).append(" compétence(s) technique(s) requise(s). ");
        }
        
        if (result.getLocationScore() < 50) {
            explanation.append("La localisation géographique semble éloignée. ");
        }
        
        if (result.getExperienceScore() < 60) {
            explanation.append("Le niveau d'expérience est légèrement inférieur à ce qui est attendu.");
        }
        
        return explanation.toString().trim();
    }
}



