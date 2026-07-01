package com.hybrid.framework.api;

import com.hybrid.framework.config.ConfigReader;
import io.qameta.allure.Step;
import io.qameta.allure.restassured.AllureRestAssured;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.config.LogConfig;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Centralized API utilities built on Rest Assured.
 * <p>
 * Provides pre-configured request/response specifications, convenience methods
 * for all HTTP verbs, and automatic Allure report integration for every request.
 * Response parsing and assertions belong in {@link ApiResponseUtils}.
 * Logging verbosity is controlled by {@code api.log.verbose} (default {@code true}).
 * Sensitive headers are redacted from Rest Assured console logs when
 * {@code api.log.blacklist.enabled} is {@code true} (configurable header list).
 * </p>
 *
 * <b>Usage — simple GET:</b>
 * <pre>
 *   Response response = ApiUtils.get("/api/users/2");
 *   assertThat(response.statusCode()).isEqualTo(200);
 * </pre>
 *
 * <b>Usage — GET with path parameters (template endpoint):</b>
 * <pre>
 *   Response response = ApiUtils.getWithPathParams("/api/users/{userId}", Map.of("userId", 2));
 * </pre>
 *
 * <b>Usage — create resource then fetch by ID:</b>
 * <pre>
 *   Response created = ApiUtils.post("/api/users", body);
 *   String userId = ApiResponseUtils.requireString(created, "id");
 *   Response fetched = ApiUtils.getWithPathParams("/api/users/{userId}", Map.of("userId", userId));
 * </pre>
 * <p>
 * PUT, PATCH, and DELETE always require path parameters (resource-scoped updates/removals).
 * GET may use path or query parameters. POST creates at a collection URL
 * (e.g. {@code /api/users}) and does not use path params.
 * </p>
 *
 * <b>Usage — PUT with path parameter:</b>
 * <pre>
 *   Response response = ApiUtils.put("/api/users/{userId}", Map.of("userId", userId), body);
 * </pre>
 *
 * <b>Usage — POST with POJO body:</b>
 * <pre>
 *   CreateUserRequest body = new CreateUserRequest("John", "QA Lead");
 *   Response response = ApiUtils.post("/api/users", body);
 * </pre>
 *
 * <b>Usage — Token / Bearer auth (set token via {@link ApiAuthManager#setAccessToken} or config credentials):</b>
 * <pre>
 *   // RealWorld: api.auth.scheme=Token; BDD steps store token after login
 *   ApiAuthManager.setAccessToken(jwt);
 *   Response response = ApiUtils.get("/api/articles/feed");
 * </pre>
 */
public final class ApiUtils {

    private static final Logger LOG = LogManager.getLogger(ApiUtils.class);

    private static final List<String> DEFAULT_BLACKLISTED_HEADERS = List.of(
            "Authorization",
            "x-api-key",
            "Cookie",
            "Set-Cookie"
    );

    private static volatile boolean restAssuredLogConfigApplied;

    private ApiUtils() {
        // Utility class — no instantiation
    }

    // ──────────────────────────────────────────────────────────────
    // Log Configs
    // ──────────────────────────────────────────────────────────────

    private static void ensureRestAssuredLogConfig() {
        if (restAssuredLogConfigApplied) {
            return;
        }
        synchronized (ApiUtils.class) {
            if (restAssuredLogConfigApplied) {
                return;
            }
            RestAssured.config = RestAssured.config().logConfig(buildLogConfig());
            restAssuredLogConfigApplied = true;
        }
    }

    private static LogConfig buildLogConfig() {
        ConfigReader config = ConfigReader.getInstance();
        LogConfig logConfig = LogConfig.logConfig();

        if (config.getBoolean("api.log.blacklist.enabled", true)) {
            List<String> headers = resolveBlacklistedHeaders(config);
            logConfig = logConfig.blacklistHeaders(headers).blacklistDefaultSensitiveHeaders();
        }

        return logConfig;
    }

    /**
     * Resolves headers to redact in Rest Assured logs.
     * If {@code api.log.blacklist.headers} is set, it replaces the default list.
     * Otherwise defaults are merged with {@code api.log.blacklist.headers.extra}.
     */
    private static List<String> resolveBlacklistedHeaders(ConfigReader config) {
        String override = config.getProperty("api.log.blacklist.headers", "");
        if (override != null && !override.isBlank()) {
            return parseHeaderList(override);
        }

        LinkedHashSet<String> mergedHeadersList = new LinkedHashSet<>(DEFAULT_BLACKLISTED_HEADERS);
        String extra = config.getProperty("api.log.blacklist.headers.extra", "");
        if (extra != null && !extra.isBlank()) {
            mergedHeadersList.addAll(parseHeaderList(extra));
        }
        return List.copyOf(mergedHeadersList);
    }

    private static List<String> parseHeaderList(String headers) {
        return Arrays.stream(headers.split(","))
                .map(String::trim)
                .filter(header -> !header.isEmpty())
                .toList();
    }

    private static boolean isVerboseLogging() {
        return ConfigReader.getInstance().getBoolean("api.log.verbose", true);
    }

    private static LogDetail requestLogDetail() {
        return isVerboseLogging() ? LogDetail.ALL : LogDetail.URI;
    }

    private static LogDetail responseLogDetail() {
        return isVerboseLogging() ? LogDetail.ALL : LogDetail.STATUS;
    }

    // ──────────────────────────────────────────────────────────────
    // Specifications
    // ──────────────────────────────────────────────────────────────

    /**
     * Builds a default RequestSpecification with base URI, content type,
     * configurable logging ({@code api.log.verbose}), and Allure filter.
     */
    public static RequestSpecification getDefaultRequestSpec() {
        ensureRestAssuredLogConfig();
        ensureTokenOnce();
        ConfigReader config = ConfigReader.getInstance();
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(config.getApiBaseUri())
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("user-agent", "hybrid-bdd")
                .addFilter(new AllureRestAssured())
                .log(requestLogDetail());

        //checking if token is present is not null or blank
        if (ApiAuthManager.isAuthenticated()) {
            String scheme = config.getProperty("api.auth.scheme", "Bearer");//used as here Token is used instead of Bearer
            builder.addHeader("Authorization", scheme + " " + ApiAuthManager.getAccessToken());
        }

        return builder.build();
    }

    /**
     * Builds a default ResponseSpecification with configurable response logging.
     */
    public static ResponseSpecification getDefaultResponseSpec() {
        return new ResponseSpecBuilder()
                .log(responseLogDetail())
                .build();
    }

    /**
     * Builds a RequestSpecification with a Bearer token (stored in {@link ApiAuthManager} for all verbs).
     * not used currently, if custom token passed
     */
    public static RequestSpecification getAuthRequestSpec(String token) {
        ApiAuthManager.setAccessToken(token);
        return getDefaultRequestSpec();
    }

    /**
     * Clears the JWT for the current thread (called from Cucumber {@code @After} hooks).
     */
    public static void clearAuthentication() {
        ApiAuthManager.clear();
    }

    private static void ensureTokenOnce() {
        ConfigReader config = ConfigReader.getInstance();
        if (config.hasApiAuthCredentials() && !ApiAuthManager.isAuthenticated()) {
            ApiAuthManager.authenticate();
        }
    }

    // ──────────────────────────────────────────────────────────────
    // HTTP Verbs
    // ──────────────────────────────────────────────────────────────

    /**
     * Path- and query-param methods chain {@code .pathParams()} / {@code .queryParams()}
     * on top of the default request spec. POST uses the default request spec only (collection URLs).
     */

    @Step("GET {endpoint}")
    public static Response get(String endpoint) {
        LOG.info("GET → {}", endpoint);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .when()
                .get(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    @Step("GET {endpoint} with query params")
    public static Response get(String endpoint, Map<String, ?> queryParams) {
        LOG.info("GET → {} with params: {}", endpoint, queryParams);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .queryParams(queryParams)
                .when()
                .get(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    @Step("GET {endpoint} with path params")
    public static Response getWithPathParams(String endpoint, Map<String, ?> pathParams) {
        LOG.info("GET → {} pathParams: {}", endpoint, pathParams);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .pathParams(pathParams)
                .when()
                .get(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    @Step("GET {endpoint} with path and query params")
    public static Response getWithPathAndQueryParams(String endpoint, Map<String, ?> pathParams,
                                                     Map<String, ?> queryParams) {
        LOG.info("GET → {} pathParams: {} queryParams: {}", endpoint, pathParams, queryParams);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .pathParams(pathParams)
                .queryParams(queryParams)
                .when()
                .get(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    @Step("POST {endpoint}")
    public static Response post(String endpoint, Object body) {
        LOG.info("POST → {}", endpoint);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    /**
     * Only for 2 step process
     * POSTs to the specified endpoint with a Bearer token.
     * Can be used for 2 step process to get the token and then use it in the request.
     * Not currently used in the framework.
     * @param endpoint the endpoint to POST to
     * @param body the body to POST
     * @param token the Bearer token to use
     * @return the response from the POST request
     */
    @Step("POST {endpoint} with auth token")
    public static Response postWithAuth(String endpoint, Object body, String token) {
        LOG.info("POST (auth) → {}", endpoint);
        return RestAssured.given()
                .spec(getAuthRequestSpec(token))
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    @Step("PUT {endpoint} with path params")
    public static Response put(String endpoint, Map<String, ?> pathParams, Object body) {
        LOG.info("PUT → {} pathParams: {}", endpoint, pathParams);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .pathParams(pathParams)
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    @Step("PATCH {endpoint} with path params")
    public static Response patch(String endpoint, Map<String, ?> pathParams, Object body) {
        LOG.info("PATCH → {} pathParams: {}", endpoint, pathParams);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .pathParams(pathParams)
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

    @Step("DELETE {endpoint} with path params")
    public static Response delete(String endpoint, Map<String, ?> pathParams) {
        LOG.info("DELETE → {} pathParams: {}", endpoint, pathParams);
        return RestAssured.given()
                .spec(getDefaultRequestSpec())
                .pathParams(pathParams)
                .when()
                .delete(endpoint)
                .then()
                .spec(getDefaultResponseSpec())
                .extract().response();
    }

}
