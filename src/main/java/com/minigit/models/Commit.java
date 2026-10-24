package com.minigit.models;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class Commit {

    private final String id;
    private final String parent;
    private final String timestamp;
    private final String message;
    private final Map<String, String> files;

    public Commit(String id, String parent, String timestamp, String message, Map<String, String> files) {
        this.id = id;
        this.parent = parent;
        this.timestamp = timestamp;
        this.message = message;
        this.files = Collections.unmodifiableMap(new TreeMap<>(files));
    }

    public String getId() {
        return id;
    }

    public String getParent() {
        return parent;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getFiles() {
        return files;
    }

    public String getBody() {
        return buildBody(parent, timestamp, message, files);
    }

    public static String buildBody(String parent, String timestamp, String message, Map<String, String> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("parent: ").append(parent.isEmpty() ? "none" : parent).append("\n");
        sb.append("timestamp: ").append(timestamp).append("\n");
        sb.append("message: ").append(message).append("\n");
        sb.append("files:\n");
        for (Map.Entry<String, String> entry : new TreeMap<>(files).entrySet()) {
            sb.append(entry.getKey()).append("\t").append(entry.getValue()).append("\n");
        }
        return sb.toString();
    }
}
