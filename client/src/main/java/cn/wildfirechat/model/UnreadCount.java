package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

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
}
