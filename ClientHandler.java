import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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
        boolean authorised = false;
        int chatID;

        clientOutput.println("Please select a chat number."/*or type \"create\" to start creating a new chat."*/);
        while (!authorised) {
            String userInput = clientInput.readLine();
            try {
                // Ask for a chat.
                chatID = Integer.parseInt(userInput);

                // Confirm choice and ask for a password.
                clientOutput.println("You have selected " + chatID + ". Checking key.");
                clientOutput.println(HandleModes.CHALLENGE_RESPONSE_STRING);

                // Send a challenge to the client.
                // FOR NOW JUST ALLOW THEM IN
                try {
                    if (true) {
                        authorised = true;

                        // Enter a server chat room.
                        chatBackend(chatID);

                        // Once the chat room has been exited, repeat the chat selection.
                        clientOutput
                                .println("Exited chat. Please select another chat number or type exit to disconnect.");
                    } else {
                        clientOutput.println("Invalid credentials. Please select a chat number.");
                    }
                } catch (/*SQL*/Exception e) {
                    clientOutput.println(
                            "Failed to check credentials against database. " + e.toString());
                }
            } catch (NumberFormatException e) {
                clientOutput.println("Failed to read \"" + userInput + "\". Please enter a valid integer.");
            }
        }
    }

    private void chatBackend(int chatID) throws IOException {
        clientOutput.println(
                "Entered chat " + chatID
                        + ". Printing latest messages."/*Use \"/previous\" to load earlier messages."*/);
        clientOutput.println(HandleModes.CHAT_STRING);
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
                if (inputLine.length() > 0 && inputLine.charAt(0) != '/') {
                    DBUtils.sendToChat(chatID, inputLine);
                }
            }
        } catch (SQLException e) {
            clientOutput.println("Failed to read chat. Exiting chat.");
            clientOutput.println(e.toString());
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
