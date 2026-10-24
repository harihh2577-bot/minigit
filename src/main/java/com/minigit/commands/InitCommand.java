package com.minigit.commands;

import com.minigit.core.RepositoryManager;
import java.io.IOException;

public class InitCommand {

    private final RepositoryManager repo;

    public InitCommand(RepositoryManager repo) {
        this.repo = repo;
    }

    public void run() throws IOException {
        repo.init();
        System.out.println("Initialized MiniGit repository in "
                + repo.gitDir().toAbsolutePath().normalize());
    }
}