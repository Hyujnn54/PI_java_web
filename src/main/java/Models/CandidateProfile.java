package Models;

import java.util.ArrayList;
import java.util.List;

/**
 * Modèle représentant le profil d'un candidat pour le matching
 */
public class CandidateProfile {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String location;
    private Double latitude;
    private Double longitude;
    private List<CandidateSkill> skills;
    private List<ContractType> preferredContractTypes;
    private int yearsOfExperience;
    private String desiredPosition;
    private Double expectedSalaryMin;
    private Double expectedSalaryMax;

    public CandidateProfile() {
        this.skills = new ArrayList<>();
        this.preferredContractTypes = new ArrayList<>();
    }

    public CandidateProfile(Long userId, String name, String location) {
        this();
        this.userId = userId;
        this.name = name;
        this.location = location;
    }

    // Classe interne pour les compétences du candidat
    public static class CandidateSkill {
        private String skillName;
        private SkillLevel level;
        private int yearsOfExperience;

        public CandidateSkill(String skillName, SkillLevel level) {
            this.skillName = skillName;
            this.level = level;
            this.yearsOfExperience = 0;
        }

        public CandidateSkill(String skillName, SkillLevel level, int yearsOfExperience) {
            this.skillName = skillName;
            this.level = level;
            this.yearsOfExperience = yearsOfExperience;
        }

        public String getSkillName() { return skillName; }
        public void setSkillName(String skillName) { this.skillName = skillName; }
        public SkillLevel getLevel() { return level; }
        public void setLevel(SkillLevel level) { this.level = level; }
        public int getYearsOfExperience() { return yearsOfExperience; }
        public void setYearsOfExperience(int years) { this.yearsOfExperience = years; }
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public List<CandidateSkill> getSkills() { return skills; }
    public void setSkills(List<CandidateSkill> skills) { this.skills = skills; }

    public void addSkill(CandidateSkill skill) {
        this.skills.add(skill);
    }

    public void addSkill(String skillName, SkillLevel level) {
        this.skills.add(new CandidateSkill(skillName, level));
    }

    public List<ContractType> getPreferredContractTypes() { return preferredContractTypes; }
    public void setPreferredContractTypes(List<ContractType> types) { this.preferredContractTypes = types; }

    public void addPreferredContractType(ContractType type) {
        if (!this.preferredContractTypes.contains(type)) {
            this.preferredContractTypes.add(type);
        }
    }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int years) { this.yearsOfExperience = years; }

    public String getDesiredPosition() { return desiredPosition; }
    public void setDesiredPosition(String position) { this.desiredPosition = position; }

    public Double getExpectedSalaryMin() { return expectedSalaryMin; }
    public void setExpectedSalaryMin(Double salary) { this.expectedSalaryMin = salary; }

    public Double getExpectedSalaryMax() { return expectedSalaryMax; }
    public void setExpectedSalaryMax(Double salary) { this.expectedSalaryMax = salary; }

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}

