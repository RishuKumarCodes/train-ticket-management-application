package com.trainticket.controller;

import com.trainticket.model.AuthSession;
import com.trainticket.model.User;
import com.trainticket.model.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Controller orchestrating authentication workflows between Views and Models.
 * Strictly guarantees that heavy cryptographic hashing executes asynchronously
 * off the Swing Event Dispatch Thread (EDT) and all view updates dispatch on the EDT.
 */
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final AuthSession authSession;

    public AuthController() {
        this(new AuthService(), AuthSession.getInstance());
    }

    public AuthController(AuthService authService, AuthSession authSession) {
        this.authService = authService;
        this.authSession = authSession;
    }

    /**
     * Executes asynchronous passenger login.
     *
     * @param identifier Username or email
     * @param password   Raw password char array
     * @param onSuccess  EDT callback on successful authentication
     * @param onError    EDT callback with human-readable error message
     */
    public void handleLogin(String identifier, char[] password, Consumer<User> onSuccess, Consumer<String> onError) {
        char[] passCopy = Arrays.copyOf(password, password.length);

        CompletableFuture.runAsync(() -> {
            try {
                User user = authService.login(identifier, passCopy);
                SwingUtilities.invokeLater(() -> {
                    authSession.login(user);
                    if (onSuccess != null) {
                        onSuccess.accept(user);
                    }
                });
            } catch (Exception ex) {
                logger.warn("Passenger login failed for '{}': {}", identifier, ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    if (onError != null) {
                        onError.accept(ex.getMessage());
                    }
                });
            } finally {
                Arrays.fill(passCopy, '\0');
            }
        });
    }

    /**
     * Executes asynchronous administrator login.
     *
     * @param username  Admin Operator ID
     * @param password  Access Key char array
     * @param onSuccess EDT callback on successful authentication
     * @param onError   EDT callback with human-readable error message
     */
    public void handleAdminLogin(String username, char[] password, Consumer<User> onSuccess, Consumer<String> onError) {
        char[] passCopy = Arrays.copyOf(password, password.length);

        CompletableFuture.runAsync(() -> {
            try {
                User admin = authService.loginAdmin(username, passCopy);
                SwingUtilities.invokeLater(() -> {
                    authSession.login(admin);
                    if (onSuccess != null) {
                        onSuccess.accept(admin);
                    }
                });
            } catch (Exception ex) {
                logger.warn("Admin portal login failed for '{}': {}", username, ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    if (onError != null) {
                        onError.accept(ex.getMessage());
                    }
                });
            } finally {
                Arrays.fill(passCopy, '\0');
            }
        });
    }

    /**
     * Executes asynchronous passenger account registration.
     *
     * @param fullName        Full name
     * @param username        Desired username
     * @param email           Email address
     * @param phone           Phone number
     * @param password        Password char array
     * @param confirmPassword Confirmation password char array
     * @param onSuccess       EDT callback on successful registration
     * @param onError         EDT callback with human-readable error message
     */
    public void handleRegister(String fullName, String email, String phone,
                               char[] password, char[] confirmPassword,
                               Consumer<User> onSuccess, Consumer<String> onError) {

        if (!Arrays.equals(password, confirmPassword)) {
            if (onError != null) {
                onError.accept("Passwords do not match. Please verify and retype.");
            }
            return;
        }

        char[] passCopy = Arrays.copyOf(password, password.length);

        CompletableFuture.runAsync(() -> {
            try {
                User created = authService.register(fullName, email, phone, passCopy);
                SwingUtilities.invokeLater(() -> {
                    authSession.login(created);
                    if (onSuccess != null) {
                        onSuccess.accept(created);
                    }
                });
            } catch (Exception ex) {
                logger.warn("Registration failed for '{}': {}", fullName, ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    if (onError != null) {
                        onError.accept(ex.getMessage());
                    }
                });
            } finally {
                Arrays.fill(passCopy, '\0');
            }
        });
    }

    public void handleRegister(String fullName, String username, String email, String phone,
                               char[] password, char[] confirmPassword,
                               Consumer<User> onSuccess, Consumer<String> onError) {
        handleRegister(fullName, email, phone, password, confirmPassword, onSuccess, onError);
    }

    /**
     * Logs the current user out, returning session to GUEST mode.
     */
    public void handleLogout() {
        authSession.logout();
    }
}
