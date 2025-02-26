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
 * Uses JUnit 5 to test the core database operations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensures tests run in order
public class JSONDatabaseTest {
    private static JSONDatabase db;
    private static final String TEST_DB_PATH = "./test_database"; // Test database directory

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
        futureInsert.get();  // Ensures the insert operation completes before assertion

        Future<User> futureUser = db.read("users", "Alice");
        User retrievedUser = futureUser.get();  // Ensures the read operation completes

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
        User user = futureUser.get();  // Ensures asynchronous read is completed

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
        List<String> users = futureUsers.get();  // Ensures async operation completes

        assertFalse(users.isEmpty()); // Ensures at least one user exists
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

        user.company = "Google"; // Modify the company name
        db.insertOrUpdate("users", "Alice", user).get();  // Ensure update is applied

        Future<User> futureUpdatedUser = db.read("users", "Alice");
        User updatedUser = futureUpdatedUser.get();  // Read updated user

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
        db.delete("users", "Alice").get();  // Ensures deletion completes

        Future<User> futureUser = db.read("users", "Alice");
        User user = futureUser.get();

        assertNull(user); // Ensures user is deleted
        Logger.log("TEST", "testDeleteUser passed!");
    }

    /**
     * Tests reading a non-existent user from the database.
     */
    @Test
    @Order(6)
    public void testReadMissingUser() throws ExecutionException, InterruptedException {
        Future<User> futureUser = db.read("users", "Bob");
        User user = futureUser.get();  // Ensures async read completes

        assertNull(user); // Bob should not exist
        Logger.log("TEST", "testReadMissingUser passed!");
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
                    file.delete(); // Deletes all files in the test database
                }
            }
            testDB.delete(); // Deletes the test database directory
        }
        Logger.log("TEST", "Database cleaned up after tests!");
        db.shutdown();
    }
}
