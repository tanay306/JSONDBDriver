package com.driver;

public class Logger {
    public static void log(String level, String message) {
        long threadId = Thread.currentThread().getId();
        System.out.println("[" + level + "] [Thread-" + threadId + "] " + message);
    }
}
