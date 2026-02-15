package Models;

public class Candidate {
    private long id; // Shared PK with users.id
    private String location;
    private String educationLevel;
    private int experienceYears;
    private String cv_path;

    // Constructeur par défaut
    public Candidate() {
    }

    // Constructeur complet
    public Candidate(long id, String location, String educationLevel,
            int experienceYears, String cv_path) {
        this.id = id;
        this.location = location;
        this.educationLevel = educationLevel;
        this.experienceYears = experienceYears;
        this.cv_path = cv_path;
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getEducationLevel() {
        return educationLevel;
    }

    public void setEducationLevel(String educationLevel) {
        this.educationLevel = educationLevel;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getCvPath() {
        return cv_path;
    }

    public void setCvPath(String cvPath) {
        this.cv_path = cvPath;
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "id=" + id +
                ", location='" + location + '\'' +
                ", educationLevel='" + educationLevel + '\'' +
                ", experienceYears=" + experienceYears +
                ", cvPath='" + cv_path + '\'' +
                '}';
    }
}
