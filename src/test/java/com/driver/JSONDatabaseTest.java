package com.driver;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class) // Ensures tests run in order
public class JSONDatabaseTest {
    private static JSONDatabase db;
    private static final String TEST_DB_PATH = "./test_database";

    @BeforeAll
    public static void setup() {
        db = new JSONDatabase(TEST_DB_PATH);
    }

    @Test
    @Order(1)
    public void testInsertUser() throws IOException {
        User user = new User("Alice", "28", "9999999999", "Amazon",
                new Address("Seattle", "Washington", "USA", "98101"));

        db.insertOrUpdate("users", user.name, user);
        assertNotNull(db.read("users", "Alice"));
        Logger.log("TEST", "testInsertUser passed!");
    }

    @Test
    @Order(2)
    public void testReadUser() throws IOException {
        User user = db.read("users", "Alice");
        assertNotNull(user);
        assertEquals("Amazon", user.company);
        Logger.log("TEST", "testReadUser passed!");
    }

    @Test
    @Order(3)
    public void testReadAllUsers() throws IOException {
        List<String> users = db.readAll("users");
        assertFalse(users.isEmpty());
        Logger.log("TEST", "testReadAllUsers passed!");
    }

    @Test
    @Order(4)
    public void testUpdateUser() throws IOException {
        User user = db.read("users", "Alice");
        assertNotNull(user);

        user.company = "Google";
        db.insertOrUpdate("users", "Alice", user);

        User updatedUser = db.read("users", "Alice");
        assertNotNull(updatedUser);
        assertEquals("Google", updatedUser.company);
        Logger.log("TEST", "testUpdateUser passed!");
    }

    @Test
    @Order(5)
    public void testDeleteUser() throws IOException {
        db.delete("users", "Alice");
        assertNull(db.read("users", "Alice"));
        Logger.log("TEST", "testDeleteUser passed!");
    }

    @Test
    @Order(6)
    public void testReadMissingUser() throws IOException {
        assertNull(db.read("users", "Bob"));
        Logger.log("TEST", "testReadMissingUser passed!");
    }

    @AfterAll
    public static void cleanup() {
        File testDB = new File(TEST_DB_PATH);
        if (testDB.exists()) {
            for (File file : testDB.listFiles()) {
                file.delete();
            }
            testDB.delete();
        }
        Logger.log("TEST", "Database cleaned up after tests!");
    }
}
