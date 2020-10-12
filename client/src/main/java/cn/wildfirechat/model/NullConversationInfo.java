/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

public class NullConversationInfo extends ConversationInfo {
    public NullConversationInfo(Conversation conversation) {
        super();
        this.conversation = conversation;
        this.unreadCount = new UnreadCount();
    }
}
