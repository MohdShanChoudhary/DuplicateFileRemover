package com.duplicatefinder.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Module 1: File Scanner
 * Recursively scans folders and collects all files.
 * Reports progress via a callback consumer.
 */
public class FileScanner {

    private final List<String> fileTypeFilters; // e.g. ["jpg", "png", "pdf"]
    private Consumer<String> progressCallback;

    public FileScanner() {
        this.fileTypeFilters = new ArrayList<>();
    }

    public FileScanner(List<String> fileTypeFilters) {
        this.fileTypeFilters = fileTypeFilters != null ? fileTypeFilters : new ArrayList<>();
    }

    public void setProgressCallback(Consumer<String> callback) {
        this.progressCallback = callback;
    }

    /**
     * Scans the given folder recursively and returns all matching files.
     *
     * @param folder Root folder to scan
     * @return List of all files found
     * @throws IOException if folder cannot be accessed
     */
    public List<File> scanFolder(File folder) throws IOException {
        if (!folder.exists() || !folder.isDirectory()) {
            throw new IllegalArgumentException("Invalid folder: " + folder.getAbsolutePath());
        }

        List<File> collectedFiles = new ArrayList<>();

        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                File f = file.toFile();

                // Skip hidden files and system files
                if (f.isHidden() || !f.canRead()) {
                    return FileVisitResult.CONTINUE;
                }

                // Apply file type filter if set
                if (!fileTypeFilters.isEmpty()) {
                    String ext = getExtension(f.getName()).toLowerCase();
                    if (!fileTypeFilters.contains(ext)) {
                        return FileVisitResult.CONTINUE;
                    }
                }

                collectedFiles.add(f);

                if (progressCallback != null) {
                    progressCallback.accept("Scanning: " + f.getName());
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Skip files we can't access (permission denied etc.)
                if (progressCallback != null) {
                    progressCallback.accept("Skipped (no access): " + file.getFileName());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (progressCallback != null) {
                    progressCallback.accept("Entering folder: " + dir.getFileName());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return collectedFiles;
    }

    /**
     * Scans multiple folders and merges results.
     */
    public List<File> scanFolders(List<File> folders) throws IOException {
        List<File> allFiles = new ArrayList<>();
        for (File folder : folders) {
            allFiles.addAll(scanFolder(folder));
        }
        return allFiles;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot == -1 || dot == filename.length() - 1) return "";
        return filename.substring(dot + 1);
    }
}
