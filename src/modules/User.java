package modules;

import java.util.*;

public class User {
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}

	
	public String getPasssword() {
		return passsword;
	}
	public void setPasssword(String passsword) {
		this.passsword = passsword;
	}

	
	public List<UserChatInfo> getListUser() {
	    if (listUser == null) {
	        listUser = new ArrayList<>();
	    }
	    return listUser;
	}

	public void setListUser(List<UserChatInfo> listUser) {
		this.listUser = listUser;
	}
	
	

	private String username;
	private String passsword;
	private List<UserChatInfo> listUser;
	
	
	
	
	public static class UserChatInfo{
		public String getUsername() {
			return username;
		}
		public void setUsername(String username) {
			this.username = username;
		}
		public String getLastMessage() {
			return lastMessage;
		}
		public void setLastMessage(String lastMessage) {
			this.lastMessage = lastMessage;
		}
		
        public UserChatInfo(String username, String lastMessage) {
            this.username = username;
            this.lastMessage = lastMessage;
        }
        
		private String username;
		private String lastMessage;
		
	}
}

