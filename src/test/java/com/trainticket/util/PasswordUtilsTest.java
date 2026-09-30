package com.trainticket.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilsTest {

    @Test
    @DisplayName("generateSalt should produce unique 32-character hex strings")
    void testGenerateSalt() {
        String salt1 = PasswordUtils.generateSalt();
        String salt2 = PasswordUtils.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertEquals(32, salt1.length());
        assertEquals(32, salt2.length());
        assertNotEquals(salt1, salt2, "Two generated salts should be cryptographically unique");
    }

    @Test
    @DisplayName("hashPassword should produce consistent hashes for identical inputs")
    void testHashConsistency() {
        String password = "SecurePassword123!";
        String salt = PasswordUtils.generateSalt();

        String hash1 = PasswordUtils.hashPassword(password, salt);
        String hash2 = PasswordUtils.hashPassword(password, salt);

        assertEquals(64, hash1.length(), "SHA-512 derived 256-bit key should be 64 hex characters");
        assertEquals(hash1, hash2, "Identical password and salt must produce the exact same hash");
    }

    @Test
    @DisplayName("verifyPassword should return true for correct password and false for incorrect")
    void testVerifyPassword() {
        String password = "admin";
        String salt = PasswordUtils.generateSalt();
        String expectedHash = PasswordUtils.hashPassword(password, salt);

        assertTrue(PasswordUtils.verifyPassword(password, salt, expectedHash), "Correct password must verify to true");
        assertFalse(PasswordUtils.verifyPassword("wrongpass", salt, expectedHash), "Incorrect password must verify to false");
        assertFalse(PasswordUtils.verifyPassword("", salt, expectedHash), "Empty password must verify to false");
        String adminSalt = "0123456789abcdef0123456789abcdef";
        String adminHash = "463d121d09680f21241659d31b0389901d0479bcf389c231258d6c3f5c36050f";
        assertTrue(PasswordUtils.verifyPassword("admin", adminSalt, adminHash), "Precomputed admin hash must match password 'admin'");
        assertFalse(PasswordUtils.verifyPassword((String) null, salt, expectedHash), "Null password must return false");
    }
}
