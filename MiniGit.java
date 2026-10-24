import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MiniGit {

    // Repo root folder. Test-la temp folder-a maathurom, normal-a current folder.
    static Path root = Paths.get(".");

    static Path gitDir()      { return root.resolve(".minigit"); }
    static Path objectsDir()  { return gitDir().resolve("objects"); }
    static Path commitsDir()  { return gitDir().resolve("commits"); }
    static Path indexFile()   { return gitDir().resolve("index"); }
    static Path headFile()    { return gitDir().resolve("HEAD"); }

    // ---------- Commit data class ----------
    static class Commit {
        String id;
        String parent = "";      // "" = first commit (parent illa)
        String timestamp;
        String message;
        TreeMap<String, String> files = new TreeMap<>(); // path -> blob hash

        // Idhe text thaan hash aagum, file-layum ezhudhapadum
        String body() {
            StringBuilder sb = new StringBuilder();
            sb.append("parent: ").append(parent.isEmpty() ? "none" : parent).append("\n");
            sb.append("timestamp: ").append(timestamp).append("\n");
            sb.append("message: ").append(message).append("\n");
            sb.append("files:\n");
            for (Map.Entry<String, String> e : files.entrySet()) {
                sb.append(e.getKey()).append("\t").append(e.getValue()).append("\n");
            }
            return sb.toString();
        }
    }

    // ---------- SHA-256 helpers ----------
    static String sha256(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    static String sha256(String text) throws Exception {
        return sha256(text.getBytes(StandardCharsets.UTF_8));
    }

    // ---------- init ----------
    static void init() throws IOException {
        Files.createDirectories(objectsDir());
        Files.createDirectories(commitsDir());
        if (!Files.exists(indexFile())) Files.writeString(indexFile(), "");
        if (!Files.exists(headFile()))  Files.writeString(headFile(), "");
        System.out.println("Initialized MiniGit repository in " + gitDir().toAbsolutePath().normalize());
    }

    static boolean isInitialized() {
        return Files.isDirectory(gitDir());
    }

    // ---------- index (path  hash) ----------
    // Tab-aa irundhaalum space-aa irundhaalum work aagum.
    // Hash-la whitespace irukkadhu, so LAST whitespace-la pirikkirom.
    static TreeMap<String, String> loadIndex() throws IOException {
        TreeMap<String, String> index = new TreeMap<>();
        if (!Files.exists(indexFile())) return index;
        for (String line : Files.readAllLines(indexFile())) {
            line = line.trim();
            if (line.isEmpty()) continue;
            int idx = Math.max(line.lastIndexOf('\t'), line.lastIndexOf(' '));
            if (idx < 0) continue;                          // bad line skip
            String path = line.substring(0, idx).trim();
            String hash = line.substring(idx + 1).trim();
            index.put(path, hash);
        }
        return index;
    }

    static void saveIndex(TreeMap<String, String> index) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : index.entrySet()) {
            sb.append(e.getKey()).append("\t").append(e.getValue()).append("\n");
        }
        Files.writeString(indexFile(), sb.toString());
    }

    // ---------- add (blob store + stage) ----------
    static void add(String fileName) throws Exception {
        if (!isInitialized()) { System.out.println("Not a MiniGit repo. Run: init"); return; }
        Path file = root.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            System.out.println("Error: file not found -> " + fileName);
            return;
        }
        byte[] content = Files.readAllBytes(file);
        String hash = sha256(content);

        Files.createDirectories(objectsDir());
        Path blob = objectsDir().resolve(hash);
        if (!Files.exists(blob)) {            // same content-na thirumba ezhudha maatom
            Files.write(blob, content);
        }

        TreeMap<String, String> index = loadIndex();
        index.put(Paths.get(fileName).normalize().toString(), hash);
        saveIndex(index);
        System.out.println("Staged: " + fileName + " (" + hash.substring(0, 12) + ")");
    }

    // ---------- HEAD ----------
    static String headId() throws IOException {
        if (!Files.exists(headFile())) return "";
        return Files.readString(headFile()).trim();
    }

    // ---------- read a commit from disk ----------
    static Commit readCommit(String id) throws IOException {
        Path f = commitsDir().resolve(id);
        if (!Files.exists(f)) return null;
        Commit c = new Commit();
        c.id = id;
        boolean inFiles = false;
        for (String line : Files.readAllLines(f)) {
            if (inFiles) {
                if (line.isEmpty()) continue;
                String[] p = line.split("\t");
                c.files.put(p[0], p[1]);
            } else if (line.startsWith("parent: ")) {
                String v = line.substring(8);
                c.parent = v.equals("none") ? "" : v;
            } else if (line.startsWith("timestamp: ")) {
                c.timestamp = line.substring(11);
            } else if (line.startsWith("message: ")) {
                c.message = line.substring(9);
            } else if (line.equals("files:")) {
                inFiles = true;
            }
        }
        return c;
    }

    // ---------- COMMIT ----------
    // Success-na pudhu commit ID return pannum. Nothing to commit-na null.
    static String commit(String message) throws Exception {
        if (!isInitialized()) { System.out.println("Not a MiniGit repo. Run: init"); return null; }
        if (message == null || message.isBlank()) {
            System.out.println("Error: commit message required. Use: commit -m \"message\"");
            return null;
        }

        // Step 1: staged files padikkirom
        TreeMap<String, String> index = loadIndex();
        if (index.isEmpty()) {
            System.out.println("Nothing to commit (no staged files).");
            return null;
        }

        // Step 2: parent commit + same snapshot check
        String parentId = headId();
        if (!parentId.isEmpty()) {
            Commit parent = readCommit(parentId);
            if (parent != null && parent.files.equals(index)) {
                System.out.println("Nothing to commit (staged files are same as last commit).");
                return null;
            }
        }

        // Step 3: blobs irukka-nu verify (READ ONLY - blob-a modify pannala)
        for (Map.Entry<String, String> e : index.entrySet()) {
            if (!Files.exists(objectsDir().resolve(e.getValue()))) {
                System.out.println("Error: blob missing for " + e.getKey() + ". Run add again.");
                return null;
            }
        }

        // Step 4: commit object build
        Commit c = new Commit();
        c.parent = parentId;
        c.timestamp = Instant.now().toString();
        c.message = message.replace("\r", " ").replace("\n", " ").trim();
        c.files.putAll(index);

        // Step 5: SHA-256 of commit info = commit ID
        c.id = sha256(c.body());

        // Step 6: commit file ezhudhurom (CREATE_NEW = already irundha overwrite aagadhu)
        Files.createDirectories(commitsDir());
        Files.writeString(commitsDir().resolve(c.id),
                "commit: " + c.id + "\n" + c.body(),
                StandardOpenOption.CREATE_NEW);

        // Step 7: HEAD update
        Files.writeString(headFile(), c.id);

        System.out.println("[" + c.id.substring(0, 12) + "] " + c.message);
        System.out.println(" " + c.files.size() + " file(s) committed");
        return c.id;
    }

    // ---------- history (NEW) ----------
    // HEAD-la irundhu parent parent-a nadandhu, commits-a LIST-la serkkum.
    // List-oda index 0 = latest commit, kadaisi = initial commit.
    static List<Commit> history() throws IOException {
        List<Commit> list = new ArrayList<>();
        String id = headId();
        while (!id.isEmpty()) {
            Commit c = readCommit(id);
            if (c == null) break;          // commit file missing-na safe-a nillu
            list.add(c);
            id = c.parent;                 // adutha step: parent-ku poga
        }
        return list;
    }

    // ---------- log (UPDATED) ----------
    static void log() throws IOException {
        if (!isInitialized()) { System.out.println("Not a MiniGit repo. Run: init"); return; }
        List<Commit> commits = history();
        if (commits.isEmpty()) {
            System.out.println("No commits yet.");
            return;
        }
        for (Commit c : commits) {
            System.out.println("Commit:  " + c.id);
            System.out.println("Message: " + c.message);
            System.out.println("Date:    " + c.timestamp);
            System.out.println("Parent:  " + (c.parent.isEmpty() ? "None" : c.parent));
            System.out.println();
        }
    }

    // ---------- status ----------
    static void status() throws IOException {
        TreeMap<String, String> index = loadIndex();
        String head = headId();
        System.out.println("HEAD: " + (head.isEmpty() ? "(no commits yet)" : head.substring(0, 12)));
        if (index.isEmpty()) { System.out.println("No staged files."); return; }
        System.out.println("Staged files:");
        for (Map.Entry<String, String> e : index.entrySet()) {
            System.out.println("  " + e.getKey() + "  " + e.getValue().substring(0, 12));
        }
    }

    // ---------- self test ----------
    static Map<String, String> snapshotObjects() throws Exception {
        Map<String, String> snap = new TreeMap<>();
        try (Stream<Path> s = Files.list(objectsDir())) {
            for (Path p : s.collect(Collectors.toList())) {
                snap.put(p.getFileName().toString(), sha256(Files.readAllBytes(p)));
            }
        }
        return snap;
    }

    static int passed = 0;
    static void check(boolean cond, String name) {
        if (!cond) throw new AssertionError("FAILED: " + name);
        passed++;
        System.out.println("  PASS: " + name);
    }

    static void selfTest() throws Exception {
        root = Files.createTempDirectory("minigit-test");   // temp folder, un real repo touch aagadhu
        init();
        Files.writeString(root.resolve("a.txt"), "hello");
        Files.writeString(root.resolve("b.txt"), "world");

        System.out.println("\n--- Test 0: no commits yet ---");
        check(history().isEmpty(), "no commits -> history is empty");
        log();   // "No commits yet." varanum

        System.out.println("\n--- Test 1: nothing staged ---");
        check(commit("Empty") == null, "commit without staging returns nothing-to-commit");

        System.out.println("\n--- Test 2: first commit ---");
        add("a.txt"); add("b.txt");
        Map<String, String> blobsBefore1 = snapshotObjects();
        String id1 = commit("Initial commit");
        check(id1 != null && id1.length() == 64, "commit ID is 64-char SHA-256");
        check(snapshotObjects().equals(blobsBefore1), "blobs not modified by commit 1");
        Commit c1 = readCommit(id1);
        check(c1.message.equals("Initial commit"), "message stored");
        check(c1.timestamp != null && !c1.timestamp.isEmpty(), "timestamp stored");
        check(c1.parent.isEmpty(), "first commit has no parent");
        check(c1.files.size() == 2 && c1.files.containsKey("a.txt"), "file snapshot stored");
        check(sha256(c1.body()).equals(id1), "commit ID matches SHA-256 of its content");
        check(headId().equals(id1), "HEAD points to commit 1");

        System.out.println("\n--- Test 3: log with only one commit ---");
        check(history().size() == 1, "history has exactly 1 commit");
        check(history().get(0).id.equals(id1), "that commit is the initial commit");
        check(history().get(0).parent.isEmpty(), "initial commit shows no parent");
        log();

        System.out.println("--- Test 4: same snapshot again ---");
        check(commit("Again") == null, "no new changes -> nothing to commit");
        check(history().size() == 1, "history still 1 commit");

        System.out.println("\n--- Test 5: second commit ---");
        Files.writeString(root.resolve("c.txt"), "new file");
        add("c.txt");
        Map<String, String> blobsBefore2 = snapshotObjects();
        String id2 = commit("Add c.txt");
        check(id2 != null && !id2.equals(id1), "second commit has different ID");
        check(readCommit(id2).parent.equals(id1), "parent of commit 2 = commit 1");
        check(headId().equals(id2), "HEAD points to commit 2");
        check(snapshotObjects().equals(blobsBefore2), "blobs not modified by commit 2");

        // Pazhaya commit files-oda text-a save pannikkirom (apram compare panna)
        String text1 = Files.readString(commitsDir().resolve(id1));
        String text2 = Files.readString(commitsDir().resolve(id2));

        System.out.println("\n--- Test 6: multiple commits, order + parents ---");
        Files.writeString(root.resolve("a.txt"), "hello v2");
        add("a.txt");
        String id3 = commit("Update a.txt");
        check(id3 != null, "third commit created");

        List<Commit> h = history();
        check(h.size() == 3, "history has 3 commits");
        check(h.get(0).id.equals(id3), "position 0 = latest commit (id3)");
        check(h.get(1).id.equals(id2), "position 1 = id2");
        check(h.get(2).id.equals(id1), "position 2 = initial commit (id1)");
        check(h.get(0).parent.equals(id2), "commit 3 parent = commit 2");
        check(h.get(1).parent.equals(id1), "commit 2 parent = commit 1");
        check(h.get(2).parent.isEmpty(), "commit 1 has no parent");

        System.out.println("\n--- Test 7: old commits not changed ---");
        check(Files.readString(commitsDir().resolve(id1)).equals(text1), "commit 1 file unchanged");
        check(Files.readString(commitsDir().resolve(id2)).equals(text2), "commit 2 file unchanged");
        try (Stream<Path> s = Files.list(commitsDir())) {
            check(s.count() == 3, "3 commit files exist (nothing deleted)");
        }

        System.out.println("\n--- Full log output ---");
        log();

        System.out.println("All " + passed + " checks passed.");
    }

    // ---------- main ----------
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: init | add <file> | status | commit -m \"msg\" | log | test");
            return;
        }
        switch (args[0]) {
            case "init":   init(); break;
            case "add":
                if (args.length < 2) System.out.println("Usage: add <file>");
                else add(args[1]);
                break;
            case "status": status(); break;
            case "commit":
                if (args.length < 3 || !args[1].equals("-m")) {
                    System.out.println("Usage: commit -m \"message\"");
                } else {
                    try {
                        commit(String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
                    } catch (Exception e) {
                        System.out.println("Commit failed: " + e.getMessage());
                    }
                }
                break;
            case "log":    log(); break;
            case "test":   selfTest(); break;
            default:       System.out.println("Unknown command: " + args[0]);
        }
    }
}