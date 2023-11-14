import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
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
     * Attempts to read a given chat.
     * @param id The ID of the chat to read from.
     * @return Messages from the chat.
     */
    public static Queue<String> readChat(int id) throws SQLException {
        return readChat(id, 0);
    }

    /**
     * Attempts to read a given chat.
     * @param id The ID of the chat to read from.
     * @param offset_val An offset to allow for reading beyond the last few messages.
     * @return Messages from the cat.
     */
    public static Queue<String> readChat(int id, int offset_val) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = readChatPS(connection, id, offset_val);
                ResultSet resultSet = ps.executeQuery()) {
            Queue<String> messagesStack = new LinkedList<String>();
            while (resultSet.next()) {
                messagesStack.add(resultSet.getString(1));
            }
            return messagesStack;
        }
    }

    private static PreparedStatement readChatPS(Connection connection, int id, int offset_val)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("CALL proc_read_chat(?,?)");
        ps.setInt(1, id);
        ps.setInt(2, offset_val);
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
            if (resultSet.next()) {
                return KeyUtils.readRSAPublicKey(resultSet.getBytes(1));
            }
            return null;
        }
    }

    private static PreparedStatement readChatKeyPS(Connection connection, int id)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT rsa_public_key FROM chat WHERE id = ?");
        ps.setInt(1, id);
        return ps;
    }

    /**
    * Creates a new chat.
    * How to call a stored procedure that returns output parameters, using JDBC program? - Jennifer Nicholas - https://www.tutorialspoint.com/how-to-call-a-stored-procedure-that-returns-output-parameters-using-jdbc-program - Accessed 06.04.2023
    * @param chat_name The encrypted name of the chat.
    * @param rsa_public_key The public key for the chat.
    * @return The ID of the new chat.
    */
    public static int createChat(String chat_name, byte[] rsa_public_key)
            throws SQLException {
        try (Connection connection = openConnection();
                CallableStatement cs = createChatPS(connection, chat_name, rsa_public_key);) {
            System.out.println("Public key length: " + rsa_public_key.length);
            cs.executeUpdate();
            return cs.getInt(3);
        }
    }

    private static CallableStatement createChatPS(Connection connection, String chat_name, byte[] rsa_public_key)
            throws SQLException {
        CallableStatement cs = connection.prepareCall("CALL proc_create_chat(?,?,?)");
        cs.setString(1, chat_name);
        cs.setBytes(2, rsa_public_key);
        cs.registerOutParameter(3, Types.INTEGER);
        return cs;
    }
}
