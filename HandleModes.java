public enum HandleModes {
    CHAT_SELECT,
    CHALLENGE_RESPONSE,
    CHAT,
    CHAT_CREATION;

    public static final String CHAT_SELECT_SIGNAL = "SWITCH_TO_CHAT_SELECT";
    public static final String CHALLENGE_RESPONSE_SIGNAL = "SWITCH_TO_CHALLENGE_RESPONSE";
    public static final String CHAT_SIGNAL = "SWITCH_TO_CHAT";
    public static final String CHAT_CREATION_SIGNAL = "SWITCH_TO_CHAT_CREATION";
    public static final String CHAT_CREATION_COMMAND = "/create";
    public static final String EXIT_COMMAND = "/exit";
}
