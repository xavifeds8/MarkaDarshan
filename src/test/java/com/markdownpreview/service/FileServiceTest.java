package com.markdownpreview.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService(tempDir);
    }

    // =========================================================================
    // listDirectory
    // =========================================================================

    @Test
    void listDirectory_emptyDir_returnsEmptyItems() throws IOException {
        FileService.DirectoryListing listing = fileService.listDirectory("");
        assertEquals("", listing.currentPath);
        assertTrue(listing.items.isEmpty());
    }

    @Test
    void listDirectory_withFiles_returnsCorrectEntries() throws IOException {
        Files.createDirectory(tempDir.resolve("docs"));
        Files.writeString(tempDir.resolve("readme.md"), "# Hello");
        Files.writeString(tempDir.resolve("notes.txt"), "plain text");

        FileService.DirectoryListing listing = fileService.listDirectory("");

        // Should have: docs (dir), notes.txt (file), readme.md (file)
        assertEquals(3, listing.items.size());

        // Directories come first
        assertTrue(listing.items.get(0).isDirectory);
        assertEquals("docs", listing.items.get(0).name);

        // Markdown file detected
        FileService.FileEntry mdEntry = listing.items.stream()
                .filter(e -> e.name.equals("readme.md"))
                .findFirst().orElseThrow();
        assertTrue(mdEntry.isMarkdown);
        assertFalse(mdEntry.isDirectory);
    }

    @Test
    void listDirectory_hidesHiddenFiles() throws IOException {
        Files.writeString(tempDir.resolve(".hidden"), "secret");
        Files.writeString(tempDir.resolve("visible.md"), "# Visible");

        FileService.DirectoryListing listing = fileService.listDirectory("");
        assertEquals(1, listing.items.size());
        assertEquals("visible.md", listing.items.get(0).name);
    }

    @Test
    void listDirectory_subdirectory_includesParentEntry() throws IOException {
        Path subDir = Files.createDirectory(tempDir.resolve("sub"));
        Files.writeString(subDir.resolve("file.md"), "content");

        FileService.DirectoryListing listing = fileService.listDirectory("sub");
        assertEquals("sub", listing.currentPath);

        // First item should be ".." parent entry
        assertEquals("..", listing.items.get(0).name);
        assertTrue(listing.items.get(0).isDirectory);
    }

    @Test
    void listDirectory_nonExistent_throwsNoSuchFileException() {
        assertThrows(NoSuchFileException.class, () ->
                fileService.listDirectory("nonexistent"));
    }

    @Test
    void listDirectory_traversalAttack_throwsSecurityException() {
        assertThrows(SecurityException.class, () ->
                fileService.listDirectory("../../etc"));
    }

    // =========================================================================
    // readFile
    // =========================================================================

    @Test
    void readFile_existingFile_returnsContent() throws IOException {
        Files.writeString(tempDir.resolve("test.md"), "# Test Content");

        FileService.FileContent content = fileService.readFile("test.md");
        assertEquals("test.md", content.path);
        assertEquals("test.md", content.name);
        assertEquals("# Test Content", content.content);
    }

    @Test
    void readFile_nonExistent_throwsNoSuchFileException() {
        assertThrows(NoSuchFileException.class, () ->
                fileService.readFile("missing.md"));
    }

    // =========================================================================
    // saveFile
    // =========================================================================

    @Test
    void saveFile_newFile_createsFile() throws IOException {
        fileService.saveFile("new-file.md", "# New File");

        assertTrue(Files.exists(tempDir.resolve("new-file.md")));
        assertEquals("# New File", Files.readString(tempDir.resolve("new-file.md")));
    }

    @Test
    void saveFile_nestedPath_createsParentDirs() throws IOException {
        fileService.saveFile("a/b/c/deep.md", "deep content");

        assertTrue(Files.exists(tempDir.resolve("a/b/c/deep.md")));
        assertEquals("deep content", Files.readString(tempDir.resolve("a/b/c/deep.md")));
    }

    @Test
    void saveFile_emptyPath_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                fileService.saveFile("", "content"));
    }

    @Test
    void saveFile_nullPath_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                fileService.saveFile(null, "content"));
    }

    // =========================================================================
    // createDirectory
    // =========================================================================

    @Test
    void createDirectory_newDir_createsIt() throws IOException {
        fileService.createDirectory("new-dir");
        assertTrue(Files.isDirectory(tempDir.resolve("new-dir")));
    }

    @Test
    void createDirectory_nested_createsAllParents() throws IOException {
        fileService.createDirectory("a/b/c");
        assertTrue(Files.isDirectory(tempDir.resolve("a/b/c")));
    }

    // =========================================================================
    // rename
    // =========================================================================

    @Test
    void rename_file_renamesSuccessfully() throws IOException {
        Files.writeString(tempDir.resolve("old.md"), "content");

        String newPath = fileService.rename("old.md", "new.md");

        assertFalse(Files.exists(tempDir.resolve("old.md")));
        assertTrue(Files.exists(tempDir.resolve("new.md")));
        assertEquals("new.md", newPath);
    }

    @Test
    void rename_directory_renamesSuccessfully() throws IOException {
        Files.createDirectory(tempDir.resolve("old-dir"));

        String newPath = fileService.rename("old-dir", "new-dir");

        assertFalse(Files.exists(tempDir.resolve("old-dir")));
        assertTrue(Files.isDirectory(tempDir.resolve("new-dir")));
        assertEquals("new-dir", newPath);
    }

    // =========================================================================
    // delete
    // =========================================================================

    @Test
    void delete_file_deletesIt() throws IOException {
        Files.writeString(tempDir.resolve("to-delete.md"), "bye");

        fileService.delete("to-delete.md");

        assertFalse(Files.exists(tempDir.resolve("to-delete.md")));
    }

    @Test
    void delete_directory_deletesRecursively() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("dir-to-delete"));
        Files.writeString(dir.resolve("file1.md"), "content1");
        Files.writeString(dir.resolve("file2.md"), "content2");

        fileService.delete("dir-to-delete");

        assertFalse(Files.exists(tempDir.resolve("dir-to-delete")));
    }

    @Test
    void delete_emptyPath_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () ->
                fileService.delete(""));
    }

    // =========================================================================
    // move
    // =========================================================================

    @Test
    void move_fileIntoDirectory_movesSuccessfully() throws IOException {
        Files.writeString(tempDir.resolve("moveme.md"), "content");
        Files.createDirectory(tempDir.resolve("target-dir"));

        String newPath = fileService.move("moveme.md", "target-dir");

        assertFalse(Files.exists(tempDir.resolve("moveme.md")));
        assertTrue(Files.exists(tempDir.resolve("target-dir/moveme.md")));
        assertEquals("target-dir/moveme.md", newPath);
    }

    @Test
    void move_intoNonDirectory_throwsIllegalArgument() throws IOException {
        Files.writeString(tempDir.resolve("file1.md"), "content");
        Files.writeString(tempDir.resolve("file2.md"), "content");

        assertThrows(IllegalArgumentException.class, () ->
                fileService.move("file1.md", "file2.md"));
    }
}
