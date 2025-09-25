package mongodb;

import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import org.bson.Document;

import java.time.Instant;
import java.util.*;

public class MongoDBUtil {
    private static final String URI = "mongodb://localhost:27017";
    private static final String DB_NAME = "chatDB";

    private static MongoClient client = null;
    private static MongoDatabase db = null;

    public static synchronized void connect() {
        if (client == null) {
            client = MongoClients.create(URI);
            db = client.getDatabase(DB_NAME);
            // ensure collections exist
            var names = new HashSet<String>();
            db.listCollectionNames().into(new ArrayList<>()).forEach(names::add);
            if (!names.contains("users")) db.createCollection("users");
            if (!names.contains("chat_rooms")) db.createCollection("chat_rooms");
            System.out.println("MongoDB connected -> " + DB_NAME);
        }
    }

    public static synchronized MongoDatabase getDatabase() {
        if (db == null) connect();
        return db;
    }

    public static synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
            db = null;
            System.out.println("MongoDB closed");
        }
    }

    // --- user methods ---
    public static boolean registerUser(String username, String password) {
        var users = getDatabase().getCollection("users");
        Document existing = users.find(Filters.eq("username", username)).first();
        if (existing != null) return false;
        Document doc = new Document("username", username)
                .append("password", password);
        users.insertOne(doc);
        return true;
    }

    public static boolean loginUser(String username, String password) {
        var users = getDatabase().getCollection("users");
        Document doc = users.find(Filters.and(Filters.eq("username", username), Filters.eq("password", password))).first();
        return doc != null;
    }

    public static List<String> getAllUsers() {
        var users = getDatabase().getCollection("users");
        List<String> res = new ArrayList<>();
        for (Document d : users.find()) res.add(d.getString("username"));
        return res;
    }

    public static List<String> searchUsers(String kw) {
        var users = getDatabase().getCollection("users");
        List<String> res = new ArrayList<>();
        var iter = users.find(org.bson.conversions.Bson.class.cast(com.mongodb.client.model.Filters.regex("username", ".*" + kw + ".*", "i"))).iterator();
        try (iter) { while (iter.hasNext()) res.add(iter.next().getString("username")); }
        return res;
    }

    // --- room & messages ---
    // deterministic room id for pair (alphabetical)
    public static String createRoomIfNotExists(String a, String b) {
        List<String> u = Arrays.asList(a, b);
        Collections.sort(u);
        String roomId = u.get(0) + "_" + u.get(1);
        var coll = getDatabase().getCollection("chat_rooms");
        Document existing = coll.find(Filters.eq("_id", roomId)).first();
        if (existing == null) {
            Document doc = new Document("_id", roomId)
                    .append("users", u)
                    .append("messages", new ArrayList<Document>());
            coll.insertOne(doc);
        }
        return roomId;
    }

    public static void saveMessage(String roomId, String sender, String base64Content, String send) {
        var coll = getDatabase().getCollection("chat_rooms");
        String now = Instant.now().toString();
        Document msg = new Document("username", sender)
                .append("message", base64Content)
                .append("createAt", now)
        		.append("send", send);
        Document room = coll.find(Filters.eq("_id", roomId)).first();
        if (room == null) {
            // create minimal room if missing
            Document newRoom = new Document("_id", roomId)
                    .append("users", Arrays.asList("unknownA", "unknownB"))
                    .append("messages", Arrays.asList(msg));
            coll.insertOne(newRoom);
        } else {
            coll.updateOne(Filters.eq("_id", roomId), Updates.push("messages", msg));
        }
    }

    public static List<Document> getMessages(String roomId) {
        var coll = getDatabase().getCollection("chat_rooms");
        Document room = coll.find(Filters.eq("_id", roomId)).first();
        if (room == null) return Collections.emptyList();
        List<Document> msgs = room.getList("messages", Document.class);
        if (msgs == null) return Collections.emptyList();
        return msgs;
    }
}
