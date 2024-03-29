/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.model;

public class NullChatRoomInfo extends ChatRoomInfo {
    public NullChatRoomInfo(String chatRoomId) {
        this.chatRoomId = chatRoomId;
        //this.title = "<" + chatRoomId + ">";
        this.title = "聊天室";
    }
}