package Models.joboffers;

/**
 * Model class for storing dynamically calculated moderation scores for a Job Offer text.
 */
public class ModerationResult {
    private double toxicity;
    private double insult;
    private double threat;
    private double identityAttack;

    public ModerationResult(double toxicity, double insult, double threat, double identityAttack) {
        this.toxicity = toxicity;
        this.insult = insult;
        this.threat = threat;
        this.identityAttack = identityAttack;
    }

    public double getToxicity() { return toxicity; }
    public void setToxicity(double toxicity) { this.toxicity = toxicity; }

    public double getInsult() { return insult; }
    public void setInsult(double insult) { this.insult = insult; }

    public double getThreat() { return threat; }
    public void setThreat(double threat) { this.threat = threat; }

    public double getIdentityAttack() { return identityAttack; }
    public void setIdentityAttack(double identityAttack) { this.identityAttack = identityAttack; }

    /**
     * Revoie true si n'importe quel score dépasse les seuils définis.
     * Seuils abaissés pour capter la discrimination subtile : 
     * TOXICITY >= 0.50, INSULT >= 0.50, THREAT >= 0.40, IDENTITY_ATTACK >= 0.25
     */
    public boolean isFlagged() {
        return toxicity >= 0.50 || insult >= 0.50 || threat >= 0.40 || identityAttack >= 0.25;
    }

    @Override
    public String toString() {
        return String.format("Toxicité: %.2f\nInsulte: %.2f\nMenace: %.2f\nAttaque d'identité: %.2f", 
                toxicity, insult, threat, identityAttack);
    }
}
