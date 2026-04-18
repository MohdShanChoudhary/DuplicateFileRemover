package com.duplicatefinder.ui;

import com.duplicatefinder.model.DuplicateGroup;
import com.duplicatefinder.model.ScanResult;
import com.duplicatefinder.service.DuplicateFinder;
import com.duplicatefinder.service.FileManager;
import com.duplicatefinder.util.FileUtils;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Module 4: Main GUI Window
 * JavaFX-based user interface for the Duplicate File Remover.
 *
 * Layout:
 *   Top    — App title bar
 *   Left   — Folder selection panel + options
 *   Center — Results table with duplicate groups
 *   Bottom — Status bar + action buttons
 */
public class MainWindow {

    private final Stage stage;
    private final FileManager fileManager;

    // UI Components
    private ListView<String> folderListView;
    private List<File> selectedFolders;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Label statsLabel;
    private VBox resultsBox;
    private Button scanButton;
    private Button deleteSelectedButton;
    private Button deleteAllButton;
    private Button undoButton;

    private ScanResult currentResult;
    private List<DuplicateGroup> selectedGroups;

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.fileManager = new FileManager();
        this.selectedFolders = new ArrayList<>();
        this.selectedGroups = new ArrayList<>();
    }

    public void show() {
        stage.setTitle("Duplicate File Remover");
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        BorderPane root = new BorderPane();
        root.setTop(buildTitleBar());
        root.setLeft(buildLeftPanel());
        root.setCenter(buildCenterPanel());
        root.setBottom(buildBottomBar());

        Scene scene = new Scene(root, 1000, 700);
        scene.getStylesheets().add(getClass().getResource("/styles.css") != null
                ? getClass().getResource("/styles.css").toExternalForm()
                : "");
        stage.setScene(scene);
        stage.show();
    }

    // ---- UI Construction ----

    private HBox buildTitleBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(16, 20, 16, 20));
        bar.setStyle("-fx-background-color: #2C3E50;");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🔍 Duplicate File Remover");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("  Content-based • SHA-256 • Multi-threaded");
        subtitle.setFont(Font.font("System", 12));
        subtitle.setTextFill(Color.web("#95a5a6"));

        bar.getChildren().addAll(title, subtitle);
        return bar;
    }

    private VBox buildLeftPanel() {
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(260);
        panel.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 1 0 0;");

        // --- Folder Section ---
        Label folderHeader = new Label("📂 Scan Folders");
        folderHeader.setFont(Font.font("System", FontWeight.BOLD, 13));

        folderListView = new ListView<>();
        folderListView.setPrefHeight(150);
        folderListView.setPlaceholder(new Label("No folders selected"));

        Button addFolderBtn = new Button("+ Add Folder");
        addFolderBtn.setMaxWidth(Double.MAX_VALUE);
        addFolderBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 12px; -fx-padding: 8 12;");
        addFolderBtn.setOnAction(e -> addFolder());

        Button removeFolderBtn = new Button("✕ Remove Selected");
        removeFolderBtn.setMaxWidth(Double.MAX_VALUE);
        removeFolderBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 6 12;");
        removeFolderBtn.setOnAction(e -> removeSelectedFolder());

        // --- Options Section ---
        Separator sep1 = new Separator();
        Label optionsHeader = new Label("⚙️ Options");
        optionsHeader.setFont(Font.font("System", FontWeight.BOLD, 13));

        CheckBox includeSubfolders = new CheckBox("Include subfolders");
        includeSubfolders.setSelected(true);

        Label filterLabel = new Label("File type filter (comma-separated):");
        filterLabel.setFont(Font.font("System", 11));
        filterLabel.setTextFill(Color.web("#6c757d"));
        TextField filterField = new TextField();
        filterField.setPromptText("e.g. jpg,png,mp4 (blank=all)");
        filterField.setStyle("-fx-font-size: 11px;");

        // --- Scan Button ---
        Separator sep2 = new Separator();
        scanButton = new Button("🔍  Start Scan");
        scanButton.setMaxWidth(Double.MAX_VALUE);
        scanButton.setPrefHeight(42);
        scanButton.setFont(Font.font("System", FontWeight.BOLD, 13));
        scanButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 13px;");
        scanButton.setOnAction(e -> startScan(filterField.getText()));

        // --- Stats Summary ---
        Separator sep3 = new Separator();
        statsLabel = new Label("No scan performed yet.");
        statsLabel.setWrapText(true);
        statsLabel.setFont(Font.font("System", 11));
        statsLabel.setTextFill(Color.web("#6c757d"));

        panel.getChildren().addAll(
                folderHeader, folderListView, addFolderBtn, removeFolderBtn,
                sep1, optionsHeader, includeSubfolders,
                filterLabel, filterField,
                sep2, scanButton,
                sep3, statsLabel
        );

        return panel;
    }

    private VBox buildCenterPanel() {
        VBox center = new VBox(0);

        // Results header
        HBox resultsHeader = new HBox(12);
        resultsHeader.setPadding(new Insets(12, 16, 8, 16));
        resultsHeader.setAlignment(Pos.CENTER_LEFT);
        resultsHeader.setStyle("-fx-background-color: #ffffff; -fx-border-color: #dee2e6; -fx-border-width: 0 0 1 0;");

        Label resultsTitle = new Label("Duplicate Groups");
        resultsTitle.setFont(Font.font("System", FontWeight.BOLD, 14));

        Label hint = new Label("Click a group to expand and select which file to keep");
        hint.setFont(Font.font("System", 11));
        hint.setTextFill(Color.web("#6c757d"));

        resultsHeader.getChildren().addAll(resultsTitle, hint);

        // Progress bar
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(6);
        progressBar.setVisible(false);
        progressBar.setStyle("-fx-accent: #27ae60;");

        // Scrollable results area
        resultsBox = new VBox(8);
        resultsBox.setPadding(new Insets(16));

        Label placeholder = new Label("Select a folder and click 'Start Scan' to find duplicate files.");
        placeholder.setTextFill(Color.web("#adb5bd"));
        placeholder.setFont(Font.font("System", 13));
        resultsBox.getChildren().add(placeholder);

        ScrollPane scroll = new ScrollPane(resultsBox);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #ffffff;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        center.getChildren().addAll(resultsHeader, progressBar, scroll);
        return center;
    }

    private HBox buildBottomBar() {
        HBox bar = new HBox(10);
        bar.setPadding(new Insets(12, 16, 12, 16));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 1 0 0 0;");

        statusLabel = new Label("Ready.");
        statusLabel.setFont(Font.font("System", 11));
        statusLabel.setTextFill(Color.web("#6c757d"));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        deleteSelectedButton = new Button("🗑 Delete Selected Groups");
        deleteSelectedButton.setDisable(true);
        deleteSelectedButton.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-size: 12px;");
        deleteSelectedButton.setOnAction(e -> deleteSelectedGroups());

        deleteAllButton = new Button("🗑 Delete ALL Duplicates");
        deleteAllButton.setDisable(true);
        deleteAllButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 12px;");
        deleteAllButton.setOnAction(e -> deleteAllDuplicates());

        undoButton = new Button("↩ Undo Last");
        undoButton.setDisable(true);
        undoButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-size: 12px;");
        undoButton.setOnAction(e -> undoLastDeletion());

        bar.getChildren().addAll(statusLabel, deleteSelectedButton, deleteAllButton, undoButton);
        return bar;
    }

    // ---- Actions ----

    private void addFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Folder to Scan");
        File folder = chooser.showDialog(stage);
        if (folder != null && !selectedFolders.contains(folder)) {
            selectedFolders.add(folder);
            folderListView.getItems().add(folder.getAbsolutePath());
        }
    }

    private void removeSelectedFolder() {
        int idx = folderListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            selectedFolders.remove(idx);
            folderListView.getItems().remove(idx);
        }
    }

    private void startScan(String filterText) {
        if (selectedFolders.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Folders Selected",
                    "Please add at least one folder before scanning.");
            return;
        }

        // Parse file type filters
        List<String> filters = new ArrayList<>();
        if (!filterText.trim().isEmpty()) {
            for (String ext : filterText.split(",")) {
                String trimmed = ext.trim().toLowerCase().replace(".", "");
                if (!trimmed.isEmpty()) filters.add(trimmed);
            }
        }

        // Reset UI
        scanButton.setDisable(true);
        deleteAllButton.setDisable(true);
        deleteSelectedButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        resultsBox.getChildren().clear();
        selectedGroups.clear();

        DuplicateFinder finder = new DuplicateFinder(filters);

        finder.setStatusCallback(msg -> Platform.runLater(() -> setStatus(msg)));

        finder.setProgressCallback((done, total) -> Platform.runLater(() -> {
            progressBar.setProgress((double) done / total);
            setStatus("Hashing: " + done + " / " + total + " files...");
        }));

        // Run scan on background thread
        Thread scanThread = new Thread(() -> {
            try {
                ScanResult result = finder.findDuplicates(selectedFolders);
                Platform.runLater(() -> displayResults(result));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showAlert(Alert.AlertType.ERROR, "Scan Error", ex.getMessage());
                    scanButton.setDisable(false);
                    progressBar.setVisible(false);
                    setStatus("Scan failed: " + ex.getMessage());
                });
            }
        });
        scanThread.setDaemon(true);
        scanThread.start();
    }

    private void displayResults(ScanResult result) {
        currentResult = result;
        progressBar.setVisible(false);
        scanButton.setDisable(false);
        resultsBox.getChildren().clear();

        // Stats
        long durationSec = result.getScanDurationMs() / 1000;
        String statsText = String.format(
                "✅ Scan complete in %ds\n📁 %d files scanned\n🔁 %d duplicate groups\n💾 %s recoverable",
                durationSec, result.getTotalFilesScanned(),
                result.getTotalDuplicateGroups(),
                ScanResult.formatSize(result.getTotalWastedSpace())
        );
        statsLabel.setText(statsText);

        if (result.getDuplicateGroups().isEmpty()) {
            Label noResults = new Label("✅ No duplicate files found! Your folder is clean.");
            noResults.setFont(Font.font("System", FontWeight.BOLD, 14));
            noResults.setTextFill(Color.web("#27ae60"));
            resultsBox.getChildren().add(noResults);
            setStatus("No duplicates found.");
            return;
        }

        deleteAllButton.setDisable(false);

        // Build a card per duplicate group
        for (int i = 0; i < result.getDuplicateGroups().size(); i++) {
            DuplicateGroup group = result.getDuplicateGroups().get(i);
            resultsBox.getChildren().add(buildGroupCard(group, i + 1));
        }

        setStatus(String.format("Found %d duplicate groups — %s recoverable space.",
                result.getTotalDuplicateGroups(),
                ScanResult.formatSize(result.getTotalWastedSpace())));
    }

    private VBox buildGroupCard(DuplicateGroup group, int index) {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; " +
                "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;");

        // Card header (clickable to toggle collapse)
        HBox header = new HBox(10);
        header.setPadding(new Insets(10, 14, 10, 14));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #f1f3f5; -fx-background-radius: 6 6 0 0; " +
                "-fx-cursor: hand;");

        CheckBox groupCheck = new CheckBox();
        groupCheck.selectedProperty().addListener((obs, old, val) -> {
            if (val) selectedGroups.add(group);
            else selectedGroups.remove(group);
            deleteSelectedButton.setDisable(selectedGroups.isEmpty());
        });

        File firstFile = group.getFiles().get(0);
        Label icon = new Label(FileUtils.getFileTypeIcon(firstFile));
        icon.setFont(Font.font(16));

        Label groupTitle = new Label(String.format("Group %d — %d copies — Waste: %s",
                index, group.getFiles().size(),
                ScanResult.formatSize(group.getWastedSpace())));
        groupTitle.setFont(Font.font("System", FontWeight.BOLD, 12));
        HBox.setHgrow(groupTitle, Priority.ALWAYS);

        Label hashLabel = new Label("SHA-256: " + group.getHash().substring(0, 16) + "...");
        hashLabel.setFont(Font.font("System", 10));
        hashLabel.setTextFill(Color.web("#adb5bd"));

        header.getChildren().addAll(groupCheck, icon, groupTitle, hashLabel);

        // Files list (inside the card)
        VBox filesList = new VBox(4);
        filesList.setPadding(new Insets(8, 14, 10, 14));

        ToggleGroup keepToggle = new ToggleGroup();

        for (File file : group.getFiles()) {
            HBox fileRow = new HBox(10);
            fileRow.setAlignment(Pos.CENTER_LEFT);
            fileRow.setPadding(new Insets(4, 8, 4, 8));
            fileRow.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 4;");

            RadioButton keepRadio = new RadioButton();
            keepRadio.setToggleGroup(keepToggle);
            keepRadio.setSelected(file.equals(group.getFileToKeep()));
            keepRadio.setOnAction(e -> group.setFileToKeep(file));

            Label fileName = new Label(file.getName());
            fileName.setFont(Font.font("System", FontWeight.BOLD, 12));

            Label filePath = new Label(FileUtils.getShortPath(file));
            filePath.setFont(Font.font("System", 10));
            filePath.setTextFill(Color.web("#6c757d"));
            HBox.setHgrow(filePath, Priority.ALWAYS);

            Label fileSize = new Label(FileUtils.formatSize(file.length()));
            fileSize.setFont(Font.font("System", 11));
            fileSize.setTextFill(Color.web("#495057"));

            fileRow.getChildren().addAll(keepRadio, fileName, filePath, fileSize);
            filesList.getChildren().add(fileRow);
        }

        Label keepHint = new Label("⬆ Select which file to KEEP (others will be deleted)");
        keepHint.setFont(Font.font("System", 10));
        keepHint.setTextFill(Color.web("#868e96"));
        filesList.getChildren().add(keepHint);

        card.getChildren().addAll(header, filesList);
        return card;
    }

    private void deleteSelectedGroups() {
        if (selectedGroups.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete duplicates from " + selectedGroups.size() + " groups?");
        confirm.setContentText("Files will be moved to trash. You can undo this action.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        performDeletion(new ArrayList<>(selectedGroups));
    }

    private void deleteAllDuplicates() {
        if (currentResult == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete All Duplicates");
        confirm.setHeaderText("Delete ALL duplicate files from " +
                currentResult.getTotalDuplicateGroups() + " groups?");
        confirm.setContentText(String.format(
                "This will free up %s of space.\nFiles will be moved to trash — you can undo.",
                ScanResult.formatSize(currentResult.getTotalWastedSpace())));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        performDeletion(currentResult.getDuplicateGroups());
    }

    private void performDeletion(List<DuplicateGroup> groups) {
        Thread deleteThread = new Thread(() -> {
            try {
                fileManager.softDeleteAll(groups);
                Platform.runLater(() -> {
                    undoButton.setDisable(false);
                    setStatus(String.format("Moved %d duplicate files to trash. Click Undo to restore.",
                            fileManager.getTrashCount()));
                    // Re-scan to refresh view
                    startScan("");
                });
            } catch (Exception e) {
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,
                        "Deletion Error", e.getMessage()));
            }
        });
        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    private void undoLastDeletion() {
        int restored = fileManager.undoAll();
        setStatus("Restored " + restored + " files from trash.");
        if (fileManager.getTrashCount() == 0) {
            undoButton.setDisable(true);
        }
        // Re-scan to update results
        if (!selectedFolders.isEmpty()) {
            startScan("");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
