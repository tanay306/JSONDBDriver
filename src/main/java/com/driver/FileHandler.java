package com.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Handles file operations for reading/writing JSON data.
 */
public class FileHandler {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void writeToFile(File file, Object data) throws IOException {
        objectMapper.writeValue(file, data);
    }

    public static <T> T readFromFile(File file, Class<T> clazz) throws IOException {
        return objectMapper.readValue(file, clazz);
    }

    public static boolean fileExists(File file) {
        return file.exists();
    }

    public static void deleteFile(File file) {
        file.delete();
    }
}
