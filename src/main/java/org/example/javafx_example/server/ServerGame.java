package org.example.javafx_example.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGame {
    private GameServer server;
    private ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();
    private boolean isGameRunning = false;
    private boolean isPaused = false;
    
    private double target1Y = 300.0;
    private double target2Y = 300.0;
    private boolean target1MovingDown = true;
    private boolean target2MovingDown = true;
    private Thread target1Thread;
    private Thread target2Thread;
    
    // Размеры и позиции мишеней 
    private static final double TARGET1_RADIUS = 30.0;
    private static final double TARGET2_RADIUS = 15.0;
    private static final double TARGET1_X = 400.0;
    private static final double TARGET2_X = 600.0;
    
    // Размеры игрового поля
    private static final double FIELD_WIDTH = 800.0;
    private static final double FIELD_HEIGHT = 600.0;
    private static final double SHOOTER_BASE_WIDTH = 72.0;
    
    private String pauseRequestedBy = null;
    
    public ServerGame(GameServer server) {
        this.server = server;
        setupTargetThreads();
    }
    
    private void setupTargetThreads() {
        target1Thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (isGameRunning && !isPaused) {
                    moveTarget1();
                    server.broadcastGameState();
                }
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        target1Thread.setDaemon(true);
        
        target2Thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (isGameRunning && !isPaused) {
                    moveTarget2();
                    server.broadcastGameState();
                }
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        target2Thread.setDaemon(true);
    }
    
    private void moveTarget1() {
        double speed = 1.0;
        double minY = 100.0;
        double maxY = 500.0;
        
        if (target1MovingDown) {
            target1Y += speed;
            if (target1Y >= maxY) {
                target1MovingDown = false;
            }
        } else {
            target1Y -= speed;
            if (target1Y <= minY) {
                target1MovingDown = true;
            }
        }
    }
    
    private void moveTarget2() {
        double speed = 2.0;
        double minY = 100.0;
        double maxY = 500.0;
        
        if (target2MovingDown) {
            target2Y += speed;
            if (target2Y >= maxY) {
                target2MovingDown = false;
            }
        } else {
            target2Y -= speed;
            if (target2Y <= minY) {
                target2MovingDown = true;
            }
        }
    }
    
    public void startGame() {
        if (players.isEmpty()) return;
        
        // Сбрасываем счет и выстрелы для всех игроков
        for (Player player : players.values()) {
            player.setScore(0);
            player.setShots(15);
        }
        
        // Сбрасываем позиции мишеней
        target1Y = 300.0;
        target2Y = 300.0;
        target1MovingDown = true;
        target2MovingDown = true;
        
        isGameRunning = true;
        isPaused = false;
        
        // Запускаем потоки мишеней, если они еще не запущены
        if (!target1Thread.isAlive()) {
            target1Thread.start();
        }
        if (!target2Thread.isAlive()) {
            target2Thread.start();
        }
        
        server.broadcast("GAME_STARTED");
        server.broadcastGameState();
    }
    
    public void pauseGame(String playerName) {
        isPaused = true;
        pauseRequestedBy = playerName;
        server.broadcast("GAME_PAUSED:" + playerName);
        server.broadcastGameState();
    }
    
    public void resumeGame(String playerName) {
        // Только игрок, запросивший паузу, может возобновить игру
        if (playerName.equals(pauseRequestedBy)) {
            isPaused = false;
            pauseRequestedBy = null;
            server.broadcast("GAME_RESUMED");
            server.broadcastGameState();
        }
    }
    
    public void addPlayer(String name) {
        players.put(name, new Player(name));
    }
    
    public void removePlayer(String name) {
        players.remove(name);
    }
    
    public void handlePlayerShoot(String playerName) {
        if (!isGameRunning || isPaused) return;
        
        Player player = players.get(playerName);
        if (player == null || player.getShots() <= 0) return;
        
        player.decrementShots();
        
        // Расчет Y-позиции игрока для выстрела
        double arrowY = calculatePlayerPosition(playerName, players.size());
        
        // Генерируем уникальный ID для стрелы
        String arrowId = playerName + "_" + System.currentTimeMillis();
        
        // Сначала отправляем информацию о выстреле клиентам с ID стрелы
        server.broadcast("ARROW:" + playerName + ":" + arrowY + ":" + arrowId);
        server.broadcastGameState();
        
        // Создаем объект стрелы с начальной позицией
        Arrow arrow = new Arrow(playerName, SHOOTER_BASE_WIDTH, arrowY, arrowId);
        
        // Создаем поток для симуляции движения стрелы
        Thread arrowThread = new Thread(() -> {
            double arrowSpeed = 10.0;
            boolean hitTarget = false;
            
            while (!hitTarget && arrow.getX() < FIELD_WIDTH) {
                // Продвигаем стрелу вперед
                arrow.setX(arrow.getX() + arrowSpeed);
                
                // Точно так же уведомляем клиентов о новой позиции стрелы, добавляя ID стрелы
                server.broadcast("ARROW_POSITION:" + playerName + ":" + arrow.getX() + ":" + arrow.getY() + ":" + arrow.getId());
                
                // Проверяем попадание в первую мишень
                if (Math.abs(arrow.getX() - TARGET1_X) < arrowSpeed && 
                    checkHitTarget(TARGET1_X, target1Y, TARGET1_RADIUS, arrow.getY())) {
                    hitTarget = true;
                    player.addScore(1);
                    System.out.println("Игрок " + playerName + " попал в мишень 1! Счет: " + player.getScore());
                    
                    // Отправляем информацию о попадании с ID стрелы
                    server.broadcast("HIT:" + playerName + ":1:" + TARGET1_X + ":" + target1Y + ":" + arrow.getId());
                    
                    // Немедленно обновляем состояние игры
                    server.broadcastGameState();
                }
                
                // Проверяем попадание во вторую мишень
                else if (Math.abs(arrow.getX() - TARGET2_X) < arrowSpeed && 
                        checkHitTarget(TARGET2_X, target2Y, TARGET2_RADIUS, arrow.getY())) {
                    hitTarget = true;
                    player.addScore(2);
                    System.out.println("Игрок " + playerName + " попал в мишень 2! Счет: " + player.getScore());
                    
                    // Отправляем информацию о попадании с ID стрелы
                    server.broadcast("HIT:" + playerName + ":2:" + TARGET2_X + ":" + target2Y + ":" + arrow.getId());
                    
                    // Немедленно обновляем состояние игры
                    server.broadcastGameState();
                }
                
                try {
                    Thread.sleep(16); // ~60 fps
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            // Если стрела не попала, уведомляем клиентов с ID стрелы
            if (!hitTarget) {
                server.broadcast("MISS:" + playerName + ":" + arrow.getId());
            }
            
            // Проверка на победителя
            if (player.getScore() >= 6) {
                endGame(player.getName());
            }
            // Проверка на 0 выстрелов у всех
            int sum = 0;
            for (Player p : players.values()) {
                sum += p.getShots();
            }
            if (sum==0) {
                endGame("None");
            }
        });
        
        arrowThread.setDaemon(true);
        arrowThread.start();
    }
    
    private double calculatePlayerPosition(String playerName, int totalPlayers) {
        // Находим индекс игрока
        int playerIndex = 0;
        List<String> playerNames = new ArrayList<>(players.keySet());
        for (int i = 0; i < playerNames.size(); i++) {
            if (playerNames.get(i).equals(playerName)) {
                playerIndex = i;
                break;
            }
        }
        
        // Рассчитываем позицию Y для игрока
        double yStep = 500.0 / (totalPlayers + 1);
        return yStep * (playerIndex + 1);
    }
    
    private boolean checkHitTarget(double targetX, double targetY, double targetRadius, double arrowY) {
        // Точная проверка попадания
        return Math.abs(targetY - arrowY) <= targetRadius;
    }
    
    private void endGame(String winnerName) {
        isGameRunning = false;
        isPaused = false;
        
        // Сбрасываем статус готовности всех игроков
        server.resetAllPlayersReady();
        
        server.broadcast("GAME_OVER:" + winnerName);
    }
    
    public String getGameStateAsString() {
        StringBuilder state = new StringBuilder("GAME_STATE:");
        state.append(isGameRunning).append(":");
        state.append(isPaused).append(":");
        state.append(target1Y).append(":");
        state.append(target2Y).append(":");
        state.append(pauseRequestedBy == null ? "null" : pauseRequestedBy).append(":");
        
        state.append(players.size()).append(":");
        for (Player player : players.values()) {
            state.append(player.getName()).append(",");
            state.append(player.getScore()).append(",");
            state.append(player.getShots()).append(";");
        }
        
        return state.toString();
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public String getPauseRequestedBy() {
        return pauseRequestedBy;
    }
    
    // Внутренний класс для представления стрелы
    private static class Arrow {
        private String playerName;
        private double x;
        private double y;
        private String id;
        
        public Arrow(String playerName, double startX, double startY, String id) {
            this.playerName = playerName;
            this.x = startX;
            this.y = startY;
            this.id = id;
        }
        
        public double getX() {
            return x;
        }
        
        public void setX(double x) {
            this.x = x;
        }
        
        public double getY() {
            return y;
        }
        
        public String getId() {
            return id;
        }
    }
} 