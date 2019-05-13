package cn.wildfirechat.message;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Arrays;

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
     * 消息在会话中定向发送给指定用户
     */
    public String[] toUsers;
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
