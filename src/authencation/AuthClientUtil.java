package authencation;

import java.io.*;
import java.net.*;

public class AuthClientUtil {
    private String host;
    private int port;

    public AuthClientUtil(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String sendRequest(String command, String username, String password) {
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(command);
            out.println(username);
            out.println(password);

            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR";
        }
    }
}
