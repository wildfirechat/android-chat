package cn.wildfirechat.message.notification;

import android.os.Parcel;

public abstract class GroupNotificationMessageContent extends NotificationMessageContent {
    public String groupId;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.groupId);
    }

    public GroupNotificationMessageContent() {
    }

    protected GroupNotificationMessageContent(Parcel in) {
        super(in);
        this.groupId = in.readString();
    }

}

