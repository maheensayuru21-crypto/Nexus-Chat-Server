package com.nexus;

import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatServer {
    private static final int PORT = 5000;
    // This list keeps track of all connected clients
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Nexus Chat Server is live on port " + PORT);

            // --- TEST DB CONNECTION ---
            if (DatabaseManager.getConnection() != null) {
                System.out.println("Successfully linked to Nexus Banking Database!");
            } else {
                System.out.println("Warning: Running without database access.");
            }
            // --------------------------


            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New connection established!");

                ClientHandler handler = new ClientHandler(socket, clients);
                clients.add(handler); // Add to the list
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    // This method sends a message to EVERYONE in the list
    public static void broadcast(String message, ClientHandler sender) {
        // Create a format like 10:45 AM
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        String time = LocalTime.now().format(formatter);
        
        // Create the final stamped message
        String stampedMessage = "[" + time + "] " + message;

        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(stampedMessage);
            }
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter writer;
    private CopyOnWriteArrayList<ClientHandler> clients;
    private String clientName; // New field

    public ClientHandler(Socket socket, CopyOnWriteArrayList<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try {
            InputStream input = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            // --- AUTHENTICATION GATE ---
            this.sendMessage("System: Welcome to Nexus. Please authenticate.");
            this.sendMessage("System: Use /register <username> <password> OR /login <username> <password>");

            boolean isAuthenticated = false;
            String authMessage;

            while (!isAuthenticated && (authMessage = reader.readLine()) != null) {
                if (authMessage.startsWith("/register ") || authMessage.startsWith("/login ")) {
                    String[] parts = authMessage.split(" ");
                    if (parts.length == 3) {
                        String command = parts[0];
                        String uName = parts[1];
                        String pass = parts[2];

                        if (command.equalsIgnoreCase("/register")) {
                            if (DatabaseManager.registerUser(uName, pass)) {
                                this.clientName = uName;
                                isAuthenticated = true;
                                this.sendMessage("[Nexus Security]: Registration successful. Identity verified.");
                                
                                // Fetch and deliver offline messages upon successful authentication
                                String missedMessages = DatabaseManager.getAndClearOfflineMessages(this.clientName);
                                if (!missedMessages.isEmpty()) {
                                    this.sendMessage(missedMessages);
                                }
                            } else {
                                this.sendMessage("[Nexus Security]: Username taken. Try /login or a different name.");
                            }
                        } else if (command.equalsIgnoreCase("/login")) {
                            if (DatabaseManager.authenticateUser(uName, pass)) {
                                this.clientName = uName;
                                isAuthenticated = true;
                                this.sendMessage("[Nexus Security]: Login successful. Welcome back.");

                                // Fetch and deliver offline messages upon successful authentication
                                String missedMessages = DatabaseManager.getAndClearOfflineMessages(this.clientName);
                                if (!missedMessages.isEmpty()) {
                                    this.sendMessage(missedMessages);
                                }
                            } else {
                                this.sendMessage("[Nexus Security]: Access Denied. Invalid credentials.");
                            }
                        }
                    } else {
                        this.sendMessage("System: Invalid format. Use /login <user> <pass> or /register <user> <pass>");
                    }
                } else {
                    this.sendMessage("System: You must authenticate first. Use /login or /register.");
                }
            }

            // Authentication successful; proceed to standard routing
            System.out.println("User " + clientName + " has joined the Nexus.");
            ChatServer.broadcast("--- " + clientName + " joined the chat ---", this);

            String message;
            while ((message = reader.readLine()) != null) {
                if (message.equalsIgnoreCase("exit")) break;
                
                // --- THE INTERCEPTOR ---
                if (message.startsWith("/")) {
                    if (message.equalsIgnoreCase("/users")) {
                        StringBuilder userList = new StringBuilder("--- Active Nexus Users ---\n");
                        for (ClientHandler client : clients) {
                            userList.append("- ").append(client.getClientName()).append("\n");
                        }
                        this.sendMessage(userList.toString());
                    } 
                    // ---> NEW BANKING COMMAND <---
                    else if (message.equalsIgnoreCase("/balance")) {
                        this.sendMessage("[Nexus Bank]: Checking securely...");
                        String balanceResponse = DatabaseManager.getBalance(this.clientName);
                        this.sendMessage("[Nexus Bank]: " + balanceResponse);
                    }

                    // Private messaging and offline routing logic
                    else if (message.startsWith("/msg ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length >= 3) {
                            String targetUser = parts[1];
                            String text = parts[2];
                            boolean isOnline = false;

                            // Scan active client threads for the recipient
                            for (ClientHandler client : clients) { // Update 'clients' to your actual list name if different
                                if (client.clientName != null && client.clientName.equalsIgnoreCase(targetUser)) {
                                    client.sendMessage("[Private] " + this.clientName + ": " + text);
                                    this.sendMessage("[Private to " + targetUser + "]: " + text);
                                    isOnline = true;
                                    break;
                                }
                            }

                            // Route to database if recipient is not connected
                            if (!isOnline) {
                                if (DatabaseManager.userExists(targetUser)) {
                                    DatabaseManager.saveOfflineMessage(this.clientName, targetUser, text);
                                    this.sendMessage("[Server]: User '" + targetUser + "' is offline. Message queued for delivery.");
                                } else {
                                    this.sendMessage("[Server]: User '" + targetUser + "' does not exist.");
                                }
                            }
                        } else {
                            this.sendMessage("[Server]: Invalid syntax. Usage: /msg [username] [message]");
                        }
                    }

                    // ---> NEW HISTORY COMMAND <---
                    else if (message.equalsIgnoreCase("/history")) {
                        this.sendMessage("[Nexus Bank]: Fetching transaction ledger...");
                        String historyResponse = DatabaseManager.getTransactionHistory(this.clientName);
                        this.sendMessage(historyResponse);
                    }
                    // ---> ADMIN GOD MODE COMMANDS <---
                    else if (message.startsWith("/freeze ") && this.clientName.equalsIgnoreCase("admin")) {
                        String targetUser = message.split(" ")[1];
                        this.sendMessage("[Nexus Admin]: " + DatabaseManager.setFreezeStatus(targetUser, true));
                    }
                    else if (message.startsWith("/unfreeze ") && this.clientName.equalsIgnoreCase("admin")) {
                        String targetUser = message.split(" ")[1];
                        this.sendMessage("[Nexus Admin]: " + DatabaseManager.setFreezeStatus(targetUser, false));
                    }
                    else if (message.startsWith("/reset ") && this.clientName.equalsIgnoreCase("admin")) {
                        String targetUser = message.split(" ")[1];
                        this.sendMessage("[Nexus Admin]: " + DatabaseManager.resetBalance(targetUser));
                    }
                    // Block normal users from trying to use admin commands
                    else if ((message.startsWith("/freeze") || message.startsWith("/unfreeze") || message.startsWith("/reset")) && !this.clientName.equalsIgnoreCase("admin")) {
                        this.sendMessage("[Nexus Security]: Access Denied. Administrator privileges required.");
                    }

                    // ---> NEW TRANSFER COMMAND <---
                    else if (message.startsWith("/transfer ")) {
                        // Split into exactly 3 parts so the name can have spaces
                        String[] parts = message.split(" ", 3);
                        
                        if (parts.length < 3) {
                            this.sendMessage("System: Invalid format. Use /transfer [amount] [recipient]");
                        } else {
                            try {
                                double amount = Double.parseDouble(parts[1]);
                                String recipient = parts[2];
                                
                                this.sendMessage("[Nexus Bank]: Processing transfer...");
                                String result = DatabaseManager.transferFunds(this.clientName, recipient, amount);
                                this.sendMessage("[Nexus Bank]: " + result);

                                // BONUS: If the transfer works AND the recipient is online, notify them!
                                if (result.startsWith("Successfully")) {
                                    for (ClientHandler client : clients) {
                                        if (client.getClientName().equalsIgnoreCase(recipient)) {
                                            client.sendMessage("[Nexus Bank ALERTS]: You just received $" + String.format("%.2f", amount) + " from " + this.clientName + "!");
                                        }
                                    }
                                }

                            } catch (NumberFormatException e) {
                                this.sendMessage("System: Invalid amount. Please enter a valid number.");
                            }
                        }
                    }
                    
                    else {
                        this.sendMessage("System: Unknown command.");
                    }
                } 
                // --- NORMAL MESSAGE ---
                else {
                    ChatServer.broadcast("[" + clientName + "]: " + message, this);
                }
            }

        } catch (IOException e) {
            System.out.println(clientName + " disconnected.");
        } finally {
            cleanup();
        }
    }

    // Helper method to send a message to this specific client
    public void sendMessage(String message) {
        writer.println(message);
    }

    private void cleanup() {
        try {
            // Remove the user from the active list
            clients.remove(this);
            
            // Broadcast their departure to everyone else
            if (clientName != null) {
                ChatServer.broadcast("--- " + clientName + " has left the Nexus ---", this);
                System.out.println(clientName + " disconnected from the server.");
            }
            
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getClientName() {
        return this.clientName;
    
    

    }

    
}