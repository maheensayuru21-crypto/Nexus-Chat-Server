package com.nexus;

import java.io.*;
import java.net.*;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ChatClient {
    public static void main(String[] args) {
        String hostname = "localhost";
        int port = 5000;

        try (Socket socket = new Socket(hostname, port)) {
            System.out.println("Connected to Nexus Chat Server!");

            // THREAD 1: Listen for messages from the server
            new Thread(() -> {
                try {
                    InputStream input = socket.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                    String serverMessage;
                    while ((serverMessage = reader.readLine()) != null) {
                        System.out.println("\n" + serverMessage);
                        System.out.print("> "); 
                    }
                } catch (SocketException e) {
                    // This catches the expected disconnection when we type 'exit'
                    System.out.println("\nDisconnected from the Nexus.");
                } catch (IOException e) {
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            }).start();

            // THREAD 2: Handle user input (main thread)
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            Scanner scanner = new Scanner(System.in);
            String text;


            System.out.print("Enter your username for Nexus Chat: ");
            String username = scanner.nextLine();
            writer.println(username); // Send the name as the very first message

            System.out.println("Welcome, " + username + "! Type your messages (type 'exit' to quit):");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

            do {
                System.out.print("> ");
                text = scanner.nextLine();
                
                // Print the timestamp on your own screen
                String time = java.time.LocalTime.now().format(formatter);
                System.out.println("[" + time + "] You: " + text);
                
                writer.println(text);
            } while (!text.equalsIgnoreCase("exit"));

        } catch (IOException ex) {
            System.out.println("Client Error: " + ex.getMessage());
        }
    }
}