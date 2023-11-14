// Adapted from example code:
// Java Echo Client Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java - Accessed 04.11.2023

import java.io.*;
import java.net.*;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

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
            System.out.println("Connected to Echo Server. Type " + HandleModes.EXIT_COMMAND + " to quit.");

            // Wait for any messages from the server.
            listener(socket, serverInput);

            // Wait for any input from the user to send to the server.
            sender(socket, serverOutput, userInput);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostAddress + "\n" + e.toString());
            System.exit(1);
        }
    }

    public void sender(SSLSocket socket, PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        while (!socket.isClosed()) {
            switch (handleMode) {
                case CHALLENGE_RESPONSE:
                    challengeResponse(serverOutput);
                    break;
                case CHAT:
                    chat(serverOutput, userInput);
                    break;
                case CHAT_CREATION:
                    chatCreation(serverOutput, userInput);
                    break;
                case CHAT_SELECT:
                    chatSelect(socket, serverOutput, userInput);
                    break;
                default:
                    directMode(socket, serverOutput, userInput);
                    break;
            }
        }
    }

    private void challengeResponse(PrintWriter serverOutput) {
        boolean attemptedChallenge = false;
        System.out.println("Entered challenge response mode.");
        while (handleMode == HandleModes.CHALLENGE_RESPONSE) {
            System.out.println(handleMode);
            if (!attemptedChallenge) {
                try {
                    if (challenge != null) {
                        System.out.println("Encrypting challenge.");
                        chatPrivateKey = KeyUtils.readRSAPrivateKey(chatID);
                        String encryptedChallenge = KeyUtils.encryptString(challenge, chatPrivateKey,
                                KeyUtils.RSA);
                        serverOutput.println(encryptedChallenge);
                        System.out.println("Sent challenge response to server.");
                        challenge = null;
                        attemptedChallenge = true;
                    }
                } catch (Exception e) {
                    System.out.println(e.toString());
                    serverOutput.println("Failed to do challenge.");
                    attemptedChallenge = true;
                }
            }
        }
        System.out.println("Quit challenge response mode.");
    }

    private void chat(PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        System.out.println("Entered chat mode.");
        chatSymmetricKey = KeyUtils.readAESKey(chatID);
        while (handleMode == HandleModes.CHAT) {
            String userInputLine;
            if ((userInputLine = userInput.readLine()) != null && userInputLine.length() > 0) {
                // Check whether the user wants to quit first.
                if (userInputLine.charAt(0) == '/') {
                    serverOutput.println(userInputLine);
                    if (userInputLine.equals(HandleModes.EXIT_COMMAND)) {
                        handleMode = HandleModes.CHAT_SELECT;
                        break;
                    }
                }

                // If the user does not want to quit, encrypt their chat message and send it to the server.
                try {
                    String encryptedString = KeyUtils.encryptString(userInputLine, chatSymmetricKey,
                            KeyUtils.AES);
                    serverOutput.println(encryptedString);
                } catch (Exception e) {
                    System.out.println("Failed to encrypt message. " + e.toString());
                }
            }
        }
        System.out.println("Quit chat mode.");
    }

    private void chatCreation(PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        System.out.println("Entered chat creation mode.");
        boolean sentChatInfo = false;
        while (handleMode == HandleModes.CHAT_CREATION) {
            if (!sentChatInfo) {
                String userInputLine;
                if ((userInputLine = userInput.readLine()) != null) {
                    // Check whether the user wants to quit first.
                    if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
                        serverOutput.println(userInputLine);
                        handleMode = HandleModes.CHAT_SELECT;
                        return;
                    }

                    // If the user does not want to quit, create keys for the new chat.
                    try {
                        KeyPair rsaKeyPair = KeyUtils.generateRSAKeyPair();
                        chatPublicKey = rsaKeyPair.getPublic();
                        chatPrivateKey = rsaKeyPair.getPrivate();
                        chatSymmetricKey = KeyUtils.generateAESKey();
                    } catch (Exception e) {
                        System.out.println("Failed to generate keys for new chat.");
                    }

                    // Encrypt the chat name and send it to the server.
                    try {
                        String encryptedChatName = KeyUtils.encryptString(userInputLine, chatSymmetricKey,
                                KeyUtils.AES);
                        serverOutput.println(encryptedChatName);
                    } catch (Exception e) {
                        System.out.println("Failed to encrypt message.");
                    }

                    // Send the public key to the server.
                    String chatPublicKeyString = Base64.getEncoder().encodeToString(chatPublicKey.getEncoded());
                    System.out.println("Sending public key bytes: " + chatPublicKeyString);
                    serverOutput.println(chatPublicKeyString);
                    sentChatInfo = true;
                    System.out.println("Generated keys successfully.");
                }
            }
        }
        System.out.println("Quit chat creation mode.");
    }

    private void chatSelect(SSLSocket socket, PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        String userInputLine;
        System.out.println("Entered chat select mode.");
        while (handleMode == HandleModes.CHAT_SELECT) {
            if ((userInputLine = userInput.readLine()) != null) {
                // Check whether the user wants to quit first.
                if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
                    shuttingDown = true;
                    System.out.println("Closing socket and shutting down client.");
                    socket.close();
                    return;
                } else if (userInputLine.equals(HandleModes.CHAT_CREATION_COMMAND)) {
                    // Check whether the user wants to switch to chat creation mode.
                    serverOutput.println(userInputLine);
                    handleMode = HandleModes.CHAT_CREATION;
                } else {
                    // If the user does not want to quit, send chat ID.
                    try {
                        chatID = Integer.parseInt(userInputLine);
                        serverOutput.println(chatID);
                        handleMode = HandleModes.CHALLENGE_RESPONSE;
                    } catch (NumberFormatException e) {
                        System.out
                                .println("Failed to read \"" + userInputLine + "\". Please enter a valid integer.");
                    }
                }
            }
        }
        System.out.println("Quit chat select mode.");
    }

    private void directMode(SSLSocket socket, PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        String userInputLine;
        if ((userInputLine = userInput.readLine()) != null) {
            // Check whether the user wants to quit first.
            if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
                shuttingDown = true;
                System.out.println("Closing socket and shutting down client.");
                socket.close();
                return;
            }

            // If the user does not want to quit, send the unencrypted chat selection message.
            serverOutput.println(userInputLine);
        }
    }

    // Java Socket Programming - Multiple Clients Chat - WittCode - https://youtu.be/gLfuZrrfKes - Accessed 11.11.2023
    public void listener(SSLSocket socket, BufferedReader serverInput) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!socket.isClosed()) {
                        // Get any messages from the server and print them.
                        String serverResponse;
                        if ((serverResponse = serverInput.readLine()) != null) {
                            if (!shuttingDown) {
                                boolean changedMode = checkHandleMode(serverResponse);
                                if (!changedMode) {
                                    switch (handleMode) {
                                        case CHAT:
                                            try {
                                                // Decrypt the message before displaying it.
                                                String decryptedString = KeyUtils.decryptString(serverResponse,
                                                        chatSymmetricKey,
                                                        KeyUtils.AES);
                                                System.out.println("> " + decryptedString);
                                            } catch (Exception e) {
                                                System.out.println("Server response: " + serverResponse);
                                            }
                                            break;
                                        case CHALLENGE_RESPONSE:
                                            System.out.println("Received challenge: " + serverResponse);
                                            challenge = serverResponse;
                                            break;
                                        case CHAT_CREATION:
                                            try {
                                                chatID = Integer.parseInt(serverResponse);
                                                System.out.println("Got new chat ID: " + chatID);

                                                // Wait for the new chat to be created on the database before saving the keys.
                                                KeyUtils.saveRSAPrivateKey(chatID, chatPrivateKey);
                                                KeyUtils.saveRSAPublicKey(chatID, chatPublicKey);
                                                KeyUtils.saveAESKey(chatID, chatSymmetricKey);
                                            } catch (NumberFormatException e) {
                                                System.out.println("Server response: " + serverResponse);
                                            }
                                            break;
                                        default:
                                            System.out.println("Server response: " + serverResponse);
                                            break;
                                    }
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

    private boolean checkHandleMode(String serverResponse) {
        HandleModes newHandleMode = HandleModes.stringToHandleMode(serverResponse);
        if (newHandleMode != null) {
            handleMode = newHandleMode;
            System.out.println("Switched handle mode: " + handleMode);
            return true;
        }
        return false;
    }
}