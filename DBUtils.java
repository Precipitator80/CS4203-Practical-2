
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
// import java.util.function.UnaryOperator;
import java.util.Stack;

/**
 * Utilities class that performs database actions for the application.
 * JavaFX Login and Signup Form with Database Connection - Thomas Wittmann - https://youtu.be/ltX5AtW9v30 - Accessed 18.03.2023
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

    // // Filter to allow only positive numbers in ID and table fields.
    // // Numeric TextField for Integers in JavaFX 8 with TextFormatter and/or UnaryOperator - James_D - https://stackoverflow.com/questions/40472668/numeric-textfield-for-integers-in-javafx-8-with-textformatter-and-or-unaryoperat - Accessed 03.04.2023
    // public static UnaryOperator<Change> integerFilter = change -> {
    //     String newText = change.getControlNewText();
    //     if (newText.matches("([1-9][0-9]*)?")) {
    //         return change;
    //     }
    //     /* Allows negatives
    //     if (newText.matches("-?([1-9][0-9]*)?")) {
    //         return change;
    //     }
    //     */
    //     return null;
    // };

    // // Filter to allow 0-5 length alphabetical strings in invitation code fields.
    // public static UnaryOperator<Change> invitationCodeFilter = change -> {
    //     String newText = change.getControlNewText();
    //     if (newText.length() < 6 && newText.matches("[A-z]*")) {
    //         return change;
    //     }
    //     return null;
    // };

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
    public static boolean validChatCredentials(String id, String password) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = validChatCredentialsPS(connection, id, password);
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    private static PreparedStatement validChatCredentialsPS(Connection connection, String id, String password)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT func_valid_chat_credentials(?,?)");
        ps.setString(1, id);
        ps.setString(2, password);
        return ps;
    }

    /**
     * Attemps to read a given chat, passing in credentials.
     * @param id The ID of the chat to enter.
     * @param password The password of the chat.
     * @return Messages from the chat.
     * @throws SQLException
     */
    public static Stack<String> readChat(String id, String password) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = readChatPS(connection, id, password);
                ResultSet resultSet = ps.executeQuery()) {
            Stack<String> messagesStack = new Stack<String>();
            while (resultSet.next()) {
                messagesStack.add(resultSet.getString(1));
            }
            return messagesStack;
        }
    }

    private static PreparedStatement readChatPS(Connection connection, String id, String password)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT func_read_chat(?,?)");
        ps.setString(1, id);
        ps.setString(2, password);
        return ps;
    }

    /**
     * Generates a new invitation code.
     * How do I generate a unique, random string for one of my MySql table columns? - Rick James - https://stackoverflow.com/questions/39257391/how-do-i-generate-a-unique-random-string-for-one-of-my-mysql-table-columns - Accessed 19.03.2023
     * @return The generated invitation code.
     * @throws SQLException
     */
    public static String generateInvitationCode() throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = connection.prepareStatement("SELECT func_generate_invitation_string()");
                ResultSet resultSet = ps.executeQuery()) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    /**
     * Checks whether login details for an organiser are valid.
     * @param id The ID of the organiser.
     * @param password The password of the organiser.
     * @return true if the login details match an entry in the database.
     * @throws SQLException
     */
    public static boolean validOrganiserLogin(String id, String password) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = validOrganiserLoginPS(connection, id, password);
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    private static PreparedStatement validOrganiserLoginPS(Connection connection, String id, String password)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT func_valid_organiser_login(?,?)");
        ps.setString(1, id);
        ps.setString(2, password);
        return ps;
    }

    /**
     * Checks whether a person is an organiser.
     * @param id The ID of the organiser.
     * @return true if the person is an organiser.
     * @throws SQLException
     */
    public static boolean isOrganiser(int id) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = isOrganiserPS(connection, id);
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    private static PreparedStatement isOrganiserPS(Connection connection, int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT func_is_organiser(?)");
        ps.setInt(1, id);
        return ps;
    }

    /**
     * Defines a person as an organiser.
     * @param id The ID of the person.
     * @param password The password to use for logging in.
     * @throws SQLException
     */
    public static void addOrganiser(int id, String password) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = addOrganiserPS(connection, id, password);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    private static PreparedStatement addOrganiserPS(Connection connection, int id, String password)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("INSERT INTO organiser VALUES (?,?)");
        ps.setInt(1, id);
        ps.setString(2, password);
        return ps;
    }

    /**
     * Gets relevant information on all people associated with a certain invitation code.
     * @param invitation_code The invitation code to look up.
     * @return A ResultSet of all the matching people.
     * @throws SQLException
     */
    public static ResultSet getPeopleByInvitationString(String invitation_code) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = getPeopleByInvitationStringPS(connection, invitation_code);
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet;
        }
    }

    private static PreparedStatement getPeopleByInvitationStringPS(Connection connection, String invitation_code)
            throws SQLException {
        PreparedStatement ps = connection
                .prepareStatement("SELECT id, full_name, response FROM person WHERE invitation_code = ?");
        ps.setString(1, invitation_code);
        return ps;
    }

    /**
     * Gets information on all the people in the database relevant to the organiser page.
     * @return A ResultSet of all people.
     * @throws SQLException
     */
    public static ResultSet getAllPeople() throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = connection.prepareStatement("SELECT id, full_name, table_no FROM person");
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet;
        }
    }

    /**
     * Gets all the people in the database who have not responded to an invitation.
     * @return A ResultSet of all people without a response.
     * @throws SQLException
     */
    public static ResultSet getAllPeopleNotResponded() throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = connection
                        .prepareStatement("SELECT id, full_name, table_no FROM person WHERE response IS NULL");
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet;
        }
    }

    /**
     * Adds a person to the database.
     * How to call a stored procedure that returns output parameters, using JDBC program? - Jennifer Nicholas - https://www.tutorialspoint.com/how-to-call-a-stored-procedure-that-returns-output-parameters-using-jdbc-program - Accessed 06.04.2023
     * @param full_name The full name of the person to add.
     * @param notes Any notes on the person.
     * @param invitation_code The invitation code they should be associated with.
     * @param table_no The table number to assign them to.
     * @return The ID of the inserted person.
     * @throws SQLException
     */
    public static int addPerson(String full_name, String notes, String invitation_code, int table_no)
            throws SQLException {
        try (Connection connection = openConnection();
                CallableStatement cs = addPersonPS(connection, full_name, notes, invitation_code, table_no);) {
            cs.executeUpdate();
            return cs.getInt(5);
        }
    }

    private static CallableStatement addPersonPS(Connection connection, String full_name, String notes,
            String invitation_code,
            int table_no)
            throws SQLException {
        CallableStatement cs = connection.prepareCall("CALL proc_add_person(?,?,?,?,?)");
        cs.setString(1, full_name);
        cs.setString(2, notes);
        cs.setString(3, invitation_code);
        cs.setInt(4, table_no);
        cs.registerOutParameter(5, Types.INTEGER);
        return cs;
    }

    /**
     * Updates an invitation's address and date.
     * @param invitation_code The code of the invitaton to update.
     * @param address The address to set the invitation to.
     * @param date_sent The date to set the invitation to.
     * @throws SQLException
     */
    public static void updateInvitation(String invitation_code, String address, Date date_sent) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = updateInvitationPS(connection, invitation_code, address, date_sent);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    private static PreparedStatement updateInvitationPS(Connection connection, String invitation_code, String address,
            Date date_sent)
            throws SQLException {
        PreparedStatement ps = connection
                .prepareStatement("UPDATE invitation SET address = ?, date_sent = ? WHERE code = ?");
        ps.setString(1, address);
        ps.setDate(2, date_sent);
        ps.setString(3, invitation_code);
        return ps;
    }

    /**
     * Gets a list of all table numbers.
     * @return A list of all table numbers.
     * @throws SQLException
     */
    public static List<Integer> getTableNumbers() throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = connection
                        .prepareStatement("SELECT table_no FROM dinner_table ORDER BY table_no");
                ResultSet resultSet = ps.executeQuery()) {
            List<Integer> tableNumbers = new ArrayList<Integer>();
            while (resultSet.next()) {
                tableNumbers.add(resultSet.getInt(1));
            }
            return tableNumbers;
        }
    }

    /**
     * Adds a new table to the database.
     * @param tableNumber The number of the table.
     * @param capacity The capacity of the table.
     * @throws SQLException
     */
    public static void addTable(int tableNumber, int capacity) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = addTablePS(connection, tableNumber, capacity);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    private static PreparedStatement addTablePS(Connection connection, int tableNumber,
            int capacity)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("INSERT INTO dinner_table VALUES (?,?)");
        ps.setInt(1, tableNumber);
        ps.setInt(2, capacity);
        return ps;
    }

    /**
     * Updates the table a person is assigned to.
     * @param person_id The ID of the person.
     * @param table_no The table to change them to.
     * @throws SQLException
     */
    public static void updatePersonTable(int person_id, int table_no) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = updatePersonTablePS(connection, person_id, table_no);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    private static PreparedStatement updatePersonTablePS(Connection connection, int person_id,
            int table_no)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("UPDATE person SET table_no = ? WHERE id = ?");
        ps.setInt(1, table_no);
        ps.setInt(2, person_id);
        return ps;
    }

    /**
     * Checks whether a guest has a certain dietary requirement.
     * @param person_id The ID of the person to check.
     * @param dietary_requirement_name The name of the dietary requirement to check.
     * @return true if the person has the dietary requirement.
     * @throws SQLException
     */
    public static boolean guestHasDiet(int person_id, String dietary_requirement_name) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = guestHasDietPS(connection, person_id, dietary_requirement_name);
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    private static PreparedStatement guestHasDietPS(Connection connection, int person_id,
            String dietary_requirement_name)
            throws SQLException {
        PreparedStatement ps = connection
                .prepareStatement("SELECT * FROM guest_diet WHERE person_id = ? AND dietary_requirement_name = ?");
        ps.setInt(1, person_id);
        ps.setString(2, dietary_requirement_name);
        return ps;
    }

    /**
     * Adds or removes a guest diet entry to or from the database.
     * INSERT VALUES WHERE NOT EXISTS - Gareth Davison - https://stackoverflow.com/questions/17991479/insert-values-where-not-exists - Accessed 02.04.2023
     * @param person_id The ID of the person to change.
     * @param dietary_requirement_name The name of the dietary requirement.
     * @param add Whether to add or remove the requirement.
     * @throws SQLException
     */
    public static void setGuestDiet(int person_id, String dietary_requirement_name, boolean add) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = setGuestDietPS(connection, person_id, dietary_requirement_name, add);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    private static PreparedStatement setGuestDietPS(Connection connection, int person_id,
            String dietary_requirement_name,
            boolean add)
            throws SQLException {
        PreparedStatement ps;
        if (add) {
            // ps = connection.prepareStatement("IF NOT EXISTS (SELECT * FROM guest_diet WHERE person_id = ? AND dietary_requirement_name = ?) THEN INSERT INTO guest_diet VALUES (?,?); END IF");
            ps = connection.prepareStatement("INSERT INTO guest_diet VALUES (?,?)");
            ps.setInt(1, person_id);
            ps.setString(2, dietary_requirement_name);
            ps.setInt(3, person_id);
            ps.setString(4, dietary_requirement_name);
        } else {
            ps = connection
                    .prepareStatement("DELETE FROM guest_diet WHERE person_id = ? AND dietary_requirement_name = ?");
            ps.setInt(1, person_id);
            ps.setString(2, dietary_requirement_name);
        }
        return ps;
    }

    /**
     * Updates a guest's response.
     * @param id The ID of the person.
     * @param response A non-primitive Boolean to represent the response value or no response.
     * @throws SQLException
     */
    public static void setGuestResponse(int id, Boolean response) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = setGuestResponsePS(connection, id, response);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    private static PreparedStatement setGuestResponsePS(Connection connection, int id, Boolean response)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("UPDATE person SET response = ? WHERE id = ?");
        ps.setObject(1, response);
        ps.setInt(2, id);
        return ps;
    }

    /**
     * Adds a new dietary requirement to the database.
     * @param short_name The short name of the requirement.
     * @param description The description of the requirement.
     * @throws SQLException
     */
    public static void addDietaryRequirement(String short_name, String description) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = addDietaryRequirementPS(connection, short_name, description);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    public static PreparedStatement addDietaryRequirementPS(Connection connection, String short_name,
            String description)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("INSERT INTO dietary_requirement VALUES (?,?)");
        ps.setString(1, short_name);
        ps.setString(2, description);
        return ps;
    }

    /**
     * Gets all dietary requirements.
     * @return A ResultSet of all dietary requirements.
     * @throws SQLException
     */
    public static ResultSet getDietaryRequirements() throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = connection.prepareStatement("SELECT * FROM dietary_requirement");
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet;
        }
    }

    /**
     * Checks whether a passed invitation code exists in the database.
     * @param invitation_code The invitation code to check.
     * @return true if the code is in the database.
     * @throws SQLException
     */
    public static boolean invitationExists(String invitation_code) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = invitationExistsPS(connection, invitation_code);
                ResultSet resultSet = ps.executeQuery()) {
            return resultSet.next() && resultSet.getBoolean(1);
        }
    }

    public static PreparedStatement invitationExistsPS(Connection connection, String invitation_code)
            throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT code FROM invitation WHERE code = ?");
        ps.setString(1, invitation_code);
        return ps;
    }

    /**
     * Removes a person from the database.
     * @param id The ID of the person to remove.
     * @throws SQLException
     */
    public static void removePerson(int id) throws SQLException {
        try (Connection connection = openConnection();
                PreparedStatement ps = removePersonPS(connection, id);
                ResultSet resultSet = ps.executeQuery()) {
        }
    }

    public static PreparedStatement removePersonPS(Connection connection, int id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM person WHERE id = ?");
        ps.setInt(1, id);
        return ps;
    }

    // /**
    //  * Adds a person from the wedding data spreadsheet to the database.
    //  * @param person The person to add.
    //  */
    // public static void addPersonFromSpreadsheet(SpreadsheetPerson person) {
    //     // Add the person to the database.
    //     int id;
    //     try {
    //         id = addPerson(person.full_name, person.notes, person.invitation_code, person.table_no);
    //     } catch (SQLException e) {
    //         System.out.println("Could not add person to database!\n" + e.toString());
    //         return;
    //     }

    //     // Set the person as an organiser if applicable.
    //     if (person.is_organiser) {
    //         try {
    //             addOrganiser(id, person.password);
    //         } catch (SQLException e) {
    //             System.out.println("Could not set person as organiser!\n" + e.toString());
    //             return;
    //         }
    //     }

    //     // Set the person's response.
    //     try {
    //         setGuestResponse(id, person.response);
    //     } catch (SQLException e) {
    //         System.out
    //                 .println("Could not set guest response!\n" + e.toString() + ". Their table is " + person.table_no);
    //         return;
    //     }

    //     // Update the invitation of the person.
    //     // Convert from a java.util.Date Object to a java.sql.Date Object : Date « Data Type « Java Tutorial - Java2s - http://www.java2s.com/Tutorial/Java/0040__Data-Type/ConvertfromajavautilDateObjecttoajavasqlDateObject.htm - Accessed 04.04.2023
    //     try {
    //         updateInvitation(person.invitation_code, person.address, new java.sql.Date(person.date_sent.getTime()));
    //     } catch (SQLException e) {
    //         System.out.println("Could not update invitation!\n" + e.toString());
    //         return;
    //     }

    //     try {
    //         if (person.vegetarian) {
    //             setGuestDiet(id, SpreadsheetDietaryRequirement.VEGETARIAN_SHORT_NAME, true);
    //         }
    //         if (person.vegan) {
    //             setGuestDiet(id, SpreadsheetDietaryRequirement.VEGAN_SHORT_NAME, true);
    //         }
    //         if (person.gluten_free) {
    //             setGuestDiet(id, SpreadsheetDietaryRequirement.GLUTEN_FREE_SHORT_NAME, true);
    //         }
    //         if (person.halal) {
    //             setGuestDiet(id, SpreadsheetDietaryRequirement.HALAL_SHORT_NAME, true);
    //         }
    //     } catch (SQLException e) {
    //         System.out.println("Could not set guest diet!\n" + e.toString());
    //         return;
    //     }
    // }
}
