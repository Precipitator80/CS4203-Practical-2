import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Queue;

import javax.net.ssl.SSLSocket;

public class ClientHandler implements Runnable {
    public static List<ClientHandler> activeClientHandlers;
    SSLSocket clientSocket;
    BufferedReader clientInput;
    PrintWriter clientOutput;
    int chatID = -1;
    HandleModes handleMode;

    public ClientHandler(SSLSocket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            // Set up a reader and writer to transfer data between the client and server.
            clientInput = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientOutput = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        activeClientHandlers.add(this);
        try {
            System.out.println("Starting client handler.");
            chatSelect();

            // Close the client socket when done.
            System.out.println("Closing socket to client.");
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to handle a client. " + e.getMessage());
            closeEverything();
        }
        activeClientHandlers.remove(this);
    }

    private void chatSelect()
            throws IOException {
        clientOutput.println("Please select a chat number or type \"" + HandleModes.CHAT_CREATION_COMMAND
                + "\" to start creating a new chat.");
        while (!clientSocket.isClosed()) {
            switchHandleMode(HandleModes.CHAT_SELECT);

            // Ask for a chat.
            String userInputLine = clientInput.readLine();
            if (userInputLine.equals(HandleModes.CHAT_CREATION_COMMAND)) {
                chatCreation();
            } else {
                try {
                    chatID = Integer.parseInt(userInputLine);

                    // Confirm choice and ask for a password.
                    clientOutput.println("You have selected " + chatID + ". Checking key.");
                    challengeResponse();
                } catch (NumberFormatException e) {
                    clientOutput.println("Failed to read \"" + userInputLine + "\". Please enter a valid integer.");
                }
            }
        }
    }

    private void challengeResponse() throws IOException {
        switchHandleMode(HandleModes.CHALLENGE_RESPONSE);

        // Send a challenge to the client.
        SecureRandom random = new SecureRandom();
        byte[] challenge = new byte[245];
        random.nextBytes(challenge);
        String challengeString = Base64.getEncoder().encodeToString(challenge);
        clientOutput.println(challengeString);

        byte[] encryptedResponse = Base64.getDecoder().decode(clientInput.readLine());

        try {
            PublicKey publicKey = DBUtils.readChatKey(chatID);
            byte[] decryptedResponse = KeyUtils.decryptBytes(encryptedResponse, publicKey, KeyUtils.RSA);

            if (Arrays.equals(challenge, decryptedResponse)) {
                // Enter a server chat room.
                System.out.println("Passed challenge-response!");
                chat();
            } else {
                clientOutput.println("Invalid credentials. Please select a chat number.");
            }
        } catch (Exception e) {
            clientOutput.println(
                    "Failed to check credentials against database. " + e.getStackTrace().toString());
            e.printStackTrace();
        }
    }

    private void chatCreation() throws IOException {
        switchHandleMode(HandleModes.CHAT_CREATION);
        clientOutput.println(
                "Please write a name for your chat. Alternatively, type " + HandleModes.EXIT_COMMAND
                        + " to cancel chat creation.");

        String userInputLine = clientInput.readLine();
        if (userInputLine.equals(HandleModes.EXIT_COMMAND)) {
            return;
        }

        String chat_name = userInputLine;

        String chatPublicKeyString = clientInput.readLine();
        byte[] rsa_public_key = Base64.getDecoder().decode(chatPublicKeyString);
        System.out.println("Received public key bytes: " + chatPublicKeyString);
        try {
            chatID = DBUtils.createChat(chat_name, rsa_public_key);
            System.out.println("Created chat: " + chatID);
            clientOutput.println(chatID);
            System.out.println("Switching to new chat: " + chatID);
            chat(); // TODO Make sure handler can receive messages after chat creation.
        } catch (SQLException e) {
            System.out.println("Failed to create chat!");
            clientOutput.println("Failed to create chat! Returning to chat selection.");
        }
    }

    private void chat() throws IOException {
        switchHandleMode(HandleModes.CHAT);
        clientOutput.println(
                "Entered chat " + chatID
                        + ". Printing latest messages. Use " + HandleModes.PREVIOUS_COMMAND
                        + " with an offset value to load earlier messages (i.e. " + HandleModes.PREVIOUS_COMMAND
                        + " 10).");
        try {
            Queue<String> messages = DBUtils.readChat(chatID);

            if (messages.size() == 0) {
                clientOutput.println("No messages to display.");
            } else {
                while (messages.size() > 0) {
                    clientOutput.println(messages.poll());
                }
            }

            System.out.println("Waiting for user input.");

            String inputLine;
            while ((inputLine = clientInput.readLine()) != null) {
                System.out.println("Still waiting for user input.");
                if (inputLine.length() > 0) {
                    System.out.println("Got user input.");
                    if (inputLine.charAt(0) != '/') {
                        System.out.println("Sending message to DB.");
                        DBUtils.sendToChat(chatID, inputLine);

                        for (ClientHandler otherHandler : activeClientHandlers) {
                            if (otherHandler != this && otherHandler.handleMode == HandleModes.CHAT
                                    && chatID == otherHandler.chatID) {
                                otherHandler.clientOutput.println(inputLine);
                            }
                        }
                    } else {
                        if (inputLine.equals(HandleModes.EXIT_COMMAND)) {
                            System.out.println("User exited chat.");
                            break;
                        } else if (inputLine.contains(HandleModes.PREVIOUS_COMMAND)
                                && inputLine.length() >= HandleModes.PREVIOUS_COMMAND.length() + 2) {
                            inputLine = inputLine.substring(HandleModes.PREVIOUS_COMMAND.length() + 1);
                            try {
                                int previous = Integer.parseInt(inputLine);
                                messages = DBUtils.readChat(chatID, previous);

                                if (messages.size() == 0) {
                                    clientOutput.println("No messages to display.");
                                } else {
                                    while (messages.size() > 0) {
                                        clientOutput.println(messages.poll());
                                    }
                                }
                            } catch (NumberFormatException e) {
                                clientOutput.println(
                                        "Failed to read \"" + inputLine + "\". Please enter a valid integer.");
                            }
                        }
                    }
                }
            }

            System.out.println("Finished chat backend.");
        } catch (SQLException e) {
            clientOutput.println("Failed to read chat. Exiting chat.");
            clientOutput.println(e.toString());
        }

        // Once the chat room has been exited, repeat the chat selection.
        chatID = -1;
        switchHandleMode(HandleModes.CHAT_SELECT);
        clientOutput
                .println(
                        "Exited chat. Please select another chat number or type " + HandleModes.EXIT_COMMAND
                                + " to disconnect.");
    }

    private void switchHandleMode(HandleModes newHandleMode) {
        if (handleMode != newHandleMode) {
            clientOutput.println(newHandleMode);
            handleMode = newHandleMode;
        }
    }

    private void closeEverything() {
        System.out.println("Closing ClientHandler.");
        try {
            if (clientInput != null) {
                clientInput.close();
            }
            if (clientOutput != null) {
                clientOutput.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error while trying to close ClientHandler.");
        }
    }
}
