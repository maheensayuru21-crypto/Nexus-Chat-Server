package com.nexus;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.util.Base64;

public class DatabaseManager {
    // Update these credentials to match the Nexus Banking System database configuration
    private static final String URL = "jdbc:mysql://localhost:3306/bank_db?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "Mmsql@21"; 

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            if (conn != null) {
                return conn;
            }
        } catch (SQLException e) {
            // This will print the EXACT reason (wrong password, wrong port, etc.)
            System.err.println("[Critical]: Cannot reach MySQL at " + URL);
            System.err.println("[Reason]: " + e.getMessage());
        }
        return null;
    }

    /**
     * Queries the database for a specific account balance.
     */
    public static String getBalance(String username) {
        String query = "SELECT balance FROM accounts WHERE account_holder = ?";

        try (Connection conn = getConnection()) {
            if (conn == null) return "Bank System Error: Database offline.";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username); 
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    double balance = rs.getDouble("balance");
                    return "$" + String.format("%.2f", balance);
                } else {
                    return "No banking profile found for account holder: " + username;
                }
            }
        } catch (SQLException e) {
            return "Bank System Error: " + e.getMessage();
        }
    }

    /**
     * Safely transfers funds using SQL Transactions and logs the history.
     */
    public static String transferFunds(String sender, String recipient, double amount) {
        if (amount <= 0) return "Amount must be greater than zero.";
        if (isFrozen(sender)) return "Transfer failed: The sender account is FROZEN by an Admin.";
        if (isFrozen(recipient)) return "Transfer failed: The recipient account is FROZEN.";
        if (sender.equalsIgnoreCase(recipient)) return "Cannot transfer to the same account.";

        String checkSenderQuery = "SELECT balance FROM accounts WHERE account_holder = ?";
        String checkRecipientQuery = "SELECT 1 FROM accounts WHERE account_holder = ?";
        String withdrawQuery = "UPDATE accounts SET balance = balance - ? WHERE account_holder = ?";
        String depositQuery = "UPDATE accounts SET balance = balance + ? WHERE account_holder = ?";
        String logQuery = "INSERT INTO transactions (sender, recipient, amount) VALUES (?, ?, ?)";

        try (Connection conn = getConnection()) {
            if (conn == null) return "Database connection error: System offline.";
            
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
                    if (!rs.next()) return "Transfer failed: Sender account was not found.";
                    if (rs.getDouble("balance") < amount) return "Transfer failed: Insufficient funds.";
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

                // 5. Log the transaction into the ledger
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
        // Generate a random 4-digit account number
        String generatedAccNumber = String.valueOf((int)(Math.random() * 9000) + 1000);
        
        String sql = "INSERT INTO accounts (account_number, account_holder, balance, password) VALUES (?, ?, 0.00, ?)";
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.err.println("[Error]: Database connection failed during registration.");
                return false;
            }

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, generatedAccNumber);
                stmt.setString(2, username);
                stmt.setString(3, hashPassword(password));
                stmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            // Detailed error reporting for troubleshooting
            System.err.println("[Error]: Registration DB Error: " + e.getMessage()); 
            e.printStackTrace(); 
            return false; 
        }
    }

    /**
     * Authenticates a user by hashing the provided password and comparing it to the database record.
     */
    public static boolean authenticateUser(String username, String password) {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                System.err.println("[Error]: Database connection failed during authentication.");
                return false; 
            }

            String query = "SELECT password FROM accounts WHERE account_holder = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                // If user exists, hash the provided password and compare it to the stored hash
                if (rs.next()) {
                    String storedHash = rs.getString("password");
                    String inputHash = hashPassword(password);
                    return storedHash != null && storedHash.equals(inputHash);
                }
                return false; // User not found
            }
        } catch (SQLException e) {
            System.err.println("[Error]: Authentication DB Error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves the last 5 transactions for a specific user.
     */
    public static String getTransactionHistory(String username) {
        String query = "SELECT sender, recipient, amount, transfer_date FROM transactions " +
                       "WHERE sender = ? OR recipient = ? ORDER BY transfer_date DESC LIMIT 5";
        StringBuilder history = new StringBuilder("--- Recent Transactions ---\n");
        boolean hasHistory = false;

        try (Connection conn = getConnection()) {
            if (conn == null) return "Bank System Error: Database offline.";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                stmt.setString(2, username);
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    hasHistory = true;
                    String sender = rs.getString("sender");
                    String recipient = rs.getString("recipient");
                    double amount = rs.getDouble("amount");
                    String date = rs.getTimestamp("transfer_date").toString().substring(0, 16); 

                    if (sender.equals(username)) {
                        history.append("[").append(date).append("] SENT $").append(String.format("%.2f", amount)).append(" to ").append(recipient).append("\n");
                    } else {
                        history.append("[").append(date).append("] RECEIVED $").append(String.format("%.2f", amount)).append(" from ").append(sender).append("\n");
                    }
                }
                
                if (!hasHistory) return "No transaction history found.";
                return history.toString();
            }
        } catch (SQLException e) {
            return "Error retrieving history: " + e.getMessage();
        }
    }

    /**
     * Checks if an account is currently frozen.
     */
    public static boolean isFrozen(String username) {
        String query = "SELECT is_frozen FROM accounts WHERE account_holder = ?";
        try (Connection conn = getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getBoolean("is_frozen");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Admin command: Freezes or unfreezes an account.
     */
    public static String setFreezeStatus(String username, boolean freeze) {
        String query = "UPDATE accounts SET is_frozen = ? WHERE account_holder = ?";
        try (Connection conn = getConnection()) {
            if (conn == null) return "Database error: System offline.";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setBoolean(1, freeze);
                stmt.setString(2, username);
                int rows = stmt.executeUpdate();
                if (rows > 0) return freeze ? "Account '" + username + "' is now FROZEN." : "Account '" + username + "' is now UNFROZEN.";
                return "Account '" + username + "' not found.";
            }
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Admin command: Wipes a user's balance to zero.
     */
    public static String resetBalance(String username) {
        String query = "UPDATE accounts SET balance = 0.00 WHERE account_holder = ?";
        try (Connection conn = getConnection()) {
            if (conn == null) return "Database error: System offline.";

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                int rows = stmt.executeUpdate();
                if (rows > 0) return "Account '" + username + "' balance has been RESET to $0.00.";
                return "Account '" + username + "' not found.";
            }
        } catch (SQLException e) {
            return "Database error: " + e.getMessage();
        }
    }

    /**
     * Verifies if a specific username exists within the accounts table.
     */
    public static boolean userExists(String username) {
        String query = "SELECT 1 FROM accounts WHERE account_holder = ?";
        try (Connection conn = getConnection()) {
            if (conn == null) return false;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Inserts a direct message into the offline queue for pending delivery.
     */
    public static void saveOfflineMessage(String sender, String recipient, String message) {
        String query = "INSERT INTO offline_messages (sender, recipient, message) VALUES (?, ?, ?)";
        try (Connection conn = getConnection()) {
            if (conn == null) return;

            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, sender);
                stmt.setString(2, recipient);
                stmt.setString(3, message);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves all pending messages for a user and clears them from the queue via SQL transaction.
     */
    public static String getAndClearOfflineMessages(String username) {
        String selectQuery = "SELECT sender, message, timestamp FROM offline_messages WHERE recipient = ? ORDER BY timestamp ASC";
        String deleteQuery = "DELETE FROM offline_messages WHERE recipient = ?";
        StringBuilder messages = new StringBuilder();

        try (Connection conn = getConnection()) {
            if (conn == null) return "";

            conn.setAutoCommit(false);

            try (PreparedStatement selectStmt = conn.prepareStatement(selectQuery)) {
                selectStmt.setString(1, username);
                ResultSet rs = selectStmt.executeQuery();
                boolean hasMessages = false;
                
                while (rs.next()) {
                    if (!hasMessages) {
                        messages.append("\n--- Missed Messages ---\n");
                        hasMessages = true;
                    }
                    String sender = rs.getString("sender");
                    String text = rs.getString("message");
                    String time = rs.getTimestamp("timestamp").toString().substring(0, 16);
                    messages.append("[").append(time).append("] ").append(sender).append(" (offline): ").append(text).append("\n");
                }
                
                if (hasMessages) {
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
                        deleteStmt.setString(1, username);
                        deleteStmt.executeUpdate();
                    }
                }
                
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages.toString();
    }
}