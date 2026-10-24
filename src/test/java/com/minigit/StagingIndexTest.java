package com.minigit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minigit.core.StagingIndex;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class StagingIndexTest {

    @TempDir
    Path tempDir;

    @Test
    void newIndexIsEmptyWhenFileMissing() throws IOException {
        StagingIndex index = new StagingIndex(tempDir.resolve("index"));
        index.load();

        assertTrue(index.isEmpty());
    }

    @Test
    void savedEntriesCanBeLoadedBack() throws IOException {
        Path file = tempDir.resolve("index");

        StagingIndex first = new StagingIndex(file);
        first.stage("a.txt", "hash1");
        first.stage("b.txt", "hash2");
        first.save();

        StagingIndex second = new StagingIndex(file);
        second.load();

        assertEquals(2, second.getEntries().size());
        assertEquals("hash1", second.getEntries().get("a.txt"));
        assertEquals("hash2", second.getEntries().get("b.txt"));
    }

    @Test
    void stagingSamePathAgainUpdatesHash() {
        StagingIndex index = new StagingIndex(tempDir.resolve("index"));
        index.stage("a.txt", "old");
        index.stage("a.txt", "new");

        assertEquals(1, index.getEntries().size());
        assertEquals("new", index.getEntries().get("a.txt"));
    }

    @Test
    void entriesAreSortedByPath() {
        StagingIndex index = new StagingIndex(tempDir.resolve("index"));
        index.stage("c.txt", "h3");
        index.stage("a.txt", "h1");
        index.stage("b.txt", "h2");

        List<String> keys = new ArrayList<>(index.getEntries().keySet());

        assertEquals(List.of("a.txt", "b.txt", "c.txt"), keys);
    }
}