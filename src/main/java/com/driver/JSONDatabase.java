package com.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class JSONDatabase {
    private final String directory;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ReentrantLock> collectionLocks;

    public JSONDatabase(String directory) {
        this.directory = directory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.collectionLocks = new ConcurrentHashMap<>();

        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
            Logger.log("INFO", "Database directory created at: " + directory);
        } else {
            Logger.log("INFO", "Using existing database directory: " + directory);
        }
    }

    private ReentrantLock getOrCreateLock(String collection) {
        return collectionLocks.computeIfAbsent(collection, k -> new ReentrantLock());
    }

    public synchronized void insertOrUpdate(String collection, String resource, User user) throws IOException {
        if (collection.isEmpty() || resource.isEmpty()) 
            throw new IllegalArgumentException("[ERROR] Collection and resource names cannot be empty!");

        ReentrantLock lock = getOrCreateLock(collection);
        lock.lock();
        try {
            File collectionDir = new File(directory, collection);
            if (!collectionDir.exists()) {
                collectionDir.mkdirs();
                Logger.log("INFO", "Created new collection directory: " + collection);
            }

            File file = new File(collectionDir, resource + ".json");
            objectMapper.writeValue(file, user);
            Logger.log("SUCCESS", "Inserted/Updated: " + resource + " in collection: " + collection);
        } finally {
            lock.unlock();
        }
    }

    public synchronized User read(String collection, String resource) throws IOException {
        ReentrantLock lock = getOrCreateLock(collection);
        lock.lock();
        try {
            File file = new File(directory + "/" + collection + "/" + resource + ".json");
            if (!file.exists()) {
                Logger.log("ERROR", "File not found: " + file.getPath());
                return null;
            }
            Logger.log("INFO", "Reading data from: " + file.getPath());
            return objectMapper.readValue(file, User.class);
        } finally {
            lock.unlock();
        }
    }

    public synchronized List<String> readAll(String collection) throws IOException {
        ReentrantLock lock = getOrCreateLock(collection);
        lock.lock();
        try {
            File dir = new File(directory, collection);
            if (!dir.exists() || !dir.isDirectory()) {
                Logger.log("ERROR", "Collection does not exist: " + collection);
                return new ArrayList<>();
            }

            List<String> records = new ArrayList<>();
            for (File file : Objects.requireNonNull(dir.listFiles())) {
                if (file.isFile() && file.getName().endsWith(".json")) {
                    records.add(new String(Files.readAllBytes(file.toPath())));
                }
            }
            Logger.log("SUCCESS", "Read " + records.size() + " records from collection: " + collection);
            return records;
        } finally {
            lock.unlock();
        }
    }

    public synchronized void delete(String collection, String resource) throws IOException {
        ReentrantLock lock = getOrCreateLock(collection);
        lock.lock();
        try {
            File target = new File(directory, collection + (resource.isEmpty() ? "" : "/" + resource + ".json"));

            if (!target.exists()) {
                Logger.log("ERROR", "Resource does not exist: " + target.getPath());
                return;
            }

            if (target.isDirectory()) {
                for (File file : Objects.requireNonNull(target.listFiles())) file.delete();
            }
            target.delete();

            Logger.log("SUCCESS", "Deleted resource: " + target.getPath());
        } finally {
            lock.unlock();
        }
    }
}
