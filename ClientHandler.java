import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Stack;

import javax.net.ssl.SSLSocket;

public class ClientHandler implements Runnable {
    SSLSocket clientSocket;
    BufferedReader in;
    PrintWriter out;

    public ClientHandler(SSLSocket clientSocket, BufferedReader in, PrintWriter out) {
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            chatSelect();

            // Close the client socket when done.
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to handle a client.");
            System.out.println(e.getMessage());
        }
    }

    private void chatSelect()
            throws IOException {
        boolean authorised = false;
        String chatID;
        String password;

        out.println("Please select a chat number."/*or type \"create\" to start creating a new chat."*/);
        while (!authorised) {
            // Ask for a chat.
            chatID = in.readLine();

            // Confirm choice and ask for a password.
            out.println("You have selected " + chatID + ". Please enter the chat's password.");
            password = in.readLine();

            // Check that both make sense.
            try {
                if (DBUtils.validChatCredentials(chatID, password)) {
                    // Enter a server chat room.
                    chatBackend(chatID, password);

                    // Once the chat room has been exited, repeat the chat selection.
                    out.println("Exited chat. Please select another chat number or type exit to disconnect.");
                } else {
                    out.println("Invalid credentials. Please select a chat number.");
                }
            } catch (SQLException e) {
                out.println(
                        "Failed to check credentials against database. Please select a chat number. " + e.toString());
            }
        }
    }

    private void chatBackend(String chatID, String password) throws IOException {
        out.println(
                "Entered chat " + chatID
                        + ". Printing latest messages."/*Use \"/previous\" to load earlier messages."*/);
        try {
            Stack<String> messages = DBUtils.readChat(chatID, password);
            while (!messages.empty()) {
                out.println(messages.pop());
            }

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                out.println("No further features programmed");
                System.out.println(inputLine);
            }
        } catch (SQLException e) {
            out.println("Failed to read chat. Exiting chat.");
        }
    }
}
