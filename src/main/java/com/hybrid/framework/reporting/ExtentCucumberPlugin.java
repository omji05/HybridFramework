package com.hybrid.framework.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.GherkinKeyword;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.hybrid.framework.utils.ScreenshotUtils;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.HookTestStep;
import io.cucumber.plugin.event.PickleStepTestStep;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import io.cucumber.plugin.event.TestRunStarted;
import io.cucumber.plugin.event.TestStepFinished;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cucumber plugin that maps Feature → Scenario → Step into Extent Reports.
 * Disabled when {@code extent.report.enabled=false}.
 */
public class ExtentCucumberPlugin implements ConcurrentEventListener {

    private static final Map<String, ExtentTest> FEATURE_NODES = new ConcurrentHashMap<>();
    private static final ThreadLocal<ExtentTest> SCENARIO_NODE = new ThreadLocal<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, e -> { /* init via getReports() */ });
        publisher.registerHandlerFor(TestCaseStarted.class, this::onScenarioStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::onStepFinished);
        publisher.registerHandlerFor(TestCaseFinished.class, e -> SCENARIO_NODE.remove());
        publisher.registerHandlerFor(TestRunFinished.class, this::onRunFinished);
    }

    private void onScenarioStarted(TestCaseStarted event) {
        if (ExtentReportManager.getReports() == null) return;

        TestCase testCase = event.getTestCase();
        String featureName = featureNameFrom(testCase.getUri());

        ExtentTest featureNode = FEATURE_NODES.computeIfAbsent(featureName,
                name -> ExtentReportManager.getReports().createTest(
                        com.aventstack.extentreports.gherkin.model.Feature.class, name));

        ExtentTest scenarioNode = featureNode.createNode(
                com.aventstack.extentreports.gherkin.model.Scenario.class, testCase.getName());
        testCase.getTags().forEach(scenarioNode::assignCategory);
        SCENARIO_NODE.set(scenarioNode);
    }

    private void onStepFinished(TestStepFinished event) {
        ExtentTest scenarioNode = SCENARIO_NODE.get();
        if (scenarioNode == null) return;

        Result result = event.getResult();
        if (event.getTestStep() instanceof HookTestStep hookStep) {
            if (result.getStatus() == Status.FAILED) {
                ExtentTest hookNode = scenarioNode.createNode("Hook: " + hookStep.getHookType().name());
                logFailure(hookNode, result);
            }
            return;
        }

        if (!(event.getTestStep() instanceof PickleStepTestStep pickleStep)) return;

        String keyword = pickleStep.getStep().getKeyword().trim();
        String stepText = pickleStep.getStep().getText();
        ExtentTest stepNode = createStepNode(scenarioNode, keyword, stepText);
        logResult(stepNode, result);
    }

    private ExtentTest createStepNode(ExtentTest scenarioNode, String keyword, String stepText) {
        try {
            return scenarioNode.createNode(new GherkinKeyword(keyword), stepText);
        } catch (ClassNotFoundException e) {
            return scenarioNode.createNode(keyword + " " + stepText);
        }
    }

    private void logResult(ExtentTest stepNode, Result result) {
        switch (result.getStatus()) {
            case PASSED -> stepNode.pass("");
            case FAILED -> logFailure(stepNode, result);
            case SKIPPED -> {
                Throwable error = result.getError();
                if (error != null) stepNode.skip(error);
                else stepNode.skip("Step skipped");
            }
            case PENDING -> stepNode.warning("Step pending");
            case UNDEFINED -> stepNode.warning("Step undefined");
            case AMBIGUOUS -> stepNode.warning("Step ambiguous");
            default -> stepNode.info("Status: " + result.getStatus());
        }
    }

    private void logFailure(ExtentTest stepNode, Result result) {
        Throwable error = result.getError();
        if (error != null) {
            stepNode.fail(error);
            stepNode.info(MarkupHelper.createCodeBlock(stackTrace(error)));
        } else {
            stepNode.fail("Step failed");
        }
        attachScreenshot(stepNode);
    }

    private void attachScreenshot(ExtentTest stepNode) {
        String base64 = ScreenshotUtils.captureAsBase64();
        if (base64 == null) return;
        try {
            stepNode.info("Failure Screenshot",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(base64).build());
        } catch (Exception ignored) {
            // Screenshot is best-effort; step failure is already recorded
        }
    }

    private void onRunFinished(TestRunFinished event) {
        FEATURE_NODES.clear();
        ExtentReportManager.flushReports();
    }

    private static String featureNameFrom(URI featureUri) {
        String path = featureUri.getSchemeSpecificPart();
        int lastSlash = path.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
        return fileName.replace(".feature", "").replace("_", " ").replace("-", " ");
    }

    private static String stackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        throwable.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
