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
 * Uses JUnit 5 to test the core database operations, including transactions.
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
    public void testInsertUser() throws IOException, ExecutionException, InterruptedException {
        User user = new User("Alice", "28", "9999999999", "Amazon",
                new Address("Seattle", "Washington", "USA", "98101"));

        Future<Void> futureInsert = db.insertOrUpdate("users", user.name, user);
        futureInsert.get();

        Future<User> futureUser = db.read("users", "Alice");
        User retrievedUser = futureUser.get(); 

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
        Future<User> futureUser = db.read("users", "Alice");
        User user = futureUser.get();
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
        Future<List<String>> futureUsers = db.readAll("users");
        List<String> users = futureUsers.get();

        assertFalse(users.isEmpty());
        Logger.log("TEST", "testReadAllUsers passed!");
    }

    /**
     * Tests updating an existing user's data.
     */
    @Test
    @Order(4)
    public void testUpdateUser() throws ExecutionException, InterruptedException {
        Future<User> futureUser = db.read("users", "Alice");
        User user = futureUser.get();
        assertNotNull(user);

        user.company = "Google";
        db.insertOrUpdate("users", "Alice", user).get();

        Future<User> futureUpdatedUser = db.read("users", "Alice");
        User updatedUser = futureUpdatedUser.get();

        assertNotNull(updatedUser);
        assertEquals("Google", updatedUser.company);
        Logger.log("TEST", "testUpdateUser passed!");
    }

    /**
     * Tests deleting an existing user from the database.
     */
    @Test
    @Order(5)
    public void testDeleteUser() throws ExecutionException, InterruptedException {
        db.delete("users", "Alice").get();

        Future<User> futureUser = db.read("users", "Alice");
        User user = futureUser.get();

        assertNull(user);
        Logger.log("TEST", "testDeleteUser passed!");
    }

    /**
     * Tests reading a non-existent user from the database.
     */
    @Test
    @Order(6)
    public void testReadMissingUser() throws ExecutionException, InterruptedException {
        Future<User> futureUser = db.read("users", "Bob");
        User user = futureUser.get();

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

        Future<User> futureUser = db.read("users", "TransactionUser");
        User retrievedUser = futureUser.get();

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

        Future<User> futureUser = db.read("users", "RollbackUser");
        User retrievedUser = futureUser.get();

        assertNull(retrievedUser);
        Logger.log("TEST", "testTransactionRollback passed!");
    }

    /**
     * Cleans up the test database after all tests have completed.
     */
    @AfterAll
    public static void cleanup() {
        File testDB = new File(TEST_DB_PATH);
        if (testDB.exists()) {
            for (File file : testDB.listFiles()) {
                if (file.isFile()) {
                    file.delete();
                }
            }
            testDB.delete();
        }
        Logger.log("TEST", "Database cleaned up after tests!");
        db.shutdown();
    }
}
