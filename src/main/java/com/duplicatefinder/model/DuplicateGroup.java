package com.duplicatefinder.model;

import java.io.File;
import java.util.List;

/**
 * Represents a group of files that have identical content (same SHA-256 hash).
 */
public class DuplicateGroup {

    private final String hash;
    private final List<File> files;
    private File fileToKeep;

    public DuplicateGroup(String hash, List<File> files) {
        this.hash = hash;
        this.files = files;
        // By default, keep the first file (original)
        this.fileToKeep = files.isEmpty() ? null : files.get(0);
    }

    public String getHash() {
        return hash;
    }

    public List<File> getFiles() {
        return files;
    }

    public File getFileToKeep() {
        return fileToKeep;
    }

    public void setFileToKeep(File fileToKeep) {
        this.fileToKeep = fileToKeep;
    }

    /**
     * Returns files that are duplicates (all except the one to keep).
     */
    public List<File> getFilesToDelete() {
        return files.stream()
                .filter(f -> !f.equals(fileToKeep))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Returns total wasted space by duplicate files (in bytes).
     */
    public long getWastedSpace() {
        return getFilesToDelete().stream()
                .mapToLong(File::length)
                .sum();
    }

    /**
     * Total number of duplicate copies (excluding the one to keep).
     */
    public int getDuplicateCount() {
        return files.size() - 1;
    }

    @Override
    public String toString() {
        return "DuplicateGroup{hash='" + hash + "', files=" + files.size() + "}";
    }
}
