package com.hybrid.framework.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hybrid.framework.config.FrameworkConstants;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for JSON serialization/deserialization using Jackson.
 * <p>
 * Methods are grouped by primary consumer:
 * <ul>
 *   <li><b>BDD (Cucumber)</b> — payload templates, Jayway JsonPath overrides, context placeholders
 *       ({@link com.hybrid.framework.services.ApiHttpService}).</li>
 *   <li><b>General</b> — POJO deserialization, direct tree mutation, serialization.</li>
 * </ul>
 * </p>
 */
public final class JsonUtils {

    private static final Logger LOG = LogManager.getLogger(JsonUtils.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private JsonUtils() {
        // Utility class — no instantiation
    }

    // ══════════════════════════════════════════════════════════════
    // BDD (Cucumber) — payload templates, JsonPath overrides, context placeholders
    // Used by ApiHttpService, RealWorldApiService, and feature-file sentinels.
    // ══════════════════════════════════════════════════════════════

    /** Feature-file sentinel: omit the field from the payload (field absent in JSON). */
    public static final String OVERRIDE_MISSING = "<missing>";

    /** Feature-file sentinel: set the field to an empty string ({@code ""}). */
    public static final String OVERRIDE_EMPTY = "<empty>";

    /** Feature-file sentinel: set the field to JSON {@code null}. */
    public static final String OVERRIDE_NULL = "<null>";

    /**
     * Feature-file sentinel prefix for API chaining: resolve the field value from scenario context.
     * Format: {@code <context:keyName>} (e.g. {@code <context:articleSlug>}).
     */
    public static final String OVERRIDE_CONTEXT_PREFIX = "<context:";

    private static final Pattern CONTEXT_OVERRIDE_PATTERN =
            Pattern.compile("^<context:([^>]+)>$");

    /**
     * Reads a JSON payload file as a mutable tree (JsonNode).
     */
    public static ObjectNode readPayloadAsTree(String fileName) {
        Path filePath = FrameworkConstants.PAYLOADS_DIR.resolve(fileName);
        try {
            JsonNode node = MAPPER.readTree(filePath.toFile());
            //java 17 + way of comaparing and assigning without explicit casting
            // if (node instanceof ObjectNode objectNode) {
            //     return objectNode;
            // }
            if (node instanceof ObjectNode) {
                ObjectNode objectNode = (ObjectNode) node;
                return objectNode;
            }
            throw new IllegalStateException("Root of JSON is not an object: " + fileName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON tree: " + filePath, e);
        }
    }

    /**
     * Loads a JSON payload file as a template and applies dot-notation field overrides
     * using Jayway JsonPath ({@code DocumentContext#set} / {@code delete}).
     * <p>
     * Override values support feature-file sentinels:
     * <ul>
     *   <li>{@link #OVERRIDE_MISSING} — remove the field from the payload</li>
     *   <li>{@link #OVERRIDE_EMPTY} — set the field to an empty string</li>
     *   <li>{@link #OVERRIDE_NULL} — set the field to JSON {@code null}</li>
     *   <li>{@code <context:keyName>} — set the field from scenario context (API chaining);
     *       resolved inline with other overrides in the same map — no separate file load</li>
     *   <li>any other value — set or replace the field</li>
     * </ul>
     * Prefer {@code ApiHttpService#setPayloadFromFile(fileName, overrides)} in tests so
     * {@code <context:...>} is resolved automatically from {@code TestContext}.
     * </p>
     *
     * <pre>{@code
     *   ObjectNode login = JsonUtils.readPayloadWithOverrides("login_user.json", Map.of(
     *       "user.email", OVERRIDE_MISSING,
     *       "user.password", OVERRIDE_EMPTY
     *   ));
     * }</pre>
     *
     * @param fileName       payload file name under {@code src/test/resources/payloads/}
     * @param fieldOverrides map of dot paths (e.g. {@code user.email}) to string values
     * @return mutable copy of the template with overrides applied
     */
    public static ObjectNode readPayloadWithOverrides(String fileName, Map<String, String> fieldOverrides) {
        return readPayloadWithOverrides(fileName, fieldOverrides, null);
    }

    /**
     * Loads the template once and applies all overrides (including {@code <context:keyName>})
     * in a single mutation pass.
     *
     * @param contextLookup resolves {@code <context:keyName>} sentinels (e.g. {@code testContext::get});
     *                      required when the override map contains context sentinels
     */
    public static ObjectNode readPayloadWithOverrides(
            String fileName,
            Map<String, String> fieldOverrides,
            Function<String, Object> contextLookup) {
        ObjectNode tree = readPayloadAsTree(fileName);
        ObjectNode result = applyOverrides(tree, fieldOverrides, contextLookup);
        LOG.info("Loaded payload template '{}' with {} field override(s)", fileName,
                fieldOverrides == null ? 0 : fieldOverrides.size());
        return result;
    }

    /** Returns {@code true} when {@code value} is a {@code <context:keyName>} sentinel. */
    public static boolean isContextOverride(String value) {
        return value != null && CONTEXT_OVERRIDE_PATTERN.matcher(value).matches();
    }

    /** Extracts the context key from a {@code <context:keyName>} sentinel. */
    public static String extractContextKey(String value) {
        Matcher matcher = CONTEXT_OVERRIDE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Not a context override sentinel: " + value);
        }
        return matcher.group(1);
    }

    /**
     * Converts a JsonNode tree to a JSON string.
     */
    public static String treeToString(JsonNode tree) {
        try {
            return MAPPER.writeValueAsString(tree);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert tree to string", e);
        }
    }

    /**
     * Sets or removes a String field at a dot-notation path on an existing tree.
     * Blank or {@code null} values remove the field; non-blank values set or replace it.
     *
     * @return the same root ObjectNode (for chaining)
     */
    public static ObjectNode modifyFieldAtPath(ObjectNode tree, String dotPath, String value) {
        String effectiveValue = (value == null || value.isBlank()) ? OVERRIDE_MISSING : value;
        ObjectNode mutated = applyOverrides(tree, Map.of(dotPath, effectiveValue), null);
        tree.removeAll();
        tree.setAll(mutated);
        return tree;
    }

    /**
     * Removes a field at a dot-notation path (e.g. {@code user.password}).
     *
     * @return the same root ObjectNode (for chaining)
     */
    public static ObjectNode removeFieldAtPath(ObjectNode tree, String dotPath) {
        return modifyFieldAtPath(tree, dotPath, OVERRIDE_MISSING);
    }

    // ── BDD private helpers (JsonPath override pipeline) ─────────

    private static ObjectNode applyOverrides(
            ObjectNode tree,
            Map<String, String> fieldOverrides,
            Function<String, Object> contextLookup) {

        //as ObjectNode is mutable for Jackson only, while Jayway JsonPath set(), delete() works on DocumentContext
        DocumentContext doc = JsonPath.parse(treeToString(tree));
        if (fieldOverrides != null) {
            fieldOverrides.forEach((path, value) -> applyOverride(doc, path, value, contextLookup));
        }
        return documentContextToObjectNode(doc);
    }

    private static void applyOverride(
            DocumentContext doc,
            String dotPath,
            String value,
            Function<String, Object> contextLookup) {
        String effectiveValue = resolveContextOverride(value, contextLookup);
        String jsonPath = toJsonPath(dotPath);
        if (OVERRIDE_MISSING.equals(effectiveValue)) {
            doc.delete(jsonPath);
            LOG.debug("Removed JSON field '{}' (missing override)", dotPath);
        } else if (OVERRIDE_EMPTY.equals(effectiveValue)) {
            doc.set(jsonPath, "");
            LOG.debug("Set JSON field '{}' → '' (empty override)", dotPath);
        } else if (OVERRIDE_NULL.equals(effectiveValue)) {
            doc.set(jsonPath, null);
            LOG.debug("Set JSON field '{}' → null (null override)", dotPath);
        } else if (effectiveValue == null) {
            throw new IllegalArgumentException(
                    "Override value for '" + dotPath + "' must not be null; use " + OVERRIDE_MISSING
                            + ", " + OVERRIDE_EMPTY + ", or " + OVERRIDE_NULL);
        } else {
            Object coerced = coerceToExistingType(doc, jsonPath, effectiveValue);
            doc.set(jsonPath, coerced);
            LOG.debug("Set JSON field '{}' → '{}' on template", dotPath, effectiveValue);
        }
    }

    private static String resolveContextOverride(String value, Function<String, Object> contextLookup) {
        if (!isContextOverride(value)) {
            return value;
        }
        String contextKey = extractContextKey(value);
        if (contextLookup == null) {
            throw new IllegalArgumentException(
                    "Override '" + value + "' requires TestContext; use"
                            + " ApiHttpService#setPayloadFromFile(fileName, overrides)");
        }
        Object resolved = contextLookup.apply(contextKey);
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "Context key '" + contextKey + "' must be set before applying override '" + value + "'");
        }
        String resolvedValue = String.valueOf(resolved);
        LOG.debug("Resolved context override '{}' → '{}'", value, resolvedValue);
        return resolvedValue;
    }

    private static Object coerceToExistingType(DocumentContext doc, String jsonPath, String value) {
        try {
            Object existing = doc.read(jsonPath);
            if (existing == null) {
                return value;
            }
            if (existing instanceof Boolean) {
                Boolean parsed = tryParseBoolean(value);
                return parsed != null ? parsed : value;
            }
            if (existing instanceof Integer) {
                Integer parsed = tryParseInt(value);
                return parsed != null ? parsed : value;
            }
            if (existing instanceof Long) {
                Long parsed = tryParseLong(value);
                return parsed != null ? parsed : value;
            }
            if (existing instanceof Double || existing instanceof Float) {
                Double parsed = tryParseDouble(value);
                return parsed != null ? parsed : value;
            }
            if (existing instanceof BigDecimal) {
                Double parsed = tryParseDouble(value);
                return parsed != null ? parsed : value;
            }
        } catch (Exception ignored) {
            // Path absent or unreadable — fall through to string default.
        }
        return value;
    }

    private static String toJsonPath(String dotPath) {
        if (dotPath == null || dotPath.isBlank()) {
            throw new IllegalArgumentException("Field path must not be blank");
        }
        if (dotPath.startsWith("$")) {
            return dotPath;
        }
        return "$." + dotPath;
    }

    private static ObjectNode documentContextToObjectNode(DocumentContext doc) {
        try {
            JsonNode node = MAPPER.readTree(doc.jsonString());
            if (node instanceof ObjectNode objectNode) {
                return objectNode;
            }
            throw new IllegalStateException("Mutated JSON root is not an object");
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert mutated JSON to ObjectNode", e);
        }
    }

    private static Boolean tryParseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return null;
    }

    private static Integer tryParseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long tryParseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // General — POJO deserialization, direct tree mutation, serialization
    // ══════════════════════════════════════════════════════════════

    // ── Deserialization ──────────────────────────────────────────

    /**
     * Reads a JSON file from the payloads directory and deserializes to a POJO.
     *
     * @param fileName payload file name (e.g., "register_user.json")
     * @param clazz    target class
     * @return deserialized object
     */
    public static <T> T readPayload(String fileName, Class<T> clazz) {
        Path filePath = FrameworkConstants.PAYLOADS_DIR.resolve(fileName);
        try {
            T result = MAPPER.readValue(filePath.toFile(), clazz);
            LOG.info("Deserialized payload '{}' into {}", fileName, clazz.getSimpleName());
            return result;
        } catch (IOException e) {
            LOG.error("Failed to read payload '{}': {}", fileName, e.getMessage());
            throw new RuntimeException("Payload deserialization failure: " + filePath, e);
        }
    }

    /**
     * Deserializes a JSON string to a POJO.
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * Deserializes a JSON string to a generic type (e.g., List, Map).
     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (IOException e) {
            throw new RuntimeException("JSON deserialization failed", e);
        }
    }

    /**
     * Reads a JSON file as a Map.
     */
    public static Map<String, Object> readPayloadAsMap(String fileName) {
        Path filePath = FrameworkConstants.PAYLOADS_DIR.resolve(fileName);
        try {
            return MAPPER.readValue(filePath.toFile(), new TypeReference<>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to read JSON as Map: " + filePath, e);
        }
    }

    // ── Serialization ────────────────────────────────────────────

    /**
     * Serializes an object to a JSON string.
     */
    public static String toJson(Object object) {
        try {
            return MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    // ── JSON tree manipulation (without POJO) ────────────────────

    // ── Modify / Add fields (put creates if absent, overwrites if present) ──

    /**
     * Sets a String value on the given field. Creates the field if it does not exist.
     *
     * @return the same ObjectNode (for chaining)
     */
    public static ObjectNode modifyField(ObjectNode tree, String field, String value) {
        tree.put(field, value);
        LOG.debug("Set JSON field '{}' → '{}'", field, value);
        return tree;
    }

    /**
     * Sets an int value on the given field.
     */
    public static ObjectNode modifyField(ObjectNode tree, String field, int value) {
        tree.put(field, value);
        LOG.debug("Set JSON field '{}' → {}", field, value);
        return tree;
    }

    /**
     * Sets a long value on the given field.
     */
    public static ObjectNode modifyField(ObjectNode tree, String field, long value) {
        tree.put(field, value);
        LOG.debug("Set JSON field '{}' → {}", field, value);
        return tree;
    }

    /**
     * Sets a double value on the given field.
     */
    public static ObjectNode modifyField(ObjectNode tree, String field, double value) {
        tree.put(field, value);
        LOG.debug("Set JSON field '{}' → {}", field, value);
        return tree;
    }

    /**
     * Sets a boolean value on the given field.
     */
    public static ObjectNode modifyField(ObjectNode tree, String field, boolean value) {
        tree.put(field, value);
        LOG.debug("Set JSON field '{}' → {}", field, value);
        return tree;
    }

    /**
     * Sets a JsonNode value (object, array, or any node) on the given field.
     * Use this to set nested JSON objects or arrays.
     *
     * <pre>{@code
     *   ObjectNode address = JsonUtils.createObjectNode();
     *   address.put("city", "Bangalore");
     *   address.put("zip", "560001");
     *   JsonUtils.modifyField(payload, "address", address);
     * }</pre>
     */
    public static ObjectNode modifyField(ObjectNode tree, String field, JsonNode value) {
        tree.set(field, value);
        LOG.debug("Set JSON field '{}' → [JsonNode]", field);
        return tree;
    }

    /**
     * Parses a raw JSON string and sets the resulting node on the given field.
     * Useful when the value is an inline JSON object or array expressed as a string.
     *
     * <pre>{@code
     *   JsonUtils.modifyFieldFromJson(payload, "address", "{\"city\":\"Bangalore\",\"zip\":\"560001\"}");
     *   JsonUtils.modifyFieldFromJson(payload, "tags", "[\"api\",\"regression\"]");
     * }</pre>
     *
     * @return the same ObjectNode (for chaining)
     */
    public static ObjectNode modifyFieldFromJson(ObjectNode tree, String field, String jsonValue) {
        try {
            JsonNode node = MAPPER.readTree(jsonValue);
            tree.set(field, node);
            LOG.debug("Set JSON field '{}' from raw JSON string", field);
            return tree;
        } catch (IOException e) {
            throw new RuntimeException("Invalid JSON value for field '" + field + "': " + jsonValue, e);
        }
    }

    // ── Remove fields ────────────────────────────────────────────

    /**
     * Removes a single field from the JSON tree.
     *
     * @return the same ObjectNode (for chaining)
     */
    public static ObjectNode removeField(ObjectNode tree, String field) {
        tree.remove(field);
        LOG.debug("Removed JSON field '{}'", field);
        return tree;
    }

    /**
     * Removes multiple fields from the JSON tree in one call.
     *
     * <pre>{@code
     *   JsonUtils.removeFields(payload, "email", "phone", "address");
     * }</pre>
     *
     * @return the same ObjectNode (for chaining)
     */
    public static ObjectNode removeFields(ObjectNode tree, String... fields) {
        for (String field : fields) {
            tree.remove(field);
            LOG.debug("Removed JSON field '{}'", field);
        }
        return tree;
    }

    // ── Factory helpers ──────────────────────────────────────────

    /**
     * Creates an empty ObjectNode that can be populated and used as a nested JSON object.
     */
    public static ObjectNode createObjectNode() {
        return MAPPER.createObjectNode();
    }

    /**
     * Creates an empty ArrayNode that can be populated and used as a JSON array field.
     */
    public static ArrayNode createArrayNode() {
        return MAPPER.createArrayNode();
    }

    /**
     * Creates an ArrayNode pre-populated with the given string values.
     *
     * <pre>{@code
     *   JsonUtils.modifyField(payload, "tags", JsonUtils.createArrayNode("api", "smoke", "regression"));
     * }</pre>
     */
    public static ArrayNode createArrayNode(String... values) {
        ArrayNode array = MAPPER.createArrayNode();
        for (String v : values) {
            array.add(v);
        }
        return array;
    }

    /**
     * Converts a Java object (Map, List, POJO, etc.) to a JsonNode.
     * Useful when you have a Java collection and want to set it as a JSON field value.
     *
     * <pre>{@code
     *   Map<String, Object> address = Map.of("city", "Bangalore", "zip", "560001");
     *   JsonUtils.modifyField(payload, "address", JsonUtils.toJsonNode(address));
     *
     *   List<String> tags = List.of("api", "smoke");
     *   JsonUtils.modifyField(payload, "tags", JsonUtils.toJsonNode(tags));
     * }</pre>
     */
    public static JsonNode toJsonNode(Object value) {
        return MAPPER.valueToTree(value);
    }
}
