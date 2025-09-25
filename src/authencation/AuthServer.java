package authencation;

import mongodb.MongoDBUtil;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class AuthServer {
    public static final int PORT = 12345;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        MongoDBUtil.connect();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close(); } catch (Exception ignored){}
            MongoDBUtil.close();
        }));

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("AuthServer listening on " + PORT);
            while (true) {
                Socket s = serverSocket.accept();
                new Thread(() -> handleClient(s)).start();
            }
        } catch (IOException e) {
            System.out.println("AuthServer stopped: " + e.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("REGISTER:")) {
                    // REGISTER:username:password
                    String[] p = line.split(":", 3);
                    if (p.length < 3) { out.println("ERROR:BAD_REQUEST"); continue; }
                    boolean ok = MongoDBUtil.registerUser(p[1], p[2]);
                    out.println(ok ? "REGISTER_OK" : "REGISTER_FAIL");
                } else if (line.startsWith("LOGIN:")) {
                    String[] p = line.split(":", 3);
                    if (p.length < 3) { out.println("ERROR:BAD_REQUEST"); continue; }
                    boolean ok = MongoDBUtil.loginUser(p[1], p[2]);
                    out.println(ok ? "LOGIN_OK" : "LOGIN_FAIL");
                } else {
                    out.println("ERROR:UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            // client disconnected
        }
    }
}
