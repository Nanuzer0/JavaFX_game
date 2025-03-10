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
        
        controlsBox.getChildren().addAll(readyButton, shootButton);
        
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
    
    public void showHitEffect(int targetNum, double x, double y) {
        Platform.runLater(() -> {
            Circle targetCircle = targetNum == 1 ? target1 : target2;
            Circle hitEffect = new Circle(targetCircle.getCenterX(), y, targetNum == 1 ? 30 : 15);
            hitEffect.setFill(Color.ORANGE.deriveColor(1, 1, 1, 0.7));
            hitEffect.setStroke(Color.YELLOW);
            hitEffect.setStrokeWidth(2);
            
            root.getChildren().add(hitEffect);
            
            // Анимация исчезновения эффекта
            new Thread(() -> {
                try {
                    Thread.sleep(500);
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
    
    public void removeArrow(String playerName, String arrowId) {
        if (playerArrows.containsKey(playerName) && playerArrows.get(playerName).containsKey(arrowId)) {
            Group arrow = playerArrows.get(playerName).get(arrowId);
            Platform.runLater(() -> {
                root.getChildren().remove(arrow);
                playerArrows.get(playerName).remove(arrowId);
            });
        }
    }
    
    // Для обратной совместимости
    public void removeArrow(String playerName) {
        if (playerArrows.containsKey(playerName)) {
            Map<String, Group> arrowsMap = playerArrows.get(playerName);
            for (Group arrow : new ArrayList<>(arrowsMap.values())) {
                Platform.runLater(() -> {
                    root.getChildren().remove(arrow);
                });
            }
            arrowsMap.clear();
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
} 