package org.example.javafx_example.server;

public class Player {
    private String name;
    private int score;
    private int shots;
    
    public Player(String name) {
        this.name = name;
        this.score = 0;
        this.shots = 15;
    }
    
    public String getName() {
        return name;
    }
    
    public int getScore() {
        return score;
    }
    
    public void setScore(int score) {
        this.score = score;
    }
    
    public void addScore(int points) {
        score += points;
    }
    
    public int getShots() {
        return shots;
    }
    
    public void setShots(int shots) {
        this.shots = shots;
    }
    
    public void decrementShots() {
        if (shots > 0) {
            shots--;
        }
    }
} 