package chat;

import com.mongodb.client.model.Filters;
import mongodb.MongoDBUtil;
import org.bson.Document;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    public static final int PORT = 12354;
    private static ServerSocket serverSocket;
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    // cache room participants for quick broadcast
    private static final ConcurrentHashMap<String, Set<String>> rooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        MongoDBUtil.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (Exception ignored){}
            MongoDBUtil.close();
        }));

        try {
            serverSocket = new ServerSocket(PORT);
            // keep a single startup message (not chat content)
            System.out.println("ChatServer listening on " + PORT);
            while (true) {
                Socket s = serverSocket.accept();
                new Thread(new ClientHandler(s)).start();
            }
        } catch (IOException e) {
            System.err.println("ChatServer stopped: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;

        ClientHandler(Socket socket) { this.socket = socket; }

        /**
         * Safe send: kiểm tra out != null, bọc try/catch để tránh crash nếu client đã disconnect.
         */
        private void sendLine(String line) {
            try {
                PrintWriter w = out; // local ref
                if (w != null) {
                    synchronized (w) { w.println(line); }
                }
            } catch (Exception ignored) {
                // swallow: nếu gửi thất bại thì không in message ra console (theo yêu cầu)
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                username = in.readLine();
                if (username == null || username.trim().isEmpty()) { closeSocket(); return; }
                username = username.trim();
                clients.put(username, this);
                broadcastUserList();

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("CREATE_OR_GET_ROOM:")) handleCreateRoom(line);
                    else if (line.startsWith("GET_MESSAGES:")) handleGetMessages(line);
                    else if (line.startsWith("SEND_FILE:")) handleSendFile(line);  // ✓ Thêm dòng này
                    else if (line.startsWith("SEND:")) handleSendText(line);        // ✓ Đổi tên
                    else if (line.equalsIgnoreCase("GET_USERS")) sendUserList();
                    else if (line.equalsIgnoreCase("LOGOUT")) break;
                }
            } catch (IOException e) {
                // connection lost
            } finally {
                cleanup();
            }
        }

        // Tách riêng method xử lý TEXT
        private void handleSendText(String line) {
            try {
                // Format: SEND:roomId:message
                String[] parts = line.split(":", 3);
                if (parts.length < 3) return;
                String roomId = parts[1].trim();
                String message = parts[2];

                if (roomId.isEmpty() || message == null || message.trim().isEmpty()) return;

                // Lưu vào MongoDB
                MongoDBUtil.saveMessage(roomId, username, message, "TEXT", null);

                // Đảm bảo danh sách user trong phòng có trong cache
                Set<String> participants = rooms.computeIfAbsent(roomId, r -> {
                    Document doc = MongoDBUtil.getDatabase()
                            .getCollection("chat_rooms")
                            .find(Filters.eq("_id", r))
                            .first();
                    if (doc != null) {
                        List<String> us = doc.getList("users", String.class);
                        return new HashSet<>(us != null ? us : Collections.emptyList());
                    }
                    return new HashSet<>();
                });

                participants.add(username);

                String createdAt = Instant.now().toString();
                for (String user : participants) {
                    ClientHandler ch = clients.get(user);
                    if (ch != null) {
                        ch.sendLine("MSG:" + roomId + ":" + username + ":" + message + ":" + createdAt);
                        System.out.println("💬 Sent message from " + username + " to " + user);
                    } else {
                        System.out.println("⚠️  User " + user + " offline (message not sent)");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                sendLine("ERROR:SEND");
            }
        }

        // Tách riêng method xử lý FILE
        private void handleSendFile(String line) {
            try {
                // Format: SEND_FILE:roomId:fileName:base64
                String[] parts = line.split(":", 4);
                if (parts.length < 4) {
                    System.err.println("Invalid SEND_FILE format: " + line.substring(0, Math.min(50, line.length())));
                    return;
                }
                
                String roomId = parts[1].trim();
                String fileName = parts[2].trim();
                String base64 = parts[3];

                if (roomId.isEmpty() || fileName.isEmpty() || base64 == null || base64.trim().isEmpty()) {
                    System.err.println("Empty field in SEND_FILE");
                    return;
                }

                System.out.println("📎 Receiving file: " + fileName + " for room: " + roomId);

                // Lưu vào MongoDB
                MongoDBUtil.saveMessage(roomId, username, base64, "FILE", fileName);

                // Đảm bảo danh sách user trong phòng có trong cache
                Set<String> participants = rooms.computeIfAbsent(roomId, r -> {
                    Document doc = MongoDBUtil.getDatabase()
                            .getCollection("chat_rooms")
                            .find(Filters.eq("_id", r))
                            .first();
                    if (doc != null) {
                        List<String> us = doc.getList("users", String.class);
                        return new HashSet<>(us != null ? us : Collections.emptyList());
                    }
                    return new HashSet<>();
                });

                participants.add(username);

                String createdAt = Instant.now().toString();
                int sentCount = 0;
                for (String user : participants) {
                    ClientHandler ch = clients.get(user);
                    if (ch != null) {
                        ch.sendLine("FILE_MSG:" + roomId + ":" + username + ":" + fileName + ":" + base64 + ":" + createdAt);
                        System.out.println("📎 Sent file '" + fileName + "' from " + username + " to " + user);
                        sentCount++;
                    } else {
                        System.out.println("⚠️  User " + user + " offline (file not sent)");
                    }
                }
                
                System.out.println("✓ File sent to " + sentCount + "/" + participants.size() + " participants");

            } catch (Exception e) {
                System.err.println("Error handling file send:");
                e.printStackTrace();
                sendLine("ERROR:SEND_FILE");
            }
        }

        private void handleCreateRoom(String line) {
            try {
                String[] parts = line.split(":", 2);
                if (parts.length < 2) return;
                String[] users = parts[1].split(",", 2);
                if (users.length < 2) return;
                String a = users[0].trim(), b = users[1].trim();
                if (a.isEmpty() || b.isEmpty()) return;

                String roomId = MongoDBUtil.createRoomIfNotExists(a, b);

                // update cache of participants (synchronized via compute)
                Document roomDoc = MongoDBUtil.getDatabase().getCollection("chat_rooms")
                        .find(Filters.eq("_id", roomId)).first();
                if (roomDoc != null) {
                    List<String> us = roomDoc.getList("users", String.class);
                    rooms.put(roomId, new HashSet<>(us != null ? us : Collections.emptyList()));
                }

                // send back room id (client will request messages)
                sendLine("ROOM_ID:" + roomId);
            } catch (Exception e) {
                // don't expose internal errors to clients; simply send a generic error
                sendLine("ERROR:CREATE_ROOM");
            }
        }

private void handleGetMessages(String line) {
    try {
        String[] parts = line.split(":", 2);
        if (parts.length < 2) return;
        String roomId = parts[1].trim();
        if (roomId.isEmpty()) return;

        List<Document> msgs = MongoDBUtil.getMessages(roomId);
        for (Document m : msgs) {
            String sender = safeGetString(m, "username");
            String content = safeGetString(m, "message");
            String type = safeGetString(m, "type");
            String createAt = safeGetString(m, "createAt");
            
            if ("FILE".equals(type)) {
                // Gửi tin nhắn dạng file
                String fileName = safeGetString(m, "fileName");
                sendLine("FILE_MSG:" + roomId + ":" + sender + ":" + fileName + ":" + content + ":" + createAt);
            } else {
                // Gửi tin nhắn text
                sendLine("MSG:" + roomId + ":" + sender + ":" + content + ":" + createAt);
            }
        }
        sendLine("END_MESSAGES");
    } catch (Exception e) {
        System.err.println("Error in handleGetMessages:");
        e.printStackTrace();
        sendLine("ERROR:GET_MESSAGES");
    }
}


        
        private void sendUserList() {
            StringBuilder sb = new StringBuilder("USER_LIST:");
            boolean first = true;
            for (String u : clients.keySet()) {
                if (!u.equals(username)) {
                    if (!first) sb.append(",");
                    sb.append(u);
                    first = false;
                }
            }
            sendLine(sb.toString());
        }

        private void broadcastUserList() {
            StringBuilder sb = new StringBuilder("USER_LIST:");
            boolean first = true;
            for (String u : clients.keySet()) {
                if (!first) sb.append(",");
                sb.append(u);
                first = false;
            }
            String payload = sb.toString();
            // send to all clients; sendLine handles failures quietly
            for (ClientHandler ch : clients.values()) {
                ch.sendLine(payload);
            }
        }

        /** Safe getter from BSON Document */
        private String safeGetString(Document d, String key) {
            if (d == null) return "";
            Object o = d.get(key);
            return o == null ? "" : o.toString();
        }

        private void cleanup() {
            try {
                if (username != null) {
                    clients.remove(username);
                    // optionally remove user from cached rooms (keep history in DB)
                    // remove user from any in-memory room participant sets
                    for (Map.Entry<String, Set<String>> e : rooms.entrySet()) {
                        Set<String> set = e.getValue();
                        if (set != null) set.remove(username);
                    }
                    broadcastUserList();
                }
                closeSocket();
            } catch (Exception ignored) {}
        }

        private void closeSocket() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ignored) {}
        }
    }
}
