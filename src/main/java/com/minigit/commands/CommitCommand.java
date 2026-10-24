package com.minigit.commands;

import com.minigit.core.RepositoryManager;
import com.minigit.core.StagingIndex;
import com.minigit.models.Commit;
import java.io.IOException;

public class CommitCommand {

    private final RepositoryManager repo;

    public CommitCommand(RepositoryManager repo) {
        this.repo = repo;
    }

    public void run(String message) throws IOException {
        if (!repo.isInitialized()) {
            System.out.println("Not a MiniGit repo. Run: init");
            return;
        }

        StagingIndex index = repo.stagingIndex();
        index.load();

        try {
            Commit commit = repo.commitManager().createCommit(index, message);
            System.out.println("[" + commit.getId().substring(0, 12) + "] " + commit.getMessage());
            System.out.println(" " + commit.getFiles().size() + " file(s) committed");
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
