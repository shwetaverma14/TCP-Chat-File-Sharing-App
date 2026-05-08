package server;
import common.Protocol;
import java.util.concurrent.ConcurrentHashMap;
import java.util.*;

public class ClientManager {
    private ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    
    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
    
        broadcast(username + " joined the chat!", "Server");
    }
    
    public void removeClient(String username) {
        clients.remove(username);
        
        broadcast(username + " left the chat!", "Server");
    }
    
    public ClientHandler getClient(String username) {
        return clients.get(username);
    }
    
    public boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }
    
    public void broadcast(String message, String sender) {
        String formattedMessage = Protocol.buildMessage(Protocol.BROADCAST, sender, message);
        for (ClientHandler client : clients.values()) {
            client.sendMessage(formattedMessage);
        }
    }
    
    public void sendPrivate(String sender, String target, String message) {
        ClientHandler targetClient = clients.get(target);
        if (targetClient != null) {
            String formattedMessage = Protocol.buildMessage(Protocol.PRIVATE, sender, message);
            targetClient.sendMessage(formattedMessage);
            
            // Send confirmation to sender
            ClientHandler senderClient = clients.get(sender);
            if (senderClient != null) {
                senderClient.sendMessage(Protocol.buildMessage(Protocol.PRIVATE, "System", 
                    "Message sent to " + target));
            }
        } else {
            ClientHandler senderClient = clients.get(sender);
            if (senderClient != null) {
                senderClient.sendMessage(Protocol.buildMessage(Protocol.PRIVATE, "System", 
                    "User " + target + " not found"));
            }
        }
    }
    
    public void sendFile(String sender, String target, String filename, int filesize) {
        ClientHandler targetClient = clients.get(target);
        if (targetClient != null) {
            String fileRequest = Protocol.buildMessage(Protocol.FILE_START, sender, filename, 
                String.valueOf(filesize));
            targetClient.sendMessage(fileRequest);
        }
    }
    
    public void broadcastUserList() {
        String userList = String.join(",", clients.keySet());
        String userListMessage = Protocol.buildMessage(Protocol.USER_LIST, userList);
        for (ClientHandler client : clients.values()) {
            client.sendMessage(userListMessage);
        }
    }
    
    public Collection<String> getAllUsernames() {
        return clients.keySet();
    }
}