package Models.joboffers;

public class OfferSkill {
    private Long id;
    private Long offerId;
    private String skillName;
    private SkillLevel levelRequired;


    // Constructors
    public OfferSkill() {
    }

    public OfferSkill(Long id, Long offerId, String skillName, SkillLevel levelRequired) {
        this.id = id;
        this.offerId = offerId;
        this.skillName = skillName;
        this.levelRequired = levelRequired;
    }

    public OfferSkill(Long offerId, String skillName, SkillLevel levelRequired) {
        this.offerId = offerId;
        this.skillName = skillName;
        this.levelRequired = levelRequired;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public SkillLevel getLevelRequired() {
        return levelRequired;
    }

    public void setLevelRequired(SkillLevel levelRequired) {
        this.levelRequired = levelRequired;
    }

    @Override
    public String toString() {
        return "OfferSkill{" +
                "id=" + id +
                ", offerId=" + offerId +
                ", skillName='" + skillName + '\'' +
                ", levelRequired=" + levelRequired +
                '}';
    }
}

