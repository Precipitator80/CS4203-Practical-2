package MiscellaneousFiles;
// Adapted from example code:

// Java Echo Client Example Code - Oracle - https://docs.oracle.com/javase/tutorial/networking/sockets/examples/EchoClient.java - Accessed 04.11.2023

public abstract class AbstractEchoClient {
    public static final String DEFAULT_HOST_ADDRESS = "localhost";
    public static final int DEFAULT_PORT_NUMBER = 8000;

    public String hostAddress = DEFAULT_HOST_ADDRESS;
    public int port = DEFAULT_PORT_NUMBER;

    public void readServerData(String[] args) {
        // Try to read a host information from the args if specified.
        if (args.length == 2) {
            try {
                hostAddress = args[0];
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println(
                        "Usage: java EchoClient <host address> <port number>");
                System.exit(1);
            }
        }
    }

    public abstract void start();
}