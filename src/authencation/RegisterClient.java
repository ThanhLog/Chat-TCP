package authencation;

import javax.swing.*;
import java.awt.*;

public class RegisterClient extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private AuthClientUtil authClient;

    public RegisterClient() {
        authClient = new AuthClientUtil("localhost", 12345);

        setTitle("Đăng ký");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());
        getContentPane().setBackground(new Color(244, 246, 249));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Tạo tài khoản mới", SwingConstants.CENTER);
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

        JButton registerBtn = createButton("Đăng ký", new Color(33, 150, 243));
        JButton backBtn = createButton("Quay lại", new Color(158, 158, 158));

        gbc.gridx = 0;
        gbc.gridy = 3;
        add(registerBtn, gbc);
        gbc.gridx = 1;
        add(backBtn, gbc);

        registerBtn.addActionListener(e -> handleRegister());
        backBtn.addActionListener(e -> {
            new LoginClient().setVisible(true);
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

    private void handleRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String response = authClient.sendRequest("REGISTER", username, password);

        if ("REGISTER_SUCCESS".equals(response)) {
            JOptionPane.showMessageDialog(this, "Đăng ký thành công!");
            new LoginClient().setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Tài khoản đã tồn tại!");
        }
    }
}
