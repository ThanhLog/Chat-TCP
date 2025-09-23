package mongodb;

import com.mongodb.client.*;
import com.mongodb.client.model.*;
import java.util.*;
import org.bson.Document;


public class MongoDBUtil {
    private static final String URI_STRING = "mongodb://localhost:27017";
    private static final String DATA_NAME = "chatDB";
    
    private static MongoClient client;
    private static MongoDatabase database;
    
    static {
        client = MongoClients.create(URI_STRING);
        database = client.getDatabase(DATA_NAME);
    }
    
    public static MongoDatabase getDatabase() {
        return database;
    }

    public static List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        MongoCollection<Document> collection = database.getCollection("users");
        for (Document doc : collection.find()) {
            users.add(doc.getString("username"));
        }
        return users;
    }
    
    public static List<String> getListUserByUsername(String username) {
        List<String> list = new ArrayList<>();
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            Document userDoc = collection.find(Filters.eq("username", username)).first();

            if (userDoc != null) {
                List<Document> userChats = userDoc.getList("listUser", Document.class);
                if (userChats != null) {
                    for (Document doc : userChats) {
                        list.add(doc.getString("username"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public static List<String> searchUsers(String keyword) {
        List<String> result = new ArrayList<>();
        try {
            MongoCollection<Document> collection = database.getCollection("users");
            FindIterable<Document> docs = collection.find(Filters.regex("username", ".*" + keyword + ".*", "i"));
            for (Document doc : docs) {
                result.add(doc.getString("username"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String createRoomIfNotExists(String userA, String userB) {
        MongoCollection<Document> collection = database.getCollection("chat_rooms");

        // Tạo key duy nhất cho 2 user, sắp xếp alphabet để không bị trùng
        List<String> users = Arrays.asList(userA, userB);
        Collections.sort(users);
        String roomId = users.get(0) + "_" + users.get(1);

        // Kiểm tra room đã tồn tại chưa
        Document existingRoom = collection.find(Filters.eq("_id", roomId)).first();
        if (existingRoom == null) {
            Document newRoom = new Document("_id", roomId)
                    .append("users", users)
                    .append("messages", new ArrayList<Document>());
            collection.insertOne(newRoom);
        }
        return roomId;
    }

    
    public static void close() {
        client.close();
    }
}
