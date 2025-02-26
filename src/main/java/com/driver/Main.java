package com.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.ArrayList;

/**
 * Main class for demonstrating JSON database operations with ACID transactions and multi-threading.
 */
public class Main {
    public static void main(String[] args) {
        String dbPath = "./database";
        JSONDatabase db = new JSONDatabase(dbPath);
        ObjectMapper objectMapper = new ObjectMapper();

        File jsonFile = new File("users.json");
        if (!jsonFile.exists()) {
            Logger.log("ERROR", "users.json file not found! Ensure it's in the project root.");
            return;
        }

        try {
            Logger.log("INFO", "Reading users from JSON file...");
            User[] users = objectMapper.readValue(jsonFile, User[].class);
            Logger.log("SUCCESS", "Found " + users.length + " users in JSON.");

            // Perform Insertions in Parallel
            insertUsersInParallel(db, users);

            // Read all users
            readAllUsers(db);

            // Read and update a user
            updateUser(db, "John Doe", "Amazon");

            // Simulate transaction with rollback
            simulateTransaction(db);

            // Delete a user
            deleteUser(db, "Jane Doe");

        } catch (IOException | InterruptedException | ExecutionException e) {
            Logger.log("ERROR", "Failed to process JSON: " + e.getMessage());
        } finally {
            db.shutdown();
        }
    }

    /**
     * Inserts multiple users in parallel.
     */
    private static void insertUsersInParallel(JSONDatabase db, User[] users) throws InterruptedException {
        Logger.log("INFO", "Inserting users in parallel...");
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (User user : users) {
            tasks.add(() -> {
                db.insertOrUpdate("users", user.name, user).get();
                return null;
            });
        }

        db.executeTasks(tasks);
        Logger.log("SUCCESS", "All users inserted successfully!");
    }

    /**
     * Reads all users from the database and logs the results.
     */
    private static void readAllUsers(JSONDatabase db) throws InterruptedException, ExecutionException {
        Logger.log("INFO", "Fetching all users after insertion...");
        Future<List<String>> futureUsers = db.readAll("users");
        List<String> allUsers = futureUsers.get();

        for (String user : allUsers) {
            Logger.log("DATABASE", user);
        }
    }

    /**
     * Reads, updates, and re-inserts a user.
     */
    private static void updateUser(JSONDatabase db, String userName, String newCompany) throws InterruptedException, ExecutionException {
        Logger.log("INFO", "Updating user: " + userName + "...");
        Future<User> futureUser = db.read("users", userName);
        User user = futureUser.get();

        if (user != null) {
            user.company = newCompany;
            db.insertOrUpdate("users", userName, user).get();
            Logger.log("SUCCESS", userName + "'s company updated to " + newCompany);
        } else {
            Logger.log("ERROR", userName + " not found!");
        }
    }

    /**
     * Simulates a transaction with rollback in case of an error.
     */
    private static void simulateTransaction(JSONDatabase db) throws InterruptedException, ExecutionException {
        Logger.log("TRANSACTION", "Starting a new transaction...");
        db.startTransaction();

        try {
            User testUser = new User("TransactionUser", "35", "5551234567", "TestCorp",
                    new Address("New York", "NY", "USA", "10001"));

            db.insertOrUpdate("users", "TransactionUser", testUser).get();
            Logger.log("TRANSACTION", "Inserted TransactionUser");

            // Simulate an error condition
            if (true) { 
                throw new RuntimeException("Simulated error! Rolling back...");
            }

            db.commitTransaction();
            Logger.log("TRANSACTION", "Transaction committed successfully!");

        } catch (Exception e) {
            Logger.log("ERROR", "Transaction failed: " + e.getMessage());
            db.rollbackTransaction();
            Logger.log("TRANSACTION", "Transaction rolled back!");
        }
    }

    /**
     * Deletes a user from the database.
     */
    private static void deleteUser(JSONDatabase db, String userName) throws InterruptedException, ExecutionException {
        Logger.log("INFO", "Deleting user: " + userName + "...");
        db.delete("users", userName).get();
        Logger.log("SUCCESS", "User " + userName + " deleted successfully!");
    }
}
