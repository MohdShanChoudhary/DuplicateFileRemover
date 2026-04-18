# 🔍 Duplicate File Remover

A **content-based** duplicate file detection desktop application built in **Java + JavaFX**.

## Features

| Feature | Details |
|---|---|
| Content-based detection | Uses SHA-256 hashing — file names don't matter |
| Size-based pre-filter | Skips hashing unique-size files (major speed boost) |
| Multi-threaded hashing | Uses all available CPU cores via `ExecutorService` |
| Soft delete / Undo | Files moved to trash — not permanently deleted |
| Multiple folder scan | Scan multiple folders simultaneously |
| File type filter | Optionally scan only specific extensions (jpg, mp4, etc.) |
| Recoverable space display | Shows exactly how much space you'll save |

---

## Project Structure

```
DuplicateFileRemover/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/duplicatefinder/
    │   │   ├── MainApp.java               ← Entry point
    │   │   ├── model/
    │   │   │   ├── DuplicateGroup.java    ← Data model for one group
    │   │   │   └── ScanResult.java        ← Full scan result
    │   │   ├── service/
    │   │   │   ├── FileScanner.java       ← Module 1: Recursive scanner
    │   │   │   ├── HashGenerator.java     ← Module 2: SHA-256 + size filter
    │   │   │   ├── DuplicateFinder.java   ← Module 3: Orchestrator
    │   │   │   └── FileManager.java       ← Module 5: Safe delete + undo
    │   │   ├── ui/
    │   │   │   └── MainWindow.java        ← Module 4: JavaFX GUI
    │   │   └── util/
    │   │       └── FileUtils.java         ← Utility helpers
    │   └── resources/
    │       └── styles.css                 ← JavaFX stylesheet
    └── test/
        └── java/com/duplicatefinder/
            └── service/
                └── HashGeneratorTest.java ← JUnit 5 tests
```

---

## Requirements

- **Java 11+** (Java 17 LTS recommended)
- **Maven 3.6+**
- JavaFX is downloaded automatically by Maven

---

## How to Run

### Option 1: Maven (recommended)
```bash
cd DuplicateFileRemover

# Run directly
mvn javafx:run

# OR build fat JAR and run
mvn package
java -jar target/DuplicateFileRemover-1.0.0.jar
```

### Option 2: IntelliJ IDEA
1. Open the project (File → Open → select `DuplicateFileRemover` folder)
2. Maven will auto-import dependencies
3. Right-click `MainApp.java` → Run

### Option 3: Eclipse
1. File → Import → Existing Maven Projects
2. Select the `DuplicateFileRemover` folder
3. Run `MainApp.java` as Java Application

---

## How to Use

1. **Add Folders** — Click "+ Add Folder" and select folders to scan
2. **Set Filters** (optional) — Enter file extensions like `jpg,png,mp4`
3. **Start Scan** — Click the green "Start Scan" button
4. **Review Results** — Each duplicate group shows all copies
5. **Select Keep File** — Use the radio button to choose which file to keep
6. **Delete** — Click "Delete ALL" or select specific groups and "Delete Selected"
7. **Undo** — Click "↩ Undo Last" to restore files from trash

---

## How It Works (Algorithm)

```
All Files
    │
    ▼
Step 1: Group by File Size        ← O(n) — free, no I/O
    │
    ├── Unique sizes → SKIP (cannot be duplicates)
    │
    ▼
Step 2: SHA-256 Hash Candidates   ← Multi-threaded, only size-collision files
    │
    ▼
Step 3: Group by Hash             ← HashMap<String, List<File>>
    │
    ▼
DuplicateGroup[] → Display Results
```

**Why size filter first?**
In a typical folder, ~70–80% of files have unique sizes. Skipping them means
we only hash ~20–30% of files — making the scan dramatically faster.

---

## Data Structures

```java
// Step 1 - Size grouping
HashMap<Long, List<File>> sizeMap

// Step 2 - Hash grouping (only size-collision files)
ConcurrentHashMap<String, List<File>> hashMap

// Result model
class DuplicateGroup {
    String hash;
    List<File> files;
    File fileToKeep;    // user-selected
}
```

---

## Running Tests

```bash
mvn test
```

Tests cover:
- Same content → same SHA-256 hash
- Different content → different hash
- Size filter correctly skips unique-size files
- Full scan correctly finds duplicates
- Empty folder returns no results

---

## Future Enhancements (Roadmap)

- [ ] Image thumbnail preview inside duplicate cards
- [ ] Dark mode UI
- [ ] Export duplicate report to CSV/PDF
- [ ] Scheduled auto-scan
- [ ] System tray integration
- [ ] Permanent trash cleanup scheduler

---

## Author

Built as a Java + JavaFX desktop application project.
Technologies: Java 11+, JavaFX 17, SHA-256 (MessageDigest), ExecutorService, Maven
