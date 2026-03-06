package com.markdownpreview.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Application configuration. Supports CLI arguments and environment variables.
 *
 * <p>Usage:
 * <pre>
 *   java -jar app.jar [rootDir] [--port=8080] [--max-file-size=10485760]
 * </pre>
 *
 * <p>Environment variables:
 * <ul>
 *   <li>MD_PREVIEW_PORT — server port (default: 8080)</li>
 *   <li>MD_PREVIEW_ROOT — root directory (default: user home)</li>
 *   <li>MD_PREVIEW_MAX_FILE_SIZE — max upload size in bytes (default: 10MB)</li>
 * </ul>
 */
public class AppConfig {

    private static final int DEFAULT_PORT = 8080;
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int DEFAULT_MAX_REQUEST_BODY = 10 * 1024 * 1024; // 10 MB

    private final int port;
    private final Path rootDir;
    private final long maxFileSize;
    private final int maxRequestBody;

    private AppConfig(int port, Path rootDir, long maxFileSize, int maxRequestBody) {
        this.port = port;
        this.rootDir = rootDir;
        this.maxFileSize = maxFileSize;
        this.maxRequestBody = maxRequestBody;
    }

    /**
     * Parse configuration from command-line arguments and environment variables.
     * CLI args take precedence over environment variables.
     */
    public static AppConfig fromArgs(String[] args) {
        int port = intEnv("MD_PREVIEW_PORT", DEFAULT_PORT);
        Path rootDir = null;
        long maxFileSize = longEnv("MD_PREVIEW_MAX_FILE_SIZE", DEFAULT_MAX_FILE_SIZE);
        int maxRequestBody = intEnv("MD_PREVIEW_MAX_REQUEST_BODY", DEFAULT_MAX_REQUEST_BODY);

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--max-file-size=")) {
                maxFileSize = Long.parseLong(arg.substring("--max-file-size=".length()));
            } else if (arg.startsWith("--max-request-body=")) {
                maxRequestBody = Integer.parseInt(arg.substring("--max-request-body=".length()));
            } else if (!arg.startsWith("--")) {
                rootDir = Paths.get(arg).toAbsolutePath().normalize();
            }
        }

        if (rootDir == null) {
            String envRoot = System.getenv("MD_PREVIEW_ROOT");
            if (envRoot != null && !envRoot.isBlank()) {
                rootDir = Paths.get(envRoot).toAbsolutePath().normalize();
            } else {
                rootDir = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
            }
        }

        if (!Files.isDirectory(rootDir)) {
            throw new IllegalArgumentException("Root directory does not exist: " + rootDir);
        }

        return new AppConfig(port, rootDir, maxFileSize, maxRequestBody);
    }

    public int getPort() {
        return port;
    }

    public Path getRootDir() {
        return rootDir;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public int getMaxRequestBody() {
        return maxRequestBody;
    }

    // --- helpers ---

    private static int intEnv(String name, int defaultValue) {
        String val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static long longEnv(String name, long defaultValue) {
        String val = System.getenv(name);
        if (val != null && !val.isBlank()) {
            try {
                return Long.parseLong(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }
}
