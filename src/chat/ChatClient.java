package chat;

import mongodb.MongoDBUtil;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class ChatClient extends JFrame {
    private String username;
    private Socket socket;
    private PrintWriter out;
    private JTextArea messageArea;
    private JList<String> userList;
    private List<String> originalUsers; // Lưu danh sách user gốc

    public ChatClient(String username) {
        this.username = username;
        setTitle("Chat 1-1 - " + username);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // ==== Khởi tạo UI trước ====
        messageArea = new JTextArea();
        messageArea.setEditable(false);

        originalUsers = MongoDBUtil.getListUserByUsername(username);
        if (originalUsers.isEmpty()) {
            originalUsers.add("⚠️ Không có người dùng nào trong listUser");
        }
        userList = new JList<>(originalUsers.toArray(new String[0]));

        // Thêm ô search
        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Tìm");

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);

        // Left panel
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(searchPanel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        // Right panel
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);

        JTextField messageField = new JTextField();
        JButton sendButton = new JButton("Gửi");
        JPanel sendPanel = new JPanel(new BorderLayout());
        sendPanel.add(messageField, BorderLayout.CENTER);
        sendPanel.add(sendButton, BorderLayout.EAST);
        rightPanel.add(sendPanel, BorderLayout.SOUTH);

        // Split panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(200);
        add(splitPane);

        // ==== Kết nối server sau khi UI đã xong ====
        try {
            socket = new Socket("localhost", 12354);
            out = new PrintWriter(socket.getOutputStream(), true);
            out.println(username);

            new Thread(() -> listenServer()).start();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được ChatServer!");
        }

        // Xử lý gửi tin nhắn
        sendButton.addActionListener(e -> {
            String recipient = userList.getSelectedValue();
            String msg = messageField.getText();
            if (recipient != null && !msg.isEmpty()) {
                out.println(recipient + "|" + msg);
                messageArea.append("Tôi: " + msg + "\n");
                messageField.setText("");
            }
        });

        // Xử lý tìm kiếm user
        searchButton.addActionListener(e -> {
            String keyword = searchField.getText().trim();
            if (!keyword.isEmpty()) {
                List<String> foundUsers = MongoDBUtil.searchUsers(keyword);
                if (foundUsers.isEmpty()) {
                    foundUsers.add("❌ Không tìm thấy user");
                }
                userList.setListData(foundUsers.toArray(new String[0]));
            } else {
                // Nếu ô tìm kiếm trống => hiển thị lại danh sách gốc
                userList.setListData(originalUsers.toArray(new String[0]));
            }
        });

        setVisible(true);
    }

    // Lắng nghe tin nhắn từ server
    private void listenServer() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String msg;
            while ((msg = in.readLine()) != null) {
                String finalMsg = msg;
                SwingUtilities.invokeLater(() -> messageArea.append(finalMsg + "\n"));
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> messageArea.append("Mất kết nối server\n"));
        }
    }
}
