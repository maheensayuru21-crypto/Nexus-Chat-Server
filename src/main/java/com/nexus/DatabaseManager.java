package com.nexus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
}