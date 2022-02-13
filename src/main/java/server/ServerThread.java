package server;

import GUI.ServerGUI;
import common.ChatMessage;
import common.ChatMessageType;
import common.ClientUser;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

public class ServerThread extends Thread {

//    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat dateTimeFormat;

    private final List<ClientConnectionThread> clientList;

    private final int port;
    private boolean keepRunning;
    private ServerSocket serverSocket;
    private final ServerGUI serverGUI;

    public ServerThread(int port, ServerGUI serverGUI) {
        this.port = port;
        this.serverGUI = serverGUI;
        this.clientList = new ArrayList<ClientConnectionThread>();
//        this.timeFormat = new SimpleDateFormat("HH:mm:ss");
        this.dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public void run() {
        keepRunning = true;

        ClientConnectionThread clientConnectionThread;

        try {
            serverSocket = new ServerSocket(port);
            log("Server started on port " + port + ".");

            /* Infinite loop to keep receiving clients request. */
            while (!isInterrupted()) {
                log("Waiting for clients...");

                Socket socket = serverSocket.accept();

                /* Close connection if server stopped. */
                if (!keepRunning) break;

                clientConnectionThread = new ClientConnectionThread(socket);
                clientConnectionThread.start();

                clientList.add(clientConnectionThread);

                log(clientConnectionThread.getClientUser().getUsername() + " joined.");
            }

            serverSocket.close();
            for (ClientConnectionThread c : clientList) {
                c.closeConnection();
            }
            keepRunning = false;
            log("Server stopped.");
        } catch (IOException e) {
            showWarningMessage(serverGUI, "Invalid port '" + port + "'. Can not start the server.\n" + e.getMessage());
        }
    }

    public boolean isServerRunning() {
        return keepRunning && serverSocket != null && !serverSocket.isClosed() && isAlive();
    }

    public void stopServer() {
        keepRunning = false;
        interrupt();
        try {
            Socket tempSocket = new Socket(serverSocket.getInetAddress(), port);
            tempSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private synchronized boolean broadcast(ChatMessageType messageType, Object message, ClientUser sender, ClientUser receiver) {
        boolean messageSent = false;
        if (messageType == ChatMessageType.PrivateMessage) {
            if (receiver == null) {
                log("Error: Can not send private message to empty username.");
                return false;
            }

            for (int i = clientList.size() - 1; i >= 0; i--) {
                ClientConnectionThread c = clientList.get(i);
                if (c.getClientUser().equals(receiver) ||
                        c.getClientUser().equals(sender)) {
                    ChatMessage chatMessage = new ChatMessage(messageType, message, sender);
                    chatMessage.setSendToAll(false);
                    chatMessage.setReceiverUser(receiver);
                    if (!c.writeMessage(chatMessage)) {
                        clientList.remove(i);
                        log(c.getClientUser().getUsername() + " is disconnected from the server.");
                    } else {
                        messageSent = true;
                    }
                }
            }
        } else if (messageType == ChatMessageType.ServerNotify) {
            messageSent = true;
            log("Notify: " + sender + " " + message + " " + (receiver != null ? "(" + receiver + ")" : ""));
        } else {
            messageSent = true;
            for (int i = clientList.size() - 1; i >= 0; i--) {
                ClientConnectionThread c = clientList.get(i);

                ChatMessage chatMessage = new ChatMessage(messageType, message, sender);

                if (!c.getClientUser().equals(sender) || messageType != ChatMessageType.LogOut) {
                    if (!c.writeMessage(chatMessage)) {
                        clientList.remove(i);
                        log(c.getClientUser().getUsername() + " is disconnected from the server.");
                    }
                }
            }
        }
        return messageSent;
    }

    private synchronized void removeClient(int clientID) {
        ClientUser disconnectedClient = null;
        int index = 0;
        for (ClientConnectionThread c : clientList) {
            if (c.getClientUser().getClientID() == clientID) {
                disconnectedClient = c.clientUser;
                break;
            }
            index++;
        }

        if (disconnectedClient == null) {
            return;
        }

        clientList.remove(index);

        broadcast(ChatMessageType.ServerNotify, disconnectedClient + " left the chat.", disconnectedClient,null);

        Map<ClientUser, String> connectedUsers = new HashMap<ClientUser, String>();
        for (ClientConnectionThread c : clientList) {
            connectedUsers.put(c.getClientUser(), dateTimeFormat.format(c.connectedTime));
        }
        broadcast(ChatMessageType.LogOut, connectedUsers, disconnectedClient, null);
    }

    private void log(String message) {
        if (serverGUI != null) {
            serverGUI.showMessage(dateTimeFormat.format(new Date()) + ": " + message);
        } else {
            System.out.println(dateTimeFormat.format(new Date()) + ": " + message);
        }
    }

    public static void showWarningMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static int getPortNumberFromUser(Component parent) {
        String result = (String) JOptionPane.showInputDialog(
                null, "Enter port number", "Input Dialog",
                JOptionPane.PLAIN_MESSAGE, null, null, 5000);
        int port = -1;
        try {
            if (result != null) {
                port = Integer.parseInt(result);
            }
        } catch (NumberFormatException ex) {
            showWarningMessage(parent, "Invalid input. Please enter integer value.");
            port = getPortNumberFromUser(parent);
        }

        return port;
    }

    class ClientConnectionThread extends Thread {
        private final Socket clientSocket;
        private ObjectInputStream clientInputStream;
        private ObjectOutputStream clientOutputStream;

        private ClientUser clientUser;
        private final Date connectedTime;

        public ClientConnectionThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.connectedTime = new Date();
            this.clientUser = null;

            try {
                clientOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                clientInputStream = new ObjectInputStream(clientSocket.getInputStream());

                clientUser = (ClientUser) clientInputStream.readObject();
                clientUser.setClientID((new Random()).nextInt(999999));

//                broadcast(ChatMessageType.NewUser, "joined to the chat.", username, null);
            } catch (IOException | ClassNotFoundException e) {
                log("Exception occurred. " + e.getMessage());
            }
        }

        public void run() {
            /* Loop until user select logout. */
            boolean keepGoing = true;
            while (!isInterrupted()) {
                /* Read message object from socket */
                ChatMessage chatMessage;
                try {
                    chatMessage = (ChatMessage) clientInputStream.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    for (int i = clientList.size() - 1; i >= 0; i--) {
                        ClientConnectionThread c = clientList.get(i);
                        if (c.isClientNotAvailable()) {
                            clientList.remove(i);
                        }
                    }
                    log("Exception: Reading from client. " + clientUser.getUsername() + ": " + e.getMessage());
                    break;
                }

//                String message = (String) chatMessage.getMessage();

                switch (chatMessage.getChatMessageType()) {
                    case WhoIsIn:
                    case LogOut:
                    case NewUser:
                        Map<ClientUser, String> connectedUsers = new HashMap<ClientUser, String>();
                        for (ClientConnectionThread c : clientList) {
                            connectedUsers.put(c.clientUser, dateTimeFormat.format(c.connectedTime));
                        }

                        if (chatMessage.getChatMessageType() == ChatMessageType.WhoIsIn) {
                            ChatMessage whoIsInChatMessage = new ChatMessage(
                                    ChatMessageType.WhoIsIn, connectedUsers, clientUser);
                            whoIsInChatMessage.setSendToAll(false);
                            whoIsInChatMessage.setReceiverUser(clientUser);
                            writeMessage(whoIsInChatMessage);
                        } else {
                            broadcast(chatMessage.getChatMessageType(), connectedUsers, clientUser, null);
                        }

                        if (chatMessage.getChatMessageType() == ChatMessageType.LogOut) {
                            keepGoing = false;
                        }

                        break;
                    case GeneralMessage:
                    case PrivateMessage:
                    case ServerNotify:
                    case ClientNotify:
                        if (!broadcast(chatMessage.getChatMessageType(),
                                chatMessage.getMessage(), clientUser,
                                (!chatMessage.isSendToAll() ? chatMessage.getReceiverUser() : null))) {
                            if (!writeMessage(new ChatMessage(ChatMessageType.ClientNotify, "No such user exists. ", clientUser))) {
                                log("Client is not connected to write message.");
                            }
                        }
                        break;
                }

                if (!keepGoing) {
                    break;
                }
            }

            removeClient(clientUser.getClientID());
            closeConnection();
        }

        public void closeConnection() {
            try {
                if (clientOutputStream != null) clientOutputStream.close();
                if (clientInputStream != null) clientInputStream.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                log("Exception: While closing the client thread. " + e.getMessage());
            }
        }

        public boolean isClientNotAvailable() {
            return clientSocket == null || !clientSocket.isConnected();
        }

        private boolean writeMessage(ChatMessage chatMessage) {
            /* Check client connection */
            if (isClientNotAvailable()) {
                closeConnection();
                return false;
            }

            try {
                /* Send message to client */
                clientOutputStream.writeObject(chatMessage);
            } catch (IOException e) {
                log("Error sending message to " + (chatMessage.isSendToAll() ? "All" : chatMessage.getReceiverUser()) + ". " + e.getMessage());
                e.printStackTrace();
            }

            return true;
        }

        public ClientUser getClientUser() {
            return clientUser;
        }
    }

    public static void main(String[] args) {
        int port;
        if (args.length != 1) {
            port = getPortNumberFromUser(null);
        } else {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ex) {
                port = getPortNumberFromUser(null);
            }
        }

        ServerThread serverThread = new ServerThread(port, null);
        serverThread.start();
    }
}
