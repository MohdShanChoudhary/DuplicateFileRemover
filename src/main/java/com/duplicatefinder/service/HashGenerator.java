package com.duplicatefinder.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/**
 * Module 2: Hash Generator
 * Generates SHA-256 hashes for files using multi-threading.
 *
 * Performance optimization:
 *   Step 1 - Group files by size (free operation)
 *   Step 2 - Only hash files that share a size with at least one other file
 *   This avoids hashing unique-sized files entirely.
 */
public class HashGenerator {

    private static final int BUFFER_SIZE = 8 * 1024; // 8 KB read buffer
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    private BiConsumer<Integer, Integer> progressCallback; // (done, total)

    public void setProgressCallback(BiConsumer<Integer, Integer> callback) {
        this.progressCallback = callback;
    }

    /**
     * Step 1: Groups files by file size.
     * Files with a unique size cannot have duplicates — skip them.
     *
     * @param files All scanned files
     * @return Map of size -> list of files with that size (only groups with 2+ files)
     */
    public Map<Long, List<File>> groupBySize(List<File> files) {
        Map<Long, List<File>> sizeMap = new HashMap<>();

        for (File f : files) {
            long size = f.length();
            sizeMap.computeIfAbsent(size, k -> new ArrayList<>()).add(f);
        }

        // Remove size groups with only one file — they can't be duplicates
        sizeMap.entrySet().removeIf(e -> e.getValue().size() < 2);
        return sizeMap;
    }

    /**
     * Step 2: Hash only the size-collision candidates using a thread pool.
     *
     * @param sizeGroups Output from groupBySize()
     * @return Map of SHA-256 hash -> list of files with that hash
     */
    public Map<String, List<File>> hashCandidates(Map<Long, List<File>> sizeGroups) {
        List<File> candidates = new ArrayList<>();
        for (List<File> group : sizeGroups.values()) {
            candidates.addAll(group);
        }

        int total = candidates.size();
        Map<String, List<File>> hashMap = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<?>> futures = new ArrayList<>();
        int[] doneCount = {0};

        for (File file : candidates) {
            futures.add(executor.submit(() -> {
                try {
                    String hash = computeHash(file);
                    hashMap.computeIfAbsent(hash, k -> Collections.synchronizedList(new ArrayList<>())).add(file);
                } catch (IOException | NoSuchAlgorithmException e) {
                    // Skip files that fail to hash
                }
                synchronized (doneCount) {
                    doneCount[0]++;
                    if (progressCallback != null) {
                        progressCallback.accept(doneCount[0], total);
                    }
                }
            }));
        }

        // Wait for all hashing tasks to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
            }
        }

        executor.shutdown();

        // Remove hash groups with only one file — not duplicates
        hashMap.entrySet().removeIf(e -> e.getValue().size() < 2);
        return hashMap;
    }

    /**
     * Computes SHA-256 hash of a file.
     */
    public String computeHash(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[BUFFER_SIZE];

        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
