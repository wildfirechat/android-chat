/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 未读消息数统计类
 * <p>
 * 用于表示会话的未读消息数量统计信息。
 * 包含单聊未读数、群聊@提及数、群聊@All数等未读统计信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class UnreadCount implements Parcelable {
    public UnreadCount(ProtoUnreadCount protocolUnreadCount) {
        this.unread = protocolUnreadCount.getUnread();
        this.unreadMention = protocolUnreadCount.getUnreadMention();
        this.unreadMentionAll = protocolUnreadCount.getUnreadMentionAll();
    }

    public UnreadCount() {
    }

    /**
     * 单聊未读数
     */
    public int unread;
    /**
     * 群聊@数
     */
    public int unreadMention;
    /**
     * 群聊@All数
     */
    public int unreadMentionAll;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.unread);
        dest.writeInt(this.unreadMention);
        dest.writeInt(this.unreadMentionAll);
    }

    protected UnreadCount(Parcel in) {
        this.unread = in.readInt();
        this.unreadMention = in.readInt();
        this.unreadMentionAll = in.readInt();
    }

    public static final Creator<UnreadCount> CREATOR = new Creator<UnreadCount>() {
        @Override
        public UnreadCount createFromParcel(Parcel source) {
            return new UnreadCount(source);
        }

        @Override
        public UnreadCount[] newArray(int size) {
            return new UnreadCount[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        UnreadCount that = (UnreadCount) o;
        return unread == that.unread && unreadMention == that.unreadMention && unreadMentionAll == that.unreadMentionAll;
    }

    @Override
    public int hashCode() {
        int result = unread;
        result = 31 * result + unreadMention;
        result = 31 * result + unreadMentionAll;
        return result;
    }

}
