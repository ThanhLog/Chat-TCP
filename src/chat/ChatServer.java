package chat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12354;
    private static Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static Map<String, Set<String>> rooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ ChatServer started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                new ClientHandler(socket).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Error in ChatServer: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Nhận username từ client
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) {
                    socket.close();
                    return;
                }

                clients.put(username, this);
                sendUserList();
                broadcast("SERVER", username + " đã online");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CREATE_ROOM:")) {
                        handleCreateRoom(message);
                    } else if (message.startsWith("SEND_ROOM:")) {
                        handleSendRoom(message);
                    } else if ("GET_USERS".equalsIgnoreCase(message)) {
                        sendUserList();
                    }
                }
            } catch (IOException e) {
                System.out.println("❌ Mất kết nối với " + username);
            } finally {
                if (username != null) {
                    clients.remove(username);
                    broadcast("SERVER", username + " đã offline");
                }
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handleCreateRoom(String message) {
            // CREATE_ROOM:user1,user2
            String[] parts = message.split(":", 2);
            if (parts.length < 2) return;

            String[] users = parts[1].split(",");
            String roomId = "ROOM_" + System.currentTimeMillis();
            Set<String> roomUsers = new HashSet<>(Arrays.asList(users));

            rooms.put(roomId, roomUsers);
            out.println("SERVER: Room created with ID " + roomId);
        }

        private void handleSendRoom(String message) {
            // SEND_ROOM:roomId:message
            String[] parts = message.split(":", 3);
            if (parts.length < 3) return;

            String roomId = parts[1];
            String msg = parts[2];

            Set<String> roomUsers = rooms.get(roomId);
            if (roomUsers != null) {
                for (String user : roomUsers) {
                    ClientHandler client = clients.get(user);
                    if (client != null) {
                        client.out.println("[" + roomId + "] " + username + ": " + msg);
                    }
                }
            } else {
                out.println("SERVER: Room " + roomId + " không tồn tại!");
            }
        }

        private void broadcast(String sender, String message) {
            for (ClientHandler client : clients.values()) {
                client.out.println(sender + ": " + message);
            }
        }

        private void sendUserList() {
            StringBuilder userList = new StringBuilder("USER_LIST:");
            for (String user : clients.keySet()) {
                userList.append(user).append(",");
            }
            if (userList.length() > 10) {
                userList.setLength(userList.length() - 1);
            }
            out.println(userList);
        }
    }
}
