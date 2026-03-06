package com.markdownpreview.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * HTTP utility methods shared across all handlers.
 */
public final class HttpUtils {

    private static final Logger logger = Logger.getLogger(HttpUtils.class.getName());

    private HttpUtils() {
        // utility class
    }

    /**
     * Send a JSON response with the given status code.
     */
    public static void sendJson(HttpExchange exchange, int statusCode, Object responseObj) throws IOException {
        String json = JsonUtils.toJson(responseObj);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Send a JSON error response.
     */
    public static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("error", message);
        sendJson(exchange, statusCode, error);
    }

    /**
     * Send a JSON success response with optional extra fields.
     */
    public static void sendSuccess(HttpExchange exchange, Map<String, Object> extraFields) throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        if (extraFields != null) {
            response.putAll(extraFields);
        }
        sendJson(exchange, 200, response);
    }

    /**
     * Send a 404 Not Found response.
     */
    public static void send404(HttpExchange exchange) throws IOException {
        byte[] resp = "404 Not Found".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(404, resp.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(resp);
        }
    }

    /**
     * Send an HTML response.
     */
    public static void sendHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Read the request body as a string, enforcing a maximum size.
     *
     * @param exchange       the HTTP exchange
     * @param maxRequestBody maximum allowed body size in bytes
     * @return the body as a string
     * @throws IOException if reading fails or body exceeds limit
     */
    public static String readRequestBody(HttpExchange exchange, int maxRequestBody) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int totalRead = 0;
        int bytesRead;
        while ((bytesRead = is.read(chunk)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxRequestBody) {
                throw new IOException("Request body exceeds maximum allowed size of " + maxRequestBody + " bytes");
            }
            buffer.write(chunk, 0, bytesRead);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * Extract a query parameter value from the request URI.
     */
    public static String getQueryParam(HttpExchange exchange, String paramName) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return "";
        }
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if (kv[0].equals(paramName) && kv.length == 2) {
                try {
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    logger.warning("Failed to decode query param: " + kv[1]);
                    return kv[1];
                }
            }
        }
        return "";
    }

    /**
     * Validate that the request method matches the expected method.
     * Sends a 405 error if it doesn't match.
     *
     * @return true if the method matches, false if a 405 was sent
     */
    public static boolean requireMethod(HttpExchange exchange, String expectedMethod) throws IOException {
        if (!expectedMethod.equalsIgnoreCase(exchange.getRequestMethod())) {
            sendError(exchange, 405, "Method not allowed");
            return false;
        }
        return true;
    }
}
