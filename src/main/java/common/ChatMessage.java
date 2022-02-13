package common;

import java.io.Serializable;

/**
 * Types of message that can be transferred between client and server.
 */
public class ChatMessage implements Serializable {
    private final ChatMessageType chatMessageType;
    private final Object message;
    private boolean sendToAll;
    private final ClientUser senderUser;
    private ClientUser receiverUser; /* Used only for the private */


    public ChatMessage(ChatMessageType chatMessageType, Object message, ClientUser senderUser) {
        this.chatMessageType = chatMessageType;
        this.message = message;
        this.senderUser = senderUser;
        this.sendToAll = true;
        this.receiverUser = null;
    }

    public ChatMessageType getChatMessageType() {
        return chatMessageType;
    }

    public Object getMessage() {
        return message;
    }

    public boolean isSendToAll() {
        return sendToAll;
    }

    public void setSendToAll(boolean sendToAll) {
        this.sendToAll = sendToAll;
    }

    public ClientUser getReceiverUser() {
        return receiverUser;
    }

    public void setReceiverUser(ClientUser receiverUser) {
        this.receiverUser = receiverUser;
    }

    public ClientUser getSenderUser() {
        return senderUser;
    }
}
