package com.nexus;

import java.io.*;
import java.net.*;
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
                        System.out.print("> "); // Keep the prompt visible
                    }
                } catch (IOException e) {
                    System.out.println("Error reading from server: " + e.getMessage());
                }
            }).start();

            // THREAD 2: Handle user input (main thread)
            OutputStream output = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(output, true);
            Scanner scanner = new Scanner(System.in);
            String text;

            System.out.println("Type your messages (type 'exit' to quit):");
            do {
                System.out.print("> ");
                text = scanner.nextLine();
                writer.println(text);
            } while (!text.equalsIgnoreCase("exit"));

        } catch (IOException ex) {
            System.out.println("Client Error: " + ex.getMessage());
        }
    }
}