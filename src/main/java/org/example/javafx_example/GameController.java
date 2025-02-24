package org.example.javafx_example;

import javafx.animation.AnimationTimer;
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
    private AnimationTimer gameLoop;
    private double target1Speed = 0.5;
    private double target2Speed = target1Speed*2;
    private boolean target1MovingDown = true;
    private boolean target2MovingDown = true;

    @FXML
    public void initialize() {
        setupGameLoop();
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!isPaused) {
                    updateTargets();
                }
            }
        };
    }

    private void updateTargets() {
        // Движение первой мишени (ближней)
        double newY1 = target1.getLayoutY() + (target1MovingDown ? target1Speed : -target1Speed);
        if (newY1 >= track1.getEndY() || newY1 <= track1.getStartY()) {
            target1MovingDown = !target1MovingDown;
        }
        target1.setLayoutY(newY1);

        // Движение второй мишени (дальней)
        double newY2 = target2.getLayoutY() + (target2MovingDown ? target2Speed : -target2Speed);
        if (newY2 >= track2.getEndY() || newY2 <= track2.getStartY()) {
            target2MovingDown = !target2MovingDown;
        }
        target2.setLayoutY(newY2);
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
        
        // Анимация полета стрелы
        AnimationTimer arrowAnimation = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double arrowSpeed = 5;
                arrow.setLayoutX(arrow.getLayoutX() + arrowSpeed);
                
                // Проверка попадания
                if (arrow.getBoundsInParent().intersects(target1.getBoundsInParent())) {
                    score += 1;
                    scoreLabel.setText(String.valueOf(score));
                    gamePane.getChildren().remove(arrow);
                    this.stop();
                } else if (arrow.getBoundsInParent().intersects(target2.getBoundsInParent())) {
                    score += 2;
                    scoreLabel.setText(String.valueOf(score));
                    gamePane.getChildren().remove(arrow);
                    this.stop();
                }
                
                // Удаляем стрелу, если она вылетела за пределы экрана
                if (arrow.getLayoutX() > gamePane.getWidth()) {
                    gamePane.getChildren().remove(arrow);
                    this.stop();
                }
            }
        };
        arrowAnimation.start();
        
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
        
        gameLoop.start();
    }

    @FXML
    private void stopGame() {
        isPlaying = false;
        gameLoop.stop();
    }

    @FXML
    private void pauseGame() {
        isPaused = !isPaused;
    }
} 