package chat;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient extends JFrame {
    private String username;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private DefaultListModel<String> userListModel = new DefaultListModel<>();
    private JList<String> userList = new JList<>(userListModel);

    private JPanel chatPanel = new JPanel();
    private JScrollPane chatScroll;
    private JTextField inputField = new JTextField();

    private String currentRoom = null;
    private String currentTargetUser = null;
    private Map<String, List<String>> roomMessages = new ConcurrentHashMap<>();

    public ChatClient(String username) {
        this.username = username;
        setTitle("Chat - " + username);
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Panel trái: danh sách user
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(180, 0));
        leftPanel.add(new JLabel("Online Users"), BorderLayout.NORTH);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // Panel phải: khung chat + input
        JPanel rightPanel = new JPanel(new BorderLayout());

        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        rightPanel.add(chatScroll, BorderLayout.CENTER);

        // Ô nhập tin nhắn
        inputField.setPreferredSize(new Dimension(0, 40));
        rightPanel.add(inputField, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);

        // Sự kiện chọn user
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null && !selectedUser.equals(currentTargetUser)) {
                    currentTargetUser = selectedUser;
                    out.println("CREATE_OR_GET_ROOM:" + username + "," + selectedUser);
                }
            }
        });

        // Gửi tin nhắn khi nhấn Enter
        inputField.addActionListener(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty() && currentRoom != null) {
                out.println("SEND:" + currentRoom + ":" + text);
                inputField.setText("");
                // KHÔNG append ở đây
            }
        });


        // Kết nối server
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", ChatServer.PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            out.println(username); // gửi username

            // Thread đọc dữ liệu từ server
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleServerMessage(line);
                    }
                } catch (IOException ignored) {}
            }).start();

            out.println("GET_USERS");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được server!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void handleServerMessage(String line) {
        if (line.startsWith("USER_LIST:")) {
            SwingUtilities.invokeLater(() -> {
                userListModel.clear();
                String[] users = line.substring(10).split(",");
                for (String u : users) {
                    if (!u.equals(username) && !u.isBlank()) userListModel.addElement(u);
                }
            });
        } else if (line.startsWith("ROOM_ID:")) {
            currentRoom = line.substring(8);
            roomMessages.putIfAbsent(currentRoom, new ArrayList<>());
            chatPanel.removeAll();
            chatPanel.revalidate();
            chatPanel.repaint();
            out.println("GET_MESSAGES:" + currentRoom);
        } else if (line.startsWith("MSG:")) {
            String[] parts = line.split(":", 5);
            if (parts.length == 5) {
                String roomId = parts[1];
                String sender = parts[2];
                String msg = parts[3];

                roomMessages.computeIfAbsent(roomId, k -> new ArrayList<>()).add(sender + ": " + msg);

                if (roomId.equals(currentRoom)) {
                    SwingUtilities.invokeLater(() -> addMessage(sender, msg, sender.equals(username)));
                }
            }
        } else if (line.equals("END_MESSAGES")) {
            List<String> msgs = roomMessages.getOrDefault(currentRoom, Collections.emptyList());
            SwingUtilities.invokeLater(() -> {
                chatPanel.removeAll();
                for (String m : msgs) {
                    String[] p = m.split(":", 2);
                    if (p.length == 2) addMessage(p[0], p[1].trim(), p[0].equals(username));
                }
                chatPanel.revalidate();
                chatPanel.repaint();
            });
        }
    }

    // Thêm bong bóng tin nhắn
    private void addMessage(String sender, String text, boolean isMine) {
        JPanel bubble = new JPanel(new BorderLayout());
        bubble.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel msgLabel = new JLabel("<html><p style='width:200px'>" + text + "</p></html>");
        msgLabel.setOpaque(true);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        if (isMine) {
            msgLabel.setBackground(new Color(0, 153, 255));
            msgLabel.setForeground(Color.white);
            bubble.add(msgLabel, BorderLayout.EAST);
        } else {
            msgLabel.setBackground(new Color(220, 220, 220));
            bubble.add(msgLabel, BorderLayout.WEST);
        }

        chatPanel.add(bubble);
        chatPanel.revalidate();
        chatPanel.repaint();

        // Tự cuộn xuống cuối
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    public static void main(String[] args) {
        String user = JOptionPane.showInputDialog("Nhập username:");
        if (user != null && !user.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> new ChatClient(user.trim()).setVisible(true));
        }
    }
}
