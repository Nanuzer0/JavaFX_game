package org.example.javafx_example.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private static final int PORT = 5555;
    private static final int MAX_PLAYERS = 4;
    
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(MAX_PLAYERS);
    private ServerGame game;
    private boolean isRunning = true;
    private boolean gameEnded = false;
    
    public GameServer() {
        this.game = new ServerGame(this);
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен на порту " + PORT);
            
            while (isRunning && clients.size() < MAX_PLAYERS) {
                System.out.println("Ожидание подключения игроков... (" + clients.size() + "/" + MAX_PLAYERS + ")");
                Socket clientSocket = serverSocket.accept();
                
                ClientHandler clientHandler = new ClientHandler(clientSocket, this, game);
                clients.add(clientHandler);
                pool.execute(clientHandler);
                
                System.out.println("Новый игрок подключен! Всего игроков: " + clients.size());
            }
            
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
            shutdown();
        }
    }
    
    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }
    
    public void broadcastGameState() {
        String gameState = game.getGameStateAsString();
        broadcast(gameState);
    }
    
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        game.removePlayer(client.getPlayerName());
        System.out.println("Игрок отключен. Осталось игроков: " + clients.size());
        
        // Если игра закончилась и все игроки отметили готовность, начинаем новую игру
        if (gameEnded) {
            checkAllPlayersReady();
        }
    }
    
    public boolean isNameTaken(String name) {
        for (ClientHandler client : clients) {
            if (client.getPlayerName() != null && client.getPlayerName().equals(name)) {
                return true;
            }
        }
        return false;
    }
    
    public void resetAllPlayersReady() {
        for (ClientHandler client : clients) {
            client.setReady(false);
        }
        // Сообщаем клиентам, что их статус готовности сброшен
        broadcast("RESET_READY");
    }
    
    public void checkAllPlayersReady() {
        if (clients.isEmpty()) return;
        
        boolean allReady = true;
        
        // Проверяем, все ли игроки готовы
        for (ClientHandler client : clients) {
            if (!client.isReady()) {
                allReady = false;
                break;
            }
        }
        
        // Если все готовы и игра была окончена, начинаем новую игру
        if (allReady) {
            gameEnded = false;
            game.startGame();
        } else if (gameEnded) {
            // Если не все готовы, но игра окончена, отправляем статус ожидания
            broadcast("WAITING_PLAYERS");
        }
    }
    
    public void setGameEnded() {
        gameEnded = true;
    }
    
    public void shutdown() {
        isRunning = false;
        pool.shutdown();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
} 