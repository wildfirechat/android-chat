package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by imndx on 2017/12/19.
 */

@ContentTag(type = MessageContentType.ContentType_General_Notification, flag = PersistFlag.Persist_And_Count)
public class TextNotificationContent extends NotificationMessageContent {
    @Override
    public String formatNotification() {
        return notification;
    }

    private String notification;

    public String getNotification() {
        return notification;
    }

    public void setNotification(String notification) {
        this.notification = notification;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = new MessagePayload();
        payload.content = notification;
         return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        notification = payload.content;
    }

    @Override
    public String digest() {
        return notification;
    }


    public TextNotificationContent(String notification) {
        this.notification = notification;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.notification);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    protected TextNotificationContent(Parcel in) {
        this.notification = in.readString();
        this.fromSelf = in.readByte() != 0;
    }

    public static final Creator<TextNotificationContent> CREATOR = new Creator<TextNotificationContent>() {
        @Override
        public TextNotificationContent createFromParcel(Parcel source) {
            return new TextNotificationContent(source);
        }

        @Override
        public TextNotificationContent[] newArray(int size) {
            return new TextNotificationContent[size];
        }
    };
}
