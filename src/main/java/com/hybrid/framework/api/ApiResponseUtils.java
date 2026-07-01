package com.hybrid.framework.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.config.FrameworkConstants;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

/**
 * Response parsing, extraction, and validation utilities for Rest Assured {@link Response} objects.
 * <p>
 * Use alongside {@link ApiUtils} � {@code ApiUtils} sends requests; this class inspects and
 * asserts on the result. Designed for medium-to-complex API scenarios: chained validations,
 * typed extraction, POJO deserialization, and diagnostic messages that include status and body
 * snippets on failure.
 * </p>
 *
 * <b>Usage � assertions in TestNG tests:</b>
 * <pre>
 *   Response response = ApiUtils.post("/api/users", body);
 *   ApiResponseUtils.assertStatusCode(response, 201);
 *   ApiResponseUtils.assertJsonFieldEquals(response, "name", "John Doe");
 *   String id = ApiResponseUtils.requireString(response, "id");
 * </pre>
 *
 * <b>Usage � fluent validation chain:</b>
 * <pre>
 *   ApiResponseUtils.validate(response)
 *           .statusCode(200)
 *           .jsonPathNotNull("data.id")
 *           .jsonPathEquals("data.first_name", "Janet")
 *           .responseTimeLessThan(3000)
 *           .done();
 * </pre>
 *
 * <b>Usage � deserialize paginated responses:</b>
 * <pre>
 *   ApiResponse&lt;User&gt; page = ApiResponseUtils.as(response, new TypeReference&lt;&gt;() {});
 * </pre>
 */
public final class ApiResponseUtils {

    private static final Logger LOG = LogManager.getLogger(ApiResponseUtils.class);
    private static final int DIAGNOSTIC_BODY_MAX = 500;

    private ApiResponseUtils() {
        // Utility class � no instantiation
    }

    // ??????????????????????????????????????????????????????????????
    // Extraction
    // ??????????????????????????????????????????????????????????????

    /**
     * Extracts a value from the response body using JsonPath.
     */
    //can also be of Object type, but then need to cast to the specific type
    //below reference from value on the left
    public static <T> T get(Response response, String jsonPath) {
        return response.jsonPath().get(jsonPath);
    }

    public static String getString(Response response, String jsonPath) {
        return response.jsonPath().getString(jsonPath);
    }

    public static int getInt(Response response, String jsonPath) {
        return response.jsonPath().getInt(jsonPath);
    }

    public static long getLong(Response response, String jsonPath) {
        return response.jsonPath().getLong(jsonPath);
    }

    public static boolean getBoolean(Response response, String jsonPath) {
        return response.jsonPath().getBoolean(jsonPath);
    }

    // Rule of thumb: use getList when the JSON value at that path is [...] — an array.
    // jsonPath points to the array; elementType is what each element is (String.class, Integer.class, Map.class, or a POJO class).
    public static <T> List<T> getList(Response response, String jsonPath, Class<T> elementType) {
        return response.jsonPath().getList(jsonPath, elementType);
    }

    /**
     * Extracts a string field and fails with a diagnostic message if the value is null or blank.
     */
    @Step("Extract required string from response at '{jsonPath}'")
    public static String requireString(Response response, String jsonPath) {
        return requireString(response, jsonPath,
                "Required JSON field '" + jsonPath + "' was null or blank");
    }

    @Step("Extract required string from response at '{jsonPath}'")
    public static String requireString(Response response, String jsonPath, String message) {
        String value = getString(response, jsonPath);
        Assert.assertNotNull(value, message + "\n" + formatDiagnostic(response));
        Assert.assertFalse(value.isBlank(), message + "\n" + formatDiagnostic(response));
        return value;
    }

    // ??????????????????????????????????????????????????????????????
    // Deserialization
    // ??????????????????????????????????????????????????????????????

    /**
     * Deserializes the response body into a POJO.
     */
    public static <T> T as(Response response, Class<T> clazz) {
        return response.as(clazz);
    }

    /**
     * NOT IDEAL
     * Deserializes the response body into a generic type (e.g. {@code ApiResponse<User>}).
     * Uses Jackson {@link TypeReference} for consistency with {@link com.hybrid.framework.utils.JsonUtils}.
     */
    public static <T> T as(Response response, TypeReference<T> typeRef) {
        return response.as(typeRef.getType());
    }

    // ??????????????????????????????????????????????????????????????
    // Body and metadata
    // ??????????????????????????????????????????????????????????????

