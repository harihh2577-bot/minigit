package com.minigit.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class StagingIndex {

    private final Path indexFile;
    private final TreeMap<String, String> entries = new TreeMap<>();

    public StagingIndex(Path indexFile) {
        this.indexFile = indexFile;
    }

    public void load() throws IOException {
        entries.clear();
        if (!Files.exists(indexFile)) {
            return;
        }
        for (String line : Files.readAllLines(indexFile)) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            int tab = line.lastIndexOf('\t');
            if (tab < 0) {
                continue;
            }
            String filePath = line.substring(0, tab).trim();
            String hash = line.substring(tab + 1).trim();
            entries.put(filePath, hash);
        }
    }

    public void save() throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            sb.append(entry.getKey()).append("\t").append(entry.getValue()).append("\n");
        }
        if (indexFile.getParent() != null) {
            Files.createDirectories(indexFile.getParent());
        }
        Files.writeString(indexFile, sb.toString());
    }

    public void stage(String filePath, String hash) {
        String normalized = Paths.get(filePath).normalize().toString();
        entries.put(normalized, hash);
    }

    public Map<String, String> getEntries() {
        return Collections.unmodifiableMap(entries);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}