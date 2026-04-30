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

    // Method to safely transfer funds using SQL Transactions
    public static String transferFunds(String sender, String recipient, double amount) {
        if (amount <= 0) return "Amount must be greater than zero.";
        if (sender.equalsIgnoreCase(recipient)) return "Cannot transfer to yourself.";

        String checkSenderQuery = "SELECT balance FROM accounts WHERE account_holder = ?";
        String checkRecipientQuery = "SELECT 1 FROM accounts WHERE account_holder = ?";
        String withdrawQuery = "UPDATE accounts SET balance = balance - ? WHERE account_holder = ?";
        String depositQuery = "UPDATE accounts SET balance = balance + ? WHERE account_holder = ?";

        // Get connection and start transaction
        try (Connection conn = getConnection()) {
            // Turn off auto-commit to start a manual transaction
            conn.setAutoCommit(false); 

            try {
                // 1. Check if recipient exists in the database
                try (PreparedStatement checkRecStmt = conn.prepareStatement(checkRecipientQuery)) {
                    checkRecStmt.setString(1, recipient);
                    ResultSet rs = checkRecStmt.executeQuery();
                    if (!rs.next()) {
                        return "Transfer failed: Recipient '" + recipient + "' not found.";
                    }
                }

                // 2. Check if sender has enough money
                try (PreparedStatement checkSenderStmt = conn.prepareStatement(checkSenderQuery)) {
                    checkSenderStmt.setString(1, sender);
                    ResultSet rs = checkSenderStmt.executeQuery();
                    if (!rs.next()) return "Transfer failed: Your account was not found.";
                    
                    double currentBalance = rs.getDouble("balance");
                    if (currentBalance < amount) return "Transfer failed: Insufficient funds.";
                }

                // 3. Deduct from sender
                try (PreparedStatement withdrawStmt = conn.prepareStatement(withdrawQuery)) {
                    withdrawStmt.setDouble(1, amount);
                    withdrawStmt.setString(2, sender);
                    withdrawStmt.executeUpdate();
                }

                // 4. Add to recipient
                try (PreparedStatement depositStmt = conn.prepareStatement(depositQuery)) {
                    depositStmt.setDouble(1, amount);
                    depositStmt.setString(2, recipient);
                    depositStmt.executeUpdate();
                }

                // If we reach here with no errors, save all changes!
                conn.commit(); 
                return "Successfully transferred $" + String.format("%.2f", amount) + " to " + recipient + ".";

            } catch (SQLException e) {
                // If ANYTHING goes wrong, undo everything!
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
}