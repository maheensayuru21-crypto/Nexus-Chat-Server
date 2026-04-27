package com.nexus;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String hostname = "localhost"; // "localhost" means your own computer
        int port = 5000;

        try (Socket socket = new Socket(hostname, port)) {
            System.out.println("Connected to Nexus Chat Server!");

            // Setup output stream to send data to the server
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);

            Scanner scanner = new Scanner(System.in);
            String text;

            System.out.println("Type your messages (type 'exit' to quit):");
            
            do {
                System.out.print("> ");
                text = scanner.nextLine();
                writer.println(text); // Send text to server
            } while (!text.equalsIgnoreCase("exit"));

            System.out.println("Closing connection...");
            
        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}