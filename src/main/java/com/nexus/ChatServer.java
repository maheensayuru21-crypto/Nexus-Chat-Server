package com.nexus;

import java.io.*;
import java.net.*;

public class ChatServer {
    public static void main(String[] args) {
        int port = 5000;

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Nexus Multi-User Server is running...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New user joined!");

                // Hire a new worker thread for this specific client
                ClientHandler clientThread = new ClientHandler(socket);
                new Thread(clientThread).start();
            }
        } catch (IOException e) {
            System.out.println("Server Error: " + e.getMessage());
        }
    }
}

// The Worker Class
class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {

            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Received: " + message);
                if (message.equalsIgnoreCase("exit")) break;
            }
            
            socket.close();
        } catch (IOException e) {
            System.out.println("Client handler error: " + e.getMessage());
        }
    }
}