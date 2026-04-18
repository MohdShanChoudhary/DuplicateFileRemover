package com.duplicatefinder.model;

import java.util.List;

/**
 * Holds the complete result of a duplicate scan operation.
 */
public class ScanResult {

    private final List<DuplicateGroup> duplicateGroups;
    private final int totalFilesScanned;
    private final long scanDurationMs;

    public ScanResult(List<DuplicateGroup> duplicateGroups, int totalFilesScanned, long scanDurationMs) {
        this.duplicateGroups = duplicateGroups;
        this.totalFilesScanned = totalFilesScanned;
        this.scanDurationMs = scanDurationMs;
    }

    public List<DuplicateGroup> getDuplicateGroups() {
        return duplicateGroups;
    }

    public int getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public long getScanDurationMs() {
        return scanDurationMs;
    }

    public int getTotalDuplicateGroups() {
        return duplicateGroups.size();
    }

    public int getTotalDuplicateFiles() {
        return duplicateGroups.stream()
                .mapToInt(DuplicateGroup::getDuplicateCount)
                .sum();
    }

    /**
     * Total space wasted across all duplicate groups (in bytes).
     */
    public long getTotalWastedSpace() {
        return duplicateGroups.stream()
                .mapToLong(DuplicateGroup::getWastedSpace)
                .sum();
    }

    /**
     * Converts bytes to a human-readable string (KB, MB, GB).
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
