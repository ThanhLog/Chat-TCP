package authencation;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class LoginClient extends JFrame {
    private JTextField userField;
    private JPasswordField passField;

    public LoginClient() {
        super("Đăng nhập / Đăng ký");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel chính
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        add(mainPanel);

        // Tiêu đề
        JLabel title = new JLabel("Welcome to Chat App", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(20));

        // Username
        userField = new JTextField();
        styleTextField(userField, "Username");
        mainPanel.add(userField);
        mainPanel.add(Box.createVerticalStrut(10));

        // Password
        passField = new JPasswordField();
        styleTextField(passField, "Password");
        mainPanel.add(passField);
        mainPanel.add(Box.createVerticalStrut(20));

        // Panel nút bấm
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        JButton btnLogin = new JButton("Login");
        JButton btnReg = new JButton("Register");
        styleButton(btnLogin, new Color(0, 153, 255));
        styleButton(btnReg, new Color(0, 200, 100));
        buttonPanel.add(btnLogin);
        buttonPanel.add(btnReg);
        mainPanel.add(buttonPanel);

        // Sự kiện nút
        btnReg.addActionListener(e -> doRegister());
        btnLogin.addActionListener(e -> doLogin());
    }

    private void styleTextField(JTextField field, String placeholder) {
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        field.setToolTipText(placeholder);
    }

    private void styleButton(JButton button, Color bgColor) {
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setOpaque(true);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(100, 35));
    }

    private void doRegister() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());
        if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin"); return; }
        try (Socket s = new Socket("localhost", AuthServer.PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"))) {
            out.println("REGISTER:" + u + ":" + p);
            String resp = in.readLine();
            if ("REGISTER_OK".equals(resp)) JOptionPane.showMessageDialog(this, "Đăng ký thành công");
            else JOptionPane.showMessageDialog(this, "Đăng ký thất bại");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối AuthServer: " + ex.getMessage());
        }
    }

    private void doLogin() {
        String u = userField.getText().trim();
        String p = new String(passField.getPassword());
        if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ thông tin"); return; }
        try (Socket s = new Socket("localhost", AuthServer.PORT);
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"))) {
            out.println("LOGIN:" + u + ":" + p);
            String resp = in.readLine();
            if ("LOGIN_OK".equals(resp)) {
                SwingUtilities.invokeLater(() -> {
                    new chat.ChatClient(u).setVisible(true);
                });
                this.dispose();
            } else JOptionPane.showMessageDialog(this, "Đăng nhập thất bại");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Không kết nối AuthServer: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginClient().setVisible(true));
    }
}
