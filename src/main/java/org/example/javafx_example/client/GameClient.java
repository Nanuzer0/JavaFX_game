package org.example.javafx_example.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class GameClient extends Application {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5555;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName;
    private ClientGameController gameController;
    private Stage primaryStage;
    
    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        // Окно для ввода имени
        showLoginDialog(primaryStage);
    }
    
    private void showLoginDialog(Stage primaryStage) {
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(20));
        
        Label nameLabel = new Label("Введите имя игрока:");
        TextField nameField = new TextField();
        Button connectButton = new Button("Подключиться");
        
        loginLayout.getChildren().addAll(nameLabel, nameField, connectButton);
        
        connectButton.setOnAction(e -> {
            playerName = nameField.getText().trim();
            if (playerName.isEmpty()) {
                showAlert("Ошибка", "Имя не может быть пустым");
                return;
            }
            
            try {
                connectToServer();
                sendMessage("NAME:" + playerName);
            } catch (IOException ex) {
                showAlert("Ошибка подключения", "Не удалось подключиться к серверу: " + ex.getMessage());
                return;
            }
        });
        
        Scene loginScene = new Scene(loginLayout, 300, 150);
        primaryStage.setTitle("Меткий стрелок - Вход");
        primaryStage.setScene(loginScene);
        primaryStage.show();
    }
    
    private void connectToServer() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        
        // Запускаем поток для приема сообщений от сервера
        new Thread(this::listenForMessages).start();
    }
    
    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                final String finalMessage = message;
                Platform.runLater(() -> {
                    if (finalMessage.equals("NAME_ACCEPTED")) {
                        // Закрываем окно входа и показываем игровое окно
                        primaryStage.close();
                        showGameWindow();
                    } else if (finalMessage.startsWith("ERROR:")) {
                        showAlert("Ошибка", finalMessage.substring(6));
                    } else if (gameController != null) {
                        // Передаем сообщение контроллеру только если он уже создан
                        gameController.processMessage(finalMessage);
                    }
                });
            }
        } catch (IOException e) {
            Platform.runLater(() -> showAlert("Ошибка соединения", "Соединение с сервером потеряно"));
        } finally {
            closeConnection();
        }
    }
    
    private void showGameWindow() {
        try {
            Stage gameStage = new Stage();
            gameController = new ClientGameController(this);
            gameStage.setTitle("Меткий стрелок - " + playerName);
            gameStage.setScene(new Scene(gameController.getView(), 800, 600));
            gameStage.setOnCloseRequest(e -> closeConnection());
            gameStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Ошибка", "Не удалось запустить игру: " + e.getMessage());
        }
    }
    
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void stop() {
        closeConnection();
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 