package chat;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {
    private static final int PORT = 12345;
    // Lưu username và output stream của client
    private static Map<String, PrintWriter> clients = new HashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat 1-1 Server chạy trên cổng " + PORT);

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

                // B1: Client gửi username ngay khi kết nối
                username = in.readLine();
                synchronized (clients) {
                    clients.put(username, out);
                }
                System.out.println(username + " đã tham gia.");

                // B2: Lắng nghe tin nhắn dưới dạng: recipient:message
                String input;
                while ((input = in.readLine()) != null) {
                    String[] parts = input.split(":", 2);
                    if (parts.length == 2) {
                        String recipient = parts[0];
                        String message = parts[1];
                        sendMessage(recipient, username + ": " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client " + username + " đã ngắt kết nối.");
            } finally {
                try { socket.close(); } catch (IOException e) {}
                synchronized (clients) {
                    clients.remove(username);
                }
            }
        }

        private void sendMessage(String recipient, String message) {
            synchronized (clients) {
                PrintWriter writer = clients.get(recipient);
                if (writer != null) {
                    writer.println(message);
                } else {
                    out.println("Người dùng " + recipient + " không tồn tại!");
                }
            }
        }
    }
}
