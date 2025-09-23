package authencation;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import org.bson.Document;
import com.mongodb.client.*;
import mongodb.MongoDBUtil;

public class AuthServer {
    private static final int PORT = 12345;
    private static MongoCollection<Document> userCollection;

    public static void main(String[] args) {
        // Lấy kết nối MongoDB từ MongoDBUtil
        MongoDatabase database = MongoDBUtil.getDatabase();
        userCollection = database.getCollection("users");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✅ AuthServer đang chạy tại cổng " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("🔗 Client kết nối: " + clientSocket.getInetAddress());
                new Thread(new ClientHandler(clientSocket, userCollection)).start();
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi chạy AuthServer: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;
    private MongoCollection<Document> userCollection;

    public ClientHandler(Socket socket, MongoCollection<Document> userCollection) {
        this.socket = socket;
        this.userCollection = userCollection;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String command = in.readLine();
            String username = in.readLine();
            String password = in.readLine();

            if (command == null || username == null || password == null) {
                out.println("ERROR: Thiếu dữ liệu");
                return;
            }

            switch (command.toUpperCase()) {
                case "REGISTER":
                	registerUser(username, password);
                    break;
                case "LOGIN":
                    handleLogin(username, password, out);
                    break;
                default:
                    out.println("ERROR: Lệnh không hợp lệ");
            }
        } catch (IOException e) {
            System.err.println("❌ Lỗi khi xử lý client: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("❌ Lỗi khi đóng socket: " + e.getMessage());
            }
        }
    }

    private boolean registerUser(String username, String password) {
        // Kiểm tra user đã tồn tại chưa
        Document existingUser = userCollection.find(new Document("username", username)).first();
        if (existingUser != null) return false; // Tài khoản đã tồn tại

        // Tạo document theo form User
        Document newUser = new Document()
                .append("username", username)
                .append("passsword", password) // giữ nguyên tên field như class User
                .append("listUser", new ArrayList<Document>()); // listUser ban đầu rỗng

        // Insert vào MongoDB
        userCollection.insertOne(newUser);
        return true;
    }


    private void handleLogin(String username, String password, PrintWriter out) {
        Document user = userCollection.find(
            new Document("username", username).append("password", password)
        ).first();

        if (user == null) {
            out.println("LOGIN_FAIL");
        } else {
            out.println("LOGIN_SUCCESS");
        }
    }
}
