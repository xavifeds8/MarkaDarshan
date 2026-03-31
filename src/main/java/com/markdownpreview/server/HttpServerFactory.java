package com.markdownpreview.server;

import com.markdownpreview.config.AppConfig;
import com.markdownpreview.handler.*;
import com.markdownpreview.service.FileService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Factory that creates and configures the HTTP server with all routes.
 * Centralizes server setup, making it easy to add new routes or middleware.
 */
public final class HttpServerFactory {

    private static final Logger logger = Logger.getLogger(HttpServerFactory.class.getName());

    private HttpServerFactory() {
        // utility class
    }

    /**
     * Create a fully configured HttpServer ready to be started.
     */
    public static HttpServer create(AppConfig config) throws IOException {
        FileService fileService = new FileService(config.getRootDir());

        HttpServer server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);

        // Use a thread pool instead of the default single-threaded executor
        server.setExecutor(Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * 2,
                r -> {
                    Thread t = new Thread(r, "http-worker");
                    t.setDaemon(true);
                    return t;
                }
        ));

        // Register routes
        server.createContext("/", new StaticHandler());
        server.createContext("/api/files", new FileListHandler(fileService, config));
        server.createContext("/api/content", new ContentHandler(fileService, config));
        server.createContext("/api/save", new SaveHandler(fileService, config));
        server.createContext("/api/mkdir", new MkdirHandler(fileService, config));
        server.createContext("/api/rename", new RenameHandler(fileService, config));
        server.createContext("/api/delete", new DeleteHandler(fileService, config));
        server.createContext("/api/move", new MoveHandler(fileService, config));
        server.createContext("/api/search", new SearchHandler(fileService, config));

        logger.info("Registered 8 API routes");
        return server;
    }
}
