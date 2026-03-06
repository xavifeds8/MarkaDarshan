package com.markdownpreview.service;

import com.markdownpreview.util.PathValidator;

import java.util.logging.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Business logic for all file-system operations.
 * This service is the single point of contact for file I/O,
 * keeping handlers thin and testable.
 */
public class FileService {

    private static final Logger logger = Logger.getLogger(FileService.class.getName());

    private final Path rootDir;

    public FileService(Path rootDir) {
        this.rootDir = rootDir;
    }

    public Path getRootDir() {
        return rootDir;
    }

    // =========================================================================
    // List directory contents
    // =========================================================================

    /**
     * Represents a single file/directory entry in a listing.
     */
    public static class FileEntry {
        public final String name;
        public final String path;
        public final boolean isDirectory;
        public final boolean isMarkdown;

        public FileEntry(String name, String path, boolean isDirectory, boolean isMarkdown) {
            this.name = name;
            this.path = path;
            this.isDirectory = isDirectory;
            this.isMarkdown = isMarkdown;
        }
    }

    /**
     * Result of listing a directory.
     */
    public static class DirectoryListing {
        public final String currentPath;
        public final String rootPath;
        public final List<FileEntry> items;

        public DirectoryListing(String currentPath, String rootPath, List<FileEntry> items) {
            this.currentPath = currentPath;
            this.rootPath = rootPath;
            this.items = items;
        }
    }

    /**
     * List the contents of a directory relative to the root.
     */
    public DirectoryListing listDirectory(String relativePath) throws IOException {
        Path targetDir = PathValidator.resolveAndValidate(rootDir, relativePath);

        if (!Files.isDirectory(targetDir)) {
            throw new NoSuchFileException("Directory not found: " + relativePath);
        }

        List<FileEntry> items = new ArrayList<>();

        // Add parent directory entry if not at root
        if (!targetDir.equals(rootDir)) {
            String parentPath = rootDir.relativize(targetDir.getParent()).toString();
            items.add(new FileEntry("..", parentPath, true, false));
        }

        List<Path> entries;
        try (var stream = Files.list(targetDir)) {
            entries = stream
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted((a, b) -> {
                        boolean aDir = Files.isDirectory(a);
                        boolean bDir = Files.isDirectory(b);
                        if (aDir != bDir) return aDir ? -1 : 1;
                        return a.getFileName().toString().compareToIgnoreCase(b.getFileName().toString());
                    })
                    .collect(Collectors.toList());
        }

        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            boolean isDir = Files.isDirectory(entry);
            boolean isMd = !isDir && isMarkdownFile(name);
            String entryPath = rootDir.relativize(entry).toString();
            items.add(new FileEntry(name, entryPath, isDir, isMd));
        }

        String currentPath = rootDir.relativize(targetDir).toString();
        return new DirectoryListing(currentPath, rootDir.toString(), items);
    }

    // =========================================================================
    // Read file content
    // =========================================================================

    /**
     * Result of reading a file.
     */
    public static class FileContent {
        public final String path;
        public final String name;
        public final String content;

        public FileContent(String path, String name, String content) {
            this.path = path;
            this.name = name;
            this.content = content;
        }
    }

    /**
     * Read the content of a file.
     */
    public FileContent readFile(String relativePath) throws IOException {
        Path targetFile = PathValidator.resolveAndValidate(rootDir, relativePath);

        if (!Files.isRegularFile(targetFile)) {
            throw new NoSuchFileException("File not found: " + relativePath);
        }

        String content = Files.readString(targetFile, StandardCharsets.UTF_8);
        String name = targetFile.getFileName().toString();
        return new FileContent(relativePath, name, content);
    }

    // =========================================================================
    // Save file
    // =========================================================================

    /**
     * Save content to a file, creating parent directories if needed.
     */
    public void saveFile(String relativePath, String content) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }

        Path targetFile = PathValidator.resolveAndValidate(rootDir, relativePath);

        // Create parent directories if needed
        Path parentDir = targetFile.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(targetFile, content, StandardCharsets.UTF_8);
        logger.info("Saved file: " + relativePath);
    }

    // =========================================================================
    // Create directory
    // =========================================================================

    /**
     * Create a directory (and any necessary parent directories).
     */
    public void createDirectory(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }

        Path targetDir = PathValidator.resolveAndValidate(rootDir, relativePath);
        Files.createDirectories(targetDir);
        logger.info("Created directory: " + relativePath);
    }

    // =========================================================================
    // Rename file or directory
    // =========================================================================

    /**
     * Rename a file or directory. Returns the new relative path.
     */
    public String rename(String oldRelativePath, String newName) throws IOException {
        if (oldRelativePath == null || oldRelativePath.isBlank()) {
            throw new IllegalArgumentException("Old path is required");
        }
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("New name is required");
        }

        Path source = PathValidator.resolveAndValidate(rootDir, oldRelativePath);
        Path target = source.getParent().resolve(newName);
        PathValidator.assertWithinRoot(rootDir, target);

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        String newPath = rootDir.relativize(target).toString();
        logger.info("Renamed " + oldRelativePath + " -> " + newPath);
        return newPath;
    }

    // =========================================================================
    // Delete file or directory
    // =========================================================================

    /**
     * Delete a file or directory (recursively if directory).
     */
    public void delete(String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }

        Path target = PathValidator.resolveAndValidate(rootDir, relativePath);

        if (Files.isDirectory(target)) {
            deleteDirectoryRecursively(target);
        } else {
            Files.delete(target);
        }
        logger.info("Deleted: " + relativePath);
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        }
    }

    // =========================================================================
    // Move file or directory
    // =========================================================================

    /**
     * Move a file or directory into a target directory. Returns the new relative path.
     */
    public String move(String sourceRelativePath, String targetDirRelativePath) throws IOException {
        if (sourceRelativePath == null || targetDirRelativePath == null) {
            throw new IllegalArgumentException("Source path and target path are required");
        }

        Path source = PathValidator.resolveAndValidate(rootDir, sourceRelativePath);
        Path targetDir = PathValidator.resolveAndValidate(rootDir, targetDirRelativePath);

        if (!Files.isDirectory(targetDir)) {
            throw new IllegalArgumentException("Target must be a directory");
        }

        Path target = targetDir.resolve(source.getFileName());
        PathValidator.assertWithinRoot(rootDir, target);

        Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        String newPath = rootDir.relativize(target).toString();
        logger.info("Moved " + sourceRelativePath + " -> " + newPath);
        return newPath;
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static boolean isMarkdownFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".md") || lower.endsWith(".markdown");
    }
}
