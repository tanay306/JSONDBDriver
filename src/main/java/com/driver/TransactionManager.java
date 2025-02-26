package com.driver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages ACID transactions (commit & rollback).
 */
public class TransactionManager {
    private final Map<String, String> transactionLog = new HashMap<>();
    private boolean transactionActive = false;

    public void startTransaction() {
        transactionActive = true;
        transactionLog.clear();
        Logger.log("TRANSACTION", "Transaction started.");
    }

    public void commitTransaction() throws IOException {
        if (!transactionActive) {
            Logger.log("TRANSACTION", "No active transaction to commit.");
            return;
        }

        Logger.log("TRANSACTION", "Committing transaction...");
        for (Map.Entry<String, String> entry : transactionLog.entrySet()) {
            Files.write(new File(entry.getKey()).toPath(), entry.getValue().getBytes());
        }

        transactionLog.clear();
        transactionActive = false;
        Logger.log("TRANSACTION", "Transaction committed successfully.");
    }

    public void rollbackTransaction() {
        if (!transactionActive) {
            Logger.log("TRANSACTION", "No active transaction to rollback.");
            return;
        }

        transactionLog.clear();
        transactionActive = false;
        Logger.log("TRANSACTION", "Transaction rolled back.");
    }

    public void addToTransaction(String filePath, String data) {
        if (transactionActive) {
            transactionLog.put(filePath, data);
        }
    }

    public boolean isTransactionActive() {
        return transactionActive;
    }
}
