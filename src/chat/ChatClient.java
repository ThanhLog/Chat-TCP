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
    
    private static final Color PRIMARY_COLOR = new Color(30, 144, 255); // Dodger Blue (Màu chính)
    private static final Color DANGER_COLOR = new Color(220, 53, 69); // Đỏ sẫm (Đăng xuất)
    private static final Color BACKGROUND_GRAY = new Color(245, 245, 245); // Nền JFrame
    private static final Color LIGHT_BORDER_COLOR = new Color(221, 221, 221); // Viền nhẹ
    private static final Color LEFT_PANEL_BG = Color.WHITE; // Nền khung user

public ChatClient(String username) {
    this.username = username;
    setTitle("Chat - " + username);
    setSize(900, 600);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout());
    getContentPane().setBackground(BACKGROUND_GRAY);

    // ===== Khung trái: Danh sách user =====
    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.setPreferredSize(new Dimension(220, 0));
    leftPanel.setBackground(LEFT_PANEL_BG);

    JLabel onlineLabel = new JLabel("ONLINE USERS", SwingConstants.CENTER);
    onlineLabel.setFont(new Font("Arial", Font.BOLD, 15));
    onlineLabel.setForeground(PRIMARY_COLOR);
    onlineLabel.setBorder(new EmptyBorder(15, 0, 15, 0));
    leftPanel.add(onlineLabel, BorderLayout.NORTH);

    userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.setFont(new Font("Arial", Font.PLAIN, 14));
    JScrollPane userScrollPane = new JScrollPane(userList);
    userScrollPane.setBorder(BorderFactory.createEmptyBorder());
    leftPanel.add(userScrollPane, BorderLayout.CENTER);

    JButton logoutButton = new JButton("Đăng xuất");
    logoutButton.setBackground(DANGER_COLOR);
    logoutButton.setForeground(Color.WHITE);
    logoutButton.setFont(new Font("Arial", Font.BOLD, 14));
    logoutButton.setBorder(new EmptyBorder(12, 10, 12, 10));
    logoutButton.addActionListener(e -> logout());
    leftPanel.add(logoutButton, BorderLayout.SOUTH);

    leftPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, LIGHT_BORDER_COLOR));
    add(leftPanel, BorderLayout.WEST);

    // ===== Khung phải: Chat =====
    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.setBackground(Color.WHITE);

    JLabel chatTitle = new JLabel("<html><div style='padding: 10px 15px;'>Chọn một người để bắt đầu chat</div></html>", SwingConstants.LEFT);
    chatTitle.setFont(new Font("Arial", Font.BOLD, 16));
    chatTitle.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LIGHT_BORDER_COLOR));
    rightPanel.add(chatTitle, BorderLayout.NORTH);

    chatPanel.setLayout(new BoxLayout(chatPanel, BoxLayout.Y_AXIS));
    chatPanel.setBackground(Color.WHITE);
    chatPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    chatScroll = new JScrollPane(chatPanel);
    chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    chatScroll.setBorder(BorderFactory.createEmptyBorder());
    rightPanel.add(chatScroll, BorderLayout.CENTER);

    // ===== Input Panel =====
    JPanel inputPanel = new JPanel(new BorderLayout(10, 10));
    inputPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, LIGHT_BORDER_COLOR),
            new EmptyBorder(10, 10, 10, 10)
    ));
    inputPanel.setBackground(BACKGROUND_GRAY);

    inputField.setPreferredSize(new Dimension(0, 45));
    inputField.setFont(new Font("Arial", Font.PLAIN, 14));
    inputField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(LIGHT_BORDER_COLOR), 
        new EmptyBorder(8, 10, 8, 10)
    ));
    inputField.setBackground(Color.WHITE);

    JButton sendButton = new JButton("Gửi");
    sendButton.setBackground(PRIMARY_COLOR);
    sendButton.setForeground(Color.WHITE);
    sendButton.setFont(new Font("Arial", Font.BOLD, 14));
    sendButton.setPreferredSize(new Dimension(80, 45));

    JButton fileButton = new JButton("📎"); 
    fileButton.setFont(new Font("Arial", Font.PLAIN, 20));
    fileButton.setBackground(Color.WHITE);
    fileButton.setForeground(PRIMARY_COLOR);
    fileButton.setPreferredSize(new Dimension(45, 45));
    fileButton.setBorder(BorderFactory.createLineBorder(LIGHT_BORDER_COLOR));

    JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
    rightButtons.add(fileButton);
    rightButtons.add(sendButton);
    rightButtons.setBackground(BACKGROUND_GRAY); 

    inputPanel.add(inputField, BorderLayout.CENTER);
    inputPanel.add(rightButtons, BorderLayout.EAST);
    rightPanel.add(inputPanel, BorderLayout.SOUTH);

    add(rightPanel, BorderLayout.CENTER);

    // ===== EVENT HANDLERS =====
    
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
                chatTitle.setText("<html><div style='padding: 10px 15px;'>" + selectedUser + "</div></html>");
                out.println("CREATE_OR_GET_ROOM:" + username + "," + selectedUser);
            }
        }
    });

    // ✓ FIX: Tạo method riêng để gửi tin nhắn
    Runnable sendMessage = () -> {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && currentRoom != null) {
            // CHỈ gửi lên server, KHÔNG addMessage ở đây
            // Server sẽ broadcast lại cho cả người gửi
            out.println("SEND:" + currentRoom + ":" + text);
            inputField.setText("");
        } else if (text.isEmpty()) {
            // Không làm gì nếu text rỗng
        } else {
            JOptionPane.showMessageDialog(ChatClient.this, 
                "Vui lòng chọn người nhận trước!", 
                "Thông báo", 
                JOptionPane.WARNING_MESSAGE);
        }
    };

    // ✓ FIX: Thêm ActionListener cho nút Gửi
    sendButton.addActionListener(e -> sendMessage.run());

    // Gửi tin nhắn khi nhấn Enter
    inputField.addActionListener(e -> sendMessage.run());

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
        try {
            if (line.startsWith("USER_LIST:")) {
                SwingUtilities.invokeLater(() -> {
                    userListModel.clear();
                    String[] users = line.substring(10).split(",");
                    for (String u : users) {
                        if (!u.equals(username) && !u.isBlank()) {
                            userListModel.addElement(u);
                        }
                    }
                });
            }

            else if (line.startsWith("ROOM_ID:")) {
                currentRoom = line.substring(8);
                roomMessages.putIfAbsent(currentRoom, new ArrayList<>());
                chatPanel.removeAll();
                chatPanel.revalidate();
                chatPanel.repaint();
                out.println("GET_MESSAGES:" + currentRoom);
            }

            else if (line.startsWith("MSG:")) {
                // Nhận tin nhắn text
                String[] parts = line.split(":", 5);
                if (parts.length >= 5) {
                    String roomId = parts[1];
                    String sender = parts[2];
                    String msg = parts[3];
                    // parts[4] là createAt - không cần hiển thị
                    
                    roomMessages.computeIfAbsent(roomId, k -> new ArrayList<>()).add(sender + ": " + msg);

                    if (roomId.equals(currentRoom)) {
                        SwingUtilities.invokeLater(() -> addMessage(sender, msg, sender.equals(username)));
                    }
                }
            }

            else if (line.startsWith("FILE_MSG:")) {
                // Format: FILE_MSG:roomId:sender:fileName:base64:createAt
                String[] parts = line.split(":", 6);
                if (parts.length == 6) {  // ✓ Phải đúng 6 phần
                    String roomId = parts[1];
                    String sender = parts[2];
                    String fileName = parts[3];
                    String base64Data = parts[4];
                    // parts[5] là createAt

                    System.out.println("📎 Received FILE_MSG: " + fileName + " from " + sender);

                    try {
                        // Giải mã dữ liệu base64
                        byte[] fileBytes = Base64.getDecoder().decode(base64Data);
                        File dir = new File("downloads");
                        if (!dir.exists()) {
                            dir.mkdirs();
                        }
                        
                        // Tạo tên file unique nếu đã tồn tại
                        File file = new File(dir, fileName);
                        int counter = 1;
                        while (file.exists()) {
                            String name = fileName.substring(0, fileName.lastIndexOf('.'));
                            String ext = fileName.substring(fileName.lastIndexOf('.'));
                            file = new File(dir, name + "_" + counter + ext);
                            counter++;
                        }
                        
                        java.nio.file.Files.write(file.toPath(), fileBytes);
                        System.out.println("✓ File saved: " + file.getAbsolutePath());

                        if (roomId.equals(currentRoom)) {
                            File finalFile = file;
                            SwingUtilities.invokeLater(() -> addFileMessage(sender, finalFile, sender.equals(username)));
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("❌ Invalid base64 data for file: " + fileName);
                        e.printStackTrace();
                    } catch (Exception e) {
                        System.err.println("❌ Error saving file: " + fileName);
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("❌ Invalid FILE_MSG format (expected 6 parts, got " + parts.length + ")");
                }
            }

            else if (line.equals("END_MESSAGES")) {
                // Lịch sử tin nhắn đã được load xong
                // Không cần làm gì thêm vì MSG và FILE_MSG đã được xử lý riêng
                SwingUtilities.invokeLater(() -> {
                    chatPanel.revalidate();
                    chatPanel.repaint();
                });
            }

        } catch (Exception e) {
            System.err.println("❌ Error handling server message: " + line.substring(0, Math.min(50, line.length())));
            e.printStackTrace();
        }
    }
    
    
   private void addFileMessage(String sender, File file, boolean isMine) {
	   JPanel outerContainer = new JPanel(new FlowLayout(isMine ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 0));
	   outerContainer.setBackground(Color.WHITE);

	   JPanel bubble = new JPanel();
	   bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));

	   JLabel label;
	   if (file.getName().matches(".*\\.(jpg|jpeg|png|gif)$")) {
	       // Hiển thị ảnh
	       try {
	           ImageIcon icon = new ImageIcon(file.getAbsolutePath());
	           Image scaled = icon.getImage().getScaledInstance(150, -1, Image.SCALE_SMOOTH);
	           label = new JLabel(new ImageIcon(scaled));
	       } catch (Exception e) {
	           label = new JLabel("[Không hiển thị được ảnh]");
	       }
	   } else {
	       // Hiển thị link tải
	       label = new JLabel("<html><a href='#'>" + file.getName() + "</a></html>");
	       label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	       label.addMouseListener(new java.awt.event.MouseAdapter() {
	           @Override
	           public void mouseClicked(java.awt.event.MouseEvent e) {
	               try {
	                   Desktop.getDesktop().open(file);
	               } catch (Exception ex) {
	                   JOptionPane.showMessageDialog(null, "Không mở được file!");
	               }
	           }
	       });
	   }

	   label.setOpaque(true);
	   label.setBackground(isMine ? PRIMARY_COLOR : new Color(230, 230, 230));
	   label.setForeground(isMine ? Color.WHITE : Color.BLACK);
	   label.setBorder(new EmptyBorder(5, 10, 5, 10));

	   bubble.add(label);
	   outerContainer.add(bubble);
	   chatPanel.add(outerContainer);
	   chatPanel.add(Box.createVerticalStrut(4));

	   chatPanel.revalidate();
	   chatPanel.repaint();

	   SwingUtilities.invokeLater(() -> {
	       JScrollBar vertical = chatScroll.getVerticalScrollBar();
	       vertical.setValue(vertical.getMaximum());
	   });
	   }
    // Thêm bong bóng tin nhắn

    private void addMessage(String sender, String text, boolean isMine) {
        // LỚP 1: Container ngoài cùng để căn chỉnh (trái/phải)
        // FlowLayout.LEFT/RIGHT sẽ buộc bong bóng chỉ chiếm chiều rộng cần thiết
        JPanel outerContainer = new JPanel(new FlowLayout(isMine ? FlowLayout.RIGHT : FlowLayout.LEFT, 10, 0));
        outerContainer.setBackground(Color.WHITE); // Đảm bảo nền khớp với chatPanel

        // LỚP 2: Bong bóng tin nhắn
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS)); // Dùng BoxLayout để nội dung xếp chồng (nếu cần)

        // Lớp 3: Nội dung tin nhắn (sử dụng HTML để xuống dòng và giới hạn chiều rộng)
        // **text** là nội dung cần hiển thị
        JLabel msgLabel = new JLabel("<html><p style='padding: 5px 10px; max-width: 300px; word-wrap: break-word;'>" 
                                        + text + "</p></html>");
        msgLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        msgLabel.setOpaque(true);
        
        // Đặt màu sắc và bo góc/padding
        if (isMine) {
            msgLabel.setBackground(PRIMARY_COLOR); // Xanh dương
            msgLabel.setForeground(Color.WHITE);
            // Có thể thêm BorderFactory.createLineBorder để tạo hiệu ứng bong bóng bo tròn, nhưng nó đòi hỏi thư viện bên ngoài.
        } else {
            msgLabel.setBackground(new Color(230, 230, 230)); // Xám nhạt
            msgLabel.setForeground(Color.BLACK);
        }

        // Thêm nhãn vào bong bóng (Lớp 2)
        bubble.add(msgLabel);
        bubble.setBorder(new EmptyBorder(5, 0, 5, 0)); // Khoảng cách trên/dưới cho mỗi bong bóng

        // Thêm bong bóng vào container ngoài (Lớp 1)
        outerContainer.add(bubble);

        // Thêm container ngoài vào chat panel
        chatPanel.add(outerContainer);
        
        // Thêm một khoảng cách nhỏ giữa các tin nhắn (tùy chọn)
        chatPanel.add(Box.createVerticalStrut(2)); 
        
        chatPanel.revalidate();
        chatPanel.repaint();

        // Cuộn xuống
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScroll.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    private void sendFile(File file) {
        try {
            String fileName = file.getName().toLowerCase();
            
            // Kiểm tra định dạng file
            if (!(fileName.endsWith(".jpg") || fileName.endsWith(".png") ||
                  fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                  fileName.endsWith(".pdf") || fileName.endsWith(".docx") ||
                  fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
                JOptionPane.showMessageDialog(this, 
                    "Chỉ hỗ trợ gửi ảnh, Word, hoặc Excel!", 
                    "Thông báo", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Kiểm tra room
            if (currentRoom == null) {
                JOptionPane.showMessageDialog(this, 
                    "Vui lòng chọn người nhận trước!", 
                    "Thông báo", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Kiểm tra kích thước file (max 10MB)
            long fileSize = file.length();
            if (fileSize > 10 * 1024 * 1024) {
                JOptionPane.showMessageDialog(this, 
                    "File quá lớn! Giới hạn 10MB", 
                    "Thông báo", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            System.out.println("📎 Sending file: " + file.getName() + " (" + fileSize + " bytes)");

            // Đọc file và mã hóa base64
            byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
            String base64 = Base64.getEncoder().encodeToString(fileData);

            System.out.println("✓ File encoded to base64 (" + base64.length() + " chars)");

            // Gửi lên server
            String command = "SEND_FILE:" + currentRoom + ":" + file.getName() + ":" + base64;
            out.println(command);
            out.flush(); // Đảm bảo dữ liệu được gửi ngay

            System.out.println("✓ File sent to server");

            // KHÔNG addFileMessage ở đây - server sẽ broadcast lại
            // Tin nhắn sẽ được hiển thị khi nhận FILE_MSG từ server

        } catch (IOException e) {
            System.err.println("❌ Error sending file:");
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Không thể gửi file!\n" + e.getMessage(), 
                "Lỗi", 
                JOptionPane.ERROR_MESSAGE);
        }
    }


    public static void main(String[] args) {
        String user = JOptionPane.showInputDialog("Nhập username:");
        if (user != null && !user.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> new ChatClient(user.trim()).setVisible(true));
        }
    }
}
