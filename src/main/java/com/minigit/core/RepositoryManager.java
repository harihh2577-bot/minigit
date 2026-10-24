package com.minigit.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RepositoryManager {

    private final Path root;

    public RepositoryManager(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }

    public Path gitDir() {
        return root.resolve(".minigit");
    }

    public Path objectsDir() {
        return gitDir().resolve("objects");
    }

    public Path commitsDir() {
        return gitDir().resolve("commits");
    }

    public Path indexFile() {
        return gitDir().resolve("index");
    }

    public Path headFile() {
        return gitDir().resolve("HEAD");
    }

    public boolean isInitialized() {
        return Files.isDirectory(gitDir());
    }

    public void init() throws IOException {
        Files.createDirectories(objectsDir());
        Files.createDirectories(commitsDir());
        if (!Files.exists(indexFile())) {
            Files.writeString(indexFile(), "");
        }
        if (!Files.exists(headFile())) {
            Files.writeString(headFile(), "");
        }
    }

    public BlobStorage blobStorage() {
        return new BlobStorage(objectsDir());
    }

    public StagingIndex stagingIndex() {
        return new StagingIndex(indexFile());
    }

    public CommitManager commitManager() {
        return new CommitManager(commitsDir(), headFile(), blobStorage());
    }
}