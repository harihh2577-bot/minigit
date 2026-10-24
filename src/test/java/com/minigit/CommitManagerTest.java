package com.minigit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.minigit.core.BlobStorage;
import com.minigit.core.CommitManager;
import com.minigit.core.HashService;
import com.minigit.core.StagingIndex;
import com.minigit.models.Commit;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class CommitManagerTest {

    @TempDir
    Path tempDir;

    private BlobStorage blobs;
    private StagingIndex index;
    private CommitManager manager;

    @BeforeEach
    void setUp() {
        blobs = new BlobStorage(tempDir.resolve("objects"));
        index = new StagingIndex(tempDir.resolve("index"));
        manager = new CommitManager(tempDir.resolve("commits"), tempDir.resolve("HEAD"), blobs);
    }

    private void stageFile(String path, String content) throws IOException {
        String hash = blobs.store(content.getBytes(StandardCharsets.UTF_8));
        index.stage(path, hash);
    }

    @Test
    void firstCommitHasNoParentAndHeadPointsToIt() throws IOException {
        stageFile("a.txt", "hello");

        Commit commit = manager.createCommit(index, "Initial commit");

        assertEquals(64, commit.getId().length());
        assertTrue(commit.getParent().isEmpty());
        assertEquals("Initial commit", commit.getMessage());
        assertEquals(commit.getId(), manager.headId());
    }

    @Test
    void commitWithoutStagedFilesFails() {
        assertThrows(IllegalStateException.class, () -> manager.createCommit(index, "Empty"));
    }

    @Test
    void blankMessageFails() throws IOException {
        stageFile("a.txt", "hello");

        assertThrows(IllegalArgumentException.class, () -> manager.createCommit(index, "   "));
    }

    @Test
    void sameSnapshotTwiceFails() throws IOException {
        stageFile("a.txt", "hello");
        manager.createCommit(index, "one");

        assertThrows(IllegalStateException.class, () -> manager.createCommit(index, "two"));
        assertEquals(1, manager.history().size());
    }

    @Test
    void historyIsNewestFirstAndParentsAreLinked() throws IOException {
        stageFile("a.txt", "hello");
        Commit first = manager.createCommit(index, "first");

        stageFile("b.txt", "world");
        Commit second = manager.createCommit(index, "second");

        stageFile("a.txt", "hello v2");
        Commit third = manager.createCommit(index, "third");

        List<Commit> history = manager.history();

        assertEquals(3, history.size());
        assertEquals(third.getId(), history.get(0).getId());
        assertEquals(second.getId(), history.get(1).getId());
        assertEquals(first.getId(), history.get(2).getId());
        assertEquals(second.getId(), history.get(0).getParent());
        assertEquals(first.getId(), history.get(1).getParent());
        assertTrue(history.get(2).getParent().isEmpty());
    }

    @Test
    void commitIdIsHashOfItsBodyAndCanBeReadBack() throws IOException {
        stageFile("a.txt", "hello");
        stageFile("b.txt", "world");
        Commit commit = manager.createCommit(index, "snapshot");

        assertEquals(HashService.sha256(commit.getBody()), commit.getId());

        Commit loaded = manager.readCommit(commit.getId());
        assertEquals("snapshot", loaded.getMessage());
        assertEquals(2, loaded.getFiles().size());
        assertEquals(commit.getFiles().get("a.txt"), loaded.getFiles().get("a.txt"));
    }

    @Test
    void missingBlobFails() {
        index.stage("ghost.txt", "nonexistenthash");

        assertThrows(IllegalStateException.class, () -> manager.createCommit(index, "bad"));
    }
}