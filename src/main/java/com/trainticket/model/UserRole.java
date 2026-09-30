package com.trainticket.model;

/**
 * Role-Based Access Control (RBAC) roles for RailFlow.
 */
public enum UserRole {
    /**
     * Unauthenticated guest passenger.
     * Can search trains, view timetables, and inspect station routes.
     */
    GUEST("Guest Explorer"),

    /**
     * Authenticated passenger.
     * Can book tickets, manage reservations, select berths, and cancel bookings.
     */
    PASSENGER("Passenger"),

    /**
     * Authorized railway station master / system administrator.
     * Has access to the dedicated Admin Command Center to manage trains, routes,
     * schedules, quotas, and audit passenger manifests.
     */
    ADMIN("Station Master Administrator");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isAdmin() {
        return this == ADMIN;
    }

    public boolean isPassenger() {
        return this == PASSENGER;
    }

    public boolean isGuest() {
        return this == GUEST;
    }
}
