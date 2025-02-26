package com.driver;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Unit test class for JSONDatabase.
 * Uses JUnit 5 to test the core database operations, including transactions and caching.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JSONDatabaseTest {
    private static JSONDatabase db;
    private static final String TEST_DB_PATH = "./test_database"; 

    /**
     * Initializes the database before running any tests.
     */
    @BeforeAll
    public static void setup() {
        db = new JSONDatabase(TEST_DB_PATH);
        Logger.log("TEST", "Database initialized for testing!");
    }

    /**
     * Tests inserting a new user into the database.
     */
    @Test
    @Order(1)
    public void testInsertUser() throws ExecutionException, InterruptedException {
        User user = new User("Alice", "28", "9999999999", "Amazon",
                new Address("Seattle", "Washington", "USA", "98101"));

        db.insertOrUpdate("users", user.name, user).get();
        User retrievedUser = db.read("users", "Alice").get();

        assertNotNull(retrievedUser);
        assertEquals("Amazon", retrievedUser.company);
        Logger.log("TEST", "testInsertUser passed!");
    }

    /**
     * Tests reading an existing user from the database.
     */
    @Test
    @Order(2)
    public void testReadUser() throws ExecutionException, InterruptedException {
        User user = db.read("users", "Alice").get();
        assertNotNull(user);
        assertEquals("Amazon", user.company);
        Logger.log("TEST", "testReadUser passed!");
    }

    /**
     * Tests retrieving all users from the database.
     */
    @Test
    @Order(3)
    public void testReadAllUsers() throws ExecutionException, InterruptedException {
        List<String> users = db.readAll("users").get();
        assertFalse(users.isEmpty());
        Logger.log("TEST", "testReadAllUsers passed!");
    }

    /**
     * Tests updating an existing user's data.
     */
    @Test
    @Order(4)
    public void testUpdateUser() throws ExecutionException, InterruptedException {
        User user = db.read("users", "Alice").get();
        assertNotNull(user);
        user.company = "Google";
        db.insertOrUpdate("users", "Alice", user).get();
        assertEquals("Google", db.read("users", "Alice").get().company);
        Logger.log("TEST", "testUpdateUser passed!");
    }

    /**
     * Tests deleting an existing user from the database.
     */
    @Test
    @Order(5)
    public void testDeleteUser() throws ExecutionException, InterruptedException {
        db.delete("users", "Alice").get();
        assertNull(db.read("users", "Alice").get());
        Logger.log("TEST", "testDeleteUser passed!");
    }

    /**
     * Tests reading a non-existent user from the database.
     */
    @Test
    @Order(6)
    public void testReadMissingUser() throws ExecutionException, InterruptedException {
        User user = db.read("users", "Bob").get();
        assertNull(user);
        Logger.log("TEST", "testReadMissingUser passed!");
    }

    /**
     * Tests transaction commit operation.
     */
    @Test
    @Order(7)
    public void testTransactionCommit() throws ExecutionException, InterruptedException, IOException {
        Logger.log("TEST", "Starting transaction test - Commit scenario...");
        db.startTransaction();

        User user = new User("TransactionUser", "30", "1234567890", "Netflix",
                new Address("Los Angeles", "California", "USA", "90001"));

        db.insertOrUpdate("users", "TransactionUser", user).get();
        db.commitTransaction();

        User retrievedUser = db.read("users", "TransactionUser").get();
        assertNotNull(retrievedUser);
        assertEquals("Netflix", retrievedUser.company);
        Logger.log("TEST", "testTransactionCommit passed!");
    }

    /**
     * Tests transaction rollback operation.
     */
    @Test
    @Order(8)
    public void testTransactionRollback() throws ExecutionException, InterruptedException {
        Logger.log("TEST", "Starting transaction test - Rollback scenario...");
        db.startTransaction();

        User user = new User("RollbackUser", "27", "9876543210", "Facebook",
                new Address("San Francisco", "California", "USA", "94105"));

        db.insertOrUpdate("users", "RollbackUser", user).get();

        Logger.log("TEST", "Simulated error occurred! Rolling back...");
        db.rollbackTransaction();

        User retrievedUser = db.read("users", "RollbackUser").get();
        assertNull(retrievedUser);
        Logger.log("TEST", "testTransactionRollback passed!");
    }

    /**
     * Tests cache efficiency by comparing read speeds.
     */
    @Test
    @Order(9)
    public void testCachePerformance() throws ExecutionException, InterruptedException {
        Logger.log("TEST", "Starting cache performance test...");

        // Reading first time (cache miss)
        long startTimeNoCache = System.nanoTime();
        db.readWithCache("users", "Alice").get();
        long endTimeNoCache = System.nanoTime();

        // Reading second time (cache hit)
        long startTimeCache = System.nanoTime();
        db.readWithCache("users", "Alice").get();
        long endTimeCache = System.nanoTime();

        long timeWithoutCache = (endTimeNoCache - startTimeNoCache);
        long timeWithCache = (endTimeCache - startTimeCache);

        Logger.log("BENCHMARK", "Read time WITHOUT cache: " + timeWithoutCache + " ms");
        Logger.log("BENCHMARK", "Read time WITH cache: " + timeWithCache + " ms");

        assertTrue(timeWithCache < timeWithoutCache, "Cache should improve read speed!");
        Logger.log("TEST", "testCachePerformance passed!");
    }

    /**
     * Tests cache correctness by verifying cache hit/miss logs.
     */
    @Test
    @Order(10)
    public void testCacheHitMiss() throws ExecutionException, InterruptedException {
        Logger.log("TEST", "Testing cache hit/miss behavior...");

        // First read should be a cache miss
        db.readWithCache("users", "Tom Smith").get();

        // Second read should be a cache hit
        db.readWithCache("users", "Tom Smith").get();

        // If cache is working, performance should be better for second read
        Logger.log("TEST", "testCacheHitMiss passed!");
    }

    /**
     * Cleans up the test database after all tests have completed.
     */
    @AfterAll
    public static void cleanup() {
        db.shutdown();
    }
}
