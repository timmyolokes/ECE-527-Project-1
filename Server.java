/*
 * Server.java
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {

    public static final int SERVER_PORT = 3520; // Changed port number to last 4 of UMID
    
    // Array list to store messages of the day (now dynamic)
    private static final List<String> MESSAGES_OF_DAY = new ArrayList<>(Arrays.asList(
        "Anyone who has never made a mistake has never tried anything new.",
        "Imagination is more important than knowledge.",
        "The only way to discover the limits of the possible is to go beyond them into the impossible.",
        "The greatest glory in living lies not in never falling, but in rising every time we fall.",
        "It is during our darkest moments that we must focus to see the light."
    ));
    
    // Counter to cycle through messages sequentially
    private static int messageIndex = 0;
    
    // File to persist messages
    private static final String MESSAGES_FILE = "messages.txt";
    
    // Flag to control server shutdown
    private static volatile boolean serverRunning = true;
    
    // UserID and Password pairs
    private static final String[][] USERS = {
        {"root", "root2025"},
        {"john", "john2025"},
        {"david", "david2025"},
        {"mary", "mary2025"}
    };
    
    // Session state class to track logged-in users
    private static class SessionState {
        private String loggedInUser = null;
        private boolean isLoggedIn = false;
        
        // Attempt to log in with given credentials
        public boolean login(String userId, String password) {
            for (String[] user : USERS) {
                if (user[0].equals(userId) && user[1].equals(password)) {
                    loggedInUser = userId;
                    isLoggedIn = true;
                    return true;
                }
            }
            return false;
        }
        
        // Log out the current user
        public void logout() {
            loggedInUser = null;
            isLoggedIn = false;
        }
        
        // Check if a user is logged in
        public boolean isLoggedIn() {
            return isLoggedIn;
        }
        
        // Get the currently logged-in user
        public String getLoggedInUser() {
            return loggedInUser;
        }
        
        // Check if the logged-in user is root
        public boolean isRoot() {
            return isLoggedIn && "root".equals(loggedInUser);
        }
    }

    public static void main(String args[]) {
        ServerSocket myServerice = null;
        String line;
        BufferedReader is;
        PrintStream os;
        Socket serviceSocket = null;

        // Load messages from file if it exists
        loadMessagesFromFile();

        // Try to open a server socket 
        try {
            myServerice = new ServerSocket(SERVER_PORT);
            System.out.println("Server started on port " + SERVER_PORT);
        }
        catch (IOException e) {
            System.out.println("Error starting server: " + e);
            return;
        }   

        // Create a socket object from the ServerSocket to listen and accept connections.
        // Open input and output streams
        while (serverRunning) {
            try {
                System.out.println("Waiting for client connection...");
                serviceSocket = myServerice.accept();
                System.out.println("Client connected from: " + serviceSocket.getInetAddress());

                is = new BufferedReader(new InputStreamReader(serviceSocket.getInputStream()));
                os = new PrintStream(serviceSocket.getOutputStream());
                
                // Create session state for this client connection
                SessionState session = new SessionState();

                // Process client commands until client disconnects
                while ((line = is.readLine()) != null && serverRunning) {
                    System.out.println("Received from client: " + line);
                    
                    // Parse and process commands
                    String[] parts = line.trim().split("\\s+");
                    String command = parts[0].toUpperCase();
                    
                    switch (command) {
                        case "MSGGET":
                            handleMsgGet(os);
                            break;
                        case "LOGIN":
                            handleLogin(parts, os, session);
                            break;
                        case "LOGOUT":
                            handleLogout(os, session);
                            break;
                        case "MSGSTORE":
                            handleMsgStore(is, os, session);
                            break;
                        case "SHUTDOWN":
                            if (handleShutdown(os, session)) {
                                // Close this client connection first
                                is.close();
                                os.close();
                                serviceSocket.close();
                                System.out.println("Client disconnected after SHUTDOWN");
                                
                                // Then shutdown the server
                                serverRunning = false;
                                return; // Exit to main loop which will then exit
                            }
                            break;
                        case "QUIT":
                            handleQuit(os);
                            break; // Exit the inner loop to close this client connection
                        default:
                            // Echo back any other commands (original behavior)
                            os.println(line);
                            break;
                    }
                }

                // Close input and output stream and socket when client disconnects
                if (!serverRunning) {
                    System.out.println("Server shutting down - closing client connection");
                } else {
                    System.out.println("Client disconnected");
                }
                is.close();
                os.close();
                serviceSocket.close();
            }   
            catch (IOException e) {
                System.out.println("Error handling client: " + e);
                // Try to close the socket if there was an error
                try {
                    if (serviceSocket != null && !serviceSocket.isClosed()) {
                        serviceSocket.close();
                    }
                } catch (IOException closeException) {
                    System.out.println("Error closing socket: " + closeException);
                }
            }
        }
        
        // Clean shutdown
        try {
            if (myServerice != null) {
                myServerice.close();
            }
            System.out.println("Server shutdown complete.");
        } catch (IOException e) {
            System.out.println("Error during server shutdown: " + e);
        }
    }
    
	
	/**
     * Handles the MSGGET command by sending "200 OK" followed by a message of the day
     * Messages are cycled through sequentially
     */
    private static void handleMsgGet(PrintStream os) {
        // Send "200 OK" response
        os.println("200 OK");
        
        // Send the current message of the day
        if (!MESSAGES_OF_DAY.isEmpty()) {
            String message = MESSAGES_OF_DAY.get(messageIndex % MESSAGES_OF_DAY.size());
            os.println(message);
            
            // Move to next message for next request
            messageIndex = (messageIndex + 1) % MESSAGES_OF_DAY.size();
            
            System.out.println("Sent MSGGET response with message: \"" + message + "\"");
        } else {
            os.println("No messages available");
            System.out.println("No messages available for MSGGET");
        }
    }


    /**
     * Handles the MSGSTORE command - two-step process
     * Step 1: Check authorization
     * Step 2: If authorized, read and store the message
     */
    private static void handleMsgStore(BufferedReader is, PrintStream os, SessionState session) {
        // Step 1: Check if user is logged in
        if (!session.isLoggedIn()) {
            os.println("401 You are not currently logged in, login first");
            System.out.println("MSGSTORE denied - user not logged in");
            return;
        }
        
        // Step 2: User is authorized, send OK and wait for message
        os.println("200 OK");
        System.out.println("MSGSTORE authorized for user: " + session.getLoggedInUser());
        
        try {
            // Read the message from client
            String newMessage = is.readLine();
            if (newMessage != null && !newMessage.trim().isEmpty()) {
                // Add message to the list
                MESSAGES_OF_DAY.add(newMessage);
                
                // Save to file
                saveMessagesToFile();
                
                // Send success confirmation
                os.println("200 OK");
                System.out.println("New message stored: \"" + newMessage + "\" by user: " + session.getLoggedInUser());
            } else {
                os.println("300 message format error");
                System.out.println("MSGSTORE failed - empty message received");
            }
        } catch (IOException e) {
            os.println("300 message format error");
            System.out.println("MSGSTORE failed - error reading message: " + e);
        }
    }
    
    
    /**
     * Load messages from file on server startup
     */
    private static void loadMessagesFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(MESSAGES_FILE))) {
            MESSAGES_OF_DAY.clear();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    MESSAGES_OF_DAY.add(line);
                }
            }
            System.out.println("Loaded " + MESSAGES_OF_DAY.size() + " messages from file");
        } catch (FileNotFoundException e) {
            System.out.println("No existing messages file found. Using default messages.");
        } catch (IOException e) {
            System.out.println("Error loading messages from file: " + e);
        }
        
        // Ensure we have at least the default messages
        if (MESSAGES_OF_DAY.isEmpty()) {
            MESSAGES_OF_DAY.addAll(Arrays.asList(
                "Anyone who has never made a mistake has never tried anything new.",
				"Imagination is more important than knowledge.",
        		"The only way to discover the limits of the possible is to go beyond them into the impossible.",
        		"The greatest glory in living lies not in never falling, but in rising every time we fall.",
        		"It is during our darkest moments that we must focus to see the light."
            ));
        }
    }

    
    /**
     * Save messages to file for persistence
     */
    private static void saveMessagesToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(MESSAGES_FILE))) {
            for (String message : MESSAGES_OF_DAY) {
                writer.println(message);
            }
            System.out.println("Messages saved to file successfully");
        } catch (IOException e) {
            System.out.println("Error saving messages to file: " + e);
        }
    }


	/**
     * Handles the SHUTDOWN command - only root user can execute
     */
    private static boolean handleShutdown(PrintStream os, SessionState session) {
        // Check if user is root
        if (!session.isRoot()) {
            if (!session.isLoggedIn()) {
                os.println("401 You are not currently logged in, login first");
            } else {
                os.println("402 User not allowed to execute this command");
            }
            System.out.println("SHUTDOWN denied for user: " + 
                (session.isLoggedIn() ? session.getLoggedInUser() : "not logged in"));
            return false;
        }
        
        // Root user - allow shutdown
        os.println("200 OK");
        System.out.println("SHUTDOWN command executed by root user. Server shutting down...");
        return true;
    }
	

	/**
     * Handles the LOGIN command
     * Format: LOGIN <userid> <password>
     */
    private static void handleLogin(String[] parts, PrintStream os, SessionState session) {
        if (parts.length != 3) {
            os.println("400 Bad Request");
            System.out.println("Invalid LOGIN format received");
            return;
        }
        
        String userId = parts[1];
        String password = parts[2];
        
        if (session.login(userId, password)) {
            os.println("200 OK");
            System.out.println("User " + userId + " logged in successfully");
        } else {
            os.println("410 Wrong UserID or Password");
            System.out.println("Failed login attempt for user: " + userId);
        }
    }
    

	/**
     * Handles the LOGOUT command
     */
    private static void handleLogout(PrintStream os, SessionState session) {
        if (session.isLoggedIn()) {
            String user = session.getLoggedInUser();
            session.logout();
            os.println("200 OK");
            System.out.println("User " + user + " logged out successfully");
        } else {
            os.println("User not logged in");  // Still return OK even if not logged in
            System.out.println("LOGOUT received from non-logged-in user");
        }
    }


    /**
     * Handles the QUIT command by sending "200 OK" confirmation
     * After this, the client connection will be closed
     */
    private static void handleQuit(PrintStream os) {
        // Send "200 OK" confirmation
        os.println("200 OK");
        System.out.println("Sent QUIT confirmation to client");
    }
}