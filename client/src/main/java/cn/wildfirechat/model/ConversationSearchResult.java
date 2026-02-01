/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import cn.wildfirechat.message.Message;

/**
 * 会话搜索结果类
 * <p>
 * 用于表示会话搜索的结果信息。
 * 包含会话对象、匹配消息、时间戳和匹配数量。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class ConversationSearchResult implements Parcelable {
    /**
     * 会话对象
     */
    public Conversation conversation;

    /**
     * 匹配的消息（仅当marchedCount == 1时加载）
     */
    public Message marchedMessage;

    /**
     * 时间戳
     */
    public long timestamp;

    /**
     * 匹配数量
     */
    public int marchedCount;

    public ConversationSearchResult() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.conversation, flags);
        dest.writeParcelable(this.marchedMessage, flags);
        dest.writeLong(this.timestamp);
        dest.writeInt(this.marchedCount);
    }

    protected ConversationSearchResult(Parcel in) {
        this.conversation = in.readParcelable(Conversation.class.getClassLoader());
        this.marchedMessage = in.readParcelable(Message.class.getClassLoader());
        this.timestamp = in.readLong();
        this.marchedCount = in.readInt();
    }

    public static final Creator<ConversationSearchResult> CREATOR = new Creator<ConversationSearchResult>() {
        @Override
        public ConversationSearchResult createFromParcel(Parcel source) {
            return new ConversationSearchResult(source);
        }

        @Override
        public ConversationSearchResult[] newArray(int size) {
            return new ConversationSearchResult[size];
        }
    };
}
