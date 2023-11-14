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
    HandleModes handleMode;
    int currentChat = -1;

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
            if (HandleModes.CHAT_CREATION_COMMAND.equalsIgnoreCase(userInputLine)) {
                chatCreation();
            } else {
                try {
                    int chatID = Integer.parseInt(userInputLine);

                    // Confirm choice and ask for a password.
                    clientOutput.println("You have selected " + chatID + ". Checking key.");
                    challengeResponse(chatID);
                } catch (NumberFormatException e) {
                    clientOutput.println("Failed to read \"" + userInputLine + "\". Please enter a valid integer.");
                    continue;
                }
            }

            // Once the chat room has been exited, repeat the chat selection.
            clientOutput.println(
                    "Please select another chat number or type " + HandleModes.EXIT_COMMAND + " to disconnect.");
        }
    }

    private void challengeResponse(int chatID) throws IOException {
        switchHandleMode(HandleModes.CHALLENGE_RESPONSE);

        // Send a challenge to the client by generating random bytes to encrypt.
        SecureRandom random = new SecureRandom();
        byte[] challenge = new byte[245];
        random.nextBytes(challenge);
        String challengeString = Base64.getEncoder().encodeToString(challenge);
        clientOutput.println(challengeString);

        // Wait for the response from the client.
        byte[] encryptedResponse = Base64.getDecoder().decode(clientInput.readLine());
        try {
            // Read the chat's public key from the database to decrypt the response.
            PublicKey publicKey = DBUtils.readChatKey(chatID);
            byte[] decryptedResponse = KeyUtils.decryptBytes(encryptedResponse, publicKey, KeyUtils.RSA);

            // If the decrypted response is the same as the original challenge, let the client enter the chat.
            if (Arrays.equals(challenge, decryptedResponse)) {
                // Enter a server chat room.
                System.out.println("Passed challenge-response!");
                chat(chatID);
            } else {
                clientOutput.println("Invalid credentials.");
            }
        } catch (Exception e) {
            clientOutput.println("Failed to check credentials against database. " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void chatCreation() throws IOException {
        switchHandleMode(HandleModes.CHAT_CREATION);
        clientOutput.println("Please write a name for your chat. Alternatively, type " + HandleModes.EXIT_COMMAND
                + " to cancel chat creation.");

        // Ask the user for a chat name.
        String chatName = clientInput.readLine();

        // Check whether the user wants to cancel chat creation.
        if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(chatName)) {
            return;
        }

        // If the user didn't quit, wait for the client to generate keys and send over the public key.
        String chatPublicKeyString = clientInput.readLine();
        byte[] rsa_public_key = Base64.getDecoder().decode(chatPublicKeyString);
        System.out.println("Received public key bytes: " + chatPublicKeyString);

        // Attempt to create a chat with the given name and public key.
        try {
            int chatID = DBUtils.createChat(chatName, rsa_public_key);
            System.out.println("Created chat: " + chatID + ". Switching to it now.");
            chat(chatID); // TODO Make sure handler can receive messages after chat creation.
        } catch (SQLException e) {
            System.out.println("Failed to create chat!");
            clientOutput.println("Failed to create chat!");
        }
    }

    private void chat(int chatID) throws IOException {
        currentChat = chatID;
        switchHandleMode(HandleModes.CHAT);
        try {
            clientOutput.println("Entered chat " + chatID);
            clientOutput.println("Chat name:");
            clientOutput.println(DBUtils.getChatName(chatID));
            clientOutput.println("Printing latest messages. Use " + HandleModes.PREVIOUS_COMMAND
                    + " with an offset value to load earlier messages (i.e. " + HandleModes.PREVIOUS_COMMAND + " 25).");

            readChat(chatID, 0);

            System.out.println("Waiting for user input.");
            String inputLine;
            while ((inputLine = clientInput.readLine()) != null) {
                System.out.println("Got user input.");
                if (inputLine.length() > 0) {
                    if (inputLine.charAt(0) != '/') {
                        System.out.println("Sending message to DB.");
                        try {
                            DBUtils.sendToChat(chatID, inputLine);
                        } catch (SQLException e) {
                            clientOutput.println("Failed to send message!");
                        }

                        // Sync the message with other handlers in the same chat.
                        for (ClientHandler otherHandler : activeClientHandlers) {
                            if (otherHandler != this && otherHandler.handleMode == HandleModes.CHAT
                                    && chatID == otherHandler.currentChat) {
                                otherHandler.clientOutput.println(inputLine);
                            }
                        }
                    } else {
                        if (HandleModes.EXIT_COMMAND.equalsIgnoreCase(inputLine)) {
                            System.out.println("User exited chat.");
                            break;
                        } else if (inputLine.contains(HandleModes.PREVIOUS_COMMAND)
                                && inputLine.length() >= HandleModes.PREVIOUS_COMMAND.length() + 2) {
                            inputLine = inputLine.substring(HandleModes.PREVIOUS_COMMAND.length() + 1);
                            try {
                                int offset = Integer.parseInt(inputLine);
                                readChat(chatID, offset);
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
        currentChat = -1;
    }

    private void readChat(int id, int offset_val) throws SQLException {
        Queue<String> messages = DBUtils.readChat(id, offset_val);

        if (messages.size() == 0) {
            clientOutput.println("No messages to display.");
        } else {
            while (messages.size() > 0) {
                clientOutput.println(messages.poll());
            }
        }
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
