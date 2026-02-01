/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 空会话信息类
 * <p>
 * 当会话信息不存在时返回的空对象实现。
 * 使用Null Object模式，避免上层代码不断的做空值检查。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class NullConversationInfo extends ConversationInfo {
    public NullConversationInfo(Conversation conversation) {
        super();
        this.conversation = conversation;
        this.unreadCount = new UnreadCount();
    }
}