    public static String getBody(Response response) {
        return response.getBody().asString();
    }

    public static boolean hasBody(Response response) {
        String body = getBody(response);
        return body != null && !body.isBlank();
    }

    public static boolean isJson(Response response) {
        String contentType = response.getContentType();
        return contentType != null && contentType.toLowerCase().contains("json");
    }

    //return the response time in milliseconds
    public static long getResponseTimeMs(Response response) {
        return response.getTime();
    }

    /**
     * Builds a multi-line diagnostic block for assertion failure messages and logs.
     */
    public static String formatDiagnostic(Response response) {
        String body = getBody(response);
        String bodySnippet = truncate(body, DIAGNOSTIC_BODY_MAX);

        return """
                Response diagnostic:
                  Status: %d %s
                  Content-Type: %s
                  Time: %d ms
                  Body: %s""".formatted(
                response.statusCode(),
                response.statusLine(),
                response.getContentType() == null ? "(none)" : response.getContentType(),
                response.getTime(),
                bodySnippet.isBlank() ? "(empty)" : bodySnippet);
    }

    // ??????????????????????????????????????????????????????????????
    // Assertions
    // ??????????????????????????????????????????????????????????????

    @Step("Verify response status code is {expectedStatusCode}")
    public static void assertStatusCode(Response response, int expectedStatusCode) {
        int actual = response.statusCode();
        Assert.assertEquals(actual, expectedStatusCode,
                "Status code mismatch. Expected " + expectedStatusCode + ", actual " + actual
                        + ".\n" + formatDiagnostic(response));
    }

    @Step("Verify response status code is one of {expectedStatusCodes}")
    public static void assertStatusCodeIn(Response response, int... expectedStatusCodes) {
        int actual = response.statusCode();
        boolean matched = Arrays.stream(expectedStatusCodes).anyMatch(code -> code == actual);
        Assert.assertTrue(matched,
                "Status code " + actual + " not in " + Arrays.toString(expectedStatusCodes)
                        + ".\n" + formatDiagnostic(response));
    }

    @Step("Verify response header '{headerName}' is present")
    public static void assertHeaderPresent(Response response, String headerName) {
        String value = response.getHeader(headerName);
        Assert.assertNotNull(value,
                "Header '" + headerName + "' is missing.\n" + formatDiagnostic(response));
    }

    @Step("Verify response header '{headerName}' equals '{expectedValue}'")
    public static void assertHeaderEquals(Response response, String headerName, String expectedValue) {
        assertHeaderPresent(response, headerName);
        Assert.assertEquals(response.getHeader(headerName), expectedValue,
                "Header '" + headerName + "' value mismatch.\n" + formatDiagnostic(response));
    }

    @Step("Verify JSON field '{jsonPath}' equals '{expectedValue}'")
    public static void assertJsonFieldEquals(Response response, String jsonPath, Object expectedValue) {
        Object actual = get(response, jsonPath);
        Assert.assertEquals(actual, expectedValue,
                "JSON field '" + jsonPath + "' mismatch.\n" + formatDiagnostic(response));
    }

    @Step("Verify JSON field '{jsonPath}' is not null")
    public static void assertJsonFieldNotNull(Response response, String jsonPath) {
        Object value = get(response, jsonPath);
        Assert.assertNotNull(value,
                "JSON field '" + jsonPath + "' should not be null.\n" + formatDiagnostic(response));
    }

    @Step("Verify JSON field '{jsonPath}' exists")
    public static void assertJsonPathExists(Response response, String jsonPath) {
        Object value = get(response, jsonPath);
        Assert.assertNotNull(value,
                "JSON path '" + jsonPath + "' should exist.\n" + formatDiagnostic(response));
    }

    @Step("Verify response body contains '{expectedText}'")
    public static void assertBodyContains(Response response, String expectedText) {
        String body = getBody(response);
        Assert.assertTrue(body != null && body.contains(expectedText),
                "Response body should contain '" + expectedText + "'.\n" + formatDiagnostic(response));
    }

    /**
     * Validates the response body against a JSON Schema file under {@code src/test/resources/schemas/}.
     *
     * @param response       last API response
     * @param schemaFileName schema file name only (e.g. {@code user-response.json})
     */
    @Step("Verify response body matches JSON schema '{schemaFileName}'")
    public static void assertMatchesJsonSchema(Response response, String schemaFileName) {
        Objects.requireNonNull(response, "response must not be null");
        String classpathSchema = resolveSchemaClasspath(schemaFileName);
        try {
            response.then().body(matchesJsonSchemaInClasspath(classpathSchema));
            LOG.info("Response body matches schema '{}'", classpathSchema);
        } catch (AssertionError e) {
            Assert.fail("JSON schema validation failed for '" + classpathSchema + "': " + e.getMessage()
                    + "\n" + formatDiagnostic(response));
        }
    }

