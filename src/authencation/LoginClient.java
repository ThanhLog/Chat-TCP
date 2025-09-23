package authencation;

import javax.swing.*;

import chat.ChatClient;

import java.awt.*;

public class LoginClient extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private AuthClientUtil authClient;

    public LoginClient() {
        authClient = new AuthClientUtil("localhost", 12345);

        setTitle("Đăng nhập");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(244, 246, 249));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Đăng nhập hệ thống", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(51, 51, 51));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        add(new JLabel("Tên đăng nhập:"), gbc);
        gbc.gridx = 1;
        add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        JButton loginBtn = createButton("Đăng nhập", new Color(76, 175, 80));
        JButton registerBtn = createButton("Đăng ký", new Color(33, 150, 243));

        gbc.gridx = 0;
        gbc.gridy = 3;
        add(loginBtn, gbc);
        gbc.gridx = 1;
        add(registerBtn, gbc);

        loginBtn.addActionListener(e -> handleLogin());
        registerBtn.addActionListener(e -> {
            new RegisterClient().setVisible(true);
            dispose();
        });
    }

    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        return button;
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String response = authClient.sendRequest("LOGIN", username, password);

        if ("LOGIN_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Đăng nhập thành công!");
            new ChatClient(username);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginClient().setVisible(true));
    }
}
