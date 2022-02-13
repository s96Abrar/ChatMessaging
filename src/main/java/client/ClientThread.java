package client;

import GUI.ClientGUI;
import common.ChatMessage;
import common.ChatMessageType;
import common.ClientUser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;

public class ClientThread {

    private final SimpleDateFormat dateTimeFormat;

    private Socket clientSocket;
    private ObjectInputStream socketInputStream; /* Read from socket */
    private ObjectOutputStream socketOutputStream; /* Write to socket */

    private final int port;
    private final String serverAddress;
    private final ClientUser clientUser;
//    private int clientID;
    private final ClientGUI clientGUI;

    public ClientThread(int port, String serverAddress, ClientUser clientUser, ClientGUI clientGUI) {
        this.port = port;
        this.serverAddress = serverAddress;
        this.clientUser = clientUser;
        this.clientGUI = clientGUI;
//        this.clientID = 0;
        this.dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public int getPort() {
        return port;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public ClientUser getClientUser() {
        return clientUser;
    }

    public boolean start() {
        try {
            clientSocket = new Socket(serverAddress, port);
        } catch (IOException e) {
            log("Error: Connecting to server. " + e.getMessage());
        }

        if (clientSocket == null) {
            log("Socket not created.");
            return false;
        }

        log("Connection established " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

        try {
            socketInputStream = new ObjectInputStream(clientSocket.getInputStream());
            socketOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        } catch (IOException e) {
            log("Exception: Creating input output stream. " + e.getMessage());
            return false;
        }

        (new ListenFromServer()).start();

        try {
            socketOutputStream.writeObject(clientUser);
            socketOutputStream.writeObject(new ChatMessage(ChatMessageType.NewUser, clientUser, clientUser));
        } catch (IOException e) {
            log("Exception: Log in " + clientUser + ". " + e.getMessage());
            disconnect();
            return false;
        }

        return true;
    }

    public void logOut() {
        disconnect();
    }

    private void disconnect() {
        try {
            if (socketInputStream != null) socketInputStream.close();
            if (socketOutputStream != null) socketOutputStream.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            log("Exception: " + e.getMessage());
        }
    }

    private void log(String message) {
        System.out.println(dateTimeFormat.format(new Date()) + ": " + message);
    }

    public void sendMessage(ChatMessage chatMessage) {
        try {
            socketOutputStream.writeObject(chatMessage);
        } catch (IOException e) {
            log("Exception: " + e.getMessage());
        }
    }

    public class ListenFromServer extends Thread {
        public void run() {
            while (true) {
                try {
                    ChatMessage chatMessage = (ChatMessage) socketInputStream.readObject();

                    /* Choose chat message type */
                    if (chatMessage.getChatMessageType() == ChatMessageType.WhoIsIn) {
                        Object object = chatMessage.getMessage();
                        if (object instanceof Map<?, ?>) {
                            Map<ClientUser, String> users = (Map<ClientUser, String>) object;

                            if (clientGUI != null) {
                                clientGUI.updateUserList(users);
                            } else {
                                System.out.println("List of users: ");
                                for (ClientUser user : users.keySet()) {
                                    System.out.println("\t" + user.getUsername() + " joined at " + users.get(user));
                                }
                                System.out.print("> ");
                            }
                        }
                        continue;
                    } else if (chatMessage.getChatMessageType() == ChatMessageType.ClientNotify) {
                        String message = (String) chatMessage.getMessage();
                        System.out.println(">>** " + message + " **<<");
//                        System.out.print("> ");
                        continue;
                    } else if (chatMessage.getChatMessageType() == ChatMessageType.NewUser || chatMessage.getChatMessageType() == ChatMessageType.LogOut) {
                        Object object = chatMessage.getMessage();
                        if (object instanceof Map<?, ?>) {
                            Map<ClientUser, String> users = (Map<ClientUser, String>) object;

                            if (chatMessage.getChatMessageType() == ChatMessageType.NewUser) {
                                for (ClientUser user : users.keySet()) {
                                    if (clientUser.getClientID() == -1 && user.getUsername().equalsIgnoreCase(clientUser.getUsername())) {
                                        clientUser.setClientID(user.getClientID());
                                        break;
                                    }
                                }
                            }

                            if (clientGUI != null) {
                                clientGUI.updateUserList(users);
                            }
                        }
                        continue;
                    }

                    if (clientGUI != null) {
                        clientGUI.updateMessage(chatMessage);
                    } else {
                        System.out.println((String) chatMessage.getMessage());
                        System.out.print("> ");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    log("Server closed the connection. " + e.getMessage());
                    disconnect();
                    System.exit(0);
                    break;
                }
            }
        }
    }

    public static void main(String[] args) {
        // default values if not entered
        int portNumber = 1500;
        String serverAddress = "localhost";
        String userName = "Anonymous";
        Scanner scan = new Scanner(System.in);

        System.out.println("Enter the username: ");
        userName = scan.nextLine();

        // different case according to the length of the arguments.
        switch (args.length) {
            case 3:
                // for > javac Client username portNumber serverAddr
                serverAddress = args[2];
            case 2:
                // for > javac Client username portNumber
                try {
                    portNumber = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
            case 1:
                // for > javac Client username
                userName = args[0];
            case 0:
                // for > java Client
                break;
            // if number of arguments are invalid
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                return;
        }
        // create the Client object
        ClientThread client = new ClientThread(portNumber, serverAddress, new ClientUser(userName, 0), null);
        // try to connect to the server and return if not connected
        if (!client.start())
            return;

        System.out.println("\nHello.! Welcome to the chatroom.");
        System.out.println("Instructions:");
        System.out.println("1. Simply type the message to send broadcast to all active clients");
        System.out.println("2. Type '@username<space>yourmessage' without quotes to send message to desired client");
        System.out.println("3. Type 'WHOISIN' without quotes to see list of active clients");
        System.out.println("4. Type 'LOGOUT' without quotes to logoff from server");

        // infinite loop to get the input from the user
//        while(true) {
//            System.out.print("> ");
//            // read message from user
//            String msg = scan.nextLine();
//            // logout if message is LOGOUT
//            if(msg.equalsIgnoreCase("LOGOUT")) {
//                client.sendMessage(new ChatMessage(ChatMessageType.LogOut, ""));
//                break;
//            }
//            // message to check who are present in chatroom
//            else if(msg.equalsIgnoreCase("WHOISIN")) {
//                client.sendMessage(new ChatMessage(ChatMessageType.WhoIsIn, ""));
//            }
//            // regular text message
//            else {
//                String[] list = msg.split(" ");
//                if (list[0].charAt(0) == '@') {
//                    ChatMessage chatMessage = new ChatMessage(ChatMessageType.PrivateMessage, list[1]);
//                    chatMessage.setSendToAll(false);
//                    chatMessage.setReceiverUsername(list[0].substring(1));
//                    client.sendMessage(chatMessage);
//                } else {
//                    client.sendMessage(new ChatMessage(ChatMessageType.GeneralMessage, msg));
//                }
//            }
//        }
        // close resource
        scan.close();
        // client completed its job. disconnect client.
        client.disconnect();
    }
}
