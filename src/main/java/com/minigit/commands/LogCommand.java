package com.minigit.commands;

import com.minigit.core.RepositoryManager;
import com.minigit.models.Commit;
import java.io.IOException;
import java.util.List;

public class LogCommand {

    private final RepositoryManager repo;

    public LogCommand(RepositoryManager repo) {
        this.repo = repo;
    }

    public void run() throws IOException {
        if (!repo.isInitialized()) {
            System.out.println("Not a MiniGit repo. Run: init");
            return;
        }

        List<Commit> history = repo.commitManager().history();
        if (history.isEmpty()) {
            System.out.println("No commits yet.");
            return;
        }

        for (Commit commit : history) {
            System.out.println("Commit:  " + commit.getId());
            System.out.println("Message: " + commit.getMessage());
            System.out.println("Date:    " + commit.getTimestamp());
            System.out.println("Parent:  " + (commit.getParent().isEmpty() ? "None" : commit.getParent()));
            System.out.println();
        }
    }
}