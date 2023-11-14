// Adapted from example code:

// Java Echo Server Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoServer.java - Accessed 04.11.2023

import java.util.ArrayList;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import Shared.SslUtil;

import java.io.*;

public class SecureChatServer {
    public static final int DEFAULT_PORT = 8000;
    public int port = DEFAULT_PORT;
    SSLServerSocketFactory sslsocketfactory;
    DBUtils dbUtility;

    public static void main(String[] args) throws IOException {
        new SecureChatServer(args).start();
    }

    public SecureChatServer(String[] args) {
        if (args.length > 5) {
            String caCrtFile = args[0];
            String crtFile = args[1];
            String keyFile = args[2];
            String dbURL = args[3];
            String dbUser = args[4];
            String dbPassword = args[5];
            sslsocketfactory = SslUtil.getSSLServerSocketFactory(caCrtFile, crtFile, keyFile);
            dbUtility = new DBUtils(dbURL, dbUser, dbPassword);

            try {
                if (args.length > 6) {
                    port = Integer.parseInt(args[0]);
                }
            } catch (NumberFormatException e) {
                System.err.println("Port must be a valid integer! Using default port instead!");
            }
            return;
        }
        throw new IllegalArgumentException(
                "Usage: java SecureChatServer <caCrtFile> <crtFile> <keyFile> <dbURL> <dbUser> <dbPassword> <port number [optional]>");
    }

    public void start() {
        if (sslsocketfactory == null || dbUtility == null) {
            System.err.println("Could not start server! SSLSocketFactory or DBUtils are null!");
            return;
        }
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
                    new Thread(new ClientHandler(clientSocket, dbUtility)).start();
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
}
