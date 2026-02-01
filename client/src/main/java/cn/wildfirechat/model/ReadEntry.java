/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 消息已读记录类
 * <p>
 * 用于记录会话消息的已读状态。
 * 包含用户ID、会话和已读时间信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class ReadEntry implements Parcelable {
    /**
     * 用户ID
     */
    public String userId;

    /**
     * 会话对象
     */
    public Conversation conversation;

    /**
     * 已读时间
     */
    public long readDt;

    public ReadEntry() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeInt(conversation.type.getValue());
        dest.writeString(conversation.target);
        dest.writeInt(conversation.line);
        dest.writeLong(readDt);
    }

    public ReadEntry(Parcel source) {
        userId = source.readString();
        int type = source.readInt();
        String target = source.readString();
        int line = source.readInt();
        conversation = new Conversation(Conversation.ConversationType.type(type), target, line);
        readDt = source.readLong();
    }

    public static final Creator<ReadEntry> CREATOR = new Creator<ReadEntry>() {
        @Override
        public ReadEntry createFromParcel(Parcel source) {
            return new ReadEntry(source);
        }

        @Override
        public ReadEntry[] newArray(int size) {
            return new ReadEntry[size];
        }
    };
}
