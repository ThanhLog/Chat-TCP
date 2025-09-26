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
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // ===== Khung trái: Danh sách user =====
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 0));

        JLabel onlineLabel = new JLabel("Online Users", SwingConstants.CENTER);
        onlineLabel.setFont(new Font("Arial", Font.BOLD, 14));
        onlineLabel.setBorder(new EmptyBorder(10, 0, 10, 0));
        leftPanel.add(onlineLabel, BorderLayout.NORTH);

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        JButton logoutButton = new JButton("Đăng xuất");
        logoutButton.setBackground(Color.RED);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setFont(new Font("Arial", Font.BOLD, 13));
        logoutButton.addActionListener(e -> logout());
        leftPanel.add(logoutButton, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);

        // ===== Khung phải: Chat =====
        JPanel rightPanel = new JPanel(new BorderLayout());

        // Tiêu đề người chat (Header)
        JLabel chatTitle = new JLabel("", SwingConstants.LEFT); // Mặc định hiển thị username của mình
        chatTitle.setFont(new Font("Arial", Font.BOLD, 15));
        chatTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY));
        rightPanel.add(chatTitle, BorderLayout.NORTH);


        // Lịch sử tin nhắn
        chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
        chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        chatScroll = new JScrollPane(chatPanel);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        rightPanel.add(chatScroll, BorderLayout.CENTER);

     // Ô nhập + nút gửi + nút gửi file
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputField.setPreferredSize(new Dimension(0, 40));

        JButton sendButton = new JButton("Gửi");
        sendButton.setBackground(new Color(0, 120, 215));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFont(new Font("Arial", Font.BOLD, 13));

        JButton fileButton = new JButton("📎"); // nút chọn file
        fileButton.setFont(new Font("Arial", Font.BOLD, 16));

        // Thêm nút vào panel
        JPanel rightButtons = new JPanel(new GridLayout(1, 2, 5, 5));
        rightButtons.add(fileButton);
        rightButtons.add(sendButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(rightButtons, BorderLayout.EAST);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);


        add(rightPanel, BorderLayout.CENTER);

        // Sự kiện chọn file
        fileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(ChatClient.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                sendFile(selectedFile);
            }
        });

        
        // Sự kiện chọn user
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = userList.getSelectedValue();
                if (selectedUser != null && !selectedUser.equals(currentTargetUser)) {
                    currentTargetUser = selectedUser;
                    chatTitle.setText(selectedUser);
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
            }
        });

        // Kết nối server
        connectToServer();
    }

    // Xử lý đăng xuất
    private void logout() {
        try {
            if (out != null) {
                out.println("LOGOUT");
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            dispose(); // đóng cửa sổ chat
            // Quay về màn hình đăng nhập
            SwingUtilities.invokeLater(() -> {
                new authencation.LoginClient().setVisible(true);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
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

        JLabel msgLabel = new JLabel("<html><p style='width:250px'>" + text + "</p></html>");
        msgLabel.setOpaque(true);
        msgLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        msgLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        if (isMine) {
            msgLabel.setBackground(new Color(0, 120, 215));
            msgLabel.setForeground(Color.white);
            bubble.add(msgLabel, BorderLayout.EAST);
        } else {
            msgLabel.setBackground(new Color(230, 230, 230));
            bubble.add(msgLabel, BorderLayout.WEST);
        }

        chatPanel.add(bubble);
        chatPanel.revalidate();
        chatPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void sendFile(File file) {
        try {
            // Đọc file thành mảng byte
            byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(fileData);

            // Gửi lên server với định dạng: SEND_FILE:roomId:fileName:fileDataBase64
            if (currentRoom != null) {
                out.println("SEND_FILE:" + currentRoom + ":" + file.getName() + ":" + base64);
            }

            // Hiển thị tin nhắn là link file
            addMessage(username, "[File] " + file.getName(), true);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không thể gửi file!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }


    public static void main(String[] args) {
        String user = JOptionPane.showInputDialog("Nhập username:");
        if (user != null && !user.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> new ChatClient(user.trim()).setVisible(true));
        }
    }
}
