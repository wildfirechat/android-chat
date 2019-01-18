package cn.wildfirechat.message;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessageStatus;
import cn.wildfirechat.model.Conversation;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

public class Message implements Parcelable {


    public long messageId;
    public Conversation conversation;
    public String sender;
    /**
     * 消息在会话中定向发送给该用户的
     */
    public String to;
    public MessageContent content;
    public MessageDirection direction;
    public MessageStatus status;
    public long messageUid;
    public long serverTime;

    public Message() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.messageId);
        dest.writeParcelable(this.conversation, flags);
        dest.writeString(this.sender);
        dest.writeString(this.to);
        dest.writeParcelable(content, flags);
        dest.writeInt(this.direction == null ? -1 : this.direction.ordinal());
        dest.writeInt(this.status == null ? -1 : this.status.ordinal());
        dest.writeLong(this.messageUid);
        dest.writeLong(this.serverTime);
    }

    protected Message(Parcel in) {
        this.messageId = in.readLong();
        this.conversation = in.readParcelable(Conversation.class.getClassLoader());
        this.sender = in.readString();
        this.to = in.readString();
        this.content = in.readParcelable(MessageContent.class.getClassLoader());
        int tmpDirection = in.readInt();
        this.direction = tmpDirection == -1 ? null : MessageDirection.values()[tmpDirection];
        int tmpStatus = in.readInt();
        this.status = tmpStatus == -1 ? null : MessageStatus.values()[tmpStatus];
        this.messageUid = in.readLong();
        this.serverTime = in.readLong();
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
        return messageId == message.messageId &&
                messageUid == message.messageUid &&
                serverTime == message.serverTime &&
                Objects.equals(conversation, message.conversation) &&
                Objects.equals(sender, message.sender) &&
                Objects.equals(to, message.to) &&
                Objects.equals(content, message.content) &&
                direction == message.direction &&
                status == message.status;
    }

    @Override
    public int hashCode() {

        return Objects.hash(messageId, conversation, sender, to, content, direction, status, messageUid, serverTime);
    }
}
