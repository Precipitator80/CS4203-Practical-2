package MiscellaneousFiles;

import java.io.PrintWriter;
import java.net.*;

public abstract class AbstractEchoServer {
    public static final int DEFAULT_PORT = 8000;
    public int port = DEFAULT_PORT;

    public void readPort(String[] args) {
        // Try to read a port from the args if specified.
        if (args.length == 1) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Usage: java EchoServer <port number>");
                System.exit(1);
            }
        }
    }

    public void echo(Socket clientSocket, String inputLine, PrintWriter out) {
        // Log the message to the server and echo it back to the client.
        System.out.println("Received from " + clientSocket.getInetAddress() + ": " + inputLine);
        out.println(inputLine);
    }

    public abstract void start();
}