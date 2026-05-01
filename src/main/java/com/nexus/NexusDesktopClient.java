package com.nexus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class NexusDesktopClient extends Application {
    private PrintWriter out;
    private BufferedReader in;
    private TextArea chatArea;
    private TextField inputField;

    private VBox loginScreen;
    private VBox chatScreen;
    private TextField userField;
    private PasswordField passField;
    private Label errorLabel;

    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void start(Stage primaryStage) {
        // 1. Build Authentication Screen
        Label titleLabel = new Label("Nexus Security Gateway");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #a0a0b5;");
        
        userField = new TextField();
        userField.setPromptText("Username");
        userField.setMaxWidth(250);

        passField = new PasswordField();
        passField.setPromptText("Password");
        passField.setMaxWidth(250);

        Button loginBtn = new Button("Secure Login");
        Button registerBtn = new Button("Register New Account");

        HBox btnBox = new HBox(10, loginBtn, registerBtn);
        btnBox.setAlignment(Pos.CENTER);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff4c4c; -fx-font-weight: bold;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(250);
        errorLabel.setAlignment(Pos.CENTER);

        loginScreen = new VBox(15, titleLabel, userField, passField, btnBox, errorLabel);
        loginScreen.setAlignment(Pos.CENTER);
        
        // Fallback background color
        loginScreen.setStyle("-fx-background-color: #1e1e2e;"); 

        // 2. Build Main Interface
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        
        // Ghost Watermark Configuration
        Label watermark = new Label("NEXUS SECURE\nDeveloped by M4H33N");
        watermark.setTextAlignment(TextAlignment.CENTER);
        
        // Apply styling, opacity, and rotation
        watermark.setStyle("-fx-font-size: 50px; -fx-text-fill: rgba(160, 160, 181, 0.1); -fx-font-weight: bold;");
        watermark.setRotate(-25);
        
        // Enable mouse transparency to allow text selection beneath the label
        watermark.setMouseTransparent(true); 

        // Wrap chat area and watermark in a StackPane
        StackPane chatStack = new StackPane(chatArea, watermark);
        VBox.setVgrow(chatStack, Priority.ALWAYS);

        inputField = new TextField();
        inputField.setPromptText("Type a command like /balance or /transfer...");
        Button sendBtn = new Button("Send");

        HBox inputBox = new HBox(10, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        chatScreen = new VBox(10, chatStack, inputBox);
        chatScreen.setPadding(new Insets(10));
        chatScreen.setVisible(false);

        // 3. Assemble Main Layout
        StackPane root = new StackPane(chatScreen, loginScreen);
        Scene scene = new Scene(root, 650, 450);
        
        try {
            scene.getStylesheets().add(getClass().getResource("/nexus.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("Warning: nexus.css not found.");
        }

        primaryStage.setTitle("Nexus Terminal GUI");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 4. Input Actions
        loginBtn.setOnAction(e -> sendAuthCommand("/login"));
        registerBtn.setOnAction(e -> sendAuthCommand("/register"));
        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());

        connectToServer();
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                Socket socket = new Socket("localhost", 5000);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String response;
                while ((response = in.readLine()) != null) {
                    String finalResponse = response;
                    Platform.runLater(() -> {
                        // Intercept and discard the initial terminal instruction payload
                        if (finalResponse.contains("System: Use /register") || finalResponse.contains("OR /login")) {
                            return; 
                        }

                        // Message routing based on active screen visibility
                        if (finalResponse.contains("Registration successful") || finalResponse.contains("Login successful")) {
                            errorLabel.setText(""); 
                            loginScreen.setVisible(false);
                            chatScreen.setVisible(true);
                            inputField.requestFocus();
                            chatArea.appendText(getTimestamp() + finalResponse + "\n");
                        } else if (!chatScreen.isVisible()) {
                            // Route authentication errors to the error label
                            errorLabel.setText(finalResponse);
                        } else {
                            // Append standard messages to the main chat area
                            chatArea.appendText(getTimestamp() + finalResponse + "\n");
                        }
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    if (!chatScreen.isVisible()) {
                        errorLabel.setText("Server offline: " + e.getMessage());
                    } else {
                        chatArea.appendText(getTimestamp() + "Connection error: " + e.getMessage() + "\n");
                    }
                });
            }
        }).start();
    }

    private void sendAuthCommand(String command) {
        String u = userField.getText().trim();
        String p = passField.getText().trim();
        if (!u.isEmpty() && !p.isEmpty() && out != null) {
            out.println(command + " " + u + " " + p);
            // Reset error label state on new attempt
            errorLabel.setText(""); 
        } else {
            errorLabel.setText("Please enter both Username and Password.");
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && out != null) {
            // Prepend timestamp to outgoing messages
            chatArea.appendText(getTimestamp() + "You: " + message + "\n");
            out.println(message);
            inputField.clear();
        }
    }

    private String getTimestamp() {
        return "[" + LocalTime.now().format(timeFormat) + "] ";
    }

    public static void main(String[] args) {
        launch(args);
    }
}