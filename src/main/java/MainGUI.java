import GUI.ClientGUI;
import GUI.ServerGUI;

import javax.swing.*;

public class MainGUI extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("INVALID THEME CONFIG. " + e.getMessage());
            }
            new MainGUI();
        });
    }

    private final JButton openServerButton;

    public MainGUI() {
        super("Chat Messaging");
        setBounds(0, 0, 400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(null);
        setVisible(true);

        openServerButton = new JButton("Open the server");
        openServerButton.setBounds(70, 100, 260, 40);
        openServerButton.addActionListener(event -> {
            openServerButton.setEnabled(false);
            new ServerGUI();
        });
        add(openServerButton);

        JButton openClientButton = new JButton("Open message client");
        openClientButton.setBounds(70, 180, 260, 40);
        openClientButton.addActionListener(event -> new ClientGUI());
        add(openClientButton);

        JLabel lblHelp = new JLabel("<html>Open as many client as you want but<br>you can open one server at a time.</html>");
        lblHelp.setBounds(90, 260, 240, 40);
        add(lblHelp);
    }
}
