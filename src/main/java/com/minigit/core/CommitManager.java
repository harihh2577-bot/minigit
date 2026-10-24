package com.minigit.core;

import com.minigit.models.Commit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CommitManager {

    private final Path commitsDir;
    private final Path headFile;
    private final BlobStorage blobStorage;

    public CommitManager(Path commitsDir, Path headFile, BlobStorage blobStorage) {
        this.commitsDir = commitsDir;
        this.headFile = headFile;
        this.blobStorage = blobStorage;
    }

    public String headId() throws IOException {
        if (!Files.exists(headFile)) {
            return "";
        }
        return Files.readString(headFile).trim();
    }

    public Commit readCommit(String id) throws IOException {
        Path file = commitsDir.resolve(id);
        if (!Files.exists(file)) {
            return null;
        }

        String parent = "";
        String timestamp = "";
        String message = "";
        TreeMap<String, String> files = new TreeMap<>();
        boolean inFiles = false;

        for (String line : Files.readAllLines(file)) {
            if (inFiles) {
                if (!line.isEmpty()) {
                    String[] parts = line.split("\t");
                    files.put(parts[0], parts[1]);
                }
            } else if (line.startsWith("parent: ")) {
                String value = line.substring(8);
                parent = value.equals("none") ? "" : value;
            } else if (line.startsWith("timestamp: ")) {
                timestamp = line.substring(11);
            } else if (line.startsWith("message: ")) {
                message = line.substring(9);
            } else if (line.equals("files:")) {
                inFiles = true;
            }
        }
        return new Commit(id, parent, timestamp, message, files);
    }

    public Commit createCommit(StagingIndex index, String message) throws IOException {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Commit message required. Use: commit -m \"message\"");
        }

        Map<String, String> staged = index.getEntries();
        if (staged.isEmpty()) {
            throw new IllegalStateException("Nothing to commit (no staged files).");
        }

        String parentId = headId();
        if (!parentId.isEmpty()) {
            Commit parentCommit = readCommit(parentId);
            if (parentCommit != null && parentCommit.getFiles().equals(staged)) {
                throw new IllegalStateException("Nothing to commit (staged files are same as last commit).");
            }
        }

        for (Map.Entry<String, String> entry : staged.entrySet()) {
            if (!blobStorage.exists(entry.getValue())) {
                throw new IllegalStateException("Blob missing for " + entry.getKey() + ". Run add again.");
            }
        }

        String cleanMessage = message.replace("\r", " ").replace("\n", " ").trim();
        String timestamp = Instant.now().toString();
        String body = Commit.buildBody(parentId, timestamp, cleanMessage, staged);
        String id = HashService.sha256(body);

        Files.createDirectories(commitsDir);
        Files.writeString(commitsDir.resolve(id), "commit: " + id + "\n" + body, StandardOpenOption.CREATE_NEW);
        Files.writeString(headFile, id);

        return new Commit(id, parentId, timestamp, cleanMessage, staged);
    }

    public List<Commit> history() throws IOException {
        List<Commit> result = new ArrayList<>();
        String currentId = headId();
        while (!currentId.isEmpty()) {
            Commit commit = readCommit(currentId);
            if (commit == null) {
                break;
            }
            result.add(commit);
            currentId = commit.getParent();
        }
        return result;
    }
}
