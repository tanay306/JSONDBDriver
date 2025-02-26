package com.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A simple JSON-based database supporting multi-threaded operations.
 */
public class JSONDatabase {
    private final String directory;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ReentrantLock> collectionLocks;
    private final ExecutorService executorService;

    /**
     * Initializes the database with a specified directory.
     */
    public JSONDatabase(String directory) {
        this.directory = directory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.collectionLocks = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(10);
    }

    /**
     * Allows executing multiple database operations in parallel.
     */
    public void executeTasks(List<Callable<Void>> tasks) throws InterruptedException {
        executorService.invokeAll(tasks);
    }

    /**
     * Retrieves or creates a lock for the given collection.
     */
    private ReentrantLock getOrCreateLock(String collection) {
        return collectionLocks.computeIfAbsent(collection, k -> new ReentrantLock());
    }

    /**
     * Inserts or updates a user in the database asynchronously.
     */
    public Future<Void> insertOrUpdate(String collection, String resource, User user) {
        return executorService.submit(() -> {
            long threadId = Thread.currentThread().getId();
            Logger.log("THREAD", "Thread " + threadId + " is inserting/updating " + resource);

            ReentrantLock lock = getOrCreateLock(collection);
            lock.lock();
            try {
                File collectionDir = new File(directory, collection);
                if (!collectionDir.exists()) {
                    collectionDir.mkdirs();
                    Logger.log("INFO", "Thread " + threadId + " created new collection directory: " + collection);
                }

                File file = new File(collectionDir, resource + ".json");
                objectMapper.writeValue(file, user);
                Logger.log("SUCCESS", "Thread " + threadId + " inserted/updated " + resource + " in collection: " + collection);
            } finally {
                lock.unlock();
            }
            return null;
        });
    }

    /**
     * Reads a user from the database asynchronously.
     */
    public Future<User> read(String collection, String resource) {
        return executorService.submit(() -> {
            long threadId = Thread.currentThread().getId();
            Logger.log("THREAD", "Thread " + threadId + " is reading " + resource);

            ReentrantLock lock = getOrCreateLock(collection);
            lock.lock();
            try {
                File file = new File(directory + "/" + collection + "/" + resource + ".json");
                if (!file.exists()) {
                    Logger.log("ERROR", "File not found: " + file.getPath());
                    return null;
                }
                Logger.log("INFO", "Thread " + threadId + " reading data from: " + file.getPath());
                return objectMapper.readValue(file, User.class);
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * Reads all users from the specified collection asynchronously.
     */
    public Future<List<String>> readAll(String collection) {
        return executorService.submit(() -> {
            long threadId = Thread.currentThread().getId();
            Logger.log("THREAD", "Thread " + threadId + " is reading all from collection: " + collection);

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
                Logger.log("SUCCESS", "Thread " + threadId + " read " + records.size() + " records from collection: " + collection);
                return records;
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * Deletes a user or collection from the database asynchronously.
     */
    public Future<Void> delete(String collection, String resource) {
        return executorService.submit(() -> {
            long threadId = Thread.currentThread().getId();
            Logger.log("THREAD", "Thread " + threadId + " is deleting " + resource);

            ReentrantLock lock = getOrCreateLock(collection);
            lock.lock();
            try {
                File target = new File(directory, collection + (resource.isEmpty() ? "" : "/" + resource + ".json"));

                if (!target.exists()) {
                    Logger.log("ERROR", "Resource does not exist: " + target.getPath());
                    return null;
                }

                if (target.isDirectory()) {
                    for (File file : Objects.requireNonNull(target.listFiles())) file.delete();
                }
                target.delete();

                Logger.log("SUCCESS", "Thread " + threadId + " deleted resource: " + target.getPath());
            } finally {
                lock.unlock();
            }
            return null;
        });
    }

    /**
     * Shuts down the database's thread pool.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
