package org.example.javafx_example.client;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientGameController {
    private GameClient client;
    private ClientGameView gameView;
    private boolean isGameRunning = false;
    private boolean isPaused = false;
    private Map<String, PlayerInfo> players = new HashMap<>();
    private double target1Y = 300.0;
    private double target2Y = 300.0;
    private String myPlayerName;
    private String pauseRequestedBy = null;
    
    // Доступные цвета для игроков
    private Color[] playerColors = {
        Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE
    };
    
    public ClientGameController(GameClient client) {
        this.client = client;
        this.myPlayerName = client.getPlayerName();
        this.gameView = new ClientGameView(this);
    }
    
    public Pane getView() {
        return gameView.getRoot();
    }
    
    public void setGameStarted() {
        isGameRunning = true;
        isPaused = false;
        gameView.updateGameStatus("Игра началась!");
    }
    
    public void setGamePaused() {
        isPaused = true;
        gameView.updateGameStatus("Игра на паузе");
    }
    
    public void setGameResumed() {
        isPaused = false;
        gameView.updateGameStatus("Игра продолжается");
    }
    
    public void updateGameState(String stateMessage) {
        String[] parts = stateMessage.split(":");
        if (parts.length < 7) return;
        
        isGameRunning = Boolean.parseBoolean(parts[1]);
        isPaused = Boolean.parseBoolean(parts[2]);
        target1Y = Double.parseDouble(parts[3]);
        target2Y = Double.parseDouble(parts[4]);
        pauseRequestedBy = "null".equals(parts[5]) ? null : parts[5];
        
        int playerCount = Integer.parseInt(parts[6]);
        players.clear();
        
        if (parts.length > 7) {
            String[] playerInfos = parts[7].split(";");
            for (int i = 0; i < playerCount && i < playerInfos.length; i++) {
                String[] playerData = playerInfos[i].split(",");
                if (playerData.length >= 3) {
                    String name = playerData[0];
                    int score = Integer.parseInt(playerData[1]);
                    int shots = Integer.parseInt(playerData[2]);
                    players.put(name, new PlayerInfo(name, score, shots));
                }
            }
        }
        
        // Обновляем визуальное представление игры
        gameView.updateView(target1Y, target2Y, new ArrayList<>(players.values()));
        
        // Обновляем статус игры с информацией о счете
        PlayerInfo myInfo = players.get(myPlayerName);
        if (myInfo != null) {
            gameView.updateGameStatus(String.format(
                "Игра %s | Ваш счет: %d | Выстрелов: %d",
                isPaused ? "на паузе" : "идет",
                myInfo.getScore(),
                myInfo.getShots()
            ));
        }
        
        // Обновляем состояние кнопок
        updateButtonStates();
    }
    
    public void shoot() {
        if (!isGameRunning || isPaused) return;
        
        PlayerInfo myInfo = players.get(myPlayerName);
        if (myInfo == null || myInfo.getShots() <= 0) return;
        
        client.sendMessage("SHOOT");
    }
    
    public void toggleReady() {
        if (isPaused || !isGameRunning) {
            client.sendMessage("READY");
            gameView.updateGameStatus("Ожидание других игроков...");
        } else {
            client.sendMessage("PAUSE");
            gameView.updateGameStatus("Запрошена пауза");
        }
    }
    
    public void showGameOver(String winner) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Игра окончена");
            alert.setHeaderText("Победитель: " + winner);
            alert.setContentText("Для начала новой игры нажмите кнопку 'Готов'.");
            alert.showAndWait();
            
            isGameRunning = false;
            isPaused = false;
            gameView.updateGameStatus("Игра окончена. Победитель: " + winner);
            gameView.clearArrows();
            
            // Отправляем подтверждение о получении сообщения о конце игры
            client.sendMessage("GAME_OVER_ACK");
        });
    }
    
    public void processMessage(String message) {
        if (message.equals("RESET_READY")) {
            // Сервер сбросил статус готовности
            gameView.updateGameStatus("Игра окончена. Нажмите 'Готов' для новой игры.");
        } else if (message.equals("WAITING_PLAYERS")) {
            gameView.updateGameStatus("Ожидание готовности всех игроков...");
        } else if (message.startsWith("GAME_STATE:")) {
            updateGameState(message);
        } else if (message.equals("GAME_STARTED")) {
            setGameStarted();
        } else if (message.equals("GAME_PAUSED")) {
            setGamePaused();
        } else if (message.equals("GAME_RESUMED")) {
            setGameResumed();
        } else if (message.startsWith("GAME_OVER:")) {
            String winner = message.substring(10);
            showGameOver(winner);
        } else if (message.startsWith("ARROW:")) {
            // Создаем стрелу с ID
            String[] parts = message.split(":");
            if (parts.length >= 4) {  // Проверяем, что есть ID стрелы
                String playerName = parts[1];
                double arrowY = Double.parseDouble(parts[2]);
                String arrowId = parts[3];
                
                // Находим цвет для игрока
                int playerIndex = -1;
                List<String> playerNames = new ArrayList<>(players.keySet());
                for (int i = 0; i < playerNames.size(); i++) {
                    if (playerNames.get(i).equals(playerName)) {
                        playerIndex = i;
                        break;
                    }
                }
                
                if (playerIndex >= 0) {
                    Color playerColor = gameView.getPlayerColor(playerIndex);
                    gameView.createArrow(playerName, arrowY, playerColor, arrowId);
                }
            }
        } else if (message.startsWith("ARROW_POSITION:")) {
            // Обновляем позицию существующей стрелы по ID
            String[] parts = message.split(":");
            if (parts.length >= 5) {  // Учитываем ID стрелы
                String playerName = parts[1];
                double x = Double.parseDouble(parts[2]);
                double y = Double.parseDouble(parts[3]);
                String arrowId = parts[4];
                
                gameView.updateArrowPosition(playerName, x, y, arrowId);
            }
        } else if (message.startsWith("HIT:")) {
            // Информация о попадании с ID стрелы
            String[] parts = message.split(":");
            if (parts.length >= 6) {  // Учитываем ID стрелы
                String playerName = parts[1];
                int targetNum = Integer.parseInt(parts[2]);
                double x = Double.parseDouble(parts[3]);
                double y = Double.parseDouble(parts[4]);
                String arrowId = parts[5];
                
                gameView.showHitEffect(targetNum, x, y);
                gameView.removeArrow(playerName, arrowId);
            }
        } else if (message.startsWith("MISS:")) {
            // Информация о промахе с ID стрелы
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String playerName = parts[1];
                String arrowId = parts[2];
                gameView.removeArrow(playerName, arrowId);
            }
        } else if (message.startsWith("GAME_PAUSED:")) {
            // Обрабатываем сообщение о паузе с указанием игрока
            String pausedBy = message.substring(12);
            pauseRequestedBy = pausedBy;
            setGamePaused();
            updateButtonStates();
        } else if (message.equals("GAME_RESUMED")) {
            pauseRequestedBy = null;
            setGameResumed();
            updateButtonStates();
        }
    }
    
    private void updateButtonStates() {
        boolean canToggleReady;
        
        if (!isGameRunning) {
            // Если игра не запущена, все могут нажать "Готов"
            canToggleReady = true;
        } else if (isPaused) {
            // Если игра на паузе, только запросивший паузу может нажать "Продолжить"
            canToggleReady = myPlayerName.equals(pauseRequestedBy);
        } else {
            // Во время игры кнопка "Пауза" должна быть активна для всех
            canToggleReady = true;
        }
        
        gameView.updateButtonStates(canToggleReady, isGameRunning && !isPaused);
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    
    public boolean isGameRunning() {
        return isGameRunning;
    }
    
    public static class PlayerInfo {
        private String name;
        private int score;
        private int shots;
        
        public PlayerInfo(String name, int score, int shots) {
            this.name = name;
            this.score = score;
            this.shots = shots;
        }
        
        public String getName() {
            return name;
        }
        
        public int getScore() {
            return score;
        }
        
        public int getShots() {
            return shots;
        }
    }
} 