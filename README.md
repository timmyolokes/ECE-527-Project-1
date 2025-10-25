# ECE-527-Project-1

## Overview
This project implements a simple client-server messaging system in Java using **TCP sockets**.  
It supports user authentication, message retrieval, message storage, and server control commands.

The system consists of:
- **Server.java** → Runs a server that handles client requests, user sessions, and message storage.
- **Client.java** → Provides a command-line client interface to interact with the server.

---

## Implemented Functions
- **Client Functions:**
  - `MSGGET`: Retrieves the message of the day from the server.
  - `LOGIN`: Authenticates a user with a user ID and password.
  - `LOGOUT`: Logs out the currently logged-in user.
  - `MSGSTORE`: Stores a message on the server after authorization.
  - `SHUTDOWN`: Shuts down the server (only for root users).
  - `QUIT`: Exits the client application.

- **Server Functions:**
  - Accepts client connections and processes commands.
  - Manages user sessions and authorization.
  - Stores and retrieves messages of the day.
  - Persists messages to a file for data retention.

---

## How to Run the Program

### 1. Compile the source code
```bash
javac Server.java Client.java
```

### 2. Start the server
```bash
java Server
```

### 3. Run the client
```bash
java Client <server-ip>
```

### 4. Enter one of the available commands:
- **Available Commands:**
  - LOGIN <userId> <password>
  - LOGOUT
  - MSGGET
  - MSGSTORE
  - SHUTDOWN
  - QUIT

## Known Bugs/ Limitations
 - The server does not currently support simultaneous connections and can only handle one client at a time

## Sample Outputs

- **Client Login:**
```
Enter a command: LOGIN root root2025
  Server response: 200 OK
Login successful!
```

- **Message Retrieval:**
```
Enter a command: MSGGET
Server response: 200 OK
Message of the day: Anyone who has never made a mistake has never tried anything new.
```

- **Message Storage:**
```
Enter a command: MSGSTORE
Server response: 200 OK
Enter message to store: Hello, World!
Server response: 200 OK
Message stored successfully!
```

- **Shutdown Command:**
```
Enter a command: SHUTDOWN
Server response: 200 OK
Server shutdown initiated successfully!
```
