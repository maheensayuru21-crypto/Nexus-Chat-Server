package com.nexus;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class NexusDesktopClient extends Application {
    private PrintWriter out;
    private BufferedReader in;
    private TextArea chatArea;
    private TextField inputField;

    // New Authentication UI Components
    private VBox loginScreen;
    private VBox chatScreen;
    private TextField userField;
    private PasswordField passField;

    @Override
    public void start(Stage primaryStage) {
        // -- 1. BUILD THE LOGIN SCREEN --
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

        loginScreen = new VBox(15, titleLabel, userField, passField, btnBox);
        loginScreen.setAlignment(Pos.CENTER);
        // Fallback dark background in case CSS isn't loaded
        loginScreen.setStyle("-fx-background-color: #1e1e2e;"); 

        // -- 2. BUILD THE CHAT/BANKING SCREEN --
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        inputField = new TextField();
        inputField.setPromptText("Type a command like /balance or /transfer...");
        Button sendBtn = new Button("Send");

        HBox inputBox = new HBox(10, inputField, sendBtn);
        HBox.setHgrow(inputField, Priority.ALWAYS);

        chatScreen = new VBox(10, chatArea, inputBox);
        chatScreen.setPadding(new Insets(10));
        chatScreen.setVisible(false); // Hidden until authenticated

        // -- 3. ASSEMBLE THE MAIN LAYOUT --
        // StackPane allows us to put the chat screen behind the login screen
        StackPane root = new StackPane(chatScreen, loginScreen);
        Scene scene = new Scene(root, 650, 450);
        
        // Attach your dark theme CSS
        try {
            scene.getStylesheets().add(getClass().getResource("/nexus.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("Warning: nexus.css not found.");
        }

        primaryStage.setTitle("Nexus Terminal GUI");
        primaryStage.setScene(scene);
        primaryStage.show();

        // -- 4. BUTTON BEHAVIORS --
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
                        chatArea.appendText(finalResponse + "\n");
                        
                        // THE MAGIC: Switch screens if the server says we are authenticated
                        if (finalResponse.contains("Registration successful") || finalResponse.contains("Login successful")) {
                            loginScreen.setVisible(false);
                            chatScreen.setVisible(true);
                            inputField.requestFocus();
                        }
                    });
                }
            } catch (IOException e) {
                Platform.runLater(() -> chatArea.appendText("Connection error: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    private void sendAuthCommand(String command) {
        String u = userField.getText().trim();
        String p = passField.getText().trim();
        if (!u.isEmpty() && !p.isEmpty() && out != null) {
            // Secretly format the command just like the terminal version expects
            out.println(command + " " + u + " " + p);
        } else {
            // Simple validation alert if they leave fields blank
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter both Username and Password.");
            alert.show();
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && out != null) {
            chatArea.appendText("You: " + message + "\n");
            out.println(message);
            inputField.clear();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}