# TCP Chat & File Sharing Application

A multi-client chat application built using Java Socket Programming and TCP/IP.
The application supports real-time messaging, private messaging, broadcast communication, and file sharing between connected clients using a multithreaded client-server architecture.

---

# Features

* Real-time chat communication
* Multi-client support
* Private messaging
* Broadcast messaging
* File sharing between users
* Multithreaded client handling
* Online user management
* TCP-based reliable communication

---

# Technologies Used

* Java
* TCP/IP Socket Programming
* Multithreading
* ConcurrentHashMap
* Base64 Encoding

---

# Project Architecture

```text
                    SERVER (Port 12345)
        ┌───────────────────────────────────┐
        │            ChatServer             │
        │     Accepts Client Requests       │
        └───────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
 ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
 │ClientHandler│ │ClientHandler│ │ClientHandler│
 │   Thread    │ │   Thread    │ │   Thread    │
 └─────────────┘ └─────────────┘ └─────────────┘
        │               │               │
    ┌───────┐       ┌───────┐       ┌───────┐
    │Client │       │Client │       │Client │
    │ Alice │       │  Bob  │       │ Carol │
    └───────┘       └───────┘       └───────┘
```


---

# Project Structure

```text
TCPChatApp/
├── src/
│   ├── common/
│   │   └── Protocol.java
│   ├── server/
│   │   ├── ChatServer.java
│   │   ├── ClientHandler.java
│   │   └── ClientManager.java
│   └── client/
│       └── ChatClient.java
├── out/
└── README.md
```

---

# How to Run

## Compile the Project

```bash
javac -d out src/common/*.java src/server/*.java src/client/*.java
```

## Run Server

```bash
java -cp out server.ChatServer
```

## Run Client

Open another terminal:

```bash
java -cp out client.ChatClient
```

Run multiple clients in separate terminals.

---

# Commands

| Command                   | Description          |
| ------------------------- | -------------------- |
| `/msg <user> <message>`   | Send private message |
| `/all <message>`          | Broadcast message    |
| `/users`                  | Show online users    |
| `/file <user> <filepath>` | Send file            |
| `/quit`                   | Exit application     |

---

# Screenshots

## Server



![Server Screenshot](screenshots/server.PNG)


---

## Broadcast Messaging

*Public Message sent by Alice shown to all other members*


![Broadcast Messaging from Alice](screenshots/i1.PNG)
*Broadcast Message sent by Alice*
![Broadcast Messaging received by others](screenshots/i2.PNG)
*Broadcast Message received by others*

---

## Private Messaging

*Private messages between Alice and Bob*


![Private Messaging sent by Alice](screenshots/p1.PNG)
*Private Message sent by Alice*
![Private Messaging received by Bob](screenshots/p2.PNG)
*Private Message received by Bob*

---

## File Sharing

*File sharing betweem Alice and Bob*


![File send by Alice](screenshots/f1.PNG)
*File sent by Alice*
![File received by Bob](screenshots/f2.PNG)
*File received by Bob*

---

# Key Concepts Used

* TCP Socket Communication
* Client-Server Architecture
* Multithreading
* Concurrent Client Handling
* File Transfer using Base64 Encoding
* Thread-safe Data Structures

---

# Future Improvements

* GUI using JavaFX/Swing
* End-to-end encryption
* Group chat rooms
* Voice/video communication
* Cloud deployment

---

# Author

Shweta Verma
