package com.trainticket.model;

/**
 * Functional callback interface invoked when the authentication state changes.
 */
@FunctionalInterface
public interface AuthStateListener {
    /**
     * Notified on user login, logout, or role change.
     *
     * @param currentUser The currently authenticated User (or null if GUEST)
     * @param role        The current effective UserRole (GUEST, PASSENGER, or ADMIN)
     */
    void onAuthStateChanged(User currentUser, UserRole role);
}
