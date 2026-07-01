package com.hybrid.framework.stepdefs.common;

import com.hybrid.framework.api.ApiResponseUtils;
import com.hybrid.framework.context.TestContext;
import com.hybrid.framework.services.ApiHttpService;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Shared API HTTP and response assertion steps (all domains).
 * Domain-specific steps live under {@code stepdefs.api.*}.
 */
public class CommonApiSteps {

    private final TestContext testContext;
    private final ApiHttpService apiHttpService;

    public CommonApiSteps(TestContext testContext, ApiHttpService apiHttpService) {
        this.testContext = testContext;
        this.apiHttpService = apiHttpService;
    }

    @When("I send a GET request to {string}")
    public void iSendAGETRequestTo(String endpoint) {
        apiHttpService.get(endpoint);
    }

    @When("I send a GET request to {string} with path parameter {string} {string}")
    public void iSendAGETRequestWithPathParam(String endpoint, String paramName, String paramValue) {
        apiHttpService.getWithPathParam(endpoint, paramName, paramValue);
    }

    @When("I send a GET request to {string} with path parameter {string} from context {string}")
    public void iSendAGETRequestWithPathParamFromContext(String endpoint, String paramName, String contextKey) {
        apiHttpService.getWithPathParamFromContext(endpoint, paramName, contextKey);
    }

    @When("I send a POST request to {string}")
    public void iSendAPOSTRequestTo(String endpoint) {
        apiHttpService.post(endpoint);
    }

    @When("I send a PUT request to {string} with path parameter {string} {string}")
    public void iSendAPUTRequestWithPathParam(String endpoint, String paramName, String paramValue) {
        apiHttpService.putWithPathParam(endpoint, paramName, paramValue);
    }

    @When("I send a PUT request to {string} with path parameter {string} from context {string}")
    public void iSendAPUTRequestWithPathParamFromContext(String endpoint, String paramName, String contextKey) {
        apiHttpService.putWithPathParamFromContext(endpoint, paramName, contextKey);
    }

    @When("I send a DELETE request to {string} with path parameter {string} {string}")
    public void iSendADELETERequestWithPathParam(String endpoint, String paramName, String paramValue) {
        apiHttpService.deleteWithPathParam(endpoint, paramName, paramValue);
    }

    @When("I send a DELETE request to {string} with path parameter {string} from context {string}")
    public void iSendADELETERequestWithPathParamFromContext(String endpoint, String paramName, String contextKey) {
        apiHttpService.deleteWithPathParamFromContext(endpoint, paramName, contextKey);
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int expectedStatusCode) {
        ApiResponseUtils.assertStatusCode(testContext.getLastResponse(), expectedStatusCode);
    }

    @And("the response body should contain {string}")
    public void theResponseBodyShouldContain(String key) {
        ApiResponseUtils.assertJsonFieldNotNull(testContext.getLastResponse(), "data." + key);
    }

    @And("the response body should contain the name {string}")
    public void theResponseBodyShouldContainTheName(String expectedName) {
        ApiResponseUtils.assertJsonFieldEquals(testContext.getLastResponse(), "name", expectedName);
    }

    @And("the response should have a non-null {string} field")
    public void theResponseShouldHaveANonNullField(String field) {
        ApiResponseUtils.assertJsonFieldNotNull(testContext.getLastResponse(), field);
    }

    @And("the response body should match the {string} schema")
    public void theResponseBodyShouldMatchTheSchema(String schemaFileName) {
        ApiResponseUtils.assertMatchesJsonSchema(testContext.getLastResponse(), schemaFileName);
    }
}
