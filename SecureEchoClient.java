// Adapted from example code:
// Java Echo Client Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java - Accessed 04.11.2023

import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

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
    HandleModes handleMode = HandleModes.CHAT_SELECT;
    int chatID;
    PrivateKey chatPrivateKey;
    PublicKey chatPublicKey;
    SecretKey chatSymmetricKey;
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
                        challengeResponseOutput(serverOutput);
                        break;
                    case CHAT:
                        chatHandler(userInputLine, serverOutput);
                        break;
                    case CHAT_CREATION:
                        chatCreator(userInputLine, serverOutput);
                        break;
                    case CHAT_SELECT:
                        chatSelect(userInputLine, serverOutput, socket);
                        break;
                    default:
                        if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
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

    private void challengeResponseOutput(PrintWriter serverOutput) {
        try {
            chatPrivateKey = KeyUtils.readRSAPrivateKey(chatID);
            if (challenge != null) {
                String encryptedChallenge = KeyUtils.encryptString(challenge, chatPrivateKey,
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
    }

    private void chatHandler(String userInputLine, PrintWriter serverOutput) {
        // Check whether the user wants to quit first.
        if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
            serverOutput.println(userInputLine);
            handleMode = HandleModes.CHAT_SELECT;
            return;
        }

        // If the user does not want to quit, encrypt their chat message and send it to the server.
        try {
            String encryptedString = KeyUtils.encryptString(userInputLine, chatSymmetricKey,
                    KeyUtils.AES);
            serverOutput.println(encryptedString);
        } catch (Exception e) {
            System.out.println("Failed to encrypt message.");
        }
    }

    private void chatCreator(String userInputLine, PrintWriter serverOutput) {
        // Check whether the user wants to quit first.
        if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
            serverOutput.println(userInputLine);
            handleMode = HandleModes.CHAT_SELECT;
            return;
        }

        // If the user does not want to quit, encrypt the chat name and send it to the server.
        try {
            String encryptedChatName = KeyUtils.encryptString(userInputLine, chatSymmetricKey,
                    KeyUtils.AES);
            serverOutput.println(encryptedChatName);
        } catch (Exception e) {
            System.out.println("Failed to encrypt message.");
        }

        try {
            KeyPair rsaKeyPair = KeyUtils.generateRSAKeyPair();
            chatPublicKey = rsaKeyPair.getPublic();
            chatPrivateKey = rsaKeyPair.getPrivate();
            chatSymmetricKey = KeyUtils.generateAESKey();
            serverOutput.println(chatPublicKey.getEncoded());
        } catch (Exception e) {
            System.out.println("Failed to generate keys for new chat.");
        }

        System.out.println("Generated keys successfully.");

    }

    private void chatSelect(String userInputLine, PrintWriter serverOutput, SSLSocket socket) throws IOException {
        if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
            shuttingDown = true;
            System.out.println("Closing socket and shutting down client.");
            socket.close();
            return;
        } else if (userInputLine.equals(HandleModes.CHAT_CREATION_COMMAND)) {
            serverOutput.println(userInputLine);
        }
        try {
            chatID = Integer.parseInt(userInputLine);
            serverOutput.println(chatID);
        } catch (NumberFormatException e) {
            System.out
                    .println("Failed to read \"" + userInputLine + "\". Please enter a valid integer.");
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
                                            // Decrypt the message before displaying it.
                                            String decryptedString = KeyUtils.decryptString(serverResponse,
                                                    chatSymmetricKey,
                                                    KeyUtils.AES);
                                            System.out.println("Server response (decrypted): " + decryptedString);
                                        } catch (Exception e) {
                                            System.out.println("Server response    (direct): " + serverResponse);
                                        }
                                        break;
                                    case CHALLENGE_RESPONSE:
                                        challenge = serverResponse;
                                        break;
                                    case CHAT_CREATION:
                                        KeyUtils.saveRSAPrivateKey(chatID, chatPrivateKey);
                                        KeyUtils.saveRSAPublicKey(chatID, chatPublicKey);
                                        KeyUtils.saveAESKey(chatID, chatSymmetricKey);
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
        if (serverResponse.equals(HandleModes.CHAT_SELECT_SIGNAL)) {
            handleMode = HandleModes.CHAT_SELECT;
            System.out.println("SWITCHED HANDLE MODE: " + handleMode);
        } else if (serverResponse.equals(HandleModes.CHALLENGE_RESPONSE_SIGNAL)) {
            handleMode = HandleModes.CHALLENGE_RESPONSE;
            System.out.println("SWITCHED HANDLE MODE: " + handleMode);
        } else if (serverResponse.equals(HandleModes.CHAT_SIGNAL)) {
            handleMode = HandleModes.CHAT;
            System.out.println("SWITCHED HANDLE MODE: " + handleMode);
            try {
                chatSymmetricKey = KeyUtils.readAESKey(chatID);
            } catch (Exception e) {
                System.out.println("Failed to switch to chat mode. Could not find the matching keys.");
            }
        } else if (serverResponse.equals(HandleModes.CHAT_CREATION_SIGNAL)) {
            handleMode = HandleModes.CHAT_CREATION;
            System.out.println("SWITCHED HANDLE MODE: " + handleMode);
        }
    }
}