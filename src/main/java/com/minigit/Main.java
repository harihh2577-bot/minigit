package com.minigit;

import com.minigit.commands.AddCommand;
import com.minigit.commands.CommitCommand;
import com.minigit.commands.InitCommand;
import com.minigit.commands.LogCommand;
import com.minigit.core.RepositoryManager;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {

    private static final String USAGE = "Usage: init | add <file> | commit -m \"msg\" | log";

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println(USAGE);
            return;
        }

        RepositoryManager repo = new RepositoryManager(Paths.get("."));

        switch (args[0]) {
            case "init":
                new InitCommand(repo).run();
                break;
            case "add":
                if (args.length < 2) {
                    System.out.println("Usage: add <file>");
                } else {
                    new AddCommand(repo).run(args[1]);
                }
                break;
            case "commit":
                if (args.length >= 3 && args[1].equals("-m")) {
                    String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    new CommitCommand(repo).run(message);
                } else {
                    System.out.println("Usage: commit -m \"message\"");
                }
                break;
            case "log":
                new LogCommand(repo).run();
                break;
            default:
                System.out.println("Unknown command: " + args[0]);
        }
    }
}