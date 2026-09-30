package com.trainticket.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe singleton managing the active user session in RailFlow.
 * Enforces pure Model separation with zero UI imports.
 */
public final class AuthSession {

    private static final Logger logger = LoggerFactory.getLogger(AuthSession.class);
    private static final AuthSession INSTANCE = new AuthSession();

    private volatile User currentUser;
    private final List<AuthStateListener> listeners = new CopyOnWriteArrayList<>();

    private AuthSession() {
        this.currentUser = null; // Defaults to GUEST
    }

    public static AuthSession getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the currently authenticated user, or null if operating in guest mode.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Gets the active role (GUEST, PASSENGER, or ADMIN).
     */
    public UserRole getRole() {
        User user = currentUser;
        return (user != null) ? user.getRole() : UserRole.GUEST;
    }

    /**
     * Returns true if a user or admin is currently signed in.
     */
    public boolean isAuthenticated() {
        return currentUser != null;
    }

    /**
     * Returns true if the active session is an authenticated Administrator.
     */
    public boolean isAdmin() {
        return getRole() == UserRole.ADMIN;
    }

    /**
     * Returns true if the active session is an authenticated Passenger.
     */
    public boolean isPassenger() {
        return getRole() == UserRole.PASSENGER;
    }

    /**
     * Returns true if the active session is an unauthenticated Guest.
     */
    public boolean isGuest() {
        return getRole() == UserRole.GUEST;
    }

    /**
     * Sets the active authenticated user and notifies registered listeners.
     *
     * @param user Authenticated User
     */
    public synchronized void login(User user) {
        Objects.requireNonNull(user, "User cannot be null on login.");
        this.currentUser = user;
        logger.info("Session updated: User '{}' logged in with role {}.", user.getUsername(), user.getRole());
        notifyListeners();
    }

    /**
     * Clears the active session back to GUEST mode and notifies registered listeners.
     */
    public synchronized void logout() {
        String prevUser = (currentUser != null) ? currentUser.getUsername() : "GUEST";
        this.currentUser = null;
        logger.info("Session updated: User '{}' logged out. Reverted to GUEST.", prevUser);
        notifyListeners();
    }

    /**
     * Registers a listener for authentication state changes.
     *
     * @param listener Callback listener
     */
    public void addAuthStateListener(AuthStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregisters a previously registered authentication state listener.
     */
    public void removeAuthStateListener(AuthStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        User user = currentUser;
        UserRole role = getRole();
        for (AuthStateListener listener : listeners) {
            try {
                listener.onAuthStateChanged(user, role);
            } catch (Exception ex) {
                logger.error("Error in AuthStateListener: {}", ex.getMessage(), ex);
            }
        }
    }
}
