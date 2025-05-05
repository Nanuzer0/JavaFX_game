package org.example.javafx_example.server.database;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {
    
    @Id
    private String username;
    
    private int wins;
    
    // Конструктор по умолчанию для Hibernate
    public UserEntity() {
    }
    
    public UserEntity(String username) {
        this.username = username;
        this.wins = 0;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public int getWins() {
        return wins;
    }
    
    public void setWins(int wins) {
        this.wins = wins;
    }
    
    public void incrementWins() {
        this.wins++;
    }
    
    @Override
    public String toString() {
        return "UserEntity{" +
                "username='" + username + '\'' +
                ", wins=" + wins +
                '}';
    }
} 