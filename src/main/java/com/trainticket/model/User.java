package com.trainticket.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain entity representing an authenticated user or administrator in RailFlow.
 * Pure model with zero UI dependencies.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String email;
    private final String phone;
    private final String fullName;
    private final UserRole role;
    private final String status;
    private final LocalDateTime createdAt;

    public User(Long id, String username, String email, String phone, String fullName, UserRole role, String status, LocalDateTime createdAt) {
        this.id = id;
        this.username = Objects.requireNonNull(username, "Username cannot be null").trim();
        this.email = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : null;
        this.phone = (phone != null && !phone.isBlank()) ? phone.trim() : null;

        if (this.email == null && this.phone == null) {
            throw new IllegalArgumentException("User must have at least an email address or mobile phone number.");
        }

        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null").trim();
        this.role = role != null ? role : UserRole.PASSENGER;
        this.status = status != null ? status : "ACTIVE";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }

    public static User createNewPassenger(String username, String email, String phone, String fullName) {
        return new User(null, username, email, phone, fullName, UserRole.PASSENGER, "ACTIVE", LocalDateTime.now());
    }

    public static User createAdmin(Long id, String username, String email, String fullName) {
        return new User(id, username, email, null, fullName, UserRole.ADMIN, "ACTIVE", LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getFullName() {
        return fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", role=" + role +
                ", status='" + status + '\'' +
                '}';
    }
}
