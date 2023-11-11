// Adapted from example code:
// Java Echo Client Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java - Accessed 04.11.2023

import java.io.*;
import java.net.*;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SecureEchoClient extends AbstractEchoClient {
    public static void main(String[] args) throws IOException {
        SecureEchoClient echoClient = new SecureEchoClient();
        echoClient.readServerData(args);
        echoClient.start();
    }

    boolean shuttingDown;

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
            System.out.println("Connected to Echo Server. Type 'exit' to quit.");

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
        while (socket.isConnected()) {
            if ((userInputLine = userInput.readLine()) != null) {
                // Send user input to the server.
                serverOutput.println(userInputLine);
            }
            if ("exit".equalsIgnoreCase(userInputLine)) {
                shuttingDown = true;
                System.out.println("Closing socket and shutting down client.");
                socket.close();
                break;
            }
        }
    }

    // Java Socket Programming - Multiple Clients Chat - WittCode - https://youtu.be/gLfuZrrfKes - Accessed 11.11.2023
    public void listenForMessages(SSLSocket socket, BufferedReader serverInput) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (socket.isConnected()) {
                        // Get any messages from the server and print them.
                        String serverResponse;
                        if ((serverResponse = serverInput.readLine()) != null) {
                            if (!shuttingDown) {
                                System.out.println("Server response: " + serverResponse);
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

}