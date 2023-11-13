// Adapted from example code:
// Java Echo Client Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java - Accessed 04.11.2023

import java.io.*;
import java.net.*;
import java.security.PrivateKey;

import javax.crypto.SecretKey;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SecureEchoClient extends AbstractEchoClient {
    public static void main(String[] args) throws IOException {
        SecureEchoClient echoClient = new SecureEchoClient();
        echoClient.readServerData(args);
        echoClient.start();
    }

    boolean shuttingDown;
    HandleModes.Enum handleMode = HandleModes.Enum.CHAT_SELECT;
    int chatID;
    PrivateKey chatAuthKey;
    SecretKey chatEncryptionKey;
    String challenge;

    @Override
    public void start() {
        // Try to connect to the server specified.
        SSLSocketFactory sslSocketFactory = SslUtil.getSSLSocketFactory("PEM Files/ca-cert.pem",
                "PEM Files/client-cert.pem",
                "PEM Files/client-key.pem");

        try (
                // Create a socket to connect to the server.
                SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(hostAddress, port);

                // Set up a reader and writer to transfer data between the client and server.
                BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true);

                // Use a buffered reader to let the user send messages through the client.
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            socket.startHandshake();
            System.out.println("Connected to Echo Server. Type '/exit' to quit.");

            // Wait for any messages from the server.
            listenForMessages(socket, serverInput);

            // Wait for any input from the user to send to the server.
            sendMessages(socket, serverOutput, userInput);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostAddress + "\n" + e.toString());
            System.exit(1);
        }
    }

    public void sendMessages(SSLSocket socket, PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        String userInputLine;
        while (!socket.isClosed()) {
            if ((userInputLine = userInput.readLine()) != null) {
                switch (handleMode) {
                    case CHALLENGE_RESPONSE:
                        try {
                            chatAuthKey = KeyUtils.readRSAPrivateKey(chatID);
                            if (challenge != null) {
                                String encryptedChallenge = KeyUtils.encryptString(challenge, chatAuthKey,
                                        KeyUtils.RSA);
                                serverOutput.println(encryptedChallenge);
                                System.out.println("Sent challenge response to server");
                                challenge = null;
                            } else {
                                System.out.println("Failed to read challenge from server.");
                                serverOutput.println("Failed to read challenge from server.");
                            }
                        } catch (Exception e) {
                            System.out.println(e.toString());
                            serverOutput.println("Failed to do challenge.");
                        }
                        break;
                    case CHAT:
                        // Check whether the user wants to quit first.
                        if ("/exit".equalsIgnoreCase(userInputLine)) {
                            serverOutput.println(userInputLine);
                            handleMode = HandleModes.Enum.CHAT_SELECT;
                            break;
                        }

                        // If the user does not want to quit, encrypt their chat message and send it to the server.
                        try {
                            String encryptedString = KeyUtils.encryptString(userInputLine, KeyUtils.readAESKey(1),
                                    KeyUtils.AES);
                            serverOutput.println(encryptedString);
                        } catch (Exception e) {
                            System.out.println("Failed to encrypt message.");
                        }
                        break;
                    case CHAT_SELECT:
                        if ("/exit".equalsIgnoreCase(userInputLine)) {
                            shuttingDown = true;
                            System.out.println("Closing socket and shutting down client.");
                            socket.close();
                            return;
                        }
                        try {
                            chatID = Integer.parseInt(userInputLine);
                            serverOutput.println(chatID);
                        } catch (NumberFormatException e) {
                            System.out
                                    .println("Failed to read \"" + userInputLine + "\". Please enter a valid integer.");
                        }
                        break;
                    default:
                        if ("exit".equalsIgnoreCase(userInputLine)) {
                            shuttingDown = true;
                            System.out.println("Closing socket and shutting down client.");
                            socket.close();
                            return;
                        }

                        // Send user input to the server.
                        serverOutput.println(userInputLine);
                        break;
                }
            }
        }
    }

    // Java Socket Programming - Multiple Clients Chat - WittCode - https://youtu.be/gLfuZrrfKes - Accessed 11.11.2023
    public void listenForMessages(SSLSocket socket, BufferedReader serverInput) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!socket.isClosed()) {
                        // Get any messages from the server and print them.
                        String serverResponse;
                        if ((serverResponse = serverInput.readLine()) != null) {
                            if (!shuttingDown) {
                                checkHandleMode(serverResponse);
                                switch (handleMode) {
                                    case CHAT:
                                        try {
                                            // TODO REMOVE MAGIC NUMBER
                                            // Decrypt the message before displaying it. (FOR NOW JUST SHOW ENCRYPTED MESSAGE).
                                            String decryptedString = KeyUtils.decryptString(serverResponse,
                                                    KeyUtils.readAESKey(1),
                                                    KeyUtils.AES);
                                            System.out.println("Server response (decrypted): " + decryptedString);
                                        } catch (Exception e) {
                                            System.out.println("Server response    (direct): " + serverResponse);
                                        }
                                        break;
                                    case CHALLENGE_RESPONSE:
                                        challenge = serverResponse;
                                        break;
                                    default:
                                        System.out.println("Server response: " + serverResponse);
                                        break;
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    if (!shuttingDown) {
                        System.out.println("Error while listening for server messages.");
                        System.out.println(e.toString());
                    }
                }
            }
        }).start();
    }

    private void checkHandleMode(String serverResponse) {
        if (serverResponse.equals(HandleModes.CHAT_SELECT_STRING)) {
            handleMode = HandleModes.Enum.CHAT_SELECT;
            System.out.println("SWITCHED HANDLE MODE: " + handleMode);
        } else if (serverResponse.equals(HandleModes.CHALLENGE_RESPONSE_STRING)) {
            handleMode = HandleModes.Enum.CHALLENGE_RESPONSE;
            System.out.println("SWITCHED HANDLE MODE: " + handleMode);
        } else if (serverResponse.equals(HandleModes.CHAT_STRING)) {
            handleMode = HandleModes.Enum.CHAT;
            System.out.println("SWITCHED HANDLE MODE: " + handleMode);
        }
    }
}