package com.hybrid.framework.api;

import com.hybrid.framework.config.ConfigReader;
import com.hybrid.framework.config.FrameworkConstants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Redacts sensitive values from API log text and Allure result files.
 * Does not modify outbound HTTP payloads.
 */
public final class SensitiveDataRedactor {

    private static final Logger LOG = LogManager.getLogger(SensitiveDataRedactor.class);
    private static final String REDACTED = "***";

    private static final List<String> DEFAULT_FIELDS = List.of(
            "password", "token", "access_token", "refresh_token", "secret", "api_key", "apikey"
    );

    private static final Pattern AUTHORIZATION_VALUE = Pattern.compile(
            "(Authorization\\s*:\\s*)(Bearer|Token)\\s+\\S+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "html", "json", "xml");

    private SensitiveDataRedactor() {
    }

    public static String redact(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }

        ConfigReader config = ConfigReader.getInstance();
        if (!config.getBoolean("api.log.redact.enabled", true)) {
            return content;
        }

        String redacted = content;
        for (String field : resolveFields(config)) {
            redacted = redactJsonStringField(redacted, field);
            redacted = redactJsonNonStringField(redacted, field);
        }
        return AUTHORIZATION_VALUE.matcher(redacted).replaceAll("$1$2 " + REDACTED);
    }

    /** Scans Allure result files after the suite and redacts sensitive values in attachments. */
    public static void sanitizeAllureResults() {
        ConfigReader config = ConfigReader.getInstance();
        if (!config.getBoolean("api.log.redact.enabled", true)) {
            return;
        }

        Path resultsDir = Path.of(FrameworkConstants.ALLURE_RESULTS_DIR);
        if (!Files.isDirectory(resultsDir)) {
            return;
        }

        try (Stream<Path> files = Files.walk(resultsDir)) {
            files.filter(Files::isRegularFile)
                    .filter(SensitiveDataRedactor::isTextResultFile)
                    .forEach(SensitiveDataRedactor::sanitizeFile);
        } catch (IOException e) {
            LOG.warn("Could not sanitize Allure results in {}: {}", resultsDir, e.getMessage());
        }
    }

    private static boolean isTextResultFile(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return TEXT_EXTENSIONS.contains(name.substring(dot + 1));
    }

    private static void sanitizeFile(Path file) {
        try {
            String original = Files.readString(file, StandardCharsets.UTF_8);
            String redacted = redact(original);
            if (!original.equals(redacted)) {
                Files.writeString(file, redacted, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LOG.debug("Skipped Allure file {}: {}", file, e.getMessage());
        }
    }

    private static List<String> resolveFields(ConfigReader config) {
        String override = config.getProperty("api.log.redact.fields", "");
        if (override != null && !override.isBlank()) {
            return Arrays.stream(override.split(","))
                    .map(String::trim)
                    .filter(field -> !field.isEmpty())
                    .map(field -> field.toLowerCase(Locale.ROOT))
                    .toList();
        }
        return DEFAULT_FIELDS;
    }

    private static String redactJsonStringField(String content, String fieldName) {
        Pattern pattern = Pattern.compile(
                "(\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*)\"([^\"\\\\]|\\\\.)*\"",
                Pattern.CASE_INSENSITIVE
        );
        return pattern.matcher(content).replaceAll("$1\"" + REDACTED + "\"");
    }

    private static String redactJsonNonStringField(String content, String fieldName) {
        Pattern pattern = Pattern.compile(
                "(\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*)(?!\"|\\{|\\[)([^,}\\s]+)",
                Pattern.CASE_INSENSITIVE
        );
        return pattern.matcher(content).replaceAll("$1\"" + REDACTED + "\"");
    }
}
