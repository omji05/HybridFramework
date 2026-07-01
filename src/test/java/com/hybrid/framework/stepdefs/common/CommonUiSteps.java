package com.hybrid.framework.stepdefs.common;

import com.hybrid.framework.utils.DownloadUtils;
import io.cucumber.java.en.Then;
import org.testng.Assert;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared UI assertion steps (all domains).
 */
public class CommonUiSteps {

    @Then("the file {string} should be downloaded")
    public void theFileShouldBeDownloaded(String fileName) {
        Path downloaded = DownloadUtils.waitForDownload(fileName);
        Assert.assertTrue(
                Files.exists(downloaded),
                "Expected downloaded file was not found: " + fileName
        );
    }

    @Then("the file {string} should be downloaded within {int} seconds")
    public void theFileShouldBeDownloadedWithin(String fileName, int timeoutSeconds) {
        Path downloaded = DownloadUtils.waitForDownload(fileName, timeoutSeconds);
        Assert.assertTrue(
                Files.exists(downloaded),
                "Expected downloaded file was not found within " + timeoutSeconds + "s: " + fileName
        );
    }
}
