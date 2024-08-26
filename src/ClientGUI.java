import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientGUI extends JFrame {
    private JTextField nameField, keyField, messageField;
    private JPanel messagePanel;
    private JButton sendButton, connectButton;
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;
    private JPanel topPanel;
    public ClientGUI() {
        setTitle("Client");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        topPanel = new JPanel(new GridLayout(3, 2));
        topPanel.add(new JLabel("Name: "));
        nameField = new JTextField();
        topPanel.add(nameField);
        topPanel.add(new JLabel("Connection Key: "));
        keyField = new JTextField();
        topPanel.add(keyField);
        connectButton = new JButton("Connect");
        topPanel.add(connectButton);

        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane messageScroll = new JScrollPane(messagePanel);

        messageField = new JTextField();
        sendButton = new JButton("Send");

        connectButton.addActionListener(e -> startClient());
        sendButton.addActionListener(e -> sendMessage());

        messageField.addActionListener(e -> sendMessage());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(messageScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    private void startClient() {
        try {
            String name = nameField.getText().trim();
            String key = keyField.getText().trim();
            String serverAddress = "localhost"; // Replace with the actual server IP address

            socket = new Socket(serverAddress, 7777);
            appendMessage("Connecting to server...", false);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(key); 
            String response = in.readLine();
            if ("Connected".equals(response)) {
                appendMessage("Connected to server", false);
                out.println(name); // Send name after successful connection

                remove(topPanel);
                revalidate();
                repaint();

                startReading();
            } else {
                appendMessage(response, false);
                closeResources();
            }
        } catch (Exception e) {
            e.printStackTrace();
            appendMessage("Failed to connect. Check connection key.", false);
        }
    }
    private void startReading() {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    appendMessage(msg, false);
                    if (msg.equalsIgnoreCase("bye")) {
                        appendMessage("Server terminated the connection.", false);
                        break;
                    }
                }
                closeResources();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage() {
        String msg = messageField.getText();
        if (msg != null && !msg.trim().isEmpty()) {
            out.println(msg);
            appendMessage("Me: " + msg, true);
            messageField.setText("");
            if (msg.equalsIgnoreCase("bye")) {
                closeResources();
            }
        }
    }

    private void appendMessage(String msg, boolean isSent) {
        JPanel panel = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT));
        JLabel label = new JLabel(msg);
        label.setOpaque(true);
        label.setBackground(isSent ? Color.GREEN : Color.CYAN);
        label.setForeground(Color.BLACK);
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(label);
        messagePanel.add(panel);
        messagePanel.revalidate();
        messagePanel.repaint();
    }

    private void closeResources() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            appendMessage("Resources closed.", false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        new ClientGUI();
    }
}
