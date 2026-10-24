package com.minigit.commands;

import com.minigit.core.RepositoryManager;
import com.minigit.core.StagingIndex;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AddCommand {

    private final RepositoryManager repo;

    public AddCommand(RepositoryManager repo) {
        this.repo = repo;
    }

    public void run(String filePath) throws IOException {
        if (!repo.isInitialized()) {
            System.out.println("Not a MiniGit repo. Run: init");
            return;
        }

        Path file = repo.getRoot().resolve(filePath);
        if (!Files.isRegularFile(file)) {
            System.out.println("Error: file not found -> " + filePath);
            return;
        }

        byte[] content = Files.readAllBytes(file);
        String hash = repo.blobStorage().store(content);

        StagingIndex index = repo.stagingIndex();
        index.load();
        index.stage(filePath, hash);
        index.save();

        System.out.println("Staged: " + filePath + " (" + hash.substring(0, 12) + ")");
    }
}