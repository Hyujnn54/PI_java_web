package Models;

import java.time.LocalDateTime;

public class User {
    private long id;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private boolean isActive;
    private RoleEnum role;
    private LocalDateTime createdAt;

    public User() {}

    public User(long id, String email, String password, String firstName,
                String lastName, String phone, boolean isActive, RoleEnum role, LocalDateTime createdAt) {
        this.id = id; this.email = email; this.password = password;
        this.firstName = firstName; this.lastName = lastName; this.phone = phone;
        this.isActive = isActive; this.role = role; this.createdAt = createdAt;
    }

    public User(String email, String password, String firstName,
                String lastName, String phone, boolean isActive, RoleEnum role) {
        this.email = email; this.password = password; this.firstName = firstName;
        this.lastName = lastName; this.phone = phone; this.isActive = isActive; this.role = role;
    }

    // Getters et Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public RoleEnum getRole() {
        return role;
    }

    public void setRole(RoleEnum role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", isActive=" + isActive +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}
