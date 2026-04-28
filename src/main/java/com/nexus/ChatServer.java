package com.nexus;

import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private static final int PORT = 5000;
    // This list keeps track of all connected clients
    private static CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Nexus Chat Server is live on port " + PORT);

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
        for (ClientHandler client : clients) {
            // We don't want to send the message back to the person who sent it
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private PrintWriter writer;
    private CopyOnWriteArrayList<ClientHandler> clients;

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

            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Relaying: " + message);
                // Call the static broadcast method in the Server
                ChatServer.broadcast(message, this);
                
                if (message.equalsIgnoreCase("exit")) break;
            }

        } catch (IOException e) {
            System.out.println("Connection lost with a client.");
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
            clients.remove(this);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}