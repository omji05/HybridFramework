# Hybrid Automation Framework

> Industry-level, unified solution for **UI** and **API** testing — scalable, thread-safe, and CI/CD-ready.

---

## Tech Stack


| Layer         | Technology                              |
| ------------- | --------------------------------------- |
| Language      | Java 17+                                |
| Build         | Maven 3.9+                              |
| UI Automation | Selenium 4.x, WebDriverManager          |
| API Testing   | Rest Assured 5.x, Jackson 2.x           |
| BDD           | Cucumber 7.x (Gherkin feature files)    |
| Test Runner   | TestNG 7.x (Cucumber runner host)       |
| Reporting     | Extent Reports 5.x + Allure Reports 2.x |
| Logging       | Log4j2                                  |
| Data          | Apache POI (Excel), OpenCSV             |
| Database      | JDBC (MySQL Connector)                  |
| CI/CD         | GitHub Actions + Jenkins                |


---

## Project Structure

```
hybrid-automation-framework/
│
├── pom.xml                                          # Maven build & dependencies
├── Jenkinsfile                                      # Jenkins pipeline
├── README.md
│
├── .github/workflows/ci.yml                         # GitHub Actions workflow
│
├── src/
│   ├── main/java/com/hybrid/framework/
│   │   ├── config/                                  # ConfigReader, FrameworkConstants
│   │   ├── driver/                                  # DriverManager, BrowserFactory
│   │   ├── pages/BasePage.java                      # POM base with PageFactory
│   │   ├── api/                                     # ApiUtils, ApiResponseUtils, auth
│   │   ├── db/DatabaseUtils.java                    # JDBC CRUD utility
│   │   ├── security/PasswordUtils.java              # AES/Base64 encryption
│   │   ├── reporting/                               # ExtentReportManager, ExtentCucumberPlugin
│   │   ├── listeners/TestNGListener.java            # Suite lifecycle + failure screenshots
│   │   └── utils/                                   # WaitUtils, ScreenshotUtils, JsonUtils, etc.
│   │
│   └── test/
│       ├── java/com/hybrid/framework/
│       │   ├── context/                             # TestContext, ContextKeys (PicoContainer)
│       │   ├── services/                            # ApiHttpService, RealWorldApiService
│       │   ├── pages/eventhub/                      # EventHub UI page objects
│       │   ├── api/models/realworld/                # RealWorld API POJOs
│       │   ├── stepdefs/                            # Hooks, UI/API/common step definitions
│       │   └── runners/                             # TestRunner, FailedScenariosRunner
│       │
│       └── resources/
│           ├── config.properties                    # Environment configuration
│           ├── cucumber.properties                  # Cucumber settings
│           ├── testng-bdd.xml                       # BDD suite (Cucumber runners)
│           ├── features/
│           │   ├── ui/auth/login.feature            # EventHub UI login scenarios
│           │   └── realworld_api.feature            # RealWorld API scenarios
│           ├── payloads/                            # JSON request templates
│           └── schemas/                             # JSON Schema validation files
```

---

## Architecture & Design Patterns

### Singleton + ThreadLocal (DriverManager)

- **Single** configuration reader (`ConfigReader`) shared across threads.
- **Per-thread** WebDriver via `ThreadLocal<WebDriver>` — safe for parallel BDD execution.

### Factory Pattern (BrowserFactory)

- Creates Chrome, Firefox, or Edge drivers based on configuration.
- Handles headless mode, window sizing, and timeout configuration.

### Page Object Model with Page Factory

- `BasePage` initializes elements via `PageFactory.initElements()`.
- EventHub pages under `pages/eventhub/` encapsulate locators and actions.

### BDD Workflow (Cucumber + PicoContainer)

- **PicoContainer** injects a per-scenario `TestContext` and services into step definition constructors.
- `**Hooks`** manages browser lifecycle for `@ui` scenarios and token cleanup for `@api`.
- `**ApiHttpService**` / `**RealWorldApiService**` handle HTTP orchestration; assertions via `**ApiResponseUtils**`.
- Thin step classes: `EventHubAuthSteps`, `RealWorldApiSteps`, `CommonApiSteps`.

---

## Running Tests

### Prerequisites

- Java 17+ installed (`java -version`)
- Maven 3.9+ installed (`mvn -version`)
- Chrome/Firefox/Edge browser installed

### Command Reference

```bash
# Full BDD suite (all scenarios)
mvn clean test

# Smoke scenarios (@smoke tag)
mvn clean test -Psmoke

# Regression scenarios (@regression tag)
mvn clean test -Pregression

# API-only scenarios (@api tag)
mvn clean test -Papi

# Custom tag filter
mvn clean test -Dbdd.tags="@smoke and @api"

# Specific browser
mvn clean test -Dbrowser=firefox -Dheadless=true

# Generate Allure report
mvn allure:serve
```

---

## Reports

### Extent Reports

Generated at `reports/extent/ExtentReport_<timestamp>.html`. Step-level detail for BDD via `ExtentCucumberPlugin`.

### Allure Reports

```bash
mvn allure:serve
# or
mvn allure:report
# → target/site/allure-maven-plugin/index.html
```

### Cucumber Reports

Generated at `target/cucumber-reports/cucumber.html`.

---

## CI/CD

### GitHub Actions

Trigger: push to `main`/`develop`, pull requests, or manual dispatch.

```yaml
gh workflow run ci.yml -f suite=regression -f browser=chrome -f environment=staging
```

Default CI run uses the `smoke` profile (`@smoke` tagged scenarios).

**GitHub secrets (optional):** `TEST_EMAIL`, `TEST_PASSWORD`, `API_AUTH_USERNAME`, `API_AUTH_PASSWORD`.

### Jenkins

Parameters: `SUITE` (`default`, `smoke`, `regression`, `api`), `BROWSER`, `HEADLESS`, `TAGS`, `ENVIRONMENT`.

---

## Configuration

All settings in `src/test/resources/config.properties`, with optional per-environment overlays in `src/test/resources/environments/`.

**Resolution order:** system property (`-Dkey=value`) → environment variable → environment profile → `config.properties`.

```bash
mvn test -Denvironment=staging -Dheadless=true
```

### JWT / Bearer token

When **both** `api.auth.username` and `api.auth.password` are set, the framework automatically obtains a Bearer token on the first `ApiUtils` call per thread and clears it in Cucumber `@After` hooks.

### API logging

Set `api.log.verbose=false` (or `API_LOG_VERBOSE=false` in CI) for quieter logs.

---

## Adding New Tests

### New BDD Scenario

1. Add a `.feature` file in `src/test/resources/features/`.
2. Create step definitions in `src/test/java/.../stepdefs/`.
3. Tag with `@ui` (needs browser) or `@api` (API only).
4. Use `@smoke` / `@regression` for suite filtering via Maven profiles.

### New Page Object

1. Create a class in `src/test/java/.../pages/` extending `BasePage`.
2. Define `@FindBy` elements as **private**.
3. Create action methods that return `this` (fluent).

### New API Scenario

1. Add POJOs in `src/test/java/.../api/models/` if needed.
2. Use `ApiHttpService` or extend a domain service (see `RealWorldApiService`).
3. Assert with `ApiResponseUtils` and JSON schemas under `src/test/resources/schemas/`.

---



