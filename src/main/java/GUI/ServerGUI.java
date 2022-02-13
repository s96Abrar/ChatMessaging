package GUI;

import server.ServerThread;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ServerGUI extends JFrame {
    private final JTextArea msgRec;
    private final JButton btnStartServer;
    private final JButton btnStopServer;
    private ServerThread serverThread;

    public ServerGUI() {
        super("Server GUI");
        setBounds(0, 0, 407, 495);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(null);

        msgRec = new JTextArea(100, 50);
        msgRec.setEditable(false);
        msgRec.setBackground(Color.WHITE);
        msgRec.setForeground(Color.BLACK);
        msgRec.setText("");

        msgRec.setWrapStyleWord(true);
        msgRec.setLineWrap(true);

        JScrollPane pane2 = new JScrollPane(msgRec);
        pane2.setBounds(0, 0, 400, 390);
        pane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(pane2);

        btnStartServer = new JButton("Start server");
        btnStartServer.setBounds(10, 400, 180, 40);
        btnStartServer.setEnabled(true);
        add(btnStartServer);

        btnStopServer = new JButton("Stop server");
        btnStopServer.setBounds(210, 400, 180, 40);
        btnStopServer.setEnabled(false);
        add(btnStopServer);

        btnStartServer.addActionListener(actionEvent -> {
            int port = -1;
            try {
                port = ServerThread.getPortNumberFromUser(this);
            } catch (NumberFormatException e) {
                ServerThread.showWarningMessage(this, "INVALID PORT " + port + "!!!");
                port = ServerThread.getPortNumberFromUser(this);
            }

            serverThread = new ServerThread(port, this);
            serverThread.start();

            try {
                Thread.sleep(700);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (serverThread.isServerRunning()) {
                btnStartServer.setEnabled(false);
                btnStopServer.setEnabled(true);
            }
        });

        btnStopServer.addActionListener(actionEvent -> {
            serverThread.stopServer();

            if (!serverThread.isServerRunning()) {
                btnStartServer.setEnabled(true);
                btnStopServer.setEnabled(false);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (serverThread != null && serverThread.isServerRunning()) {
                    serverThread.stopServer();
                }
                super.windowClosing(e);
            }
        });

        setVisible(true);
    }

    /* Update cursor position */
    private void cursorUpdate() {
        DefaultCaret caret = (DefaultCaret) msgRec.getCaret();
        caret.setDot(msgRec.getDocument().getLength());
    }

    public synchronized void showMessage(String message) {
        msgRec.append("\n" + message);
        cursorUpdate();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("INVALID THEME CONFIG. " + e.getMessage());
            }
            new ServerGUI();
        });
    }
}