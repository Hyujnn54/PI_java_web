package Models.user;

public class Admin extends User {
    private String assignedArea;

    public Admin() {}

    public Admin(String email, String password, String firstName, String lastName, String phoneNumber,
                 String assignedArea) {
        super(email, password, firstName, lastName, phoneNumber);
        this.assignedArea = assignedArea;
    }

    public String getAssignedArea() { return assignedArea; }
    public void setAssignedArea(String assignedArea) { this.assignedArea = assignedArea; }
}