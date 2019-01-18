package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import cn.wildfirechat.message.Message;

/**
 * Created by heavyrain on 2017/12/13.
 */

public class ConversationSearchResult implements Parcelable {
    public Conversation conversation;
    //only marchedCount == 1, load the message
    public Message marchedMessage;
    public long timestamp;
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
