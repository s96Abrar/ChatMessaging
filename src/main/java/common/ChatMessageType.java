package common;

/**
 * Types of messages that can sent.
 */
public enum ChatMessageType {
    WhoIsIn("Who is in", MessageTypeCode.WHO_IS_IN),
    GeneralMessage("General Message", MessageTypeCode.MESSAGE),
    LogOut("Log Out", MessageTypeCode.LOGOUT),
    PrivateMessage("Private Message", MessageTypeCode.PRIVATE_MESSAGE),
    ServerNotify("Server Notification", MessageTypeCode.SERVER_NOTIFY),
    ClientNotify("Server Notification", MessageTypeCode.CLIENT_NOTIFY),
    NewUser("New user entered the chat", MessageTypeCode.NEW_USER);

    private final String typeString;
    private final int type;
    private ChatMessageType(String typeString, int type) {
        this.typeString = typeString;
        this.type = type;
    }

    public String getTypeString() {
        return typeString;
    }

    public int getType() {
        return type;
    }

    static class MessageTypeCode {
        public static int
                WHO_IS_IN = 0,
                MESSAGE = 1,
                LOGOUT = 2,
                PRIVATE_MESSAGE = 3,
                SERVER_NOTIFY = 4,
                CLIENT_NOTIFY = 5,
                NEW_USER = 6;
    }
}
