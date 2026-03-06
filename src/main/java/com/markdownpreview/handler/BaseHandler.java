package com.markdownpreview.handler;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.service.FileService;
import com.markdownpreview.util.HttpUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base handler that provides common error handling, logging, and access
 * to shared services. All API handlers should extend this class.
 */
public abstract class BaseHandler implements HttpHandler {

    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final FileService fileService;
    protected final AppConfig config;

    protected BaseHandler(FileService fileService, AppConfig config) {
        this.fileService = fileService;
        this.config = config;
    }

    @Override
    public final void handle(HttpExchange exchange) throws IOException {
        try {
            handleRequest(exchange);
        } catch (SecurityException e) {
            logger.warning("Access denied: " + e.getMessage());
            HttpUtils.sendError(exchange, 403, "Access denied");
        } catch (AccessDeniedException e) {
            logger.warning("Access denied to path: " + e.getFile());
            HttpUtils.sendError(exchange, 403, "Access denied to this resource");
        } catch (NoSuchFileException e) {
            logger.fine("Not found: " + e.getFile());
            HttpUtils.sendError(exchange, 404, "Not found: " + e.getFile());
        } catch (IllegalArgumentException e) {
            logger.fine("Bad request: " + e.getMessage());
            HttpUtils.sendError(exchange, 400, e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Internal server error handling " + exchange.getRequestURI(), e);
            HttpUtils.sendError(exchange, 500, "Internal server error");
        }
    }

    /**
     * Subclasses implement this to handle the request.
     * Exceptions are caught by the base class and converted to appropriate HTTP responses.
     */
    protected abstract void handleRequest(HttpExchange exchange) throws Exception;
}
