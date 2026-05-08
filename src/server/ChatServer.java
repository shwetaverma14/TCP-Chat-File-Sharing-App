package server;
import java.net.*;
import java.io.*;

public class ChatServer {
    private static final int PORT = 12345;
    private ServerSocket serverSocket;
    private ClientManager clientManager;
    private boolean isRunning;
    
    public ChatServer() {
        clientManager = new ClientManager();
        isRunning = true;
    }
    
    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("======================================");
            System.out.println("TCP Chat Server Started on port " + PORT);
            System.out.println("Waiting for clients to connect...");
            System.out.println("======================================");
            
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected from: " + 
                    clientSocket.getInetAddress().getHostAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket, clientManager);
                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }
    
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.start();
    }
}