# Hybrid Automation Framework

> Industry-level, unified solution for **UI** and **API** testing — scalable, thread-safe, and CI/CD-ready.

The reference application is **[EventHub](https://eventhub.rahulshettyacademy.com)** (UI + REST API). API tests also include **WireMock** stubs for isolated, CI-friendly scenarios..

---

## Tech Stack

| Layer         | Technology                              |
| ------------- | --------------------------------------- |
| Language      | Java 17+                                |
| Build         | Maven 3.9+                              |
| UI Automation | Selenium 4.x, WebDriverManager          |
| API Testing   | Rest Assured 5.x, Jackson 2.x           |
| API Stubbing  | WireMock 3.x                            |
| BDD           | Cucumber 7.x (Gherkin feature files)    |
| Test Runner   | TestNG 7.x (Cucumber runner host)       |
| DI            | PicoContainer (per-scenario injection)  |
| Reporting     | Extent Reports 5.x + Allure Reports 2.x |
| Logging       | Log4j2                                  |
| Data          | Apache POI (Excel), OpenCSV, DataFaker  |
| Database      | JDBC (MySQL Connector)                  |
| Utilities     | Lombok                                  |
| CI/CD         | GitHub Actions + Jenkins                |

---

## Project Structure

```
hybrid-automation-framework/
│
├── pom.xml                                          # Maven build, profiles, dependencies
├── Jenkinsfile                                      # Jenkins pipeline
├── README.md
│
├── .github/workflows/ci.yml                         # GitHub Actions (primary + rerun + gh-pages)
│
├── src/
│   ├── main/java/com/hybrid/framework/
│   │   ├── config/                                  # ConfigReader, FrameworkConstants
│   │   ├── driver/                                  # DriverManager, BrowserFactory
│   │   ├── pages/
│   │   │   ├── BasePage.java                        # POM base (By locators, fluent API)
│   │   │   └── actions/DropdownActions.java         # Composed UI interactions
│   │   ├── api/                                     # ApiUtils, ApiResponseUtils, ApiAuthManager,
│   │   │                                            # TokenProvider, SensitiveDataRedactor
│   │   ├── db/DatabaseUtils.java                    # JDBC CRUD utility
│   │   ├── security/PasswordUtils.java            # AES/Base64 encryption
│   │   ├── reporting/                               # ExtentReportManager, ExtentCucumberPlugin
│   │   ├── listeners/TestNGListener.java            # Suite lifecycle + failure screenshots
│   │   └── utils/                                   # WaitUtils, ScreenshotUtils, JsonUtils,
│   │                                                # DownloadUtils, ExcelUtils, CsvUtils, …
│   │
│   └── test/
│       ├── java/com/hybrid/framework/
│       │   ├── context/                             # TestContext, ContextKeys (PicoContainer)
│       │   ├── services/                            # ApiHttpService, EventHubApiService
│       │   ├── wiremock/WireMockSupport.java        # Suite-level WireMock lifecycle
│       │   ├── testdata/                            # EventHubTestDataFactory, event/attendee data
│       │   ├── pages/eventhub/                      # EventHub UI page objects + components
│       │   ├── stepdefs/
│       │   │   ├── Hooks.java                       # Browser, WireMock, auth teardown
│       │   │   ├── ui/eventhub/                     # EventHub UI step definitions
│       │   │   ├── api/                             # EventHubApiSteps
│       │   │   └── common/                          # CommonUiSteps, CommonApiSteps
│       │   └── runners/                             # TestRunner, FailedScenariosRunner
│       │
│       └── resources/
│           ├── config.properties                    # Base configuration
│           ├── environments/                        # config-qa.properties, config-staging.properties
│           ├── cucumber.properties                  # Cucumber settings
│           ├── testng-bdd.xml                       # Full suite (primary + rerun tests)
│           ├── testng-bdd-primary.xml               # Primary run only (CI default)
│           ├── testng-bdd-rerun.xml                 # Failed-scenario rerun only
│           ├── features/
│           │   ├── ui/                              # login, home, events, bookings
│           │   └── api/
│           │       ├── eventhub/                    # auth, events, bookings
│           │       └── wiremock_api.feature         # In-process API stubs
│           ├── payloads/                            # JSON request templates
│           └── schemas/                             # JSON Schema validation files
```

---

## Architecture & Design Patterns

### Singleton + ThreadLocal (DriverManager)

- **Single** configuration reader (`ConfigReader`) shared across threads.
- **Per-thread** WebDriver via `ThreadLocal<WebDriver>` — safe for parallel BDD execution.
- **Per-thread** JWT storage via `ApiAuthManager` — cleared after each scenario in `Hooks`.

### Factory Pattern (BrowserFactory)

- Creates Chrome, Firefox, or Edge drivers based on configuration.
- Handles headless mode, window sizing, and timeout configuration.

### Page Object Model (By locators)

- `BasePage` in `main` provides shared waits, clicks, typing, and composed helpers.
- Subclasses declare **`private final By`** locators and resolve elements at interaction time (no PageFactory).
- EventHub pages live under `pages/eventhub/` with fluent action methods.

### BDD Workflow (Cucumber + PicoContainer)

- **PicoContainer** injects a per-scenario `TestContext` and services into step definition constructors.
- **`Hooks`** starts the browser for `@ui`, validates `@wiremock` prerequisites, and tears down driver + auth after each scenario.
- **`ApiHttpService`** handles HTTP mechanics; **`EventHubApiService`** orchestrates EventHub domain flows; assertions via **`ApiResponseUtils`**.
- Thin step classes under `stepdefs/ui/eventhub/` and `stepdefs/api/`.

### Tags

| Tag | Purpose |
| --- | ------- |
| `@ui` | Starts browser via `Hooks` |
| `@api` | API scenario (no browser) |
| `@wiremock` | Requires `-Pmock` (`wiremock.enabled=true`) |
| `@hybrid` | Cross-layer data setup hook |
| `@smoke` / `@regression` | Suite filters via Maven profiles |

---

## Running Tests

### Prerequisites

- Java 17+ installed (`java -version`)
- Maven 3.9+ installed (`mvn -version`)
- Chrome/Firefox/Edge browser installed (for `@ui` scenarios)

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

# WireMock stub scenarios (@wiremock tag)
mvn clean test -Pmock

# Custom tag filter
mvn clean test -Dbdd.tags="@smoke and @api"

# Specific browser and environment
mvn clean test -Dbrowser=firefox -Dheadless=true -Denvironment=staging

# Parallel thread count (Maven filters testng*.xml at build time)
mvn clean test -Dparallel.thread.count=5

# Rerun failed scenarios from target/rerun.txt
mvn test -Dbdd.rerun.enabled=true -Dtestng.suite.name=testng-bdd-rerun.xml

# Generate Allure report
mvn allure:serve
```

---

## Reports

### Extent Reports

Generated at `target/reports/extent/ExtentReport_<timestamp>.html`. Step-level detail for BDD via `ExtentCucumberPlugin`.

### Allure Reports

```bash
mvn allure:serve
# or
mvn allure:report
# → target/site/allure-maven-plugin/index.html
```

Sensitive JSON fields (passwords, tokens) are redacted from Allure result files when `api.log.redact.enabled=true`.

### Cucumber Reports

Generated at `target/cucumber-reports/cucumber.html` (rerun: `rerun-cucumber.html`).

---

## CI/CD

### GitHub Actions

Trigger: push to `main`/`develop`, pull requests, or manual dispatch.

```bash
gh workflow run ci.yml -f suite=regression -f browser=chrome -f environment=staging
gh workflow run ci.yml -f suite=mock -f environment=qa
```

**Workflow inputs:** `suite` (`default`, `smoke`, `regression`, `api`, `mock`), `browser`, `environment` (`qa`, `staging`), `rerun_on_failure`.

Default CI run uses the `smoke` profile against the `qa` environment. The primary job runs `testng-bdd-primary.xml` with `bdd.rerun.enabled=false`. When `rerun_on_failure` is enabled, a second job replays failures from `target/rerun.txt`. Allure reports are published to GitHub Pages on `main`.

**GitHub secrets (optional):** `TEST_EMAIL`, `TEST_PASSWORD`, `API_AUTH_USERNAME`, `API_AUTH_PASSWORD`.

### Jenkins

Parameters: `SUITE` (`default`, `smoke`, `regression`, `api`), `BROWSER`, `HEADLESS`, `TAGS`, `ENVIRONMENT` (`qa`, `staging`, `prod`).

For WireMock scenarios locally or in custom pipelines, pass `-Pmock` (not yet a Jenkins `SUITE` choice).

---

## Configuration

All settings in `src/test/resources/config.properties`, with optional per-environment overlays in `src/test/resources/environments/config-{env}.properties`.

**Resolution order:** system property (`-Dkey=value`) → environment variable → environment profile → `config.properties`.

```bash
mvn test -Denvironment=staging -Dheadless=true
```

### EventHub URLs

| Key | Default |
| --- | ------- |
| `base.url` | `https://eventhub.rahulshettyacademy.com` |
| `api.base.uri` | `https://api.eventhub.rahulshettyacademy.com/api` |

Register a test account at `{base.url}/register`, then set `test.email` / `test.password` (or `TEST_EMAIL` / `TEST_PASSWORD` in CI).

### JWT / Bearer token

When **both** `api.auth.username` and `api.auth.password` are set, the framework obtains a token on the first `ApiUtils` call per thread via `TokenProvider` and clears it in Cucumber `@After` hooks.

| Key | Purpose |
| --- | ------- |
| `api.auth.scheme` | `Bearer` (EventHub) or `Token` |
| `api.auth.token.path` | Login endpoint (default `/auth/login`) |
| `api.auth.token.jsonpath` | JSON path to token (default `token`) |
| `api.auth.token.expected.status` | Expected HTTP status (default `200`) |

### API logging

| Key | Purpose |
| --- | ------- |
| `api.log.verbose` | Rest Assured request/response logging (`API_LOG_VERBOSE=false` in CI) |
| `api.log.blacklist.enabled` | Redact sensitive headers in console logs |
| `api.log.redact.enabled` | Redact sensitive JSON fields in Allure results after each suite |

### Parallel execution & rerun

| Key | Purpose |
| --- | ------- |
| `parallel.thread.count` | TestNG thread pool (override: `-Dparallel.thread.count=N`) |
| `bdd.rerun.enabled` | Enable failed-scenario rerun via `FailedScenariosRunner` |

---

## Adding New Tests

### New BDD Scenario

1. Add a `.feature` file under `src/test/resources/features/ui/` or `features/api/`.
2. Create step definitions in `src/test/java/.../stepdefs/` (reuse `common/` steps where possible).
3. Tag with `@ui` (needs browser), `@api` (API only), or `@wiremock` (needs `-Pmock`).
4. Use `@smoke` / `@regression` for suite filtering via Maven profiles.

### New Page Object

1. Create a class in `src/test/java/.../pages/` extending `BasePage`.
2. Define locators as **`private final By`** fields.
3. Create fluent action methods; return `this` or the next page object.

### New API Scenario

1. Add JSON payloads under `src/test/resources/payloads/` and schemas under `schemas/`.
2. Extend **`EventHubApiService`** (or call **`ApiHttpService`** directly for generic HTTP).
3. Add steps in `stepdefs/api/`; assert with **`ApiResponseUtils`** and JSON schemas.
4. For isolated stubs, add scenarios to `wiremock_api.feature` with `@wiremock` and run with `-Pmock`.

---
