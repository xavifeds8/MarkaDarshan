package com.markdownpreview.util;

import java.util.List;
import java.util.Map;

/**
 * Lightweight JSON serialization utilities using only JDK classes.
 * Handles the simple JSON structures needed by this application.
 */
public final class JsonUtils {

    private JsonUtils() {
        // utility class
    }

    /**
     * Escape a string for safe inclusion in a JSON value.
     */
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    /**
     * Serialize a simple object to JSON string.
     * Supports: String, Number, Boolean, null, Map, List, and objects with public fields.
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String s) {
            return "\"" + escape(s) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map<?, ?> map) {
            return mapToJson(map);
        }
        if (obj instanceof List<?> list) {
            return listToJson(list);
        }
        // For POJOs, serialize public fields via reflection
        return pojoToJson(obj);
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(escape(entry.getKey().toString())).append("\":");
            sb.append(toJson(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private static String listToJson(List<?> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(toJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private static String pojoToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var field : obj.getClass().getFields()) {
            try {
                Object value = field.get(obj);
                if (!first) sb.append(",");
                sb.append("\"").append(escape(field.getName())).append("\":");
                sb.append(toJson(value));
                first = false;
            } catch (IllegalAccessException e) {
                // skip inaccessible fields
            }
        }
        sb.append("}");
        return sb.toString();
    }

    // =========================================================================
    // Simple JSON parser for request bodies
    // =========================================================================

    /**
     * Extract a string value from a JSON object by key.
     * This is a simple parser sufficient for flat JSON objects with string values.
     */
    public static String extractString(String json, String key) {
        if (json == null || key == null) return null;

        String searchPattern = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchPattern);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(':', keyIndex + searchPattern.length());
        if (colonIndex == -1) return null;

        // Skip whitespace after colon
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }

        if (valueStart >= json.length()) return null;

        // Handle null value
        if (json.startsWith("null", valueStart)) {
            return null;
        }

        // Must start with quote for string values
        if (json.charAt(valueStart) != '"') return null;

        valueStart++; // Skip opening quote
        StringBuilder value = new StringBuilder();
        boolean escaped = false;

        for (int i = valueStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '\\' -> value.append('\\');
                    case '"' -> value.append('"');
                    case '/' -> value.append('/');
                    case 'u' -> {
                        // Unicode escape sequence
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                value.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException e) {
                                value.append('u');
                            }
                        } else {
                            value.append('u');
                        }
                    }
                    default -> value.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break; // End of string
            } else {
                value.append(c);
            }
        }

        return value.toString();
    }
}
