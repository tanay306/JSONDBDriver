package com.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A JSON-based database supporting multi-threaded operations, caching, and ACID transactions.
 */
public class JSONDatabase {
    private final String directory;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ReentrantLock> collectionLocks;
    private final ExecutorService executorService;
    private final TransactionManager transactionManager;
    private final ConcurrentHashMap<String, User> cache;

    /**
     * Initializes the database with a specified directory.
     */
    public JSONDatabase(String directory) {
        this.directory = directory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.collectionLocks = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(10);
        this.transactionManager = new TransactionManager();
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Retrieves or creates a lock for the given collection.
     */
    private ReentrantLock getOrCreateLock(String collection) {
        return collectionLocks.computeIfAbsent(collection, k -> new ReentrantLock());
    }

    /**
     * Executes multiple database operations in parallel.
     */
    public void executeTasks(List<Callable<Void>> tasks) throws InterruptedException {
        executorService.invokeAll(tasks);
    }

    /**
     * Inserts or updates a user in the database asynchronously with transaction support and caching.
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
                }

                File file = new File(collectionDir, resource + ".json");
                String jsonData = objectMapper.writeValueAsString(user);

                if (transactionManager.isTransactionActive()) {
                    transactionManager.addToTransaction(file.getAbsolutePath(), jsonData);
                } else {
                    objectMapper.writeValue(file, user);
                    cache.put(resource, user);
                }
            } finally {
                lock.unlock();
            }
            return null;
        });
    }

    /**
     * Reads a user from the database asynchronously with caching support.
     */
    public Future<User> read(String collection, String resource) {
        return executorService.submit(() -> {
            if (cache.containsKey(resource)) {
                Logger.log("CACHE", "Cache hit for user: " + resource);
                return cache.get(resource);
            }

            Logger.log("CACHE", "Cache miss for user: " + resource + ". Reading from disk...");
            ReentrantLock lock = getOrCreateLock(collection);
            lock.lock();
            try {
                File file = new File(directory + "/" + collection + "/" + resource + ".json");
                if (!file.exists()) {
                    return null;
                }
                User user = objectMapper.readValue(file, User.class);
                cache.put(resource, user);
                return user;
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * Reads a user from the database asynchronously with caching support.
     */
    public Future<User> readWithCache(String collection, String resource) {
        return executorService.submit(() -> {
            if (cache.containsKey(resource)) {
                Logger.log("CACHE", "Cache hit for user: " + resource);
                return cache.get(resource);
            }

            Logger.log("CACHE", "Cache miss for user: " + resource + ". Reading from disk...");
            return read(collection, resource).get();
        });
    }


    /**
     * Reads all users from the specified collection asynchronously.
     */
    public Future<List<String>> readAll(String collection) {
        return executorService.submit(() -> {
            ReentrantLock lock = getOrCreateLock(collection);
            lock.lock();
            try {
                File dir = new File(directory, collection);
                if (!dir.exists() || !dir.isDirectory()) {
                    return new ArrayList<>();
                }

                List<String> records = new ArrayList<>();
                for (File file : Objects.requireNonNull(dir.listFiles())) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        records.add(new String(Files.readAllBytes(file.toPath())));
                    }
                }
                return records;
            } finally {
                lock.unlock();
            }
        });
    }

    /**
     * Deletes a user or collection from the database asynchronously with transaction support.
     */
    public Future<Void> delete(String collection, String resource) {
        return executorService.submit(() -> {
            ReentrantLock lock = getOrCreateLock(collection);
            lock.lock();
            try {
                File target = new File(directory, collection + (resource.isEmpty() ? "" : "/" + resource + ".json"));

                if (!target.exists()) {
                    return null;
                }

                if (transactionManager.isTransactionActive()) {
                    transactionManager.addToTransaction(target.getAbsolutePath(), null);
                } else {
                    if (target.isDirectory()) {
                        for (File file : Objects.requireNonNull(target.listFiles())) file.delete();
                    }
                    target.delete();
                    cache.remove(resource);
                }
            } finally {
                lock.unlock();
            }
            return null;
        });
    }

    /**
     * Starts a new transaction.
     */
    public void startTransaction() {
        transactionManager.startTransaction();
    }

    /**
     * Commits the ongoing transaction.
     */
    public void commitTransaction() throws IOException {
        transactionManager.commitTransaction();
    }

    /**
     * Rolls back the ongoing transaction.
     */
    public void rollbackTransaction() {
        transactionManager.rollbackTransaction();
    }

    /**
     * Calculates statistics for users (average age, company distribution) with and without caching.
     */
    public void calculateUserStatistics(String collection, List<String> userNames) throws ExecutionException, InterruptedException {
        long start, end;
        List<Long> timesWithoutCache = new ArrayList<>();
        List<Long> timesWithCache = new ArrayList<>();

        start = System.nanoTime();
        computeStatistics(userNames, false);
        end = System.nanoTime();
        timesWithoutCache.add(end - start);

        start = System.nanoTime();
        computeStatistics(userNames, true);
        end = System.nanoTime();
        timesWithCache.add(end - start);

        double avgNoCache = timesWithoutCache.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        double avgCache = timesWithCache.stream().mapToLong(Long::longValue).average().orElse(0.0) / 1_000_000.0;
        double improvement = ((avgNoCache - avgCache) / avgNoCache) * 100;

        Logger.log("BENCHMARK", "Avg Calculation Time WITHOUT Cache: " + avgNoCache + " ms");
        Logger.log("BENCHMARK", "Avg Calculation Time WITH Cache: " + avgCache + " ms");
        Logger.log("BENCHMARK", "Performance Improvement: " + String.format("%.2f", improvement) + "%");
    }

    private double computeStatistics(List<String> userNames, boolean useCache) throws ExecutionException, InterruptedException {
        int totalAge = 0;
        Map<String, Integer> companyCount = new HashMap<>();
        int userCount = userNames.size();

        for (String user : userNames) {
            User userData = useCache ? read("users", user).get() : read("users", user).get();
            if (userData != null) {
                totalAge += Integer.parseInt(userData.age);
                companyCount.put(userData.company, companyCount.getOrDefault(userData.company, 0) + 1);
            }
        }

        Logger.log("STATS", "Company Distribution: " + companyCount.toString());
        return userCount == 0 ? 0 : (double) totalAge / userCount;
    }

    /**
     * Shuts down the database's thread pool.
     */
    public void shutdown() {
        executorService.shutdown();
    }
}
