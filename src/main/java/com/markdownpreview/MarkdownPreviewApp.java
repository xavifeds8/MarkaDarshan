package com.markdownpreview;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.server.HttpServerFactory;
import com.sun.net.httpserver.HttpServer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main entry point for the Markdown Preview application.
 * Parses configuration and starts the HTTP server.
 */
public class MarkdownPreviewApp {

    private static final Logger logger = Logger.getLogger(MarkdownPreviewApp.class.getName());

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.fromArgs(args);

            logger.info("===========================================");
            logger.info("  Markdown Preview Server");
            logger.info("  Root: " + config.getRootDir());
            logger.info("  URL:  http://localhost:" + config.getPort());
            logger.info("===========================================");

            HttpServer server = HttpServerFactory.create(config);
            server.start();

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down server...");
                server.stop(2);
                logger.info("Server stopped.");
            }));

            logger.info("Server started! Open http://localhost:" + config.getPort() + " in your browser.");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start server", e);
            System.exit(1);
        }
    }
}
