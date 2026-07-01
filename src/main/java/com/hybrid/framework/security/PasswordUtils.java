package com.hybrid.framework.security;

import com.hybrid.framework.config.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;

/**
 * Utility for encrypting/decrypting sensitive credentials.
 * <p>
 * Supports two modes:
 * <ul>
 *   <li><b>Base64</b> — simple encoding (not encryption), suitable for obfuscation.</li>
 *   <li><b>AES-128</b> — symmetric encryption with a 16-character secret key.</li>
 * </ul>
 * </p>
 *
 * <b>Usage:</b>
 * <pre>
 *   // Encrypt a password to store in config.properties
 *   String encrypted = PasswordUtils.encrypt("myP@ssw0rd");
 *
 *   // Decrypt at runtime
 *   String plain = PasswordUtils.decrypt(encrypted);
 * </pre>
 */
public final class PasswordUtils {

    private static final Logger LOG = LogManager.getLogger(PasswordUtils.class);
    private static final String ALGORITHM = "AES";
    private static final Key SECRET_KEY = new SecretKeySpec(
            FrameworkConstants.AES_SECRET_KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM);

    private PasswordUtils() {
        // Utility class — no instantiation
    }

    // ──────────────────────────────────────────────────────────────
    // AES Encryption / Decryption
    // ──────────────────────────────────────────────────────────────

    /**
     * Encrypts a plaintext string using AES-128 and returns a Base64-encoded result.
     *
     * @param plainText the text to encrypt
     * @return Base64-encoded ciphertext
     */
    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            LOG.error("AES encryption failed: {}", e.getMessage());
            throw new SecurityException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded AES-128 ciphertext back to plaintext.
     *
     * @param encryptedText the Base64-encoded ciphertext
     * @return decrypted plaintext
     */
    public static String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOG.error("AES decryption failed: {}", e.getMessage());
            throw new SecurityException("Decryption failed", e);
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Base64 Encoding / Decoding (simple obfuscation)
    // ──────────────────────────────────────────────────────────────

    /**
     * Base64-encodes a string (not encryption — obfuscation only).
     */
    public static String base64Encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a Base64-encoded string.
     */
    public static String base64Decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}
