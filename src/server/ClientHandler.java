package server;
import common.Protocol;
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private ClientManager clientManager;
    private boolean isRunning = true;
    
    public ClientHandler(Socket socket, ClientManager clientManager) {
        this.socket = socket;
        this.clientManager = clientManager;
        
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.err.println("Error setting up streams: " + e.getMessage());
        }
    }
    
    @Override
    public void run() {
        try {
            // Handle login first
            if (!handleLogin()) {
                return;
            }
            
            // Main message loop
            String inputLine;
            while (isRunning && (inputLine = in.readLine()) != null) {
                handleMessage(inputLine);
            }
        } catch (IOException e) {
            System.err.println("Connection error with " + username + ": " + e.getMessage());
        } finally {
            cleanup();
        }
    }
    
    private boolean handleLogin() throws IOException {
        System.out.println("Waiting for login message...");
        
        String loginMessage = in.readLine();
        System.out.println("Debug - Received login message: '" + loginMessage + "'");
        
        if (loginMessage != null && loginMessage.startsWith(Protocol.LOGIN)) {
            String[] parts = loginMessage.split("\\" + Protocol.DELIMITER);
            System.out.println("Parts length: " + parts.length);
            
            if (parts.length == 2) {
                String requestedUsername = parts[1];
                System.out.println("Username requested: '" + requestedUsername + "'");
                
                synchronized (clientManager) {
                    if (!clientManager.isUsernameTaken(requestedUsername)) {
                        this.username = requestedUsername;
                        
                        // FIRST: Send login success message
                        String successMsg = Protocol.buildMessage(Protocol.LOGIN_SUCCESS, username);
                        System.out.println("Sending: " + successMsg);
                        sendMessage(successMsg);
                        
                        // THEN: Add client to manager (which broadcasts user list)
                        clientManager.addClient(username, this);
                        
                        System.out.println(username + " connected successfully");
                        return true;
                    } else {
                        String failMsg = Protocol.buildMessage(Protocol.LOGIN_FAILED, "Username taken");
                        System.out.println("Sending: " + failMsg);
                        sendMessage(failMsg);
                        return false;
                    }
                }
            }
        }
        String failMsg = Protocol.buildMessage(Protocol.LOGIN_FAILED, "Invalid login format");
        System.out.println("Sending: " + failMsg);
        sendMessage(failMsg);
        return false;
    }
    
    private void handleMessage(String message) {
        String[] parts = message.split("\\" + Protocol.DELIMITER);
        if (parts.length < 2) return;
        
        String command = parts[0];
        
        switch (command) {
            case Protocol.MESSAGE:
                if (parts.length >= 3) {
                    String target = parts[1];
                    String content = parts[2];
                    if (target.equalsIgnoreCase("all")) {
                        clientManager.broadcast(content, username);
                    } else {
                        clientManager.sendPrivate(username, target, content);
                    }
                }
                break;
                
            case Protocol.BROADCAST:
                if (parts.length >= 2) {
                    clientManager.broadcast(parts[1], username);
                }
                break;
                
            case Protocol.PRIVATE:
                if (parts.length >= 3) {
                    clientManager.sendPrivate(username, parts[1], parts[2]);
                }
                break;
                
            case Protocol.FILE_REQUEST:
                if (parts.length >= 4) {
                    String target = parts[1];
                    String filename = parts[2];
                    int filesize = Integer.parseInt(parts[3]);
                    clientManager.sendFile(username, target, filename, filesize);
                }
                break;
                
            case Protocol.FILE_DATA:
                if (parts.length >= 3) {
                    String target = parts[1];
                    ClientHandler targetClient = clientManager.getClient(target);
                    if (targetClient != null) {
                        targetClient.sendMessage(message);
                    }
                }
                break;
                
            case Protocol.DISCONNECT:
                isRunning = false;
                break;
                
            default:
                System.out.println("Unknown command from " + username + ": " + command);
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
        out.flush();
    }
    
    private void cleanup() {
        try {
            isRunning = false;
            if (username != null) {
                clientManager.removeClient(username);
                System.out.println(username + " disconnected");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}