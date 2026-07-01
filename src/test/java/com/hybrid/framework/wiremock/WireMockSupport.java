package com.hybrid.framework.wiremock;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Suite-scoped embedded WireMock for {@code @wiremock} scenarios.
 * <p>
 * Started once via {@link #startOnce()} before all scenarios and stopped via
 * {@link #stopOnce()} after the suite. File-based stubs live under
 * {@code src/test/resources/wiremock/} (mappings + __files).
 * </p>
 * <p>
 * Safe for parallel execution: one shared server, immutable file stubs, no per-scenario reset/stop.
 * Enable with {@code -Pmock} ({@code wiremock.enabled=true}).
 * </p>
 */
public final class WireMockSupport {

    private static WireMockServer server;

    private WireMockSupport() {
    }

    public static synchronized void startOnce() {
        //if WireMock is already up, do nothing. (in case of retry )
        if (server != null && server.isRunning()) {
            return;
        }
        server = new WireMockServer(wireMockConfig().dynamicPort().usingFilesUnderClasspath("wiremock"));
        server.start();
        //setting base uri so that api requests are made to the wiremock server instead of the real backend
        System.setProperty("api.base.uri", server.baseUrl());
    }

    public static synchronized void stopOnce() {
        if (server != null) {
            server.stop();
            server = null;
        }
        System.clearProperty("api.base.uri");
    }

    public static boolean isRunning() {
        return server != null && server.isRunning();
    }
}
