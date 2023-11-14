package MiscellaneousFiles;
// Adapted from example code:

// Java Echo Server Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java - Accessed 04.11.2023

import java.net.*;

import java.io.*;

public class SimpleEchoServer extends AbstractEchoServer {
    public static void main(String[] args) throws IOException {
        SimpleEchoServer echoServer = new SimpleEchoServer();
        echoServer.readPort(args);
        echoServer.start();
    }

    @Override
    public void start() {
        // Try to start the server on the provided port.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Echo Server is running on port " + port);

            // Keep the server running indefinitely.
            while (true) {
                // Accept connections from clients attempting to connect.
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Create a new thread to handle the client. This allows multiple clients to connect.
                new Thread(() -> handleClient(clientSocket)).start();
            }

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + port + " or listening for a connection.");
            System.out.println(e.getMessage());
        }
    }

    public void handleClient(Socket clientSocket) {
        try (
                // Set up a reader and writer to transfer data between the client and server.
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Wait for any input from the client and echo it back.
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                echo(clientSocket, inputLine, out);
            }

            // Close the client socket when done.
            clientSocket.close();
        } catch (IOException e) {
            System.out.println("Exception caught when trying to handle a client.");
            System.out.println(e.getMessage());
        }
    }
}