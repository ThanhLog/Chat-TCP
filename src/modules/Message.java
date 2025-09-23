package modules;


import java.util.List;

public class Message {
    private String messageRoom;
    private List<MessageInfo> listMessages;

    public Message(String messageRoom, List<MessageInfo> listMessages) {
        this.messageRoom = messageRoom;
        this.listMessages = listMessages;
    }
    public String getMessageRoom() { return messageRoom; }
    public void setMessageRoom(String messageRoom) { this.messageRoom = messageRoom; }

    public List<MessageInfo> getListMessages() { return listMessages; }
    public void setListMessages(List<MessageInfo> listMessages) { this.listMessages = listMessages; }

    public static class MessageInfo {
        private String username;
        private String message;
        private String createAt; // ISO 8601 format

        public MessageInfo(String username, String message, String createAt) {
            this.username = username;
            this.message = message;
            this.createAt = createAt;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public String getCreateAt() { return createAt; }
        public void setCreateAt(String createAt) { this.createAt = createAt; }
    }
}
