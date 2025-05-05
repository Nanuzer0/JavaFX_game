package org.example.javafx_example.client;

import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientGameView {
    private AnchorPane root;
    private Rectangle shooterBase;
    private VBox playersBox;
    private Circle target1;
    private Circle target2;
    private Line track1;
    private Line track2;
    private Label statusLabel;
    private ClientGameController controller;
    
    // Для отслеживания стрел
    private Map<String, Map<String, Group>> playerArrows = new HashMap<>();
    
    // Доступные цвета для игроков
    private Color[] playerColors = {
        Color.BLUE, Color.GREEN, Color.ORANGE, Color.PURPLE
    };
    
    // Добавьте поля для кнопок
    private Button readyButton;
    private Button shootButton;
    private Button leaderboardButton;
    private VBox leaderboardBox;
    private boolean leaderboardVisible = false;
    
    public ClientGameView(ClientGameController controller) {
        this.controller = controller;
        createView();
    }
    
    private void createView() {
        root = new AnchorPane();
        root.setPrefSize(800, 600);
        
        // Желтая вертикальная полоса
        shooterBase = new Rectangle(0, 0, 72, 600);
        shooterBase.setFill(Color.YELLOW);
        
        // Линии движения мишеней
        track1 = new Line(400, 100, 400, 500);
        track1.setStroke(Color.GRAY);
        
        track2 = new Line(600, 100, 600, 500);
        track2.setStroke(Color.GRAY);
        
        // Мишени
        target1 = new Circle(400, 300, 30);
        target1.setFill(Color.RED);
        
        target2 = new Circle(600, 300, 15);
        target2.setFill(Color.RED);
        
        // Информационная панель
        VBox infoPanel = new VBox(10);
        infoPanel.setLayoutX(650);
        infoPanel.setLayoutY(50);
        infoPanel.setPadding(new Insets(10));
        
        Label titleLabel = new Label("Игроки:");
        titleLabel.setFont(new Font(18));
        
        playersBox = new VBox(5);
        ScrollPane scrollPane = new ScrollPane(playersBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        
        statusLabel = new Label("Ожидание начала игры...");
        statusLabel.setFont(new Font(14));
        
        infoPanel.getChildren().addAll(titleLabel, scrollPane, statusLabel);
        
        // Кнопки управления
        HBox controlsBox = new HBox(10);
        controlsBox.setLayoutX(80);
        controlsBox.setLayoutY(550);
        
        readyButton = new Button("Готов");
        readyButton.setOnAction(e -> controller.toggleReady());
        
        shootButton = new Button("Выстрел");
        shootButton.setOnAction(e -> controller.shoot());
        
        leaderboardButton = new Button("Таблица лидеров");
        leaderboardButton.setOnAction(e -> controller.requestLeaderboard());
        
        controlsBox.getChildren().addAll(readyButton, shootButton, leaderboardButton);
        
        // Добавляем все элементы на панель
        root.getChildren().addAll(shooterBase, track1, track2, target1, target2, infoPanel, controlsBox);
    }
    
    public void updateView(double target1Y, double target2Y, List<ClientGameController.PlayerInfo> playerInfos) {
        // Обновляем позиции мишеней
        target1.setCenterY(target1Y);
        target2.setCenterY(target2Y);
        
        // Обновляем информацию об игроках
        playersBox.getChildren().clear();
        
        // Очищаем фигуры игроков
        root.getChildren().removeIf(node -> node instanceof Polygon && !(node.getParent() instanceof Group));
        
        for (int i = 0; i < playerInfos.size(); i++) {
            ClientGameController.PlayerInfo player = playerInfos.get(i);
            Color playerColor = playerColors[i % playerColors.length];
            
            Label playerLabel = new Label(
                String.format("%s: %d очков, %d выстрелов", 
                    player.getName(), 
                    player.getScore(), 
                    player.getShots()
                )
            );
            
            playerLabel.setTextFill(playerColor);
            playersBox.getChildren().add(playerLabel);
            
            // Создаем треугольник игрока
            createPlayerTriangle(i, playerInfos.size(), playerColor);
        }
    }
    
    private void createPlayerTriangle(int playerIndex, int totalPlayers, Color color) {
        double yStep = 500.0 / (totalPlayers + 1);
        double yPos = yStep * (playerIndex + 1);
        
        Polygon triangle = new Polygon(
            0.0, -15.0,
            20.0, 0.0,
            0.0, 15.0
        );
        triangle.setFill(color);
        triangle.setLayoutX(50);
        triangle.setLayoutY(yPos);
        
        root.getChildren().add(triangle);
    }
    
    public void createArrow(String playerName, double startY, Color color, String arrowId) {
        // Создаем группу для стрелы (линия + наконечник)
        Group arrow = new Group();
        
        // Создаем линию стрелы
        Line arrowLine = new Line(0, 0, 20, 0);
        arrowLine.setStroke(color);
        arrowLine.setStrokeWidth(2);
        
        // Создаем наконечник стрелы
        Polygon arrowHead = new Polygon(
            0.0, 0.0,
            -5.0, -4.0,
            -5.0, 4.0
        );
        arrowHead.setFill(color);
        arrowHead.setLayoutX(20);
        
        // Добавляем компоненты в группу
        arrow.getChildren().addAll(arrowLine, arrowHead);
        
        // Устанавливаем начальную позицию стрелы
        arrow.setLayoutX(72);
        arrow.setLayoutY(startY);
        
        // Добавляем стрелу на сцену
        root.getChildren().add(arrow);
        
        // Сохраняем стрелу для игрока, используя ID стрелы
        if (!playerArrows.containsKey(playerName)) {
            playerArrows.put(playerName, new HashMap<>());
        }
        playerArrows.get(playerName).put(arrowId, arrow);
    }
    
    public void updateArrowPosition(String playerName, double x, double y, String arrowId) {
        if (playerArrows.containsKey(playerName) && playerArrows.get(playerName).containsKey(arrowId)) {
            Group arrow = playerArrows.get(playerName).get(arrowId);
            Platform.runLater(() -> {
                arrow.setLayoutX(x);
            });
        }
    }
    
    public void showHitEffect(double x, double y, int targetNum) {
        // Создаем эффект попадания (вспышка)
        Circle hitEffect = new Circle(x, y, targetNum == 1 ? 35 : 20);
        hitEffect.setFill(new Color(1, 1, 0, 0.7)); // Желтая вспышка с прозрачностью
        
        Platform.runLater(() -> {
            root.getChildren().add(hitEffect);
            
            // Запускаем анимацию исчезновения
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                    Platform.runLater(() -> root.getChildren().remove(hitEffect));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }
    
    public void clearArrows() {
        for (Map<String, Group> playerArrowsMap : playerArrows.values()) {
            for (Group arrow : new ArrayList<>(playerArrowsMap.values())) {
                Platform.runLater(() -> root.getChildren().remove(arrow));
            }
            playerArrowsMap.clear();
        }
    }
    
    public void updateGameStatus(String status) {
        statusLabel.setText(status);
    }
    
    public AnchorPane getRoot() {
        return root;
    }
    
    // Метод для получения цвета игрока по индексу
    public Color getPlayerColor(int playerIndex) {
        return playerColors[playerIndex % playerColors.length];
    }
    
    public void removeArrow(String arrowId) {
        // Проходим по всем игрокам и ищем стрелу с указанным ID
        for (String playerName : playerArrows.keySet()) {
            Map<String, Group> arrows = playerArrows.get(playerName);
            if (arrows.containsKey(arrowId)) {
                Group arrow = arrows.get(arrowId);
                Platform.runLater(() -> {
                    root.getChildren().remove(arrow);
                });
                arrows.remove(arrowId);
                return;
            }
        }
    }
    
    // Добавьте метод для обновления состояния кнопок
    public void updateButtonStates(boolean canToggleReady, boolean canShoot) {
        Platform.runLater(() -> {
            readyButton.setDisable(!canToggleReady);
            shootButton.setDisable(!canShoot);
            
            // Обновляем текст кнопки "Готов" в зависимости от состояния
            if (controller.isPaused() && canToggleReady) {
                readyButton.setText("Продолжить");
            } else if (controller.isGameRunning() && !controller.isPaused()) {
                readyButton.setText("Пауза");
            } else {
                readyButton.setText("Готов");
            }
        });
    }
    
    /**
     * Отображает таблицу лидеров
     */
    public void showLeaderboard(List<ClientGameController.LeaderboardEntry> entries) {
        if (leaderboardBox == null) {
            // Создаем панель для таблицы лидеров
            leaderboardBox = new VBox(5);
            leaderboardBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.85); -fx-padding: 10px;");
            leaderboardBox.setPrefWidth(300);
            leaderboardBox.setPrefHeight(400);
            
            // Располагаем в центре экрана
            AnchorPane.setTopAnchor(leaderboardBox, 100.0);
            AnchorPane.setLeftAnchor(leaderboardBox, 250.0);
            
            Label titleLabel = new Label("Таблица лидеров");
            titleLabel.setFont(new Font(20));
            titleLabel.setStyle("-fx-font-weight: bold;");
            
            Button closeButton = new Button("Закрыть");
            closeButton.setOnAction(e -> {
                root.getChildren().remove(leaderboardBox);
                leaderboardVisible = false;
            });
            
            leaderboardBox.getChildren().addAll(titleLabel);
            
            // Создаем заголовки таблицы
            HBox headerBox = new HBox(10);
            Label usernameHeader = new Label("Игрок");
            Label winsHeader = new Label("Победы");
            
            usernameHeader.setPrefWidth(200);
            winsHeader.setPrefWidth(50);
            
            usernameHeader.setStyle("-fx-font-weight: bold;");
            winsHeader.setStyle("-fx-font-weight: bold;");
            
            headerBox.getChildren().addAll(usernameHeader, winsHeader);
            leaderboardBox.getChildren().add(headerBox);
        } else {
            // Очищаем существующую таблицу, оставляя заголовок и шапку
            if (leaderboardBox.getChildren().size() > 2) {
                leaderboardBox.getChildren().remove(2, leaderboardBox.getChildren().size());
            }
        }
        
        // Добавляем строки с данными
        for (ClientGameController.LeaderboardEntry entry : entries) {
            HBox row = new HBox(10);
            Label usernameLabel = new Label(entry.getUsername());
            Label winsLabel = new Label(Integer.toString(entry.getWins()));
            
            usernameLabel.setPrefWidth(200);
            winsLabel.setPrefWidth(50);
            
            row.getChildren().addAll(usernameLabel, winsLabel);
            leaderboardBox.getChildren().add(row);
        }
        
        // Добавляем кнопку закрытия в конец
        Button closeButton = new Button("Закрыть");
        closeButton.setOnAction(e -> {
            root.getChildren().remove(leaderboardBox);
            leaderboardVisible = false;
        });
        leaderboardBox.getChildren().add(closeButton);
        
        // Добавляем таблицу на экран, если она еще не отображается
        if (!leaderboardVisible) {
            root.getChildren().add(leaderboardBox);
            leaderboardVisible = true;
        }
    }
} 