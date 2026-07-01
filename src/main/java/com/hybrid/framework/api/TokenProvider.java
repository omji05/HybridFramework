package com.hybrid.framework.api;

import com.hybrid.framework.config.ConfigReader;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;

/**
 * Obtains JWT access tokens via a login/token API without using Bearer auth on that call.
 */
public final class TokenProvider {

    private static final Logger LOG = LogManager.getLogger(TokenProvider.class);

    private TokenProvider() {
    }

    public static String fetchAccessToken() {
        ConfigReader config = ConfigReader.getInstance();
        return fetchAccessToken(config.getApiAuthUsername(), config.getApiAuthPassword());
    }

    public static String fetchAccessToken(String username, String password) {
        ConfigReader config = ConfigReader.getInstance();
        String tokenPath = config.getApiAuthTokenPath();
        Objects.requireNonNull(tokenPath, "api.auth.token.path must be configured");

        if (username == null || username.isBlank()) {
            throw new IllegalStateException("api.auth.username must be set (e.g. API_AUTH_USERNAME in CI)");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("api.auth.password must be set (e.g. API_AUTH_PASSWORD in CI)");
        }

        Map<String, String> body = Map.of("username", username, "password", password);
        int expectedStatus = config.getApiAuthTokenExpectedStatus();

        LOG.info("Requesting JWT from {}", tokenPath);
        Response response = RestAssured.given()
                .spec(buildTokenRequestSpec())
                .body(body)
                .when()
                .post(tokenPath)
                .then()
                .extract()
                .response();

        if (response.statusCode() != expectedStatus) {
            throw new IllegalStateException(String.format(
                    "Token API returned status %d (expected %d). Body: %s",
                    response.statusCode(), expectedStatus, response.asString()));
        }

        String jsonPath = config.getApiAuthTokenJsonPath();
        String token = response.jsonPath().getString(jsonPath);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "Token API response did not contain a token at jsonPath '" + jsonPath + "'");
        }

        LOG.info("JWT obtained successfully (jsonPath: {})", jsonPath);
        return token.trim();
    }

    /** Request spec for token endpoint only (no Bearer; avoids circular auth). */
    static RequestSpecification buildTokenRequestSpec() {
        ConfigReader config = ConfigReader.getInstance();
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(config.getApiBaseUri())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("user-agent", "hybrid-bdd")
                .addFilter(new AllureRestAssured())
                .log(config.getBoolean("api.log.verbose", true) ? LogDetail.ALL : LogDetail.URI);

        return builder.build();
    }
}