    @Step("Verify response Content-Type contains '{expectedContentType}'")
    public static void assertContentTypeContains(Response response, String expectedContentType) {
        String contentType = response.getContentType();
        Assert.assertNotNull(contentType,
                "Content-Type header is missing.\n" + formatDiagnostic(response));
        Assert.assertTrue(contentType.toLowerCase().contains(expectedContentType.toLowerCase()),
                "Content-Type should contain '" + expectedContentType + "' but was '" + contentType
                        + "'.\n" + formatDiagnostic(response));
    }

    @Step("Verify response time is less than {maxMillis} ms")
    public static void assertResponseTimeLessThan(Response response, long maxMillis) {
        long actual = getResponseTimeMs(response);
        Assert.assertTrue(actual < maxMillis,
                "Response time " + actual + " ms exceeded limit of " + maxMillis + " ms.\n"
                        + formatDiagnostic(response));
    }

    @Step("Verify response time is within configured limit")
    public static void assertResponseTimeWithinDefaultLimit(Response response) {
        long maxMillis = ConfigReader.getInstance().getInt("api.response.max.time.ms", 5000);
        assertResponseTimeLessThan(response, maxMillis);
    }

    // ??????????????????????????????????????????????????????????????
    // Fluent validation
    // ??????????????????????????????????????????????????????????????

    /**
     * Starts a fluent validation chain for the given response.
     */
    public static ResponseValidator validate(Response response) {
        return new ResponseValidator(response);
    }

    /**
     * Fluent, chainable response validator for multi-step assertions in complex scenarios.
     */
    public static final class ResponseValidator {

        private final Response response;

        private ResponseValidator(Response response) {
            this.response = Objects.requireNonNull(response, "response must not be null");
        }

        public ResponseValidator statusCode(int expected) {
            assertStatusCode(response, expected);
            return this;
        }

        public ResponseValidator statusCodeAnyOf(int... expected) {
            assertStatusCodeIn(response, expected);
            return this;
        }

        public ResponseValidator headerPresent(String headerName) {
            assertHeaderPresent(response, headerName);
            return this;
        }

        public ResponseValidator headerEquals(String headerName, String expectedValue) {
            assertHeaderEquals(response, headerName, expectedValue);
            return this;
        }

        public ResponseValidator jsonPathEquals(String jsonPath, Object expectedValue) {
            assertJsonFieldEquals(response, jsonPath, expectedValue);
            return this;
        }

        public ResponseValidator jsonPathNotNull(String jsonPath) {
            assertJsonFieldNotNull(response, jsonPath);
            return this;
        }

        public ResponseValidator jsonPathExists(String jsonPath) {
            assertJsonPathExists(response, jsonPath);
            return this;
        }

        public ResponseValidator bodyContains(String expectedText) {
            assertBodyContains(response, expectedText);
            return this;
        }

        public ResponseValidator contentTypeContains(String expectedContentType) {
            assertContentTypeContains(response, expectedContentType);
            return this;
        }

        public ResponseValidator responseTimeLessThan(long maxMillis) {
            assertResponseTimeLessThan(response, maxMillis);
            return this;
        }

        public ResponseValidator responseTimeWithinDefaultLimit() {
            assertResponseTimeWithinDefaultLimit(response);
            return this;
        }

        /**
         * Returns the underlying response after all validations pass.
         */
        public Response done() {
            LOG.debug("Response validation passed � status {}", response.statusCode());
            return response;
        }
    }

    /** Maps a schema file name to a test-classpath location ({@code schemas/...}). */
    private static String resolveSchemaClasspath(String schemaFileName) {
        if (schemaFileName.contains("/")) {
            return schemaFileName.startsWith("schemas/") ? schemaFileName : "schemas/" + schemaFileName;
        }
        if (!FrameworkConstants.SCHEMAS_DIR.resolve(schemaFileName).toFile().exists()) {
            LOG.warn("Schema file not found on disk at {} - validation will use classpath only",
                    FrameworkConstants.SCHEMAS_DIR.resolve(schemaFileName));
        }
        return "schemas/" + schemaFileName;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}
