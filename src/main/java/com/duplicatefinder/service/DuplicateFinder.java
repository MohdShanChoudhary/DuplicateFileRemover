package com.duplicatefinder.service;

import com.duplicatefinder.model.DuplicateGroup;
import com.duplicatefinder.model.ScanResult;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Module 3: Duplicate Finder
 * Orchestrates the full scan pipeline:
 *   File Scanner -> Size Filter -> Hash Generator -> Group Builder -> ScanResult
 */
public class DuplicateFinder {

    private final FileScanner fileScanner;
    private final HashGenerator hashGenerator;

    private Consumer<String> statusCallback;       // status text updates
    private BiConsumer<Integer, Integer> progressCallback; // (done, total) for progress bar

    public DuplicateFinder() {
        this.fileScanner = new FileScanner();
        this.hashGenerator = new HashGenerator();
    }

    public DuplicateFinder(List<String> fileTypeFilters) {
        this.fileScanner = new FileScanner(fileTypeFilters);
        this.hashGenerator = new HashGenerator();
    }

    public void setStatusCallback(Consumer<String> callback) {
        this.statusCallback = callback;
        this.fileScanner.setProgressCallback(callback);
    }

    public void setProgressCallback(BiConsumer<Integer, Integer> callback) {
        this.progressCallback = callback;
        this.hashGenerator.setProgressCallback(callback);
    }

    /**
     * Runs the full duplicate detection pipeline on a single folder.
     *
     * @param folder Root folder to scan
     * @return ScanResult with all duplicate groups found
     * @throws IOException if folder cannot be accessed
     */
    public ScanResult findDuplicates(File folder) throws IOException {
        return findDuplicates(Collections.singletonList(folder));
    }

    /**
     * Runs the full duplicate detection pipeline on multiple folders.
     *
     * @param folders List of root folders to scan
     * @return ScanResult with all duplicate groups found
     * @throws IOException if any folder cannot be accessed
     */
    public ScanResult findDuplicates(List<File> folders) throws IOException {
        long startTime = System.currentTimeMillis();

        // Step 1: Scan all files
        updateStatus("Step 1/3: Scanning files...");
        List<File> allFiles = fileScanner.scanFolders(folders);
        updateStatus("Found " + allFiles.size() + " files. Filtering by size...");

        // Step 2: Group by size (performance optimization — skip unique sizes)
        updateStatus("Step 2/3: Size-based pre-filter...");
        Map<Long, List<File>> sizeGroups = hashGenerator.groupBySize(allFiles);
        int candidates = sizeGroups.values().stream().mapToInt(List::size).sum();
        updateStatus("Hashing " + candidates + " candidate files (skipped " +
                (allFiles.size() - candidates) + " unique-size files)...");

        // Step 3: Hash candidates and group by hash
        updateStatus("Step 3/3: Generating SHA-256 hashes...");
        Map<String, List<File>> hashGroups = hashGenerator.hashCandidates(sizeGroups);

        // Build DuplicateGroup objects
        List<DuplicateGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<File>> entry : hashGroups.entrySet()) {
            // Sort files so oldest/shallowest path is default "keep" candidate
            List<File> files = entry.getValue();
            files.sort(Comparator.comparing(File::getAbsolutePath));
            groups.add(new DuplicateGroup(entry.getKey(), files));
        }

        // Sort groups by wasted space descending (biggest savings first)
        groups.sort(Comparator.comparingLong(DuplicateGroup::getWastedSpace).reversed());

        long duration = System.currentTimeMillis() - startTime;
        updateStatus("Scan complete! Found " + groups.size() + " duplicate groups.");

        return new ScanResult(groups, allFiles.size(), duration);
    }

    private void updateStatus(String message) {
        if (statusCallback != null) {
            statusCallback.accept(message);
        }
    }
}
