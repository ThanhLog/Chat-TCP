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
                broadcastUserList(); // notify all clients of user list

                String line;
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("CREATE_OR_GET_ROOM:")) handleCreateRoom(line);
                    else if (line.startsWith("GET_MESSAGES:")) handleGetMessages(line);
                    else if (line.startsWith("SEND:")) handleSend(line);
                    else if (line.equalsIgnoreCase("GET_USERS")) sendUserList();
                    // unknown commands are ignored silently
                }
            } catch (IOException e) {
                // connection lost or IO error; do not print chat contents
            } finally {
                cleanup();
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
                    String content = safeGetString(m, "message"); // base64 stored
                    String createAt = safeGetString(m, "createAt");
                    sendLine("MSG:" + roomId + ":" + sender + ":" + content + ":" + createAt);
                }
                sendLine("END_MESSAGES");
            } catch (Exception e) {
                sendLine("ERROR:GET_MESSAGES");
            }
        }

        private void handleSend(String line) {
            try {
                String[] parts = line.split(":", 3);
                if (parts.length < 3) return;
                String roomId = parts[1].trim();
                String base64 = parts[2];
                if (roomId.isEmpty() || base64 == null) return;

                // save to DB (store base64 as-is)
                MongoDBUtil.saveMessage(roomId, username, base64, "SEND");

                // ensure room participants cached (thread-safe computeIfAbsent)
                rooms.computeIfAbsent(roomId, r -> {
                    Document doc = MongoDBUtil.getDatabase().getCollection("chat_rooms")
                            .find(Filters.eq("_id", r)).first();
                    if (doc != null) {
                        List<String> us = doc.getList("users", String.class);
                        return new HashSet<>(us != null ? us : Collections.emptyList());
                    }
                    return new HashSet<>();
                });

                String createAt = Instant.now().toString();
                Set<String> participants = rooms.getOrDefault(roomId, Collections.emptySet());

                // Broadcast only to participants that are currently online
                for (String user : participants) {
                    ClientHandler ch = clients.get(user);
                    if (ch != null) {
                        ch.sendLine("MSG:" + roomId + ":" + username + ":" + base64 + ":" + createAt);
                    }
                }
                // Server intentionally does NOT print message contents to console.
            } catch (Exception e) {
                sendLine("ERROR:SEND");
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
