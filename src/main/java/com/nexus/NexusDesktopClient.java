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

import javafx.geometry.Pos; // for centering the watermark
import javafx.scene.layout.StackPane; // for layering
import javafx.scene.text.Font; // for styling the text
import javafx.scene.text.FontWeight; // for making it bold
import javafx.scene.control.Label; // for the text object itself
import javafx.scene.paint.Color; // for controlling transparency

public class NexusDesktopClient extends Application {

    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    
    // UI Elements
    private TextArea chatArea;
    private TextField inputField;

    @Override
    public void start(Stage primaryStage) {
        // --- Layer 1: The Watermark Background ---
        Label watermark = new Label("CREATED BY\nMaheen"); 
        
        // Style the watermark: Large font, bold, semi-transparent
        watermark.setFont(Font.font("Consolas", FontWeight.BOLD, 48));
        watermark.setTextFill(Color.rgb(200, 200, 200, 0.20));
        watermark.setStyle("-fx-text-alignment: center;");
        
        // Optional: Slightly rotate the watermark for a more stylistic look
        watermark.setRotate(-30);
        
        // --- Layer 2: The Original Chat UI (Refactored) ---
        // (This code remains largely the same, just organized differently)
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        inputField = new TextField();
        inputField.setPromptText("Type your message or a command like /balance...");
        inputField.setPrefWidth(400);

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        HBox bottomBar = new HBox(10, inputField, sendButton);
        bottomBar.setPadding(new Insets(10));

        BorderPane mainUI = new BorderPane();
        mainUI.setCenter(chatArea);
        mainUI.setBottom(bottomBar);
        

        chatArea.setOpacity(0.95); // Make chat text slightly see-through
        // Make the background of the mainUI transparent
        mainUI.setStyle("-fx-background-color: transparent;"); 

        // --- Layer 3: Assemble the Stack ---
        //create the new actual 'root' container
        StackPane root = new StackPane();
        
        //add the layers: Bottom layer first (the watermark), then the UI on top.
        root.getChildren().addAll(watermark, mainUI);
        
        // Ensure the watermark stays perfectly centered
        StackPane.setAlignment(watermark, Pos.CENTER);

        // --- Final Configuration ---
        Scene scene = new Scene(root, 600, 400);
        
        // Load the sleek dark theme CSS we already made
        // JavaFX is smart enough to handle nested transparency
        String cssPath = getClass().getResource("/nexus.css").toExternalForm();
        scene.getStylesheets().add(cssPath);

        primaryStage.setTitle("Nexus Terminal GUI");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> disconnect()); 
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        try {
            // Connect to the exact same server terminal client uses
            socket = new Socket("localhost", 5000);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            chatArea.appendText("System: Connected to Nexus Server on port 5000.\n");
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