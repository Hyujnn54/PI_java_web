package Models;

public class CandidateSkill {
    private Long id;
    private Long candidateId;
    private String skillName;
    private SkillLevel level;

    public CandidateSkill() {}

    public CandidateSkill(Long id, Long candidateId, String skillName, SkillLevel level) {
        this.id = id;
        this.candidateId = candidateId;
        this.skillName = skillName;
        this.level = level;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCandidateId() { return candidateId; }
    public void setCandidateId(Long candidateId) { this.candidateId = candidateId; }

    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }

    public SkillLevel getLevel() { return level; }
    public void setLevel(SkillLevel level) { this.level = level; }
}
