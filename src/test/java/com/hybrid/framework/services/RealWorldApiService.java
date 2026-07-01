package com.hybrid.framework.services;

import com.hybrid.framework.api.ApiAuthManager;
import com.hybrid.framework.api.ApiResponseUtils;
import com.hybrid.framework.context.ContextKeys;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.utils.JsonUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.restassured.response.Response;

import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

/**
 * RealWorld API domain orchestration - delegates HTTP to {@link ApiHttpService} and
 * assertions/extraction to {@link ApiResponseUtils}.
 *
 * <p><b>Requests</b>: JSON payload files via {@link ApiHttpService#setPayloadFromFile}
 * (optional overrides in one pass, including {@code <context:keyName>} from {@link TestContext}).</p>
 *
 * <p><b>Responses</b>: validated via {@link ApiResponseUtils} (JsonPath and body assertions).</p>
 */
public class RealWorldApiService {

    private static final Logger LOG = LogManager.getLogger(RealWorldApiService.class);

    private final TestContext testContext;
    private final ApiHttpService apiHttp;

    public RealWorldApiService(TestContext testContext, ApiHttpService apiHttp) {
        this.testContext = testContext;
        this.apiHttp = apiHttp;
    }

    public void registerUser(String fileName) {
        LOG.info("Registering user via payload '{}'", fileName);
        apiHttp.setPayloadFromFile(fileName);
        apiHttp.post("/api/users");
    }

    public void loginUser(String fileName) {
        LOG.info("Logging in via payload '{}'", fileName);
        apiHttp.setPayloadFromFile(fileName);
        apiHttp.post("/api/users/login");
    }

    public void loginWithCredentials(String fileName, String email, String password) {
        LOG.info("Logging in via payload '{}' with overrides (email='{}', password present={})",
                fileName, email, password != null && !password.isBlank());
        apiHttp.setPayloadFromFile(fileName, Map.of(
                "user.email", email,
                "user.password", password
        ));
        apiHttp.post("/api/users/login");
    }

    public void createArticle(String fileName) {
        LOG.info("Creating article via payload '{}'", fileName);
        apiHttp.setPayloadFromFile(fileName);
        apiHttp.postAuthenticated("/api/articles");
    }

    public void postComplexPayloadWithMixedPathMutations(
            String fileName, Map<String, String> fieldOverrides) {
        apiHttp.setPayloadFromFile(fileName, fieldOverrides);
        ObjectNode payload = testContext.get(ContextKeys.API_PAYLOAD, ObjectNode.class);
        
        LOG.info("Posting complex mutated payload from '{}': {}", fileName,
                JsonUtils.treeToString(payload));

        // Dummy endpoint: expects { user: { email, password } } but we will send a complex template.
        apiHttp.post("/api/users/login");
    }

    public void fetchArticleBySlug() {
        String slug = testContext.get(ContextKeys.ARTICLE_SLUG, String.class);
        Assert.assertNotNull(slug, "Article slug must be stored in TestContext before fetching");
        LOG.info("Fetching article with slug '{}'", slug);
        apiHttp.getWithPathParam("/api/articles/{slug}", "slug", slug);
    }

    public void assertAuthTokenPresent() {
        Response response = requireLastResponse();
        ApiResponseUtils.assertJsonFieldNotNull(response, "user.token");
        String token = ApiResponseUtils.requireString(response, "user.token", "Auth token should be present");
        Assert.assertFalse(token.isBlank(), "Auth token should not be blank");
        LOG.info("Auth token is present in response");
    }

    public void storeAuthToken() {
        Response response = requireLastResponse();
        String token = ApiResponseUtils.requireString(response, "user.token", "Auth token should be present");
        testContext.put(ContextKeys.ACCESS_TOKEN, token);
        ApiAuthManager.setAccessToken(token);
        LOG.info("Auth token stored in TestContext and ApiAuthManager");
    }

    public void assertArticleSlugPresent() {
        Response response = requireLastResponse();
        ApiResponseUtils.assertJsonFieldNotNull(response, "article.slug");
        String slug = ApiResponseUtils.requireString(response, "article.slug", "Article slug should be present");
        Assert.assertFalse(slug.isBlank(), "Article slug should not be blank");
        LOG.info("Article slug is present in response: '{}'", slug);
    }

    public void storeArticleSlug() {
        Response response = requireLastResponse();
        String slug = ApiResponseUtils.requireString(response, "article.slug", "Article slug should be present");
        testContext.put(ContextKeys.ARTICLE_SLUG, slug);
        LOG.info("Article slug '{}' stored in TestContext", slug);
    }

    public void assertErrorResponsePresent() {
        ApiResponseUtils.assertMatchesJsonSchema(requireLastResponse(), "error-response.json");
        LOG.info("Error response validated against error-response.json schema");
    }

    private Response requireLastResponse() {
        Response response = testContext.getLastResponse();
        Assert.assertNotNull(response, "No API response available - send a request first");
        return response;
    }
}
