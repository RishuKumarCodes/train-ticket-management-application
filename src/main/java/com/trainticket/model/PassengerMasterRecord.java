package com.trainticket.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Immutable domain model representing a saved passenger in a user's master list.
 * Facilitates 1-click auto-fill during booking.
 */
public final class PassengerMasterRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final Long userId;
    private final String fullName;
    private final int age;
    private final String gender;
    private final String berthPreference;

    public PassengerMasterRecord(Long id, Long userId, String fullName, int age, String gender, String berthPreference) {
        this.id = id;
        this.userId = userId;
        this.fullName = Objects.requireNonNull(fullName, "Full name cannot be null").trim();
        this.age = Math.max(1, Math.min(125, age));
        this.gender = (gender != null && !gender.isBlank()) ? gender.trim().toUpperCase() : "M";
        this.berthPreference = (berthPreference != null && !berthPreference.isBlank()) 
                ? berthPreference.trim().toUpperCase() : "NO PREFERENCE";
    }

    public static PassengerMasterRecord create(Long userId, String fullName, int age, String gender, String berthPreference) {
        return new PassengerMasterRecord(null, userId, fullName, age, gender, berthPreference);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public int getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getBerthPreference() {
        return berthPreference;
    }

    @Override
    public String toString() {
        return fullName + " (" + age + ", " + gender + ") • " + berthPreference;
    }
}
