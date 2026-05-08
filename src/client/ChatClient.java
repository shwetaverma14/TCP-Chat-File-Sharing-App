package client;

import common.Protocol;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatClient {
    private String serverAddress;
    private int port;
    private String username;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private Scanner scanner;
    
    // File transfer variables
    private FileOutputStream fileOutputStream;
    private String currentFileName;
    private long currentFileSize;
    private long bytesReceived;
    
    // For thread-safe file acceptance
    private boolean waitingForFileResponse = false;
    private String pendingSender;
    private String pendingFilename;
    private int pendingFileSize;
    
    public ChatClient(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.scanner = new Scanner(System.in);
    }
    
    public void start() {
        try {
            System.out.println("Connecting to server at " + serverAddress + ":" + port + "...");
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            System.out.println("Connected to server!");
            
            if (!login()) {
                System.out.println("Failed to login. Exiting...");
                return;
            }
            
            connected = true;
            printHelp();
            
            Thread receiverThread = new Thread(new MessageReceiver());
            receiverThread.start();
            
            handleUserInput();
            
        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
            System.err.println("Make sure the server is running on port " + port);
        } finally {
            cleanup();
        }
    }
    
    private void printHelp() {
        System.out.println("\n==============================================");
        System.out.println("Connected to chat server as: " + username);
        System.out.println("==============================================");
        System.out.println("Commands:");
        System.out.println("  /msg <user> <message> - Private message");
        System.out.println("  /all <message> - Broadcast to all");
        System.out.println("  /file <user> <filepath> - Send file");
        System.out.println("  /quit - Exit chat");
        System.out.println("==============================================\n");
    }
    
    private boolean login() {
        try {
            System.out.print("Enter your username: ");
            username = scanner.nextLine().trim();
            
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty!");
                return false;
            }
            
            String loginMessage = Protocol.buildMessage(Protocol.LOGIN, username);
            out.println(loginMessage);
            out.flush();
            
            String response = in.readLine();
            
            if (response != null && response.startsWith(Protocol.LOGIN_SUCCESS)) {
                System.out.println("Login successful!");
                return true;
            } else if (response != null && response.startsWith(Protocol.LOGIN_FAILED)) {
                System.out.println("Login failed: Username already taken");
                return false;
            }
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Login error: " + e.getMessage());
        }
        return false;
    }
    
    private void handleUserInput() {
        while (connected) {
            if (waitingForFileResponse) {
                System.out.print("Accept file? (yes/no): ");
            }
            
            String input = scanner.nextLine();
            
            if (waitingForFileResponse) {
                if (input.equalsIgnoreCase("yes")) {
                    prepareFileReceive(pendingSender, pendingFilename, pendingFileSize);
                    System.out.println("Receiving file...");
                } else {
                    System.out.println("File rejected");
                }
                waitingForFileResponse = false;
                pendingSender = null;
                pendingFilename = null;
                continue;
            }
            
            if (input.startsWith("/quit")) {
                sendMessage(Protocol.buildMessage(Protocol.DISCONNECT));
                connected = false;
                break;
            } 
            else if (input.startsWith("/msg")) {
                String[] parts = input.split(" ", 3);
                if (parts.length == 3) {
                    sendPrivateMessage(parts[1], parts[2]);
                } else {
                    System.out.println("Usage: /msg <username> <message>");
                }
            }
            else if (input.startsWith("/all")) {
                String message = input.substring(4).trim();
                if (!message.isEmpty()) {
                    sendBroadcast(message);
                } else {
                    System.out.println("Usage: /all <message>");
                }
            }
            else if (input.startsWith("/file")) {
                String[] parts = input.split(" ", 3);
                if (parts.length == 3) {
                    sendFile(parts[1], parts[2]);
                } else {
                    System.out.println("Usage: /file <username> <filepath>");
                }
            }
            else if (!input.isEmpty()) {
                sendBroadcast(input);
            }
        }
    }
    
    private void sendPrivateMessage(String target, String message) {
        String formattedMessage = Protocol.buildMessage(Protocol.MESSAGE, target, message);
        sendMessage(formattedMessage);
    }
    
    private void sendBroadcast(String message) {
        String formattedMessage = Protocol.buildMessage(Protocol.BROADCAST, message);
        sendMessage(formattedMessage);
    }
    
    private void sendMessage(String message) {
        if (out != null) {
            out.println(message);
            out.flush();
        }
    }
    
    private void sendFile(String targetUser, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File not found: " + filePath);
            return;
        }
        
        try {
            String fileRequest = Protocol.buildMessage(Protocol.FILE_REQUEST, targetUser, 
                file.getName(), String.valueOf(file.length()));
            sendMessage(fileRequest);
            
            System.out.println("File request sent to " + targetUser);
            System.out.println("Sending file: " + file.getName() + " (" + file.length() + " bytes)");
            
            Thread.sleep(500);
            
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[Protocol.BUFFER_SIZE];
            int bytesRead;
            int totalSent = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                String dataChunk = Base64.getEncoder().encodeToString(Arrays.copyOf(buffer, bytesRead));
                String dataMessage = Protocol.buildMessage(Protocol.FILE_DATA, targetUser, dataChunk);
                sendMessage(dataMessage);
                totalSent += bytesRead;
                
                int progress = (int) ((totalSent * 100) / file.length());
                System.out.print("\rProgress: " + progress + "%");
            }
            
            fis.close();
            System.out.println("\nFile sent successfully!");
            
        } catch (Exception e) {
            System.err.println("\nError sending file: " + e.getMessage());
        }
    }
    
    private void prepareFileReceive(String sender, String filename, int fileSize) {
        try {
            String savePath = "received_" + filename;
            fileOutputStream = new FileOutputStream(savePath);
            currentFileName = filename;
            currentFileSize = fileSize;
            bytesReceived = 0;
            
            System.out.println("Saving to: " + savePath);
        } catch (IOException e) {
            System.err.println("Error preparing to receive file: " + e.getMessage());
        }
    }
    
    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while (connected && (message = in.readLine()) != null) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("\nConnection lost to server");
                    connected = false;
                }
            }
        }
        
        private void handleServerMessage(String message) {
            String[] parts = message.split("\\" + Protocol.DELIMITER);
            if (parts.length < 1) return;
            
            String command = parts[0];
            
            switch (command) {
                case Protocol.BROADCAST:
                    if (parts.length >= 3) {
                        System.out.println("\n[Broadcast] " + parts[1] + ": " + parts[2]);
                        System.out.print("> ");
                    }
                    break;
                    
                case Protocol.PRIVATE:
                    if (parts.length >= 3) {
                        System.out.println("\n[Private] " + parts[1] + ": " + parts[2]);
                        System.out.print("> ");
                    }
                    break;
                    
                case Protocol.USER_JOINED:
                    if (parts.length >= 2) {
                        System.out.println("\n*** " + parts[1] + " joined the chat ***");
                        System.out.print("> ");
                    }
                    break;
                    
                case Protocol.USER_LEFT:
                    if (parts.length >= 2) {
                        System.out.println("\n*** " + parts[1] + " left the chat ***");
                        System.out.print("> ");
                    }
                    break;
                    
                case Protocol.FILE_START:
                    if (parts.length >= 4) {
                        pendingSender = parts[1];
                        pendingFilename = parts[2];
                        pendingFileSize = Integer.parseInt(parts[3]);
                        
                        System.out.println("\n*** " + pendingSender + " wants to send you a file: " + 
                            pendingFilename + " (" + pendingFileSize + " bytes) ***");
                        
                        waitingForFileResponse = true;
                        System.out.print("Accept? (yes/no): ");
                    }
                    break;
                    
                case Protocol.FILE_DATA:
                    handleFileData(parts);
                    break;
                    
                case Protocol.FILE_COMPLETE:
                    System.out.println("\nFile received successfully!");
                    System.out.print("> ");
                    break;
                    
                default:
                    if (!command.equals(Protocol.LOGIN_SUCCESS)) {
                        System.out.println("\n[Server]: " + message);
                        System.out.print("> ");
                    }
            }
        }
        
        private void handleFileData(String[] parts) {
            if (parts.length >= 3 && fileOutputStream != null) {
                try {
                    byte[] fileData = Base64.getDecoder().decode(parts[2]);
                    fileOutputStream.write(fileData);
                    bytesReceived += fileData.length;
                    
                    int progress = (int) ((bytesReceived * 100) / currentFileSize);
                    System.out.print("\rReceiving file: " + progress + "%");
                    
                    if (bytesReceived >= currentFileSize) {
                        fileOutputStream.close();
                        System.out.println("\nFile " + currentFileName + " saved successfully!");
                        fileOutputStream = null;
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.err.println("\nError writing file data: " + e.getMessage());
                    System.out.print("> ");
                }
            }
        }
    }
    
    private void cleanup() {
        try {
            connected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        String serverAddress = "localhost";
        int port = 12345;
        
        if (args.length > 0) {
            serverAddress = args[0];
        }
        if (args.length > 1) {
            port = Integer.parseInt(args[1]);
        }
        
        ChatClient client = new ChatClient(serverAddress, port);
        client.start();
    }
}