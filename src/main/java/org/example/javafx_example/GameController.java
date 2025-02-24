package org.example.javafx_example;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;

public class GameController {
    @FXML private AnchorPane gamePane;
    @FXML private Rectangle shooterBase;
    @FXML private Polygon shooter;
    @FXML private Circle target1;
    @FXML private Circle target2;
    @FXML private Label scoreLabel;
    @FXML private Label shotsLabel;
    @FXML private Line track1;
    @FXML private Line track2;

    private int score = 0;
    private int shots = 15;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private Thread target1Thread;
    private Thread target2Thread;
    private double target1Speed = 1.0;
    private double target2Speed = target1Speed*2;
    private boolean target1MovingDown = true;
    private boolean target2MovingDown = true;

    @FXML
    public void initialize() {
        setupTargetThreads();
    }

    private void setupTargetThreads() {
        // Поток для первой мишени
        target1Thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (isPlaying && !isPaused) {
                    Platform.runLater(() -> {
                        double newY = target1.getLayoutY() + (target1MovingDown ? target1Speed : -target1Speed);
                        if (newY >= track1.getEndY() || newY <= track1.getStartY()) {
                            target1MovingDown = !target1MovingDown;
                        }
                        target1.setLayoutY(newY);
                    });
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

        // Поток для второй мишени
        target2Thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (isPlaying && !isPaused) {
                    Platform.runLater(() -> {
                        double newY = target2.getLayoutY() + (target2MovingDown ? target2Speed : -target2Speed);
                        if (newY >= track2.getEndY() || newY <= track2.getStartY()) {
                            target2MovingDown = !target2MovingDown;
                        }
                        target2.setLayoutY(newY);
                    });
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

    @FXML
    private void shoot() {
        if (!isPlaying || isPaused || shots <= 0) return;
        
        shots--;
        shotsLabel.setText(String.valueOf(shots));
        
        // Создаем группу для стрелы (линия + наконечник)
        Group arrow = new Group();
        
        // Создаем линию стрелы
        Line arrowLine = new Line(0, 0, 20, 0);
        arrowLine.setStroke(javafx.scene.paint.Color.BLACK);
        arrowLine.setStrokeWidth(2);
        
        // Создаем наконечник стрелы
        Polygon arrowHead = new Polygon(
            0.0, 0.0,
            -5.0, -4.0,
            -5.0, 4.0
        );
        arrowHead.setFill(javafx.scene.paint.Color.BLACK);
        arrowHead.setLayoutX(20);
        
        // Добавляем компоненты в группу
        arrow.getChildren().addAll(arrowLine, arrowHead);
        
        // Устанавливаем начальную позицию стрелы
        arrow.setLayoutX(shooter.getLayoutX());
        arrow.setLayoutY(shooter.getLayoutY());
        
        gamePane.getChildren().add(arrow);
        
        // Создаем поток для анимации стрелы
        Thread arrowThread = new Thread(() -> {
            double arrowSpeed = 10;
            final boolean[] hitRegistered = {false}; // флаг для отслеживания попадания
            
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(16);
                    Platform.runLater(() -> {
                        arrow.setLayoutX(arrow.getLayoutX() + arrowSpeed);
                        
                        // Проверка попадания только если попадание еще не засчитано
                        if (!hitRegistered[0]) {
                            if (arrow.getBoundsInParent().intersects(target1.getBoundsInParent())) {
                                hitRegistered[0] = true;
                                score += 1;
                                scoreLabel.setText(String.valueOf(score));
                                gamePane.getChildren().remove(arrow);
                                Thread.currentThread().interrupt();
                            } else if (arrow.getBoundsInParent().intersects(target2.getBoundsInParent())) {
                                hitRegistered[0] = true;
                                score += 2;
                                scoreLabel.setText(String.valueOf(score));
                                gamePane.getChildren().remove(arrow);
                                Thread.currentThread().interrupt();
                            }
                        }
                        
                        // Удаляем стрелу, если она вылетела за пределы экрана
                        if (arrow.getLayoutX() > gamePane.getWidth()) {
                            gamePane.getChildren().remove(arrow);
                            Thread.currentThread().interrupt();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        arrowThread.setDaemon(true);
        arrowThread.start();
        
        // Если выстрелы закончились, останавливаем игру
        if (shots <= 0) {
            stopGame();
        }
    }

    @FXML
    private void startGame() {
        isPlaying = true;
        isPaused = false;
        score = 0;
        shots = 15;
        scoreLabel.setText("0");
        shotsLabel.setText("15");
        
        // Сброс позиций мишеней
        target1.setLayoutY(track1.getStartY());
        target2.setLayoutY(track2.getStartY());
        target1MovingDown = true;
        target2MovingDown = true;
        
        // Запускаем новые потоки
        if (target1Thread != null && target1Thread.isAlive()) {
            target1Thread.interrupt();
        }
        if (target2Thread != null && target2Thread.isAlive()) {
            target2Thread.interrupt();
        }
        setupTargetThreads();
        target1Thread.start();
        target2Thread.start();
    }

    @FXML
    private void stopGame() {
        isPlaying = false;
        if (target1Thread != null) {
            target1Thread.interrupt();
        }
        if (target2Thread != null) {
            target2Thread.interrupt();
        }
    }

    @FXML
    private void pauseGame() {
        isPaused = !isPaused;
    }
} 