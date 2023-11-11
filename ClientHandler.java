import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Stack;

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
        String chatID;
        String password;

        clientOutput.println("Please select a chat number."/*or type \"create\" to start creating a new chat."*/);
        while (!authorised) {
            // Ask for a chat.
            chatID = clientInput.readLine();

            // Confirm choice and ask for a password.
            clientOutput.println("You have selected " + chatID + ". Please enter the chat's password.");
            password = clientInput.readLine();

            // Check that both make sense.
            try {
                if (DBUtils.validChatCredentials(chatID, password)) {
                    authorised = true;

                    // Enter a server chat room.
                    chatBackend(chatID, password);

                    // Once the chat room has been exited, repeat the chat selection.
                    clientOutput.println("Exited chat. Please select another chat number or type exit to disconnect.");
                } else {
                    clientOutput.println("Invalid credentials. Please select a chat number.");
                }
            } catch (SQLException e) {
                clientOutput.println(
                        "Failed to check credentials against database. " + e.toString());
            }
        }
    }

    private void chatBackend(String chatID, String password) throws IOException {
        clientOutput.println(
                "Entered chat " + chatID
                        + ". Printing latest messages."/*Use \"/previous\" to load earlier messages."*/);
        try {
            Stack<String> messages = DBUtils.readChat(chatID, password);

            if (messages.empty()) {
                clientOutput.println("No messages to display.");
            } else {
                while (!messages.empty()) {
                    clientOutput.println(messages.pop());
                }
            }

            String inputLine;
            while ((inputLine = clientInput.readLine()) != null) {
                clientOutput.println("No further features programmed");
                System.out.println(inputLine);
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
