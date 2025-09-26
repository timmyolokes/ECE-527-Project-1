/* 
 * Client.java
 */

import java.io.*;
import java.net.*;

public class Client {
    public static final int SERVER_PORT = 3520; // Changed port number to last 4 of UMID

    public static void main(String[] args) {
        Socket clientSocket = null;  
        PrintStream os = null;
        BufferedReader is = null;
        String userInput = null;
        String serverResponse = null;
        BufferedReader stdInput = null;

        // Check the number of command line parameters
        if (args.length < 1) {
            System.out.println("Usage: java Client <Server IP Address>");
            System.exit(1);
        }

        // Try to open a socket on SERVER_PORT
        // Try to open input and output streams
        try {
            System.out.println("Connecting to server at " + args[0] + ":" + SERVER_PORT);
            clientSocket = new Socket(args[0], SERVER_PORT);
            os = new PrintStream(clientSocket.getOutputStream());
            is = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            stdInput = new BufferedReader(new InputStreamReader(System.in));
        } 
        catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + args[0]);
            System.exit(1);
        } 
        catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + args[0]);
            System.exit(1);
        }

        // If everything has been initialized then we want to process user commands
        if (clientSocket != null && os != null && is != null) {
            try {
                System.out.print("Enter a command: ");
                
                while ((userInput = stdInput.readLine()) != null) {
                    // Send command to server
                    os.println(userInput);
                    
                    // Parse command to determine response handling
                    String[] parts = userInput.trim().split("\\s+");
                    String command = parts[0].toUpperCase(); // take the first part after splitting as a command
                    
                    // Handle different command responses
                    switch (command) {
                        case "MSGGET":
                            handleMsgGetResponse(is);
                            break;
                        case "LOGIN":
                            handleLoginResponse(is);
                            break;
                        case "LOGOUT":
                            handleLogoutResponse(is);
                            break;
                        case "MSGSTORE":
                            handleMsgStoreCommand(stdInput, is, os);
                            break;
                        case "SHUTDOWN":
                            if (handleShutdownResponse(is)) {
                                // If shutdown was successful, exit the client
                                return;
                            }
                            // If not successful, continue to prompt for commands
                            break;
                        case "QUIT":
                            handleQuitResponse(is);
                            return; // Exit after receiving server confirmation
                        default:
                            // For other commands, just read and display invalid command response
                            serverResponse = is.readLine();
                            if (serverResponse != null) {
                                System.out.println("Not a valid command. Server response: " + serverResponse);
                            }
                            break;
                    }
                    
                    System.out.print("Enter a command: ");
                }

                // Close the input and output streams and socket
                os.close();
                is.close();
                stdInput.close();
                clientSocket.close();   
            } 
            catch (IOException e) {
                System.err.println("IOException: " + e);
            }
        }
    }
    
    
    /**
     * Handles the response from MSGGET command
     * Expects "200 OK" followed by the message of the day
     */
    private static void handleMsgGetResponse(BufferedReader is) {
        try {
            // Read the status line (should be "200 OK")
            String statusLine = is.readLine();
            if (statusLine != null) {
                System.out.println("Server response: " + statusLine);
                
                // If we got "200 OK", read the message of the day
                if (statusLine.equals("200 OK")) {
                    String messageOfDay = is.readLine();
                    if (messageOfDay != null) {
                        System.out.println("Message of the day: " + messageOfDay);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading MSGGET response: " + e);
        }
    }
    
    
    /**
     * Handles the MSGSTORE command - two-step process
     * Send MSGSTORE and check authorization
     * If authorized, prompt for and send message
     */
    private static void handleMsgStoreCommand(BufferedReader stdInput, BufferedReader is, PrintStream os) {
        try {
            // Read server's authorization response
            String authResponse = is.readLine();
            if (authResponse != null) {
                System.out.println("Server response: " + authResponse);
                
                if (authResponse.equals("200 OK")) {
                    // User is authorized, prompt for message
                    System.out.print("Enter message to store: ");
                    String messageToStore = stdInput.readLine();
                    
                    if (messageToStore != null && !messageToStore.trim().isEmpty()) {
                        // Send the message to server
                        os.println(messageToStore);
                        
                        // Read final confirmation
                        String confirmResponse = is.readLine();
                        if (confirmResponse != null) {
                            System.out.println("Server response: " + confirmResponse);
                            if (confirmResponse.equals("200 OK")) {
                                System.out.println("Message stored successfully!");
                            }
                        }
                    } else {
                        System.out.println("Empty message not sent.");
                    }
                } else if (authResponse.equals("401 You are not currently logged in, login first")) {
                    System.out.println("Please login first before storing messages.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling MSGSTORE command: " + e);
        }
    }
    

    /**
     * Handles the response from SHUTDOWN command
     * Expects "200 OK", "401", or "402" response
     */

    private static boolean handleShutdownResponse(BufferedReader is) {
         try {
             String statusLine = is.readLine();
             if (statusLine != null) {
                 System.out.println("Server response: " + statusLine);
                 
                if (statusLine.equals("200 OK")) {
                    System.out.println("Server shutdown initiated successfully!");
                    return true;
                } else if (statusLine.equals("401 You are not currently logged in, login first")) {
                    System.out.println("Please login first before using SHUTDOWN command.");
                } else if (statusLine.equals("402 User not allowed to execute this command")) {
                    System.out.println("Only the root user can shutdown the server.");
                }
             }
         } catch (IOException e) {
             System.err.println("Error reading SHUTDOWN response: " + e);
         }
        return false;
     } 


    /**
     * Handles the response from LOGIN command
     * Expects either "200 OK" or "410 Wrong UserID or Password"
     */
    private static void handleLoginResponse(BufferedReader is) {
        try {
            String statusLine = is.readLine();
            if (statusLine != null) {
                System.out.println("Server response: " + statusLine);
                
                if (statusLine.equals("200 OK")) {
                    System.out.println("Login successful!");
                } else if (statusLine.equals("410 Wrong UserID or Password")) {
                    System.out.println("Login failed. Please check your credentials.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading LOGIN response: " + e);
        }
    }

    
    /**
     * Handles the response from LOGOUT command
     * Expects "200 OK" confirmation from server
     */
    private static void handleLogoutResponse(BufferedReader is) {
        try {
            String statusLine = is.readLine();
            if (statusLine != null) {
                System.out.println("Server response: " + statusLine);
                
                if (statusLine.equals("200 OK")) {
                    System.out.println("Logout successful!");
                } else {
                    System.out.println("Logout failed.");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading LOGOUT response: " + e);
        }
    }


    /**
     * Handles the response from QUIT command
     * Expects "200 OK" confirmation from server before terminating
     */
    private static void handleQuitResponse(BufferedReader is) {
        try {
            // Read the status line (should be "200 OK")
            String statusLine = is.readLine();
            if (statusLine != null) {
                System.out.println("Server response: " + statusLine);
                
                if (statusLine.equals("200 OK")) {
                    System.out.println("Server confirmed termination. Exiting client...");
                } else {
                    System.out.println("Unexpected response from server. Exiting anyway...");
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading QUIT response: " + e);
        }
    }
}