package com.leekwars.generator.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * Utility class for JsonNode operations using Jackson.
 * Replaces FastJSON for security reasons (CVE fixes).
 */
public class Json {

    private static final ObjectMapper mapper = JsonMapper.builder()
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
        .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES)
        .build();

    /**
     * Get the singleton ObjectMapper instance
     */
    public static ObjectMapper getMapper() {
        return mapper;
    }

    /**
     * Create a new empty JsonNode object
     */
    public static ObjectNode createObject() {
        return mapper.createObjectNode();
    }

    /**
     * Create a new empty JsonNode array
     */
    public static ArrayNode createArray() {
        return mapper.createArrayNode();
    }

    /**
     * Convert object to JsonNode string
     */
    public static String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JacksonException e) {
            throw new RuntimeException("JsonNode serialization error", e);
        }
    }

    /**
     * Parse JsonNode string to JsonNode
     */
    public static JsonNode parse(String json) {
        try {
            return mapper.readTree(json);
        } catch (JacksonException e) {
            throw new RuntimeException("JsonNode parsing error: " + json, e);
        }
    }

    /**
     * Parse JsonNode string to ObjectNode
     */
    public static ObjectNode parseObject(String json) {
        try {
            return (ObjectNode) mapper.readTree(json);
        } catch (JacksonException e) {
            throw new RuntimeException("JsonNode parsing error: " + json, e);
        }
    }

    /**
     * Parse JsonNode string to ArrayNode
     */
    public static ArrayNode parseArray(String json) {
        try {
            return (ArrayNode) mapper.readTree(json);
        } catch (JacksonException e) {
            throw new RuntimeException("JsonNode parsing error: " + json, e);
        }
    }

    /**
     * Parse JsonNode string to specific class
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JacksonException e) {
            throw new RuntimeException("JsonNode deserialization error", e);
        }
    }

    /**
     * Convert object to another type
     */
    public static <T> T convert(Object obj, Class<T> clazz) {
        return mapper.convertValue(obj, clazz);
    }
}
