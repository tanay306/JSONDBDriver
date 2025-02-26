package com.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.ArrayList;

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

            // ✅ Run insertions in parallel
            List<Callable<Void>> tasks = new ArrayList<>();
            for (User user : users) {
                tasks.add(() -> {
                    db.insertOrUpdate("users", user.name, user).get();
                    return null;
                });
            }

            // ✅ Use the new public method instead of accessing executorService
            db.executeTasks(tasks);

            Logger.log("INFO", "Fetching all users after insertion...");
            Future<List<String>> futureUsers = db.readAll("users");
            List<String> allUsers = futureUsers.get();

            for (String user : allUsers) {
                Logger.log("DATABASE", user);
            }

            Future<User> futureJohn = db.read("users", "John Doe");
            User john = futureJohn.get();
            john.company = "Amazon";
            db.insertOrUpdate("users", "John Doe", john).get();

            db.delete("users", "Jane Doe").get();
            db.shutdown();

        } catch (IOException | InterruptedException | ExecutionException e) {
            Logger.log("ERROR", "Failed to process JSON: " + e.getMessage());
        }
    }
}
