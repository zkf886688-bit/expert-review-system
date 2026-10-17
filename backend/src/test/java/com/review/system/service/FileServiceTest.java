package com.review.system.service;

import com.review.system.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileServiceTest {

    @TempDir
    Path tempDir;

    private FileService fileService;

    @BeforeEach
    void setUp() {
        fileService = new FileService();
        org.springframework.test.util.ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    @Test
    void resolveAllowsFileInsideUploadRoot() throws Exception {
        Path nested = tempDir.resolve("materials");
        Files.createDirectories(nested);
        Path file = nested.resolve("demo.pdf");
        Files.writeString(file, "demo");

        Path resolved = fileService.resolve("materials/demo.pdf");
        assertEquals(file.toAbsolutePath().normalize(), resolved);
    }

    @Test
    void resolveRejectsPathTraversal() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> fileService.resolve("../outside.txt"));
        assertEquals("非法路径", ex.getMessage());
    }

    @Test
    void resolveRejectsBlankPath() {
        assertThrows(BusinessException.class, () -> fileService.resolve("  "));
    }

    @Test
    void saveRejectsUnsafeSubDir() {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "note.txt", "text/plain", "hello".getBytes());
        assertThrows(BusinessException.class,
                () -> fileService.save(file, "../escape"));
    }

    @Test
    void saveCreatesNestedDirectory() throws Exception {
        org.springframework.mock.web.MockMultipartFile file =
                new org.springframework.mock.web.MockMultipartFile(
                        "file", "note.txt", "text/plain", "hello".getBytes());

        String relative = fileService.save(file, "experts/1");
        Path saved = tempDir.resolve(relative);
        assertTrue(Files.exists(saved));
        assertEquals("hello", Files.readString(saved));
    }
}
