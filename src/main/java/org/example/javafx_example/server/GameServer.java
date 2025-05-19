package org.example.javafx_example.server;

import org.example.javafx_example.server.database.HibernateUtil;
import org.example.javafx_example.server.database.UserEntity;
import org.example.javafx_example.server.database.UserRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer {
    private static final int PORT = 5555;
    private static final int HTTP_PORT = 8080;
    private static final int MAX_PLAYERS = 4;
    
    private ServerSocket serverSocket;
    private ServerSocket httpServerSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(MAX_PLAYERS);
    private ServerGame game;
    private boolean isRunning = true;
    private boolean gameEnded = false;
    
    // Репозиторий для работы с пользователями
    private UserRepository userRepository;
    
    public GameServer() {
        this.game = new ServerGame(this);
        this.userRepository = new UserRepository();
        // Инициализируем Hibernate при запуске сервера
        HibernateUtil.getSessionFactory();
    }
    
    public void start() {
        try {
            // Запускаем основной игровой сервер
            serverSocket = new ServerSocket(PORT);
            System.out.println("Игровой сервер запущен на порту " + PORT);
            
            // Запускаем HTTP-сервер для Android-клиентов
            startHttpServer();
            
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
    
    private void startHttpServer() {
        new Thread(() -> {
            try {
                httpServerSocket = new ServerSocket(HTTP_PORT);
                System.out.println("HTTP-сервер запущен на порту " + HTTP_PORT);
                
                while (isRunning) {
                    Socket clientSocket = httpServerSocket.accept();
                    handleHttpRequest(clientSocket);
                }
            } catch (IOException e) {
                System.err.println("Ошибка HTTP-сервера: " + e.getMessage());
            }
        }).start();
    }
    
    private void handleHttpRequest(Socket clientSocket) {
        new Thread(() -> {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                
                String request = in.readLine();
                if (request != null && request.startsWith("GET /leaderboard")) {
                    // Отправляем заголовки HTTP-ответа
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: application/json");
                    out.println("Access-Control-Allow-Origin: *");
                    out.println();
                    
                    // Получаем и отправляем таблицу лидеров
                    List<UserEntity> leaderboard = getLeaderboard();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < leaderboard.size(); i++) {
                        UserEntity user = leaderboard.get(i);
                        json.append("{\"username\":\"").append(user.getUsername())
                            .append("\",\"wins\":").append(user.getWins()).append("}");
                        if (i < leaderboard.size() - 1) {
                            json.append(",");
                        }
                    }
                    json.append("]");
                    out.println(json.toString());
                } else {
                    // Отправляем ошибку 404
                    out.println("HTTP/1.1 404 Not Found");
                    out.println();
                }
                
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Ошибка при обработке HTTP-запроса: " + e.getMessage());
            }
        }).start();
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
    
    /**
     * Увеличивает количество побед для указанного игрока
     */
    public void incrementPlayerWins(String playerName) {
        try {
            userRepository.incrementUserWins(playerName);
            System.out.println("Игрок " + playerName + " получил победу в базе данных");
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении побед игрока: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Получает таблицу лидеров из базы данных
     */
    public List<UserEntity> getLeaderboard() {
        return userRepository.getLeaderboard();
    }
    
    /**
     * Отправляет таблицу лидеров указанному клиенту
     */
    public void sendLeaderboardToClient(ClientHandler client) {
        List<UserEntity> leaderboard = getLeaderboard();
        StringBuilder sb = new StringBuilder("LEADERBOARD:");
        
        for (UserEntity user : leaderboard) {
            sb.append(user.getUsername()).append(",")
              .append(user.getWins()).append(";");
        }
        
        client.sendMessage(sb.toString());
    }
    
    /**
     * Отправляет таблицу лидеров всем подключенным клиентам
     */
    public void broadcastLeaderboard() {
        List<UserEntity> leaderboard = getLeaderboard();
        StringBuilder sb = new StringBuilder("LEADERBOARD:");
        
        for (UserEntity user : leaderboard) {
            sb.append(user.getUsername()).append(",")
              .append(user.getWins()).append(";");
        }
        
        broadcast(sb.toString());
    }
    
    public void shutdown() {
        isRunning = false;
        pool.shutdown();
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (httpServerSocket != null) {
                httpServerSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Останавливаем Hibernate при выключении сервера
        HibernateUtil.shutdown();
    }
    
    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
} 