package com.hybrid.framework.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe holder for JWT access tokens used by {@link ApiUtils}.
 * <p>
 * One token per thread supports parallel TestNG/Cucumber execution.
 * Call {@link #clear()} in scenario teardown to avoid leaking tokens.
 * </p>
 */
public final class ApiAuthManager {

    private static final Logger LOG = LogManager.getLogger(ApiAuthManager.class);
    private static final ThreadLocal<String> ACCESS_TOKEN = new ThreadLocal<>();

    private ApiAuthManager() {
    }

    public static void setAccessToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank");
        }
        ACCESS_TOKEN.set(token.trim());
    }

    public static String getAccessToken() {
        return ACCESS_TOKEN.get();
    }

    public static boolean isAuthenticated() {
        String token = ACCESS_TOKEN.get();
        return token != null && !token.isBlank();
    }

    public static void clear() {
        ACCESS_TOKEN.remove();
        LOG.debug("Cleared JWT access token for thread {}", Thread.currentThread().getName());
    }

    /**
     * Fetches a JWT from the configured token endpoint and stores it for the current thread.
     */
    static void authenticate() {
        String token = TokenProvider.fetchAccessToken();
        setAccessToken(token);
        LOG.info("JWT access token obtained for thread {}", Thread.currentThread().getName());
    }
}
