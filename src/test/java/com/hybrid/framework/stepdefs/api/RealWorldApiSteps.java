package com.hybrid.framework.stepdefs.api;

import com.hybrid.framework.services.RealWorldApiService;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.HashMap;
import java.util.Map;

/**
 * Declarative Cucumber step definitions for the RealWorld API.
 *
 * <p>Steps are intentionally high-level and business-focused. All HTTP mechanics
 * (headers, base URI, auth scheme, JSON body construction) are encapsulated in
 * {@link RealWorldApiService}. Response status assertions are handled by the shared
 * {@code CommonApiSteps}; this class owns only RealWorld-domain concerns.</p>
 *
 * <p>Wired via PicoContainer: {@link RealWorldApiService} is injected through the constructor.</p>
 */
public class RealWorldApiSteps {

    private final RealWorldApiService realWorldApiService;

    public RealWorldApiSteps(RealWorldApiService realWorldApiService) {
        this.realWorldApiService = realWorldApiService;
    }

    @When("I register a new user using the {string} payload")
    public void iRegisterANewUserUsingThePayload(String fileName) {
        realWorldApiService.registerUser(fileName);
    }

    @When("I log in using the {string} payload")
    public void iLogInUsingThePayload(String fileName) {
        realWorldApiService.loginUser(fileName);
    }

    /**
     * Convenience step that logs in and immediately stores the token for use in subsequent steps.
     * Use this as a {@code Given} precondition in scenarios that require an authenticated user.
     */
    @Given("I am authenticated using the {string} payload")
    public void iAmAuthenticatedUsingThePayload(String fileName) {
        realWorldApiService.loginUser(fileName);
        realWorldApiService.storeAuthToken();
    }

    @When("I attempt to log in using the {string} payload with email {string} and password {string}")
    public void iAttemptToLogInUsingThePayloadWithEmailAndPassword(
            String fileName, String email, String password) {
        realWorldApiService.loginWithCredentials(fileName, email, password);
    }

    @When("I create an article using the {string} payload")
    public void iCreateAnArticleUsingThePayload(String fileName) {
        realWorldApiService.createArticle(fileName);
    }

    @When("I fetch the article using the stored slug")
    public void iFetchTheArticleUsingTheStoredSlug() {
        realWorldApiService.fetchArticleBySlug();
    }

    @When("I post a complex payload using the {string} payload with JSON overrides")
    public void iPostAComplexPayloadUsingThePayloadWithJsonOverrides(
            String fileName, DataTable overridesTable) {
        Map<String, String> overrides = new HashMap<>();
        for (Map<String, String> row : overridesTable.asMaps(String.class, String.class)) {
            String path = row.get("path");
            String value = row.get("value");
            if (path == null || value == null) {
                throw new IllegalArgumentException(
                        "Override table must contain columns 'path' and 'value'");
            }
            overrides.put(path, value);
        }
        realWorldApiService.postComplexPayloadWithMixedPathMutations(fileName, overrides);
    }

    @Then("the response should contain a valid auth token")
    public void theResponseShouldContainAValidAuthToken() {
        realWorldApiService.assertAuthTokenPresent();
    }

    @And("the auth token should be stored for subsequent requests")
    public void theAuthTokenShouldBeStoredForSubsequentRequests() {
        realWorldApiService.storeAuthToken();
    }

    @Then("the response should contain a valid article slug")
    public void theResponseShouldContainAValidArticleSlug() {
        realWorldApiService.assertArticleSlugPresent();
    }

    @And("the article slug should be stored for subsequent requests")
    public void theArticleSlugShouldBeStoredForSubsequentRequests() {
        realWorldApiService.storeArticleSlug();
    }

    @Then("the API error response should be present")
    public void theApiErrorResponseShouldBePresent() {
        realWorldApiService.assertErrorResponsePresent();
    }
}
