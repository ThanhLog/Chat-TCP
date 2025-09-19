package mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatDAO {
    private static final String COLLECTION_NAME = "messages";
    private static final String LOG_FILE = "chat_log.txt";

    private static MongoCollection<Document> collection = 
            MongoDBUtil.getDatabase().getCollection(COLLECTION_NAME);

    // Lưu tin nhắn vào MongoDB + ghi ra file
    public static void insertMessage(String username, String message) {
        Document doc = new Document("username", username)
                .append("message", message)
                .append("timestamp", System.currentTimeMillis());
        collection.insertOne(doc);
        writeToFile(username + ": " + message);
        System.out.println("✅ Đã lưu tin nhắn vào MongoDB + file");
    }

    // Lấy tất cả tin nhắn từ MongoDB
    public static List<Document> getAllMessages() {
        List<Document> messages = new ArrayList<>();
        try (MongoCursor<Document> cursor = collection.find().iterator()) {
            while (cursor.hasNext()) {
                messages.add(cursor.next());
            }
        }
        return messages;
    }

    // Cập nhật tin nhắn theo ID
    public static void updateMessage(String id, String newMessage) {
        collection.updateOne(
                Filters.eq("_id", new org.bson.types.ObjectId(id)),
                Updates.set("message", newMessage)
        );
        System.out.println("✏️ Đã cập nhật tin nhắn");
    }

    // Xóa tin nhắn theo ID
    public static void deleteMessage(String id) {
        collection.deleteOne(Filters.eq("_id", new org.bson.types.ObjectId(id)));
        System.out.println("🗑️ Đã xóa tin nhắn");
    }

    // Ghi log ra file
    private static void writeToFile(String content) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(content + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
