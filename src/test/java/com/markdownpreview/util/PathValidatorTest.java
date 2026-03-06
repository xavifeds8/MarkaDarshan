package com.markdownpreview.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathValidatorTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveAndValidate_normalPath_succeeds() {
        Path result = PathValidator.resolveAndValidate(tempDir, "subdir/file.md");
        assertTrue(result.startsWith(tempDir));
        assertEquals(tempDir.resolve("subdir/file.md").normalize(), result);
    }

    @Test
    void resolveAndValidate_emptyPath_returnsRoot() {
        Path result = PathValidator.resolveAndValidate(tempDir, "");
        assertEquals(tempDir, result);
    }

    @Test
    void resolveAndValidate_nullPath_returnsRoot() {
        Path result = PathValidator.resolveAndValidate(tempDir, null);
        assertEquals(tempDir, result);
    }

    @Test
    void resolveAndValidate_traversalAttack_throwsSecurityException() {
        assertThrows(SecurityException.class, () ->
                PathValidator.resolveAndValidate(tempDir, "../../etc/passwd"));
    }

    @Test
    void resolveAndValidate_dotDotInMiddle_throwsIfEscapes() {
        assertThrows(SecurityException.class, () ->
                PathValidator.resolveAndValidate(tempDir, "subdir/../../.."));
    }

    @Test
    void resolveAndValidate_dotDotWithinRoot_succeeds() {
        // subdir/../file.md normalizes to file.md which is still within root
        Path result = PathValidator.resolveAndValidate(tempDir, "subdir/../file.md");
        assertTrue(result.startsWith(tempDir));
    }

    @Test
    void assertWithinRoot_validPath_succeeds() {
        Path target = tempDir.resolve("some/file.md");
        assertDoesNotThrow(() -> PathValidator.assertWithinRoot(tempDir, target));
    }

    @Test
    void assertWithinRoot_escapedPath_throwsSecurityException() {
        Path target = tempDir.resolve("../../etc/passwd");
        assertThrows(SecurityException.class, () ->
                PathValidator.assertWithinRoot(tempDir, target));
    }
}
