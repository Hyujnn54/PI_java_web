package Models;

public class Recruiter extends User {
    private String companyName;
    private String companyLocation;

    public Recruiter() {}

    public Recruiter(String email, String password, String firstName, String lastName, String phoneNumber,
                     String companyName, String companyLocation) {
        super(email, password, firstName, lastName, phoneNumber);
        this.companyName = companyName;
        this.companyLocation = companyLocation;
    }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyLocation() { return companyLocation; }
    public void setCompanyLocation(String companyLocation) { this.companyLocation = companyLocation; }
}