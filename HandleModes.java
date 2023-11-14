public enum HandleModes {
    CHAT_SELECT,
    CHALLENGE_RESPONSE,
    CHAT,
    CHAT_CREATION;

    /**
     * Checks whether a string is contained in the enum.
     * Java: Check if enum contains a given string? - Richard H - https://stackoverflow.com/questions/4936819/java-check-if-enum-contains-a-given-string - Accessed 13.11.2023
     * @param test The string to test against the enum.
     * @return Whether the enum contains the given string.
     */
    public static boolean contains(String test) {
        return stringToHandleMode(test) != null;
    }

    /**
     * Checks whether a string is contained in the enum, returning the enum value if so.
     * @param test The string to test against the enum.
     * @return The matching enum value.
     */
    public static HandleModes stringToHandleMode(String test) {
        for (HandleModes handleMode : HandleModes.values()) {
            if (handleMode.name().equals(test)) {
                return handleMode;
            }
        }
        return null;
    }

    public static final String CHAT_CREATION_COMMAND = "/create";
    public static final String EXIT_COMMAND = "/exit";
    public static final String PREVIOUS_COMMAND = "/previous";
}
