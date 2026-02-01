/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 文件记录类
 * <p>
 * 用于记录会话中的文件传输记录。
 * 包含用户ID、会话、消息UID、文件名、URL、大小、下载次数和时间戳。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class FileRecord implements Parcelable {
    /**
     * 用户ID
     */
    public String userId;

    /**
     * 会话对象
     */
    public Conversation conversation;

    /**
     * 消息UID
     */
    public long messageUid;

    /**
     * 文件名
     */
    public String name;

    /**
     * 文件URL
     */
    public String url;

    /**
     * 文件大小
     */
    public int size;

    /**
     * 下载次数
     */
    public int downloadCount;

    /**
     * 时间戳
     */
    public long timestamp;

    public FileRecord() {
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
        dest.writeLong(messageUid);
        dest.writeString(name);
        dest.writeString(url);
        dest.writeInt(size);
        dest.writeInt(downloadCount);
        dest.writeLong(timestamp);
    }

    public FileRecord(Parcel source) {
        userId = source.readString();
        int type = source.readInt();
        String target = source.readString();
        int line = source.readInt();
        conversation = new Conversation(Conversation.ConversationType.type(type), target, line);
        messageUid = source.readLong();
        name = source.readString();
        url = source.readString();
        size = source.readInt();
        downloadCount = source.readInt();
        timestamp = source.readLong();
    }

    public static final Creator<FileRecord> CREATOR = new Creator<FileRecord>() {
        @Override
        public FileRecord createFromParcel(Parcel source) {
            return new FileRecord(source);
        }

        @Override
        public FileRecord[] newArray(int size) {
            return new FileRecord[size];
        }
    };

}
