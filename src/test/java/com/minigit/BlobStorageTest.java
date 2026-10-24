package com.minigit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minigit.core.BlobStorage;
import com.minigit.core.HashService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BlobStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storeReturnsHashOfContent() throws IOException {
        BlobStorage storage = new BlobStorage(tempDir.resolve("objects"));
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);

        String hash = storage.store(content);

        assertEquals(HashService.sha256(content), hash);
    }

    @Test
    void storedBlobCanBeReadBack() throws IOException {
        BlobStorage storage = new BlobStorage(tempDir.resolve("objects"));
        byte[] content = "world".getBytes(StandardCharsets.UTF_8);

        String hash = storage.store(content);

        assertTrue(storage.exists(hash));
        assertArrayEquals(content, storage.read(hash));
    }

    @Test
    void sameContentStoredTwiceCreatesOnlyOneFile() throws IOException {
        Path objects = tempDir.resolve("objects");
        BlobStorage storage = new BlobStorage(objects);
        byte[] content = "same".getBytes(StandardCharsets.UTF_8);

        String hash1 = storage.store(content);
        String hash2 = storage.store(content);

        assertEquals(hash1, hash2);
        assertEquals(1, objects.toFile().list().length);
    }

    @Test
    void unknownHashDoesNotExistAndReadFails() {
        BlobStorage storage = new BlobStorage(tempDir.resolve("objects"));

        assertFalse(storage.exists("abc123"));
        assertThrows(IOException.class, () -> storage.read("abc123"));
    }
}
