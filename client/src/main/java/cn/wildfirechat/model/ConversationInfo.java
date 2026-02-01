/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import cn.wildfirechat.message.Message;

/**
 * 会话信息类
 * <p>
 * 表示会话列表中的一个会话项，包含会话、最新消息、未读数等信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class ConversationInfo implements Parcelable {
    /**
     * 会话对象
     */
    public Conversation conversation;

    /**
     * 最新消息
     */
    public Message lastMessage;

    /**
     * 时间戳
     */
    public long timestamp;

    /**
     * 草稿内容
     */
    public String draft;

    /**
     * 未读消息计数
     */
    public UnreadCount unreadCount;

    /**
     * 置顶标志：0 不置顶；1 置顶
     */
    public int top;

    /**
     * 是否免打扰
     */
    public boolean isSilent;

    /**
     * 默认构造函数
     */
    public ConversationInfo() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.conversation, flags);
        dest.writeParcelable(this.lastMessage, flags);
        dest.writeLong(this.timestamp);
        dest.writeString(this.draft);
        dest.writeParcelable(this.unreadCount, flags);
        dest.writeInt(this.top);
        dest.writeByte(this.isSilent ? (byte) 1 : (byte) 0);
    }

    protected ConversationInfo(Parcel in) {
        this.conversation = in.readParcelable(Conversation.class.getClassLoader());
        this.lastMessage = in.readParcelable(Message.class.getClassLoader());
        this.timestamp = in.readLong();
        this.draft = in.readString();
        this.unreadCount = in.readParcelable(UnreadCount.class.getClassLoader());
        this.top = in.readInt();
        this.isSilent = in.readByte() != 0;
    }

    public static final Creator<ConversationInfo> CREATOR = new Creator<ConversationInfo>() {
        @Override
        public ConversationInfo createFromParcel(Parcel source) {
            return new ConversationInfo(source);
        }

        @Override
        public ConversationInfo[] newArray(int size) {
            return new ConversationInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConversationInfo that = (ConversationInfo) o;
        return timestamp == that.timestamp &&
                top == that.top &&
                isSilent == that.isSilent &&
                Conversation.equals(conversation, that.conversation) &&
                Conversation.equals(lastMessage, that.lastMessage) &&
                Conversation.equals(draft, that.draft) &&
                Conversation.equals(unreadCount, that.unreadCount);
    }

    @Override
    public int hashCode() {
        return Conversation.hashCode(conversation, lastMessage, timestamp, draft, unreadCount, top, isSilent);
    }

}
