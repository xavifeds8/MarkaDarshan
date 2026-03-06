package com.markdownpreview.util;

import java.nio.file.Path;

/**
 * Centralized path validation to prevent directory traversal attacks.
 * All file-system operations must validate paths through this class.
 */
public final class PathValidator {

    private PathValidator() {
        // utility class
    }

    /**
     * Resolve a user-supplied relative path against the root directory,
     * and verify it doesn't escape the root.
     *
     * @param rootDir      the application root directory
     * @param relativePath the user-supplied path
     * @return the resolved, normalized absolute path
     * @throws SecurityException if the resolved path escapes the root
     */
    public static Path resolveAndValidate(Path rootDir, String relativePath) {
        if (relativePath == null) {
            relativePath = "";
        }
        Path resolved = rootDir.resolve(relativePath).normalize();
        if (!resolved.startsWith(rootDir)) {
            throw new SecurityException("Path traversal denied: " + relativePath);
        }
        return resolved;
    }

    /**
     * Validate that a given absolute path is within the root directory.
     *
     * @param rootDir the application root directory
     * @param target  the absolute path to check
     * @throws SecurityException if the path escapes the root
     */
    public static void assertWithinRoot(Path rootDir, Path target) {
        Path normalized = target.normalize();
        if (!normalized.startsWith(rootDir)) {
            throw new SecurityException("Path traversal denied: " + target);
        }
    }
}
