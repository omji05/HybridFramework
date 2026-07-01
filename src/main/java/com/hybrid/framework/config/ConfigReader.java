package com.hybrid.framework.config;

import com.hybrid.framework.security.PasswordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

/**
 * Thread-safe, lazily-initialized configuration reader.
 * <p>
 * Loads properties from {@code src/test/resources/config.properties} by default,
 * then overlays {@code src/test/resources/environments/config-{env}.properties}
 * when an environment profile is selected (e.g. {@code -Denvironment=qa}).
 * System properties and environment variables can override any key at runtime,
 * giving CI/CD pipelines full control without touching files.
 * </p>
 *
 * <b>Resolution order:</b> System Property → Environment Variable → env profile → config.properties
 */
public final class ConfigReader {

    private static final Logger LOG = LogManager.getLogger(ConfigReader.class);
    private static final String DEFAULT_CONFIG_PATH = "src/test/resources/config.properties";
    private static final String ENVIRONMENTS_DIR = "src/test/resources/environments/";

    private static volatile ConfigReader instance;
    private final Properties properties;

    // ──────────────────────────────────────────────────────────────
    // Constructor (private – Singleton)
    // ──────────────────────────────────────────────────────────────

    private ConfigReader() {
        properties = new Properties();
        String configFile = System.getProperty("config.file", DEFAULT_CONFIG_PATH);
        loadProperties(configFile);
        //no external config file is provided
        if (DEFAULT_CONFIG_PATH.equals(configFile)) {
            overlayEnvironmentProfile();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Singleton accessor (double-checked locking)
    // ──────────────────────────────────────────────────────────────

    public static ConfigReader getInstance() {
        if (instance == null) {
            synchronized (ConfigReader.class) {
                if (instance == null) {
                    instance = new ConfigReader();
                }
            }
        }
        return instance;
    }

    // ──────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────

    /**
     * Retrieves a configuration value.
     * Resolution: System Property → Env Var → config.properties.
     */
    public String getProperty(String key) {
        String systemProp = System.getProperty(key);
        if (systemProp != null) return systemProp;

        String envVar = System.getenv(key.replace('.', '_').toUpperCase());
        if (envVar != null) return envVar;

        return properties.getProperty(key);
    }

    /**
     * Retrieves a configuration value with a default fallback.
     */
    public String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Retrieves a boolean configuration value.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    /**
     * Retrieves an integer configuration value.
     */
    public int getInt(String key, int defaultValue) {
        String value = getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            LOG.warn("Invalid integer for key '{}': {}. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Retrieves and decrypts an encrypted property value.
     * Expects the value in the properties file to be Base64-encoded and AES-encrypted.
     */
    public String getEncryptedProperty(String key) {
        String encrypted = getProperty(key);
        Objects.requireNonNull(encrypted, "Encrypted property not found: " + key);
        return PasswordUtils.decrypt(encrypted);
    }

    /**
     * Convenience: base URL for the application under test.
     */
    public String getBaseUrl() {
        return getProperty("base.url");
    }

    /**
     * Convenience: base URI for API tests.
     */
    public String getApiBaseUri() {
        return getProperty("api.base.uri");
    }

    /**
     * API key for {@code x-api-key} header (e.g. Reqres).
     * Set via CI secret {@code API_KEY}, {@code -Dapi.key=...}, or {@code api.key} in config.properties (clear before push).
     *
     * @return the key, or {@code null} if not configured
     */
    public String getApiKey() {
        String value = getProperty("api.key");
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    /**
     * JWT auth is active when both username and password are configured (e.g. via CI secrets).
     */
    public boolean hasApiAuthCredentials() {
        return getApiAuthUsername() != null && getApiAuthPassword() != null;
    }

    public String getApiAuthTokenPath() {
        return getProperty("api.auth.token.path", "/api/login");
    }

    public String getApiAuthTokenJsonPath() {
        return getProperty("api.auth.token.jsonpath", "access_token");
    }

    public int getApiAuthTokenExpectedStatus() {
        return getInt("api.auth.token.expected.status", 200);
    }

    public String getApiAuthUsername() {
        String value = getProperty("api.auth.username");
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    /**
     * Password for token API — set via {@code API_AUTH_PASSWORD} in CI.
     */
    public String getApiAuthPassword() {
        String value = getProperty("api.auth.password");
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    /**
     * Convenience: browser name.
     */
    public String getBrowser() {
        return getProperty("browser", "chrome");
    }

    /**
     * Convenience: headless flag.
     */
    public boolean isHeadless() {
        return getBoolean("headless", false);
    }

    /**
     * Convenience: implicit wait timeout in seconds.
     */
    public int getImplicitWait() {
        return getInt("implicit.wait", 10);
    }

    /**
     * Convenience: explicit wait timeout in seconds.
     */
    public int getExplicitWait() {
        return getInt("explicit.wait", 15);
    }

    /**
     * Reloads configuration — useful for tests that need a fresh state.
     */
    public static synchronized void reload() {
        instance = null;
    }

    // ──────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────

    private void loadProperties(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
            LOG.info("Configuration loaded from: {}", filePath);
        } catch (IOException e) {
            LOG.error("Failed to load config file: {}. Framework will rely on system/env properties.", filePath);
        }
    }

    /**
     * Merges environment-specific overrides (URLs, labels) on top of the base config.
     * Profile key: {@code -Denvironment=qa} or env {@code ENVIRONMENT=qa} (defaults to {@code qa}).
     */
    private void overlayEnvironmentProfile() {
        String profileKey = resolveEnvironmentProfileKey();
        String profilePath = ENVIRONMENTS_DIR + "config-" + profileKey + ".properties";
        try (FileInputStream fis = new FileInputStream(profilePath)) {
            properties.load(fis);
            LOG.info("Environment profile applied: {} ({})", profileKey.toUpperCase(), profilePath);
        } catch (IOException e) {
            LOG.warn("No environment profile at {} — using base config only.", profilePath);
        }
    }

    private String resolveEnvironmentProfileKey() {
        String systemEnv = System.getProperty("environment");
        if (systemEnv != null && !systemEnv.isBlank()) {
            return systemEnv.trim().toLowerCase();
        }

        String envVar = System.getenv("ENVIRONMENT");
        if (envVar != null && !envVar.isBlank()) {
            return envVar.trim().toLowerCase();
        }

        String configured = properties.getProperty("environment", "qa");
        return configured.trim().toLowerCase();
    }
}
