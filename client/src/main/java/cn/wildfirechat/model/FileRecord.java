package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FileRecord implements Parcelable {
    public String userId;
    public Conversation conversation;
    public long messageUid;
    public String name;
    public String url;
    public int size;
    public int downloadCount;
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
