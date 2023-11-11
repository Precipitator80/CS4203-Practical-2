
import java.io.PrintWriter;
import java.net.*;

public abstract class AbstractEchoServer {
    static final int DEFAULT_PORT = 8000;
    int port = DEFAULT_PORT;

    protected void readPort(String[] args) {
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

    protected void echo(Socket clientSocket, String inputLine, PrintWriter out) {
        // Log the message to the server and echo it back to the client.
        System.out.println("Received from " + clientSocket.getInetAddress() + ": " + inputLine);
        out.println(inputLine);
    }

    abstract void start();

    abstract void handleClient(Socket clientSocket);
}