package chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    private static Map<String, PrintWriter> clients = new HashMap<>();
    private static Map<String, Boolean> userStatus = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat Server chạy trên cổng " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Lấy username từ client
                username = in.readLine();
                synchronized (clients) {
                    clients.put(username, out);
                    userStatus.put(username, true);
                }

                sendUserList();
                broadcastStatus();

                // Lắng nghe tin nhắn
                String input;
                while ((input = in.readLine()) != null) {
                    processMessage(input);
                }
            } catch (IOException e) {
                System.out.println("Client " + username + " ngắt kết nối.");
            } finally {
                try { socket.close(); } catch (IOException e) {}
                synchronized (clients) {
                    clients.remove(username);
                    userStatus.put(username, false);
                }
                sendUserList();
                broadcastStatus();
            }
        }

        private void processMessage(String input) {
            String[] parts = input.split("\\|", 4);
            if (parts.length < 4) return;
            String type = parts[0];
            String sender = parts[1];
            String recipient = parts[2];
            String content = parts[3];

            switch (type) {
                case "CHAT":
                    sendMessage(recipient, "CHAT|" + sender + "|" + recipient + "|" + content);
                    break;
                case "SEEN":
                    sendMessage(recipient, "SEEN|" + sender + "|" + recipient + "|" + content);
                    break;
            }
        }

        private void sendMessage(String recipient, String message) {
            synchronized (clients) {
                PrintWriter writer = clients.get(recipient);
                if (writer != null) {
                    writer.println(message);
                }
            }
        }

        private void broadcastStatus() {
            synchronized (clients) {
                StringBuilder sb = new StringBuilder("STATUS|Server|ALL|");
                for (Map.Entry<String, Boolean> entry : userStatus.entrySet()) {
                    sb.append(entry.getKey()).append("=")
                      .append(entry.getValue() ? "Online" : "Offline").append(";");
                }
                for (PrintWriter writer : clients.values()) {
                    writer.println(sb.toString());
                }
            }
        }

        private void sendUserList() {
            synchronized (clients) {
                StringBuilder sb = new StringBuilder("USER_LIST|Server|ALL|");
                for (String user : clients.keySet()) {
                    sb.append(user).append(";");
                }
                for (PrintWriter writer : clients.values()) {
                    writer.println(sb.toString());
                }
            }
        }
    }
}
