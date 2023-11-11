// Adapted from example code:
// Java Echo Client Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java - Accessed 04.11.2023

import java.io.*;
import java.net.*;

public class SimpleEchoClient extends AbstractEchoClient {
    public static void main(String[] args) throws IOException {
        SimpleEchoClient echoClient = new SimpleEchoClient();
        echoClient.readServerData(args);
        echoClient.start();
    }

    @Override
    public void start() {
        // Try to connect to the server specified.
        try (
                // Create a socket to connect to the server.
                Socket socket = new Socket(hostAddress, port);

                // Set up a reader and writer to transfer data between the client and server.
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Use a buffered reader to let the user send messages through the client.
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Connected to Echo Server. Type 'exit' to quit.");

            // Wait for any input from the user to send to the server.
            String userInputLine;
            while ((userInputLine = userInput.readLine()) != null) {
                // Send user input to the server.
                out.println(userInputLine);

                // Get the server's response and print it.
                String serverResponse = in.readLine();
                System.out.println("Server response: " + serverResponse);

                // Let the user exit when typing "exit".
                if ("exit".equalsIgnoreCase(userInputLine)) {
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostAddress);
            System.exit(1);
        }
    }
}