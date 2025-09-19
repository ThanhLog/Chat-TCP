package mongodb;

import com.mongodb.client.*;

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
	
	public static void close() {
		client.close();
	}
}

