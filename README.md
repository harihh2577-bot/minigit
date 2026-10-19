# MiniGit

A mini Git clone built in Java to learn how version control works internally.

## Features

- `init` - create a `.minigit` repository
- `add <file>` - store file content as a SHA-256 blob and stage it
- `status` - show HEAD and staged files
- `commit -m "message"` - save staged files as a snapshot commit
- `log` - show commit history from HEAD through parent commits
- `test` - run the built-in self-test (15 checks)

## How commit creation works

1. Read the staged files from `.minigit/index`.
2. If nothing is staged (or nothing changed since the last commit), print "Nothing to commit".
3. Read the parent commit ID from `.minigit/HEAD`.
4. Check that every staged blob exists in `.minigit/objects/` (blobs are never modified).
5. Build the commit text: parent ID, timestamp, message, file paths with blob hashes.
6. SHA-256 of that text becomes the commit ID.
7. Save the commit to `.minigit/commits/<id>` and update `HEAD`.

## Usage

```bash
javac MiniGit.java
java MiniGit init
java MiniGit add test.txt
java MiniGit commit -m "Initial commit"
java MiniGit log
java MiniGit test
```

## Not implemented yet

Branching, merging, and remote synchronization.

## Author

Hariharan R