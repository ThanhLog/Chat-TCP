package main;

import authencation.AuthServer;
import authencation.LoginClient;
import chat.ChatServer;
import mongodb.MongoDBUtil;

public class MainApp {
    public static void main(String[] args) {
        // ensure Mongo connected early
        MongoDBUtil.connect();

        // start AuthServer
        new Thread(() -> AuthServer.main(null)).start();
        // start ChatServer
        new Thread(() -> ChatServer.main(null)).start();

        // Shutdown hook to close resources
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown: closing MongoDB");
            MongoDBUtil.close();
        }));

        // show login UI (on EDT)
        javax.swing.SwingUtilities.invokeLater(() -> {
            LoginClient login = new LoginClient();
            login.setVisible(true);
        });
    }
}
