package Models.events;

public class EventRecruiter {
    private long id; // Shared PK with users.id
    private String companyName;
    private String companyLocation;
    private String companyDescription;

    // Constructeur par défaut
    public EventRecruiter() {
    }

    // Constructeur complet
    public EventRecruiter(long id, String companyName, String companyLocation,
            String companyDescription) {
        this.id = id;
        this.companyName = companyName;
        this.companyLocation = companyLocation;
        this.companyDescription = companyDescription;
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyLocation() {
        return companyLocation;
    }

    public void setCompanyLocation(String companyLocation) {
        this.companyLocation = companyLocation;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    @Override
    public String toString() {
        return "EventRecruiter{" +
                "id=" + id +
                ", companyName='" + companyName + '\'' +
                ", companyLocation='" + companyLocation + '\'' +
                ", companyDescription='" + companyDescription + '\'' +
                '}';
    }
}
