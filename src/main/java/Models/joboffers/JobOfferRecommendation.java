package Models.joboffers;

import java.util.ArrayList;
import java.util.List;

/**
 * Model class for storing dynamically calculated recommendations for a Job Offer.
 */
public class JobOfferRecommendation {
    private int applicationCount;
    private double averageScore;
    private boolean isUnattractive;
    private List<String> recommendations;

    public JobOfferRecommendation() {
        this.recommendations = new ArrayList<>();
    }

    public int getApplicationCount() {
        return applicationCount;
    }

    public void setApplicationCount(int applicationCount) {
        this.applicationCount = applicationCount;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public boolean isUnattractive() {
        return isUnattractive;
    }

    public void setUnattractive(boolean unattractive) {
        isUnattractive = unattractive;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public void addRecommendation(String recommendation) {
        this.recommendations.add(recommendation);
    }
}
