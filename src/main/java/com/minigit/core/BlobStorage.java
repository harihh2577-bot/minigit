package com.minigit.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlobStorage {

    private final Path objectsDir;

    public BlobStorage(Path objectsDir) {
        this.objectsDir = objectsDir;
    }

    public String store(byte[] content) throws IOException {
        String hash = HashService.sha256(content);
        Files.createDirectories(objectsDir);
        Path blobPath = objectsDir.resolve(hash);
        if (!Files.exists(blobPath)) {
            Files.write(blobPath, content);
        }
        return hash;
    }

    public boolean exists(String hash) {
        return Files.exists(objectsDir.resolve(hash));
    }

    public byte[] read(String hash) throws IOException {
        Path blobPath = objectsDir.resolve(hash);
        if (!Files.exists(blobPath)) {
            throw new IOException("Blob not found: " + hash);
        }
        return Files.readAllBytes(blobPath);
    }
}
