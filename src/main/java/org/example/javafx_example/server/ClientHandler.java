package org.example.javafx_example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private GameServer server;
    private ServerGame game;
    private PrintWriter out;
    private BufferedReader in;
    private String playerName;
    private boolean isReady = false;
    
    public ClientHandler(Socket socket, GameServer server, ServerGame game) {
        this.clientSocket = socket;
        this.server = server;
        this.game = game;
        
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void run() {
        try {
            // Первое сообщение должно быть именем игрока
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                processMessage(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Ошибка при работе с клиентом: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void processMessage(String message) {
        if (message.startsWith("NAME:")) {
            String name = message.substring(5);
            if (server.isNameTaken(name)) {
                sendMessage("ERROR:Имя уже занято");
            } else {
                this.playerName = name;
                game.addPlayer(name);
                sendMessage("NAME_ACCEPTED");
                server.broadcastGameState();
            }
        } else if (message.equals("READY")) {
            if (game.isPaused() && playerName.equals(game.getPauseRequestedBy())) {
                game.resumeGame(playerName);
            } else {
                isReady = true;
                server.checkAllPlayersReady();
            }
        } else if (message.equals("PAUSE")) {
            game.pauseGame(playerName);
        } else if (message.startsWith("SHOOT")) {
            game.handlePlayerShoot(playerName);
        } else if (message.equals("GAME_OVER_ACK")) {
            // Клиент подтвердил получение сообщения о конце игры
            server.setGameEnded();
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public boolean isReady() {
        return isReady;
    }
    
    public void setReady(boolean ready) {
        this.isReady = ready;
    }
    
    private void closeConnection() {
        try {
            server.removeClient(this);
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 