package com.duplicatefinder.util;

import java.io.File;

/**
 * Utility methods for file operations used across the application.
 */
public class FileUtils {

    private FileUtils() {}

    /**
     * Formats a byte count to a human-readable string.
     */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Returns the file extension (lowercase, without dot).
     */
    public static String getExtension(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot == -1 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase();
    }

    /**
     * Returns an icon emoji for a file based on its type.
     */
    public static String getFileTypeIcon(File file) {
        switch (getExtension(file)) {
            case "jpg": case "jpeg": case "png": case "gif":
            case "bmp": case "webp": case "svg": return "🖼";
            case "mp4": case "avi": case "mov": case "mkv":
            case "wmv": case "flv": return "🎬";
            case "mp3": case "wav": case "flac": case "aac":
            case "ogg": case "m4a": return "🎵";
            case "pdf": return "📄";
            case "doc": case "docx": return "📝";
            case "xls": case "xlsx": return "📊";
            case "ppt": case "pptx": return "📋";
            case "zip": case "rar": case "7z": case "tar":
            case "gz": return "📦";
            case "java": case "py": case "js": case "ts":
            case "cpp": case "c": case "h": return "💻";
            default: return "📁";
        }
    }

    /**
     * Truncates a file path for display if it's too long.
     */
    public static String truncatePath(String path, int maxLength) {
        if (path.length() <= maxLength) return path;
        int start = path.length() - maxLength + 3;
        return "..." + path.substring(start);
    }

    /**
     * Returns the shortened parent path of a file for display.
     */
    public static String getShortPath(File file) {
        String path = file.getAbsolutePath();
        return truncatePath(path, 60);
    }
}
