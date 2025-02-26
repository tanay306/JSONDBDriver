package com.driver;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensures tests run in order
public class JSONDatabaseTest {
    private static JSONDatabase db;
    private static final String TEST_DB_PATH = "./test_database";

    @BeforeAll
    public static void setup() {
        db = new JSONDatabase(TEST_DB_PATH);
        Logger.log("TEST", "Database initialized for testing!");
    }

    @Test
    @Order(1)
    public void testInsertUser() throws IOException, ExecutionException, InterruptedException {
        User user = new User("Alice", "28", "9999999999", "Amazon",
                new Address("Seattle", "Washington", "USA", "98101"));

        Future<Void> futureInsert = db.insertOrUpdate("users", user.name, user);
        futureInsert.get();  // ✅ Ensure operation completes before asserting

        Future<User> futureUser = db.read("users", "Alice");
        User retrievedUser = futureUser.get();  // ✅ Wait for read operation

        assertNotNull(retrievedUser);
        assertEquals("Amazon", retrievedUser.company);
        Logger.log("TEST", "testInsertUser passed!");
    }

    @Test
    @Order(2)
    public void testReadUser() throws ExecutionException, InterruptedException {
        Future<User> futureUser = db.read("users", "Alice");
        User user = futureUser.get();  // ✅ Wait for async read operation

        assertNotNull(user);
        assertEquals("Amazon", user.company);
        Logger.log("TEST", "testReadUser passed!");
    }

    @Test
    @Order(3)
    public void testReadAllUsers() throws ExecutionException, InterruptedException {
        Future<List<String>> futureUsers = db.readAll("users");
        List<String> users = futureUsers.get();  // ✅ Wait for async operation

        assertFalse(users.isEmpty());
        Logger.log("TEST", "testReadAllUsers passed!");
    }

    @Test
    @Order(4)
    public void testUpdateUser() throws ExecutionException, InterruptedException {
        Future<User> futureUser = db.read("users", "Alice");
        User user = futureUser.get();
        assertNotNull(user);

        user.company = "Google";
        db.insertOrUpdate("users", "Alice", user).get();  // ✅ Ensure update is applied

        Future<User> futureUpdatedUser = db.read("users", "Alice");
        User updatedUser = futureUpdatedUser.get();  // ✅ Read updated user

        assertNotNull(updatedUser);
        assertEquals("Google", updatedUser.company);
        Logger.log("TEST", "testUpdateUser passed!");
    }

    @Test
    @Order(5)
    public void testDeleteUser() throws ExecutionException, InterruptedException {
        db.delete("users", "Alice").get();  // ✅ Ensure deletion is complete

        Future<User> futureUser = db.read("users", "Alice");
        User user = futureUser.get();

        assertNull(user);
        Logger.log("TEST", "testDeleteUser passed!");
    }

    @Test
    @Order(6)
    public void testReadMissingUser() throws ExecutionException, InterruptedException {
        Future<User> futureUser = db.read("users", "Bob");
        User user = futureUser.get();  // ✅ Wait for async operation

        assertNull(user);
        Logger.log("TEST", "testReadMissingUser passed!");
    }

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
