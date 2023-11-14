package Client;
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

import Shared.HandleMode;
import Shared.KeyUtils;
import Shared.SslUtil;

public class SecureChatClient {
    public static void main(String[] args) throws IOException {
        new SecureChatClient(args).start();
    }

    public SecureChatClient(String[] args) {
        if (args.length > 5) {
            try {
                hostAddress = args[0];
                port = Integer.parseInt(args[1]);

                String caCrtFile = args[2];
                String crtFile = args[3];
                String keyFile = args[4];
                sslSocketFactory = SslUtil.getSSLSocketFactory(caCrtFile, crtFile, keyFile);

                keyFolder = args[5];
                return;
            } catch (NumberFormatException e) {
                System.err.println("Port must be a valid integer!");
            }
        }
        throw new IllegalArgumentException(
                "Usage: java SecureChatClient <host address> <port number> <caCrtFile> <crtFile> <keyFile> <chat key folder>");
    }

    String hostAddress;
    int port;
    String keyFolder;
    SSLSocketFactory sslSocketFactory;

    HandleMode handleMode = HandleMode.CHAT_SELECT;
    boolean shuttingDown;
    byte[] challenge;
    int chatID;
    final Object monitor = new Object();

    PrivateKey chatPrivateKey;
    PublicKey chatPublicKey;
    SecretKey chatSymmetricKey;

    public void start() {
        // Try to connect to the server specified.
        try (
                // Create a socket to connect to the server.
                SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(hostAddress, port);

                // Set up a reader and writer to transfer data between the client and server.
                BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true);

                // Use a buffered reader to let the user send messages through the client.
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            socket.startHandshake();
            System.out.println("Connected to Echo Server. Type " + HandleMode.EXIT_COMMAND + " to quit.");

            // Wait for any messages from the server.
            listener(socket, serverInput);

            // Wait for any input from the user to send to the server.
            sender(socket, serverOutput, userInput);
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("I/O for connection to " +
                    hostAddress + "\n" + e.toString());
            System.exit(1);
        }
    }

    /**
     * Output part of the client that sends messages to the server..
     * @param socket The secure socket to the server.
     * @param serverOutput A writer to output text to the server.
     * @param userInput A reader to get user input.
     * @throws IOException
     */
    public void sender(SSLSocket socket, PrintWriter serverOutput, BufferedReader userInput)
            throws IOException {
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
            if (!shuttingDown) {
                // Wait for a handle mode change.
                try {
                    synchronized (monitor) {
                        monitor.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
    }

    /**
     * Performs the challenge as part of the server's challenge response protocol when entering a chat.
     * @param serverOutput A writer to output text to the server.
     */
    private void challengeResponse(PrintWriter serverOutput) {
        System.out.println("Entered challenge response mode.");
        try {
            // Wait for the challenge to be received.
            synchronized (monitor) {
                monitor.wait();
            }

            // Encrypt the challenge using the chat's private key.
            System.out.println("Encrypting challenge.");
            chatPrivateKey = KeyUtils.readRSAPrivateKey(chatID, keyFolder);
            byte[] encryptedChallenge = KeyUtils.encryptBytes(challenge, chatPrivateKey,
                    KeyUtils.RSA);

            String encryptedChallengeString = Base64.getEncoder().encodeToString(encryptedChallenge);

            // Send the response back to the server.
            serverOutput.println(encryptedChallengeString);
            System.out.println("Sent challenge response to server.");

            // Mark the challenge as attempted.
            challenge = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Failed to do challenge.");
            System.out.println(e.toString());
            serverOutput.println(HandleMode.CHALLENGE_FAILED);
        }
        challenge = null;
        System.out.println("Quit challenge response mode.");
    }

    private void chat(PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        System.out.println("Entered chat mode.");
        chatSymmetricKey = KeyUtils.readAESKey(chatID, keyFolder);
        while (handleMode == HandleMode.CHAT) {
            String userInputLine;
            if ((userInputLine = userInput.readLine()) != null) {
                if (userInputLine.length() > 0) {
                    // Check whether the user wants to quit first.
                    if (userInputLine.charAt(0) == '/') {
                        serverOutput.println(userInputLine);
                        if (HandleMode.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
                            break;
                        }
                    } else {
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
            }
        }
        System.out.println("Quit chat mode.");
    }

    private void chatCreation(PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        System.out.println("Entered chat creation mode.");
        String userInputLine;
        if ((userInputLine = userInput.readLine()) != null) {
            // Check whether the user wants to quit first.
            if (HandleMode.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
                serverOutput.println(userInputLine);
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
            System.out.println("Generated keys successfully.");
        }
        System.out.println("Quit chat creation mode.");
    }

    private void chatSelect(SSLSocket socket, PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        String userInputLine;
        System.out.println("Entered chat select mode.");
        if ((userInputLine = userInput.readLine()) != null) {
            // Check whether the user wants to quit first.
            if (HandleMode.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
                shuttingDown = true;
                System.out.println("Closing socket and shutting down client.");
                socket.close();
                return;
            } else if (HandleMode.CHAT_CREATION_COMMAND.equalsIgnoreCase(userInputLine)) {
                // Check whether the user wants to switch to chat creation mode.
                serverOutput.println(userInputLine);
            } else {
                // If the user does not want to quit, send chat ID.
                try {
                    chatID = Integer.parseInt(userInputLine);
                    serverOutput.println(chatID);
                } catch (NumberFormatException e) {
                    System.out
                            .println("Failed to read \"" + userInputLine + "\". Please enter a valid integer.");
                }
            }
        }
        System.out.println("Quit chat select mode.");
    }

    private void directMode(SSLSocket socket, PrintWriter serverOutput, BufferedReader userInput) throws IOException {
        String userInputLine;
        if ((userInputLine = userInput.readLine()) != null) {
            // Check whether the user wants to quit first.
            if (HandleMode.EXIT_COMMAND.equalsIgnoreCase(userInputLine)) {
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
                                    processResponse(serverResponse);
                                }
                            } else {
                                break;
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
        HandleMode newHandleMode = HandleMode.stringToHandleMode(serverResponse);
        if (newHandleMode != null && handleMode != newHandleMode) {
            handleMode = newHandleMode;
            System.out.println("Switched handle mode: " + handleMode);
            synchronized (monitor) {
                monitor.notify();
            }
            return true;
        }
        return false;
    }

    private void processResponse(String serverResponse) {
        switch (handleMode) {
            case CHAT:
                try {
                    if (chatSymmetricKey == null) {
                        chatSymmetricKey = KeyUtils.readAESKey(chatID, keyFolder);
                    }
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
                if (challenge == null) {
                    try {
                        challenge = Base64.getDecoder().decode(serverResponse);
                        System.out.println("Received challenge: " + serverResponse);
                        synchronized (monitor) {
                            monitor.notify();
                        }
                    } catch (Exception e) {
                        System.out.println("Server response: " + serverResponse);
                    }
                }
                break;
            case CHAT_CREATION:
                try {
                    chatID = Integer.parseInt(serverResponse);
                    System.out.println("Got new chat ID: " + chatID);

                    // Wait for the new chat to be created on the database before saving the keys.
                    KeyUtils.saveRSAPrivateKey(chatID, chatPrivateKey, keyFolder);
                    KeyUtils.saveRSAPublicKey(chatID, chatPublicKey, keyFolder);
                    KeyUtils.saveAESKey(chatID, chatSymmetricKey, keyFolder);
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