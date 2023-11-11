// Adapted from example code:
// Java Echo Client Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java - Accessed 04.11.2023

import java.io.*;
import java.net.*;

//import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SecureEchoClient extends AbstractEchoClient {
    public static void main(String[] args) throws IOException {
        SecureEchoClient echoClient = new SecureEchoClient();
        echoClient.readServerData(args);
        echoClient.start();
    }

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
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Use a buffered reader to let the user send messages through the client.
                BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
            socket.startHandshake();
            System.out.println("Connected to Echo Server. Type 'exit' to quit.");

            // Wait for any messages from the server.
            listenForMessages(socket, userInput);

            // Wait for any input from the user to send to the server.
            String userInputLine;
            while (socket.isConnected()) {
                if ((userInputLine = userInput.readLine()) != null) {
                    // Send user input to the server.
                    out.println(userInputLine);
                }
                if ("exit".equalsIgnoreCase(userInputLine)) {
                    System.out.println("Closing socket.");
                    socket.close();
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostAddress);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostAddress + "\n" + e.toString());
            System.exit(1);
        }
    }

    public void listenForMessages(SSLSocket socket, BufferedReader in) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (socket.isConnected()) {
                    try {
                        // Get any messages from the server and print them.
                        String serverResponse;
                        if ((serverResponse = in.readLine()) != null) {
                            System.out.println("Server response: " + serverResponse);
                        }
                    } catch (IOException e) {
                        System.out.println(e.toString());
                    }
                }
            }
        }).start();
    }
}