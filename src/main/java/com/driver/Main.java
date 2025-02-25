package com.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;

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
            // Load users from JSON file
            Logger.log("INFO", "Reading users from JSON file...");
            User[] users = objectMapper.readValue(jsonFile, User[].class);
            Logger.log("SUCCESS", "Found " + users.length + " users in JSON.");

            // Insert Users
            for (User user : users) {
                db.insertOrUpdate("users", user.name, user);
            }

            // Read All Users
            Logger.log("INFO", "Fetching all users after insertion...");
            List<String> allUsers = db.readAll("users");
            for (String user : allUsers) {
                Logger.log("DATABASE", user);
            }

            // Read a Single User
            Logger.log("INFO", "Reading user: John Doe...");
            User john = db.read("users", "John Doe");
            if (john != null) {
                Logger.log("SUCCESS", "John Doe found: " + john.company);
            }

            // Update a User
            Logger.log("INFO", "Updating John Doe's company...");
            if (john != null) {
                john.company = "Amazon";
                db.insertOrUpdate("users", "John Doe", john);
                Logger.log("SUCCESS", "Updated John Doe's company to Amazon.");
            }

            // Read After Update
            Logger.log("INFO", "Verifying updated record...");
            User updatedJohn = db.read("users", "John Doe");
            if (updatedJohn != null) {
                Logger.log("SUCCESS", "John Doe's new company: " + updatedJohn.company);
            }

            // Delete a User
            Logger.log("INFO", "Deleting Jane Doe...");
            db.delete("users", "Jane Doe");

            // Fetch All Users After Deletion
            Logger.log("INFO", "Fetching all users after deletion...");
            List<String> usersAfterDelete = db.readAll("users");
            for (String user : usersAfterDelete) {
                Logger.log("DATABASE", user);
            }

        } catch (IOException e) {
            Logger.log("ERROR", "Failed to process JSON: " + e.getMessage());
        }
    }
}
