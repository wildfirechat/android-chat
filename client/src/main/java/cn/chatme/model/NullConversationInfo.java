/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.model;

public class NullConversationInfo extends ConversationInfo {
    public NullConversationInfo(Conversation conversation) {
        super();
        this.conversation = conversation;
        this.unreadCount = new UnreadCount();
    }
}
