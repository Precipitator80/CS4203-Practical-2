import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.sql.SQLException;
import java.util.Queue;

import javax.net.ssl.SSLSocket;

public class ClientHandler implements Runnable {
    SSLSocket clientSocket;
    BufferedReader clientInput;
    PrintWriter clientOutput;

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
    }

    private void chatSelect()
            throws IOException {
        int chatID;
        clientOutput.println(HandleModes.CHAT_SELECT_SIGNAL);
        clientOutput.println("Please select a chat number or type \"create\" to start creating a new chat.");
        while (!clientSocket.isClosed()) {
            // Ask for a chat.
            String userInputLine = clientInput.readLine();
            if (userInputLine.equals(HandleModes.CHAT_CREATION_COMMAND)) {
                chatCreation();
            } else {
                try {
                    chatID = Integer.parseInt(userInputLine);

                    // Confirm choice and ask for a password.
                    clientOutput.println("You have selected " + chatID + ". Checking key.");
                    clientOutput.println(HandleModes.CHALLENGE_RESPONSE_SIGNAL);

                    // Send a challenge to the client.
                    String challenge = "This is your challenge!";
                    clientOutput.println(challenge);
                    String encryptedResponse = clientInput.readLine();

                    try {
                        PublicKey publicKey = DBUtils.readChatKey(chatID);
                        String decryptedResponse = KeyUtils.decryptString(encryptedResponse, publicKey, KeyUtils.RSA);

                        if (challenge.equals(decryptedResponse)) {
                            // Enter a server chat room.
                            chatBackend(chatID);
                        } else {
                            clientOutput.println("Invalid credentials. Please select a chat number.");
                        }
                    } catch (Exception e) {
                        clientOutput.println("Failed to check credentials against database. " + e.toString());
                    }
                } catch (NumberFormatException e) {
                    clientOutput.println("Failed to read \"" + userInputLine + "\". Please enter a valid integer.");
                }
            }
        }
    }

    private void chatCreation() throws IOException {
        clientOutput.println(HandleModes.CHAT_CREATION_SIGNAL);
        clientOutput.println(
                "Please write a name for your chat. Alternatively, type " + HandleModes.EXIT_COMMAND
                        + " to cancel chat creation.");

        String userInputLine = clientInput.readLine();
        if (userInputLine.equals(HandleModes.EXIT_COMMAND)) {
            clientOutput.println(HandleModes.CHAT_SELECT_SIGNAL);
            return;
        }

        String chat_name = userInputLine;
        byte[] rsa_public_key = clientInput.readLine().getBytes();
        try {
            int chatID = DBUtils.createChat(chat_name, rsa_public_key);
            chatBackend(chatID);
        } catch (SQLException e) {
            System.out.println("Failed to create chat!");
            clientOutput.println("Failed to create chat! Returning to chat selection.");
        }
        clientOutput.println(HandleModes.CHAT_SELECT_SIGNAL);
    }

    private void chatBackend(int chatID) throws IOException {
        clientOutput.println(
                "Entered chat " + chatID
                        + ". Printing latest messages."/*Use \"/previous\" to load earlier messages."*/);
        clientOutput.println(HandleModes.CHAT_SIGNAL);
        try {
            Queue<String> messages = DBUtils.readChat(chatID);

            if (messages.size() == 0) {
                clientOutput.println("No messages to display.");
            } else {
                while (messages.size() > 0) {
                    clientOutput.println(messages.poll());
                }
            }

            String inputLine;
            while ((inputLine = clientInput.readLine()) != null) {
                if (inputLine.length() > 0) {
                    if (inputLine.charAt(0) != '/') {
                        DBUtils.sendToChat(chatID, inputLine);
                    } else {
                        if (inputLine.equals(HandleModes.EXIT_COMMAND)) {
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            clientOutput.println("Failed to read chat. Exiting chat.");
            clientOutput.println(e.toString());
        }

        // Once the chat room has been exited, repeat the chat selection.
        clientOutput.println(HandleModes.CHAT_SELECT_SIGNAL);
        clientOutput
                .println(
                        "Exited chat. Please select another chat number or type /exit to disconnect.");
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
