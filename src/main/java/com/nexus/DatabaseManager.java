package com.nexus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.util.Base64;

public class DatabaseManager {
    // UPDATE THESE to match your Nexus Banking System database details
    private static final String URL = "jdbc:mysql://localhost:3306/bank_db"; 
    private static final String USER = "root";
    private static final String PASSWORD = "Mmsql@21"; 

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("Database Connection Failed: " + e.getMessage());
            return null;
        }
    }

    // Method to query the database for a user's balance
    public static String getBalance(String username) {
        String query = "SELECT balance FROM accounts WHERE account_holder = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username); 
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");
                return "$" + String.format("%.2f", balance);
            } else {
                return "No banking profile found for account holder: " + username;
            }

        } catch (SQLException e) {
            return "Bank System Error: " + e.getMessage();
        }
    }

    // Method to safely transfer funds using SQL Transactions AND log history
    public static String transferFunds(String sender, String recipient, double amount) {
        if (amount <= 0) return "Amount must be greater than zero.";
        if (sender.equalsIgnoreCase(recipient)) return "Cannot transfer to yourself.";

        String checkSenderQuery = "SELECT balance FROM accounts WHERE account_holder = ?";
        String checkRecipientQuery = "SELECT 1 FROM accounts WHERE account_holder = ?";
        String withdrawQuery = "UPDATE accounts SET balance = balance - ? WHERE account_holder = ?";
        String depositQuery = "UPDATE accounts SET balance = balance + ? WHERE account_holder = ?";
        // NEW: The query to log the transaction
        String logQuery = "INSERT INTO transactions (sender, recipient, amount) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false); 

            try {
                // 1. Check recipient
                try (PreparedStatement checkRecStmt = conn.prepareStatement(checkRecipientQuery)) {
                    checkRecStmt.setString(1, recipient);
                    ResultSet rs = checkRecStmt.executeQuery();
                    if (!rs.next()) return "Transfer failed: Recipient '" + recipient + "' not found.";
                }

                // 2. Check sender funds
                try (PreparedStatement checkSenderStmt = conn.prepareStatement(checkSenderQuery)) {
                    checkSenderStmt.setString(1, sender);
                    ResultSet rs = checkSenderStmt.executeQuery();
                    if (!rs.next()) return "Transfer failed: Your account was not found.";
                    if (rs.getDouble("balance") < amount) return "Transfer failed: Insufficient funds.";
                }

                // 3. Deduct from sender
                try (PreparedStatement withdrawStmt = conn.prepareStatement(withdrawQuery)) {
                    withdrawStmt.setDouble(1, amount); withdrawStmt.setString(2, sender); withdrawStmt.executeUpdate();
                }

                // 4. Add to recipient
                try (PreparedStatement depositStmt = conn.prepareStatement(depositQuery)) {
                    depositStmt.setDouble(1, amount); depositStmt.setString(2, recipient); depositStmt.executeUpdate();
                }

                // 5. NEW: Log the transaction into the ledger
                try (PreparedStatement logStmt = conn.prepareStatement(logQuery)) {
                    logStmt.setString(1, sender);
                    logStmt.setString(2, recipient);
                    logStmt.setDouble(3, amount);
                    logStmt.executeUpdate();
                }

                conn.commit(); 
                return "Successfully transferred $" + String.format("%.2f", amount) + " to " + recipient + ".";

            } catch (SQLException e) {
                conn.rollback(); 
                return "Transaction failed and rolled back: " + e.getMessage();
            }
        } catch (SQLException e) {
            return "Database connection error: " + e.getMessage();
        }
    }

    /**
     * Secures a plain text password using the SHA-256 cryptographic hash function.
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Registers a new user with a generated account number, starting balance, and a hashed password.
     */
    public static boolean registerUser(String username, String password) {
        // Generate a random 4-digit account number (e.g., 4012, 8921)
        String generatedAccNumber = String.valueOf((int)(Math.random() * 9000) + 1000);
        
        // We added account_number to the INSERT statement!
        String sql = "INSERT INTO accounts (account_number, account_holder, balance, password) VALUES (?, ?, 0.00, ?)";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, generatedAccNumber);
            stmt.setString(2, username);
            stmt.setString(3, hashPassword(password));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // We are also printing the real error to the server terminal so it never hides from us again!
            System.out.println("Registration DB Error: " + e.getMessage()); 
            return false; 
        }
    }

    /**
     * Authenticates a user by hashing the provided password and comparing it to the database.
     */
    public static boolean authenticateUser(String username, String password) {
        String sql = "SELECT password FROM accounts WHERE account_holder = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");
                return storedHash.equals(hashPassword(password));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieves the last 5 transactions for a specific user (both sent and received).
     */
    public static String getTransactionHistory(String username) {
        String query = "SELECT sender, recipient, amount, transfer_date FROM transactions " +
                       "WHERE sender = ? OR recipient = ? ORDER BY transfer_date DESC LIMIT 5";
        StringBuilder history = new StringBuilder("--- Recent Transactions ---\n");
        boolean hasHistory = false;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                hasHistory = true;
                String sender = rs.getString("sender");
                String recipient = rs.getString("recipient");
                double amount = rs.getDouble("amount");
                String date = rs.getTimestamp("transfer_date").toString().substring(0, 16); // Clean up the timestamp

                if (sender.equals(username)) {
                    history.append("[").append(date).append("] SENT $").append(String.format("%.2f", amount)).append(" to ").append(recipient).append("\n");
                } else {
                    history.append("[").append(date).append("] RECEIVED $").append(String.format("%.2f", amount)).append(" from ").append(sender).append("\n");
                }
            }
            
            if (!hasHistory) return "No transaction history found.";
            return history.toString();

        } catch (SQLException e) {
            return "Error retrieving history: " + e.getMessage();
        }
    }
}