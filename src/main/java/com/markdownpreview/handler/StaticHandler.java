package com.markdownpreview.handler;

import com.markdownpreview.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Serves the single-page frontend HTML application from classpath resources.
 */
public class StaticHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(StaticHandler.class.getName());
    private static final String FRONTEND_RESOURCE = "/frontend.html";

    private final String cachedHtml;

    public StaticHandler() {
        this.cachedHtml = loadFrontendHtml();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/") && !path.equals("/index.html")) {
            HttpUtils.send404(exchange);
            return;
        }
        HttpUtils.sendHtml(exchange, 200, cachedHtml);
    }

    private String loadFrontendHtml() {
        try (InputStream is = getClass().getResourceAsStream(FRONTEND_RESOURCE)) {
            if (is == null) {
                logger.severe("Frontend resource not found: " + FRONTEND_RESOURCE);
                return "<html><body><h1>Error: frontend.html not found in classpath</h1></body></html>";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.severe("Failed to load frontend resource: " + e.getMessage());
            return "<html><body><h1>Error loading frontend</h1></body></html>";
        }
    }
}
