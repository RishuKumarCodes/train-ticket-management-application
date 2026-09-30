package com.trainticket.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Cryptographic utility providing secure password hashing and verification
 * using PBKDF2 with HMAC-SHA-512 and cryptographically secure random salting.
 */
public final class PasswordUtils {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_BYTE_LENGTH = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtils() {
        // Utility class
    }

    /**
     * Generates a cryptographically strong 16-byte random salt as a hex string.
     *
     * @return Hex-encoded 32-character salt string
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    /**
     * Hashes a password char array using PBKDF2-HMAC-SHA-512 with the given hex-encoded salt.
     * Overwrites the provided password array with zeros after use for memory safety.
     *
     * @param password Char array of the raw password
     * @param hexSalt  Hex-encoded salt string
     * @return Hex-encoded 64-character hash string
     */
    public static String hashPassword(char[] password, String hexSalt) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        if (hexSalt == null || hexSalt.isBlank()) {
            throw new IllegalArgumentException("Salt cannot be null or empty.");
        }

        byte[] saltBytes = HexFormat.of().parseHex(hexSalt);
        PBEKeySpec spec = new PBEKeySpec(password, saltBytes, ITERATIONS, KEY_LENGTH_BITS);

        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to hash password with PBKDF2: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Convenience method to hash a String password.
     *
     * @param password Raw password string
     * @param hexSalt  Hex-encoded salt string
     * @return Hex-encoded hash string
     */
    public static String hashPassword(String password, String hexSalt) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null.");
        }
        return hashPassword(password.toCharArray(), hexSalt);
    }

    /**
     * Verifies whether a candidate password matches an expected hash in constant time,
     * protecting against timing side-channel attacks.
     *
     * @param candidatePassword Raw candidate password char array
     * @param hexSalt           Hex-encoded salt used during initial hashing
     * @param expectedHexHash   Expected hex-encoded hash string
     * @return true if candidate password matches expected hash, false otherwise
     */
    public static boolean verifyPassword(char[] candidatePassword, String hexSalt, String expectedHexHash) {
        if (candidatePassword == null || hexSalt == null || expectedHexHash == null) {
            return false;
        }

        try {
            String candidateHash = hashPassword(candidatePassword, hexSalt);
            byte[] a = HexFormat.of().parseHex(candidateHash);
            byte[] b = HexFormat.of().parseHex(expectedHexHash);
            return MessageDigest.isEqual(a, b);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convenience method to verify a String password.
     *
     * @param candidatePassword Raw candidate password string
     * @param hexSalt           Hex-encoded salt used during initial hashing
     * @param expectedHexHash   Expected hex-encoded hash string
     * @return true if candidate password matches expected hash, false otherwise
     */
    public static boolean verifyPassword(String candidatePassword, String hexSalt, String expectedHexHash) {
        if (candidatePassword == null) {
            return false;
        }
        return verifyPassword(candidatePassword.toCharArray(), hexSalt, expectedHexHash);
    }
}
