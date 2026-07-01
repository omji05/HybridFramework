package com.hybrid.framework.context;

import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-scenario shared state for Cucumber (one instance per scenario via PicoContainer).
 * <p>
 * Stores arbitrary step data via {@link #put} / {@link #get} using keys from {@link ContextKeys}.
 * {@code lastResponse} is kept as a direct field for shared API assertion steps.
 * Domain services (e.g. {@link com.hybrid.framework.services.RealWorldApiService}) read and write
 * the map; this class has no user-, order-, or payload-specific methods.
 * </p>
 */
public class TestContext {

    private final Map<String, Object> scenarioData = new HashMap<>();

    private Response lastResponse;

    public void put(String key, Object value) {
        scenarioData.put(key, value);
    }

    public Object get(String key) {
        return scenarioData.get(key);
    }

    public <T> T get(String key, Class<T> type) {
        Object value = scenarioData.get(key);
        return value == null ? null : type.cast(value);
    }

    public boolean containsKey(String key) {
        return scenarioData.containsKey(key);
    }

    public Response getLastResponse() {
        return lastResponse;
    }

    public void setLastResponse(Response lastResponse) {
        this.lastResponse = lastResponse;
    }
}
