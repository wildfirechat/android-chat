package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ReadEntry implements Parcelable {
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

    public String userId;
    public Conversation conversation;
    public long readDt;
}
