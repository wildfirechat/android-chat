/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;

/**
 * 消息类
 * <p>
 * 表示IM中的一条消息，包含消息的基本信息和内容。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class Message implements Parcelable {


    /**
     * 消息ID，本地数据库自增ID
     */
    public long messageId;

    /**
     * 消息所属会话
     */
    public Conversation conversation;

    /**
     * 消息发送者ID
     */
    public String sender;

    /**
     * 消息在会话中定向发送给指定用户
     */
    public String[] toUsers;

    /**
     * 消息内容
     */
    public MessageContent content;

    /**
     * 消息方向（发送/接收）
     */
    public MessageDirection direction;

    /**
     * 消息状态
     */
    public MessageStatus status;

    /**
     * 消息UID，服务器生成，全局唯一
     */
    public long messageUid;

    /**
     * 服务器时间戳
     */
    public long serverTime;

    /**
     * 本地扩展字段
     */
    public String localExtra;

    /**
     * 默认构造函数
     */
    public Message() {
    }

    /**
     * 拷贝构造函数
     *
     * @param msg 源消息对象
     */
    public Message(Message msg){
        this.messageId = msg.messageId;
        this.conversation = msg.conversation;
        this.sender = msg.sender;
        this.toUsers = msg.toUsers;
        this.content = msg.content;
        this.direction = msg.direction;
        this.status = msg.status;
        this.messageUid = msg.messageUid;
        this.serverTime = msg.serverTime;
        this.localExtra = msg.localExtra;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * 获取消息摘要文本
     *
     * @return 消息摘要
     */
    public String digest() {
        return content.digest(this);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.messageId);
        dest.writeParcelable(this.conversation, flags);
        dest.writeString(this.sender);
        dest.writeStringArray(this.toUsers);
        dest.writeParcelable(this.content, flags);
        dest.writeInt(this.direction == null ? -1 : this.direction.ordinal());
        dest.writeInt(this.status == null ? -1 : this.status.ordinal());
        dest.writeLong(this.messageUid);
        dest.writeLong(this.serverTime);
        dest.writeString(this.localExtra);
    }

    protected Message(Parcel in) {
        this.messageId = in.readLong();
        this.conversation = in.readParcelable(Conversation.class.getClassLoader());
        this.sender = in.readString();
        this.toUsers = in.createStringArray();
        this.content = in.readParcelable(MessageContent.class.getClassLoader());
        int tmpDirection = in.readInt();
        this.direction = tmpDirection == -1 ? null : MessageDirection.values()[tmpDirection];
        int tmpStatus = in.readInt();
        this.status = tmpStatus == -1 ? null : MessageStatus.values()[tmpStatus];
        this.messageUid = in.readLong();
        this.serverTime = in.readLong();
        this.localExtra = in.readString();
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if (messageId != message.messageId) return false;
        if (messageUid != message.messageUid) return false;
        if (serverTime != message.serverTime) return false;
        if (!conversation.equals(message.conversation)) return false;
        if (!sender.equals(message.sender)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(toUsers, message.toUsers)) return false;
        if (!content.equals(message.content)) return false;
        if (direction != message.direction) return false;
        return status == message.status;
    }

    @Override
    public int hashCode() {
        int result = (int) (messageId ^ (messageId >>> 32));
        result = 31 * result + conversation.hashCode();
        result = 31 * result + sender.hashCode();
        result = 31 * result + Arrays.hashCode(toUsers);
        result = 31 * result + content.hashCode();
        result = 31 * result + direction.hashCode();
        result = 31 * result + status.hashCode();
        result = 31 * result + (int) (messageUid ^ (messageUid >>> 32));
        result = 31 * result + (int) (serverTime ^ (serverTime >>> 32));
        return result;
    }
}
