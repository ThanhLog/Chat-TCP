package modules;

public class ChatRoom {
    private String messageRoom;
    private String username1;
    private String username2;

    public ChatRoom(String messageRoom, String username1, String username2) {
        this.messageRoom = messageRoom;
        this.username1 = username1;
        this.username2 = username2;
    }

    
    public String getMessageRoom() { return messageRoom; }
    public void setMessageRoom(String messageRoom) { this.messageRoom = messageRoom; }

    public String getUsername1() { return username1; }
    public void setUsername1(String username1) { this.username1 = username1; }

    public String getUsername2() { return username2; }
    public void setUsername2(String username2) { this.username2 = username2; }
}
