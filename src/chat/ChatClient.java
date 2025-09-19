package chat;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private JFrame frame;
    private JTextArea chatArea;
    private JTextField messageField;
    private DefaultListModel<String> userListModel;
    private JList<String> userList;

    private String username;
    private String currentRecipient = "";

    public ChatClient(String username) {
        this.username = username;
        createUI();
    }

    private void createUI() {
        frame = new JFrame("Messenger - " + username);
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Panel trái: danh sách user
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.addListSelectionListener(e -> {
            currentRecipient = userList.getSelectedValue();
            chatArea.append("Đang chat với: " + currentRecipient + "\n");
        });

        // Khu chat chính
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScroll = new JScrollPane(chatArea);

        // Ô nhập tin nhắn
        messageField = new JTextField();
        messageField.addActionListener(e -> sendMessage());

        frame.add(new JScrollPane(userList), BorderLayout.WEST);
        frame.add(chatScroll, BorderLayout.CENTER);
        frame.add(messageField, BorderLayout.SOUTH);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty() && !currentRecipient.isEmpty()) {
            out.println("CHAT|" + username + "|" + currentRecipient + "|" + message);
            messageField.setText("");
        }
    }

    public void start() throws IOException {
        socket = new Socket(SERVER_IP, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Gửi username
        out.println(username);

        // Thread nhận tin nhắn
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    processMessage(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        frame.setVisible(true);
    }

    private void processMessage(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 4) return;
        String type = parts[0];
        String sender = parts[1];
        String recipient = parts[2];
        String content = parts[3];

        switch (type) {
            case "CHAT":
                chatArea.append(sender + ": " + content + "\n");
                // gửi seen
                out.println("SEEN|" + username + "|" + sender + "|Đã xem");
                break;
            case "SEEN":
                chatArea.append("(Tin nhắn đã xem bởi " + sender + ")\n");
                break;
            case "USER_LIST":
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    for (String user : content.split(";")) {
                        if (!user.isEmpty() && !user.equals(username)) {
                            userListModel.addElement(user);
                        }
                    }
                });
                break;
            case "STATUS":
                // Cập nhật trạng thái user trong danh sách
                // Có thể tô màu Online/Offline nếu muốn
                break;
        }
    }

    public static void main(String[] args) throws IOException {
        String username = JOptionPane.showInputDialog("Nhập tên của bạn:");
        new ChatClient(username).start();
    }
}
