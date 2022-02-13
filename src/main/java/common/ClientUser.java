package common;

import java.io.Serializable;

public class ClientUser implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private int clientID;

    public ClientUser(String username, int clientID) {
        this.username = username;
        this.clientID = clientID;
    }

    public String getUsername() {
        return username;
    }

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    @Override
    public String toString() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof ClientUser) {
                ClientUser clientUser = (ClientUser) o;
                return (clientUser.username.equalsIgnoreCase(this.username) && clientUser.clientID == this.clientID);
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return clientID + username.hashCode();
    }

}
