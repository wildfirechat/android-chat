/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message.notification;

import android.os.Parcel;
import android.os.Parcelable;

import cn.chatme.message.Message;
import cn.chatme.message.core.MessagePayload;

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
        super.writeToParcel(dest, flags);
        dest.writeString(this.welcome);
    }

    public ChatRoomWelcomeNotificationContent() {
    }

    protected ChatRoomWelcomeNotificationContent(Parcel in) {
        super(in);
        this.welcome = in.readString();
    }

    public static final Parcelable.Creator<ChatRoomWelcomeNotificationContent> CREATOR = new Parcelable.Creator<ChatRoomWelcomeNotificationContent>() {
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
