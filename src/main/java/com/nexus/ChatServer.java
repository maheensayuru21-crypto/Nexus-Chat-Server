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

            // The first message received is the username
            this.clientName = reader.readLine();
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