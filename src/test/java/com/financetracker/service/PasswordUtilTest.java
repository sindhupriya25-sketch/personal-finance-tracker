package com.financetracker.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PasswordUtil hashing and verification.
 */
public class PasswordUtilTest {

    @Test
    public void testSaltGenerationIsUnique() {
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2);
        assertEquals(32, salt1.length()); // 16 bytes = 32 hex chars
    }

    @Test
    public void testHashPasswordAndVerify() {
        String password = "SecretPassword123";
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        assertNotNull(hash);
        assertEquals(64, hash.length()); // 32 bytes = 64 hex chars (SHA-256)

        assertTrue(PasswordUtil.verifyPassword(password, salt, hash));
        assertFalse(PasswordUtil.verifyPassword("WrongPassword", salt, hash));
    }

    @Test
    public void testNullInputThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword(null, "salt"));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.hashPassword("pass", null));
    }
}
