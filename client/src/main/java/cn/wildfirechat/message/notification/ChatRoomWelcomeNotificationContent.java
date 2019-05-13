package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessagePayload;

public class ChatRoomWelcomeNotificationContent extends NotificationMessageContent {

    private String welcome;

    @Override
    public String formatNotification(Message message) {
        return welcome;
    }

    @Override
    public MessagePayload encode() {
        return null;
    }

    @Override
    public void decode(MessagePayload payload) {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.welcome);
    }

    public ChatRoomWelcomeNotificationContent() {
    }

    protected ChatRoomWelcomeNotificationContent(Parcel in) {
        this.welcome = in.readString();
    }

    public static final Creator<ChatRoomWelcomeNotificationContent> CREATOR = new Creator<ChatRoomWelcomeNotificationContent>() {
        @Override
        public ChatRoomWelcomeNotificationContent createFromParcel(Parcel source) {
            return new ChatRoomWelcomeNotificationContent(source);
        }

        @Override
        public ChatRoomWelcomeNotificationContent[] newArray(int size) {
            return new ChatRoomWelcomeNotificationContent[size];
        }
    };
}
