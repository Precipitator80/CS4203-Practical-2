// Adapted from example code:
// Java Echo Server Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java - Accessed 04.11.2023

import java.net.*;
import java.util.ArrayList;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import java.io.*;

public class SecureEchoServer extends AbstractEchoServer {
    public static void main(String[] args) throws IOException {
        SecureEchoServer echoServer = new SecureEchoServer();
        echoServer.readPort(args);
        echoServer.start();
    }

    @Override
    public void start() {
        SSLServerSocketFactory sslsocketfactory = SslUtil.getSSLServerSocketFactory("PEM Files/ca-cert.pem",
                "PEM Files/server-cert.pem",
                "PEM Files/server-key.pem");

        // Try to start the server on the provided port.
        try (SSLServerSocket serverSocket = (SSLServerSocket) sslsocketfactory.createServerSocket(port)) {
            System.out.println("Echo Server is running on port " + port);
            serverSocket.setNeedClientAuth(true);
            ClientHandler.activeClientHandlers = new ArrayList<ClientHandler>();

            // Keep the server running indefinitely.
            while (!serverSocket.isClosed()) {
                // Accept connections from clients attempting to connect.
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                try {
                    // Perform a handshake with the client.
                    clientSocket.startHandshake();
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    // Create a new thread to handle the client.
                    new Thread(new ClientHandler(clientSocket)).start();
                } catch (SSLHandshakeException e) {
                    // Handshake failed. Log the error and close the connection.
                    System.out.println("Handshake failed with client: " + clientSocket.getInetAddress());
                    clientSocket.close();
                } catch (SSLException e) {
                    System.out.println(
                            "Exception caught when trying to handle a client. This client may have been unauthorised.");
                    System.out.println(e.getMessage());
                    clientSocket.close();
                }
            }

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port "
                    + port + " or listening for a connection.");
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void handleClient(Socket clientSocket) {

    }
}
