package com.duplicatefinder.service;

import com.duplicatefinder.model.DuplicateGroup;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Module 5: File Manager
 * Handles safe file deletion with undo support.
 *
 * Strategy: "Soft Delete"
 *   Files are NOT immediately deleted. Instead, they are moved to a
 *   temporary trash folder. Users can undo within the same session.
 *   A cleanup can permanently delete the trash folder contents.
 */
public class FileManager {

    private static final String TRASH_FOLDER_NAME = ".DuplicateFinderTrash";
    private final File trashFolder;
    private final Map<File, File> undoMap; // original -> trash copy

    public FileManager() {
        // Place trash folder in user home directory
        String userHome = System.getProperty("user.home");
        this.trashFolder = new File(userHome, TRASH_FOLDER_NAME);
        this.undoMap = new LinkedHashMap<>();
    }

    /**
     * Soft-deletes duplicate files from the given group.
     * Files are moved to the trash folder, not permanently deleted.
     *
     * @param group DuplicateGroup to process (keeps fileToKeep, soft-deletes the rest)
     * @return List of files successfully moved to trash
     */
    public List<File> softDelete(DuplicateGroup group) throws IOException {
        ensureTrashFolderExists();
        List<File> deleted = new ArrayList<>();

        for (File file : group.getFilesToDelete()) {
            try {
                File trashDest = moveToTrash(file);
                undoMap.put(file, trashDest);
                deleted.add(file);
            } catch (IOException e) {
                System.err.println("Could not move to trash: " + file.getAbsolutePath() + " — " + e.getMessage());
            }
        }

        return deleted;
    }

    /**
     * Soft-deletes duplicates from multiple groups.
     */
    public Map<DuplicateGroup, List<File>> softDeleteAll(List<DuplicateGroup> groups) throws IOException {
        Map<DuplicateGroup, List<File>> result = new LinkedHashMap<>();
        for (DuplicateGroup group : groups) {
            result.put(group, softDelete(group));
        }
        return result;
    }

    /**
     * Restores a previously soft-deleted file to its original location.
     *
     * @param originalFile The file's original path
     * @return true if undo succeeded
     */
    public boolean undo(File originalFile) {
        File trashCopy = undoMap.get(originalFile);
        if (trashCopy == null || !trashCopy.exists()) return false;

        try {
            // Recreate parent directories if needed
            File parent = originalFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Files.move(trashCopy.toPath(), originalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            undoMap.remove(originalFile);
            return true;
        } catch (IOException e) {
            System.err.println("Undo failed for: " + originalFile.getAbsolutePath());
            return false;
        }
    }

    /**
     * Undoes all deletions performed in this session.
     *
     * @return Number of files successfully restored
     */
    public int undoAll() {
        List<File> toRestore = new ArrayList<>(undoMap.keySet());
        int count = 0;
        for (File f : toRestore) {
            if (undo(f)) count++;
        }
        return count;
    }

    /**
     * Permanently deletes all files currently in the trash.
     * This action CANNOT be undone.
     */
    public void emptyTrash() {
        if (!trashFolder.exists()) return;
        deleteRecursively(trashFolder);
        undoMap.clear();
        trashFolder.mkdirs();
    }

    /**
     * Returns how many files are currently in the trash (undoable).
     */
    public int getTrashCount() {
        return undoMap.size();
    }

    /**
     * Returns the total size of all files in the trash.
     */
    public long getTrashSize() {
        return undoMap.values().stream()
                .filter(File::exists)
                .mapToLong(File::length)
                .sum();
    }

    public File getTrashFolder() {
        return trashFolder;
    }

    // ---- Private helpers ----

    private File moveToTrash(File file) throws IOException {
        // Build a trash path that preserves original filename + timestamp to avoid collisions
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String trashName = timestamp + "_" + file.getName();
        File destination = new File(trashFolder, trashName);

        Files.move(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return destination;
    }

    private void ensureTrashFolderExists() throws IOException {
        if (!trashFolder.exists()) {
            if (!trashFolder.mkdirs()) {
                throw new IOException("Could not create trash folder: " + trashFolder.getAbsolutePath());
            }
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }
}
