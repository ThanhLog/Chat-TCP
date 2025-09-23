package main;

import authencation.AuthServer;
import chat.ChatServer;
import authencation.LoginClient;

public class MainApp {
    public static void main(String[] args) {
        // Khởi chạy AuthServer trong thread riêng
        Thread authThread = new Thread(() -> {
            try {
                AuthServer.main(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        authThread.start();

        // Khởi chạy ChatServer trong thread riêng
        Thread chatThread = new Thread(() -> {
            try {
                ChatServer.main(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        chatThread.start();

        // Chạy LoginClient với giao diện Swing
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                LoginClient login = new LoginClient();
                login.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
