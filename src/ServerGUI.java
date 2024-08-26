import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerGUI extends JFrame {
    private JPanel messagePanel;
    private JTextField messageField;
    private JButton sendButton;
    private JLabel serverInfoLabel;
    private JTextField keyField;
    private List<ClientHandler> clients;
    private ServerSocket serverSocket;

    public ServerGUI() {
        setTitle("Server");
        setSize(400, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        serverInfoLabel = new JLabel("Server IP: ");
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane messageScroll = new JScrollPane(messagePanel);

        messageField = new JTextField();
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> broadcastMessage("Server", messageField.getText()));

        messageField.addActionListener(e -> broadcastMessage("Server", messageField.getText()));

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        JPanel topPanel = new JPanel(new BorderLayout());
        keyField = new JTextField("Enter Connection Key");
        topPanel.add(keyField, BorderLayout.CENTER);
        topPanel.add(serverInfoLabel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(messageScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        clients = new ArrayList<>();

        setVisible(true);
        startServer();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(7777);
            InetAddress inetAddress = InetAddress.getLocalHost();
            serverInfoLabel.setText("Server IP: " + inetAddress.getHostAddress() + " Port: " + serverSocket.getLocalPort());

            new Thread(() -> {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        ClientHandler clientHandler = new ClientHandler(socket);
                        clients.add(clientHandler);
                        clientHandler.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String name, String msg) {
        if (msg != null && !msg.trim().isEmpty()) {
            appendMessage(name + ": " + msg, true);
            for (ClientHandler client : clients) {
                client.sendMessage(name + ": " + msg);
            }
            messageField.setText("");
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

    private class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String clientName;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String connectionKey = in.readLine();
                String serverKey = keyField.getText().trim();
                if (connectionKey.equals(serverKey) && !serverKey.isEmpty()) {
                    out.println("Connected");
                    clientName = in.readLine();
                    if (clientName != null && !clientName.trim().isEmpty()) {
                        broadcastMessage("Server", clientName + " has joined the chat.");
                        String msg;
                        while ((msg = in.readLine()) != null) {
                            broadcastMessage(clientName, msg);
                            if (msg.equalsIgnoreCase("bye")) {
                                break;
                            }
                        }
                    } else {
                        out.println("Invalid name.");
                        closeResources();
                    }
                } else {
                    out.println("Invalid connection key.");
                    closeResources();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                closeResources();
            }
        }

        private void sendMessage(String msg) {
            out.println(msg);
        }

        private void closeResources() {
            try {
                in.close();
                out.close();
                socket.close();
                clients.remove(this);
                broadcastMessage("Server", clientName + " has left the chat.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new ServerGUI();
    }
}
