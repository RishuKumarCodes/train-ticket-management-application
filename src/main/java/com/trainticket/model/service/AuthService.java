package com.trainticket.model.service;

import com.trainticket.model.User;
import com.trainticket.model.UserRole;
import com.trainticket.model.dao.UserDAO;
import com.trainticket.model.dao.UserRecord;
import com.trainticket.util.PasswordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Service encapsulating authentication, registration, input validation,
 * and role-based access verification rules.
 * Strictly decoupled from any UI classes.
 */
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");

    private final UserDAO userDAO;

    public AuthService() {
        this(new UserDAO());
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = Objects.requireNonNull(userDAO, "UserDAO cannot be null");
    }

    /**
     * Authenticates a general passenger using their registered email or mobile phone and password.
     *
     * @param identifier Email address or mobile phone number (or admin operator ID)
     * @param password   Raw password char array
     * @return Authenticated User entity
     * @throws SecurityException if credentials are invalid or account is suspended
     */
    public User login(String identifier, char[] password) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Email address or mobile phone number cannot be empty.");
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        Optional<UserRecord> recordOpt = userDAO.findByUsernameOrEmail(identifier);
        if (recordOpt.isEmpty()) {
            logger.warn("Failed login attempt: Identifier '{}' not found.", identifier);
            throw new SecurityException("Invalid email/phone or password.");
        }

        UserRecord record = recordOpt.get();
        boolean matches = PasswordUtils.verifyPassword(password, record.salt(), record.passwordHash());
        if (!matches) {
            logger.warn("Failed login attempt: Password mismatch for '{}'.", identifier);
            throw new SecurityException("Invalid email/phone or password.");
        }

        User user = record.user();
        if (!user.isActive()) {
            throw new SecurityException("Your account is currently suspended. Please contact station support.");
        }

        logger.info("User '{}' successfully authenticated as {}.", user.getUsername(), user.getRole());
        return user;
    }

    /**
     * Authenticates an administrator into the dedicated Railway Station Master Portal.
     * Enforces fixed credentials or valid admin database accounts.
     *
     * @param username Administrator username
     * @param password Administrator password char array
     * @return Authenticated Admin User entity
     * @throws SecurityException if credentials do not belong to an authorized Administrator
     */
    public User loginAdmin(String username, char[] password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Admin Operator ID cannot be empty.");
        }
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Access Key cannot be empty.");
        }

        // 1. Direct fixed credentials fast-path check
        String candidateUser = username.trim();
        if ("admin".equalsIgnoreCase(candidateUser)) {
            boolean validFixed = PasswordUtils.verifyPassword(password, UserDAO.DEFAULT_ADMIN_SALT, UserDAO.DEFAULT_ADMIN_HASH);
            if (validFixed) {
                logger.info("Administrator '{}' logged in via fixed credentials master key.", candidateUser);
                return User.createAdmin(1L, "admin", "admin@railflow.internal", "Station Master Admin");
            }
        }

        // 2. Database / fallback repository lookup
        Optional<UserRecord> recordOpt = userDAO.findByUsernameOrEmail(candidateUser);
        if (recordOpt.isEmpty()) {
            logger.warn("Unauthorized admin access attempt with Operator ID '{}'.", candidateUser);
            throw new SecurityException("Access denied: Invalid Operator ID or Access Key.");
        }

        UserRecord record = recordOpt.get();
        User user = record.user();

        if (user.getRole() != UserRole.ADMIN) {
            logger.warn("Role violation: Passenger '{}' attempted to access Admin Console.", candidateUser);
            throw new SecurityException("Access denied: Operator ID does not possess administrative privileges.");
        }

        boolean matches = PasswordUtils.verifyPassword(password, record.salt(), record.passwordHash());
        if (!matches) {
            logger.warn("Password mismatch for administrator '{}'.", candidateUser);
            throw new SecurityException("Access denied: Invalid Operator ID or Access Key.");
        }

        logger.info("Administrator '{}' successfully authenticated into Station Master Portal.", user.getUsername());
        return user;
    }

    /**
     * Registers a new passenger account using Email and/or Mobile Phone as primary identity.
     * Username is automatically derived internally without requiring the user to create one.
     *
     * @param fullName Full legal passenger name
     * @param email    Email address (optional if phone is provided)
     * @param phone    Mobile phone number (optional if email is provided)
     * @param password Raw password char array (min 6 characters)
     * @return Persisted User entity
     * @throws IllegalArgumentException if validation constraints fail
     * @throws IllegalStateException    if email or phone is already registered
     */
    public User register(String fullName, String email, String phone, char[] password) {
        if (fullName == null || fullName.trim().length() < 2) {
            throw new IllegalArgumentException("Full name must be at least 2 characters.");
        }

        String cleanEmail = (email != null && !email.isBlank()) ? email.trim().toLowerCase() : null;
        String cleanPhone = (phone != null && !phone.isBlank()) ? phone.trim() : null;

        if (cleanEmail == null && cleanPhone == null) {
            throw new IllegalArgumentException("Please enter either an email address or mobile phone number to register.");
        }

        if (cleanEmail != null) {
            if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
                throw new IllegalArgumentException("Please provide a valid email address (e.g. name@domain.com).");
            }
            if (userDAO.existsByEmail(cleanEmail)) {
                throw new IllegalStateException("Email '" + cleanEmail + "' is already registered. Please sign in instead.");
            }
        }

        if (cleanPhone != null) {
            String digitsOnly = cleanPhone.replaceAll("[^0-9]", "");
            if (digitsOnly.length() < 7 || digitsOnly.length() > 15) {
                throw new IllegalArgumentException("Please provide a valid mobile phone number (7-15 digits).");
            }
            if (userDAO.existsByPhone(cleanPhone)) {
                throw new IllegalStateException("Phone number '" + cleanPhone + "' is already registered. Please sign in instead.");
            }
        }

        if (password == null || password.length < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        // Internally derive a unique username from email or phone
        String baseUsername;
        if (cleanEmail != null) {
            String localPart = cleanEmail.split("@")[0].replaceAll("[^a-zA-Z0-9_]", "");
            baseUsername = localPart.isEmpty() ? "passenger" : localPart;
        } else {
            String digitsOnly = cleanPhone.replaceAll("[^0-9]", "");
            baseUsername = "user_" + (digitsOnly.length() > 6 ? digitsOnly.substring(digitsOnly.length() - 6) : digitsOnly);
        }

        String internalUsername = baseUsername;
        int suffix = 1;
        while (userDAO.existsByUsername(internalUsername)) {
            internalUsername = baseUsername + "_" + suffix;
            suffix++;
        }

        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hashPassword(password, salt);

        User newPassenger = User.createNewPassenger(internalUsername, cleanEmail, cleanPhone, fullName.trim());

        try {
            User created = userDAO.createUser(newPassenger, hash, salt);
            logger.info("Successfully registered new passenger '{}' with ID {}.", created.getUsername(), created.getId());
            return created;
        } catch (SQLException ex) {
            logger.error("Failed to persist new user to database: {}", ex.getMessage(), ex);
            throw new RuntimeException("Could not complete registration due to database error. Please try again.", ex);
        }
    }

    /**
     * Backward-compatible registration method allowing optional explicit username.
     */
    public User register(String fullName, String username, String email, String phone, char[] password) {
        return register(fullName, email, phone, password);
    }
}
