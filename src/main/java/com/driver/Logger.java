package com.driver;

/**
 * Utility class for logging messages with thread information.
 */
public class Logger {
    public static void log(String level, String message) {
        long threadId = Thread.currentThread().getId();
        System.out.println("[" + level + "] [Thread-" + threadId + "] " + message);
    }
}
