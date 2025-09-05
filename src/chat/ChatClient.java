package chat;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ChatClient {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private JFrame frame;
    private JTextArea textArea;
    private JTextField textField;
    private JTextField recipientField;
    private String username;

    public ChatClient(String username) {
        this.username = username;
        frame = new JFrame("Chat Client - " + username);
        textArea = new JTextArea(20, 50);
        textArea.setEditable(false);
        textField = new JTextField(40);
        recipientField = new JTextField(10);

        JPanel panel = new JPanel();
        panel.add(new JLabel("To:"));
        panel.add(recipientField);
        panel.add(textField);

        frame.getContentPane().add(new JScrollPane(textArea), BorderLayout.CENTER);
        frame.getContentPane().add(panel, BorderLayout.SOUTH);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        textField.addActionListener(e -> {
            String recipient = recipientField.getText().trim();
            String message = textField.getText().trim();
            if (!recipient.isEmpty() && !message.isEmpty()) {
                out.println(recipient + ":" + message);
                textField.setText("");
            }
        });
    }

    public void start() throws IOException {
        socket = new Socket(SERVER_IP, SERVER_PORT);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Gửi username đến server ngay khi kết nối
        out.println(username);

        // Luồng nhận tin nhắn
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    textArea.append(message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        frame.setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        String username = JOptionPane.showInputDialog("Nhập tên của bạn:");
        new ChatClient(username).start();
    }
}
