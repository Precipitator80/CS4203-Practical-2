import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Utilities class that performs database actions for the application.
 * How should I use try-with-resources with JDBC? - Jeanne Boyarsky - https://stackoverflow.com/questions/8066501/how-should-i-use-try-with-resources-with-jdbc - Accessed 06.04.2023
 */
public class DBUtils {

    //final static String DB_URL = "jdbc:mariadb://localhost/cs3101p2";
    //final static String USER = "root";
    //final static String PASSWORD = "toor";

    // Variables to allow the controller to connect to the database.
    final static String DB_URL = "jdbc:mariadb://bms1.teaching.cs.st-andrews.ac.uk/bms1_cs4203p2";
    final static String USER = "bms1";
    final static String PASSWORD = "3fpj3!3JZ2x5zT";

    /**
     * Standard method to open a connection to the database.
     * @return A connection to the database.
     * @throws SQLException
     */
    public static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASSWORD);
    }

    /**
     * Testing method that prints out all the entries in a ResultSet.
     * @param resultSet The ResultSet to print.
     */
    public static void printResultsSet(ResultSet resultSet) {
        if (resultSet != null) {
            System.out.println("Got results set");
            try {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                System.out.println("Column count is " + columnCount);
                while (resultSet.next()) {
                    System.out.println("Getting things from resultSet");
                    for (int i = 1; i <= columnCount; i++) {
                        System.out.print(resultSet.getString(i));
                        System.out.print(", ");
                    }
                    System.out.println();
                }
            } catch (SQLException e) {
                System.out.println(e.toString());
            }
        } else {
            System.out.println("The results set passed to printResultsSet is null!");
        }
    }

    /**
     * Checks whether login details for a chat are valid.
     * @param id The ID of the chat to enter.
     * @param password The password of the chat.
     * @return Whether the login details are valid.
     * @throws SQLException
     */
    public static boolean validChatCredentials(int id, String password) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = validChatCredentialsPS(connection, id, password);
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    private static PreparedStatement validChatCredentialsPS(Connection connection, int id, String password)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT func_valid_chat_credentials(?,?)");
        ps.setInt(1, id);
        ps.setString(2, password);
        return ps;
    }

    /**
     * Attempts to read a given chat, passing in credentials.
     * @param id The ID of the chat to enter.
     * @param password The password of the chat.
     * @return Messages from the chat.
     * @throws SQLException
     */
    public static Queue<String> readChat(int id) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = readChatPS(connection, id);
                ResultSet resultSet = ps.executeQuery()) {
            Queue<String> messagesStack = new LinkedList<String>();
            while (resultSet.next()) {
                messagesStack.add(resultSet.getString(1));
            }
            return messagesStack;
        }
    }

    private static PreparedStatement readChatPS(Connection connection, int id)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("CALL proc_read_chat(?)");
        ps.setInt(1, id);
        return ps;
    }

    public static void sendToChat(int chat_id, String chat_line) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = sendToChatPS(connection, chat_id, chat_line);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    private static PreparedStatement sendToChatPS(Connection connection, int chat_id, String chat_line)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("INSERT INTO chat_line (chat_id, line_text) VALUES (?,?)");
        ps.setInt(1, chat_id);
        ps.setString(2, chat_line);
        return ps;
    }

    public static PublicKey readChatKey(int id)
            throws SQLException, NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        try (Connection connection = openConnection();
                PreparedStatement ps = readChatKeyPS(connection, id);
                ResultSet resultSet = ps.executeQuery()) {
            return KeyUtils.readRSAPublicKey(resultSet.getBytes(1));
        }
    }

    private static PreparedStatement readChatKeyPS(Connection connection, int id)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT rsa_public_key FROM chat WHERE id = ?");
        ps.setInt(1, id);
        return ps;
    }
}
