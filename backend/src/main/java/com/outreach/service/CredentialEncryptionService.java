package com.outreach.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for sensitive credentials (Gmail app password, AI API keys).
 *
 * Storage format: Base64( IV(12 bytes) || CipherText+AuthTag )
 *
 * The encryption key is derived from the JWT secret configured in application.yml.
 * In production, set a dedicated CREDENTIAL_KEY env variable for extra isolation.
 */
@Slf4j
@Service
public class CredentialEncryptionService {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_LEN     = 12;   // 96 bits — GCM recommended
    private static final int    TAG_LEN    = 128;  // bits

    private final byte[] keyBytes;

    public CredentialEncryptionService(
            @Value("${app.credential.key:${app.jwt.secret}}") String rawKey) {
        // Always produce exactly 32 bytes regardless of input length
        byte[] src = rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        keyBytes = new byte[32];
        System.arraycopy(src, 0, keyBytes, 0, Math.min(src.length, 32));
    }

    /** Encrypt plaintext; returns a base64 string safe for DB storage. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LEN, iv));

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV so we can decrypt without storing it separately
            byte[] combined = new byte[IV_LEN + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, IV_LEN);
            System.arraycopy(cipherText, 0, combined, IV_LEN, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt credential", e);
        }
    }

    /** Decrypt a base64 string previously produced by {@link #encrypt}. */
    public String decrypt(String encoded) {
        if (encoded == null || encoded.isBlank()) return encoded;
        // Graceful fallback: if the value isn't valid base64 it was stored plain-text
        // (i.e. before this feature was introduced).  Return as-is so existing users
        // aren't locked out immediately.
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            if (combined.length <= IV_LEN) return encoded; // too short — plain-text legacy

            byte[] iv         = new byte[IV_LEN];
            byte[] cipherText = new byte[combined.length - IV_LEN];
            System.arraycopy(combined, 0, iv, 0, IV_LEN);
            System.arraycopy(combined, IV_LEN, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_LEN, iv));

            return new String(cipher.doFinal(cipherText), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Decryption failed → assume legacy plain-text value
            log.warn("Could not decrypt credential (assuming plain-text legacy value): {}", e.getMessage());
            return encoded;
        }
    }

    /** Returns true if the value looks encrypted (i.e. is valid base64 of expected minimum length). */
    public boolean isEncrypted(String value) {
        if (value == null || value.length() < 20) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > IV_LEN;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
