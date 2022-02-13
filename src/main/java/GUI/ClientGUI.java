package GUI;

import client.ClientThread;
import common.ChatMessage;
import common.ChatMessageType;
import common.ClientUser;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ClientGUI extends JFrame {

    private final DefaultListModel<ClientUser> userListModel;
    private final Map<ClientUser, ReceiverClient> receiverClientMap;
    private final JList<ClientUser> currentUserList;
    private final ClientThread clientThread;
    private final ClientUser allClient;

    public ClientGUI() {
        super("Client GUI");

        String username = checkUsername();

        userListModel = new DefaultListModel<ClientUser>();
        receiverClientMap = new HashMap<ClientUser, ReceiverClient>();

        clientThread = new ClientThread(5000, "localhost", new ClientUser(username, -1), this);
        if (!clientThread.start()) {
            JOptionPane.showMessageDialog(this, "Can not start client thread.", "Warning", JOptionPane.WARNING_MESSAGE);
            System.exit(1);
        }
        clientThread.sendMessage(new ChatMessage(ChatMessageType.WhoIsIn, "", clientThread.getClientUser()));

        setBounds(0, 0, 557, 495);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(null);

        currentUserList = new JList<ClientUser>(userListModel);
        currentUserList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        currentUserList.getSelectionModel().addListSelectionListener(listSelectionEvent -> {
            ListSelectionModel lsm = (ListSelectionModel) listSelectionEvent.getSource();
            if (!listSelectionEvent.getValueIsAdjusting()) {
                int firstIndex = listSelectionEvent.getFirstIndex();
                int lastIndex = listSelectionEvent.getLastIndex();

                if (firstIndex == lastIndex) {
                    receiverClientMap.get(
                            currentUserList.getModel().getElementAt(lastIndex)
                    ).setVisible(true);
                    return;
                }

                if (lsm.isSelectedIndex(firstIndex)) {
                    int temp = firstIndex;
                    firstIndex = lastIndex;
                    lastIndex = temp;
                }

                ReceiverClient c1 = receiverClientMap.get(currentUserList.getModel().getElementAt(firstIndex));
                c1.setVisible(false);

                if (lastIndex < currentUserList.getModel().getSize()) {
                    ReceiverClient c2 = receiverClientMap.get(currentUserList.getModel().getElementAt(lastIndex));
                    c2.setVisible(true);
                }
            }
        });

        JScrollPane pane3 = new JScrollPane(currentUserList);
        pane3.setBounds(5, 5, 150, 490);
        pane3.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(pane3);

        JMenu userMenu = new JMenu(clientThread.getClientUser().getUsername());

        JMenuBar topMenuBar = new JMenuBar();
        topMenuBar.add(userMenu);
        JMenuItem menuItemLogOut = new JMenuItem("Log Out");
        userMenu.add(menuItemLogOut);
        menuItemLogOut.addActionListener(actionEvent -> {
            clientThread.sendMessage(new ChatMessage(ChatMessageType.LogOut, "", clientThread.getClientUser()));
            clientThread.logOut();
            System.exit(0);
        });

        JMenu helpMenu = new JMenu("Help");
        topMenuBar.add(helpMenu);
        JMenuItem menuItemShortcutKeys = new JMenuItem("Shortcut Keys");
        helpMenu.add(menuItemShortcutKeys);
        menuItemShortcutKeys.addActionListener(actionEvent -> JOptionPane.showMessageDialog(this,
                "(shift+Enter) for new line while writing message (ctrl+x) for quit"));

        setJMenuBar(topMenuBar);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                clientThread.sendMessage(new ChatMessage(ChatMessageType.LogOut, "", clientThread.getClientUser()));
                clientThread.logOut();
                super.windowClosing(e);
            }
        });
        setVisible(true);

        allClient = new ClientUser("All", 0);
        addUserClient(allClient, true);
        currentUserList.setSelectedIndex(0);
    }

    public void updateUserList(Map<ClientUser, String> users) {
        ArrayList<ClientUser> newUsers = new ArrayList<ClientUser>();
        for (ClientUser user : users.keySet()) {
            if (!receiverClientMap.containsKey(user)) {
                newUsers.add(user);
            }
        }

        ArrayList<ClientUser> removeUsers = new ArrayList<ClientUser>();
        for (ClientUser user : receiverClientMap.keySet()) {
            if (!users.containsKey(user) && !user.equals(allClient)) {
                removeUsers.add(user);
            }
        }

        for (ClientUser user : removeUsers) {
            if (!user.equals(clientThread.getClientUser())) {
                removeUserClient(user);
            }
        }

        for (ClientUser user : newUsers) {
            if (!user.equals(clientThread.getClientUser())) {
                addUserClient(user, false);
            }
        }
    }

    public void updateMessage(ChatMessage chatMessage) {
        ClientUser senderUser = chatMessage.getSenderUser();
        String username = senderUser.getUsername();

        if (chatMessage.getChatMessageType() == ChatMessageType.PrivateMessage) {
            ClientUser receiverUser = chatMessage.getSenderUser();
            if (senderUser.equals(clientThread.getClientUser())) {
                receiverUser = chatMessage.getReceiverUser();
                username = "Me";
            }
            ReceiverClient temp = receiverClientMap.get(receiverUser);
            temp.appendConversation(username + ": " + chatMessage.getMessage());
        } else {
            if (senderUser.equals(clientThread.getClientUser())) {
                username = "Me";
            }
            receiverClientMap.get(allClient).appendConversation(username + ": " + chatMessage.getMessage());
        }
    }

    public void addUserClient(ClientUser clientUser, boolean sendToAll) {
        ReceiverClient client = new ReceiverClient(clientUser, sendToAll);
        client.setBounds(157, 5, 400, 490);
        client.setVisible(false);

        if (clientUser.getUsername().equals("All")) {
            userListModel.add(0, clientUser);
        } else {
            userListModel.addElement(clientUser);
        }
        receiverClientMap.put(clientUser, client);
        add(client);
    }

    public void removeUserClient(ClientUser user) {
        ReceiverClient client = receiverClientMap.get(user);
        remove(client);
        revalidate();
        repaint();

        userListModel.removeElement(user);
    }

    private String checkUsername() {
        String result = (String) JOptionPane.showInputDialog(
                this, "Enter username", "Input Dialog",
                JOptionPane.PLAIN_MESSAGE, null, null, "Anonymous");
        if (result == null || result.isEmpty() || result.equalsIgnoreCase("All")) {
            JOptionPane.showMessageDialog(this, "Invalid username provided", "Warning", JOptionPane.WARNING_MESSAGE);
            System.exit(1);
        }

        return result;
    }

    class ReceiverClient extends JPanel implements KeyListener {
        private final JTextArea msgRec;
        private final JTextArea msgSend;
        private final ClientUser receiverUser;
        private final boolean sendToAll;

        public ReceiverClient(ClientUser receiverUser, boolean sendToAll) {
            this.receiverUser = receiverUser;
            this.sendToAll = sendToAll;

            setBounds(0, 0, 400, 495);
            setResizable(false);
            setLayout(null);

            msgRec = new JTextArea(100, 50);
            msgRec.setEditable(false);
            msgRec.setBackground(Color.GRAY);
            msgRec.setForeground(Color.WHITE);
            msgRec.setText("");
            msgRec.setWrapStyleWord(true);
            msgRec.setLineWrap(true);

            JScrollPane pane2 = new JScrollPane(msgRec);
            pane2.setBounds(0, 0, 400, 200);
            pane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            add(pane2);

            msgSend = new JTextArea(100, 50);
            msgSend.setBackground(Color.WHITE);
            msgSend.setForeground(Color.BLACK);
            msgSend.setLineWrap(true);
            msgSend.setWrapStyleWord(true);
            msgSend.addKeyListener(this);

            JScrollPane pane1 = new JScrollPane(msgSend);
            pane1.setBounds(0, 200, 400, 200);
            pane1.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            add(pane1);

            JButton send = new JButton("Send to " + receiverUser);
            send.setBounds(0, 400, 400, 40);
            add(send);

            send.addActionListener(actionEvent -> sendMessageToClient());
        }

        private void sendMessageToClient() {
            if (!msgSend.getText().trim().isEmpty()) {
                if (sendToAll) {
                    clientThread.sendMessage(new ChatMessage(ChatMessageType.GeneralMessage, msgSend.getText(), clientThread.getClientUser()));
                } else {
                    ChatMessage chatMessage = new ChatMessage(ChatMessageType.PrivateMessage, msgSend.getText(), clientThread.getClientUser());
                    chatMessage.setReceiverUser(receiverUser);
                    chatMessage.setSendToAll(false);
                    clientThread.sendMessage(chatMessage);
                }
            }
            msgSend.setText("");
        }

        private void cursorUpdate() {
            // Update cursor position
            DefaultCaret caret = (DefaultCaret) msgRec.getCaret();
            caret.setDot(msgRec.getDocument().getLength());

            DefaultCaret caret2 = (DefaultCaret) msgSend.getCaret();
            caret2.setDot(msgSend.getDocument().getLength());
        }

        @Override
        public void keyTyped(KeyEvent keyEvent) {
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if ((e.getKeyCode() == KeyEvent.VK_ENTER) && e.isShiftDown()) {
                msgSend.append("\n");
            } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                sendMessageToClient();
                e.consume();
            } else if ((e.getKeyCode() == KeyEvent.VK_X) && e.isControlDown()) {
                clientThread.sendMessage(new ChatMessage(ChatMessageType.LogOut, "", clientThread.getClientUser()));
                clientThread.logOut();
                System.exit(0);
            }
        }

        public void appendConversation(String message) {
            msgRec.append(message + "\n");
            cursorUpdate();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("INVALID THEME CONFIG. " + e.getMessage());
            }
            new ClientGUI();
        });
    }
}
