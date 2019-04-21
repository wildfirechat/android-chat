package cn.wildfirechat.model;

public class NullChatRoomInfo extends ChatRoomInfo {
    public NullChatRoomInfo(String chatRoomId) {
        this.chatRoomId = chatRoomId;
        this.title = "<" + chatRoomId + ">";
    }
}