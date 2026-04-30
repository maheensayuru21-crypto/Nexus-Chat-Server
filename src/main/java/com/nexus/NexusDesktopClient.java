package com.nexus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NexusDesktopClient extends Application {

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    
    // UI Elements
    private TextArea chatArea;
    private TextField inputField;

    @Override
    public void start(Stage primaryStage) {
        // 1. Setup the UI Components
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 14px;");

        inputField = new TextField();
        inputField.setPromptText("Type your message or a command like /balance...");
        inputField.setPrefWidth(400);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        // Pressing Enter also sends the message
        inputField.setOnAction(e -> sendMessage());

        HBox bottomBar = new HBox(10, inputField, sendButton);
        bottomBar.setPadding(new Insets(10));

        BorderPane root = new BorderPane();
        root.setCenter(chatArea);
        root.setBottom(bottomBar);

        // 2. Configure the Window
        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Nexus Terminal GUI");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> disconnect()); // Clean cleanup on exit
        primaryStage.show();

        // 3. Connect to the Server
        connectToServer();
    }

    private void connectToServer() {
        try {
            // Connect to the exact same server your terminal client uses
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            chatArea.appendText("System: Connected to Nexus Server on port 8080.\n");
            chatArea.appendText("System: Please enter your username to log in.\n");

            // Start a background thread to listen for incoming messages
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        final String msg = serverMessage;
                        // JavaFX rule: UI updates MUST happen on the application thread
                        Platform.runLater(() -> chatArea.appendText(msg + "\n"));
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> chatArea.appendText("System: Connection to server lost.\n"));
                }
            });
            listenerThread.setDaemon(true); // Allows thread to die when app closes
            listenerThread.start();

        } catch (IOException e) {
            chatArea.appendText("Error: Could not connect to server. Is it running?\n");
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            inputField.clear();
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}