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
}