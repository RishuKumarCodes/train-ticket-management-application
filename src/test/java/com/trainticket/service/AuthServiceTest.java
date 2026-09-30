package com.trainticket.service;

import com.trainticket.model.User;
import com.trainticket.model.UserRole;
import com.trainticket.model.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    @DisplayName("Admin login succeeds with default fixed credentials 'admin'/'admin'")
    void testAdminFixedLogin() {
        User admin = authService.loginAdmin("admin", "admin".toCharArray());
        assertNotNull(admin);
        assertEquals("admin", admin.getUsername());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertTrue(admin.isAdmin());
    }

    @Test
    @DisplayName("Admin login rejects incorrect access key")
    void testAdminInvalidPassword() {
        assertThrows(SecurityException.class, () ->
                authService.loginAdmin("admin", "wrong_key".toCharArray())
        );
    }

    @Test
    @DisplayName("Passenger registration with email only (no username passed) and login via email")
    void testRegistrationWithEmailOnly() {
        long ts = System.currentTimeMillis();
        String email = "email_user_" + ts + "@example.com";
        char[] password = "Password@123".toCharArray();

        User registered = authService.register("Rishu Kumar", email, null, password);
        assertNotNull(registered);
        assertNotNull(registered.getUsername(), "Username should be auto-derived internally");
        assertEquals(email, registered.getEmail());
        assertEquals(UserRole.PASSENGER, registered.getRole());

        // Login with email
        User loggedIn = authService.login(email, "Password@123".toCharArray());
        assertNotNull(loggedIn);
        assertEquals(registered.getId(), loggedIn.getId());
    }

    @Test
    @DisplayName("Passenger registration with phone only (no username passed) and login via phone")
    void testRegistrationWithPhoneOnly() {
        long ts = System.currentTimeMillis();
        String phone = "98" + (ts % 100000000);
        char[] password = "Password@123".toCharArray();

        User registered = authService.register("Phone Passenger", null, phone, password);
        assertNotNull(registered);
        assertNotNull(registered.getUsername(), "Username should be auto-derived internally");
        assertEquals(phone, registered.getPhone());
        assertEquals(UserRole.PASSENGER, registered.getRole());

        // Login with phone number
        User loggedIn = authService.login(phone, "Password@123".toCharArray());
        assertNotNull(loggedIn);
        assertEquals(registered.getId(), loggedIn.getId());
    }

    @Test
    @DisplayName("Passenger registration with both email and phone allows login with either")
    void testRegistrationWithBothEmailAndPhone() {
        long ts = System.currentTimeMillis();
        String email = "both_" + ts + "@example.com";
        String phone = "97" + (ts % 100000000);
        char[] password = "Password@123".toCharArray();

        User registered = authService.register("Both Passenger", email, phone, password);
        assertNotNull(registered);

        // Login with email
        User loggedWithEmail = authService.login(email, "Password@123".toCharArray());
        assertNotNull(loggedWithEmail);
        assertEquals(registered.getId(), loggedWithEmail.getId());

        // Login with phone
        User loggedWithPhone = authService.login(phone, "Password@123".toCharArray());
        assertNotNull(loggedWithPhone);
        assertEquals(registered.getId(), loggedWithPhone.getId());
    }

    @Test
    @DisplayName("Registration rejects missing both email and phone")
    void testRegistrationRejectsMissingContact() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("No Contact", null, "", "Password@123".toCharArray())
        );
    }

    @Test
    @DisplayName("Registration rejects invalid email format")
    void testInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("Jane Doe", "not-an-email", null, "Password@123".toCharArray())
        );
    }

    @Test
    @DisplayName("Registration rejects short password (< 6 chars)")
    void testShortPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.register("Jane Doe", "jane@example.com", null, "123".toCharArray())
        );
    }

    @Test
    @DisplayName("Passenger account cannot access admin console")
    void testPassengerBlockedFromAdminLogin() {
        long ts = System.currentTimeMillis();
        String email = "passenger_" + ts + "@example.com";
        User user = authService.register("Test Passenger", email, null, "Pass12345".toCharArray());

        assertThrows(SecurityException.class, () ->
                authService.loginAdmin(email, "Pass12345".toCharArray())
        );
    }
}
