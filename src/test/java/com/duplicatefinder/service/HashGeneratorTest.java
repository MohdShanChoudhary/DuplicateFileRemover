package com.duplicatefinder.service;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for HashGenerator and DuplicateFinder services.
 */
public class HashGeneratorTest {

    private HashGenerator hashGenerator;
    private Path tempDir;

    @BeforeEach
    public void setUp() throws IOException {
        hashGenerator = new HashGenerator();
        tempDir = Files.createTempDirectory("duptest_");
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up temp files
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testSameContentProducesSameHash() throws Exception {
        File a = createTempFile("fileA.txt", "Hello, duplicate world!");
        File b = createTempFile("fileB.txt", "Hello, duplicate world!");

        String hashA = hashGenerator.computeHash(a);
        String hashB = hashGenerator.computeHash(b);

        assertEquals(hashA, hashB, "Files with same content should produce same hash");
    }

    @Test
    public void testDifferentContentProducesDifferentHash() throws Exception {
        File a = createTempFile("fileA.txt", "Content A");
        File b = createTempFile("fileB.txt", "Content B");

        String hashA = hashGenerator.computeHash(a);
        String hashB = hashGenerator.computeHash(b);

        assertNotEquals(hashA, hashB, "Files with different content should produce different hash");
    }

    @Test
    public void testSizeFilterSkipsUniqueSizeFiles() throws Exception {
        File unique = createTempFile("unique.txt", "This is unique content - different length!");
        File dup1   = createTempFile("dup1.txt",   "Same");
        File dup2   = createTempFile("dup2.txt",   "Same");

        List<File> allFiles = Arrays.asList(unique, dup1, dup2);
        Map<Long, List<File>> sizeGroups = hashGenerator.groupBySize(allFiles);

        assertFalse(sizeGroups.containsKey(unique.length()),
                "Unique-size files should be excluded from size groups");
        assertTrue(sizeGroups.containsKey(dup1.length()),
                "Files with matching sizes should appear in size groups");
    }

    @Test
    public void testHashCandidatesGroupsDuplicates() throws Exception {
        File dup1 = createTempFile("dup1.txt", "Identical content");
        File dup2 = createTempFile("dup2.txt", "Identical content");
        File diff = createTempFile("diff.txt", "Identical content!"); // extra char = different

        List<File> candidates = Arrays.asList(dup1, dup2);
        Map<Long, List<File>> sizeGroups = new HashMap<>();
        sizeGroups.put(dup1.length(), candidates);

        Map<String, List<File>> hashGroups = hashGenerator.hashCandidates(sizeGroups);

        assertEquals(1, hashGroups.size(), "Should find exactly 1 duplicate hash group");
        assertEquals(2, hashGroups.values().iterator().next().size(),
                "Duplicate group should contain 2 files");
    }

    @Test
    public void testEmptyFolderReturnsNoGroups() throws Exception {
        DuplicateFinder finder = new DuplicateFinder();
        var result = finder.findDuplicates(tempDir.toFile());

        assertEquals(0, result.getTotalDuplicateGroups(), "Empty folder should have no duplicates");
    }

    @Test
    public void testFullScanDetectsDuplicates() throws Exception {
        createTempFile("a.txt", "Duplicate content here");
        createTempFile("b.txt", "Duplicate content here");
        createTempFile("c.txt", "Unique content 1");
        createTempFile("d.txt", "Unique content 2");

        DuplicateFinder finder = new DuplicateFinder();
        var result = finder.findDuplicates(tempDir.toFile());

        assertEquals(1, result.getTotalDuplicateGroups(), "Should find 1 duplicate group");
        assertEquals(4, result.getTotalFilesScanned(), "Should scan all 4 files");
        assertEquals(1, result.getTotalDuplicateFiles(), "Should report 1 file as duplicate");
    }

    // ---- Helpers ----

    private File createTempFile(String name, String content) throws IOException {
        File file = tempDir.resolve(name).toFile();
        Files.write(file.toPath(), content.getBytes());
        return file;
    }
}
