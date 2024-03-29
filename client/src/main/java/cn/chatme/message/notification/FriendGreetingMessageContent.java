/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message.notification;

import android.os.Parcel;
import android.os.Parcelable;

import cn.chatme.message.Message;
import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = MessageContentType.ContentType_Friend_Greeting, flag = PersistFlag.Persist)
public class FriendGreetingMessageContent extends NotificationMessageContent {
    public FriendGreetingMessageContent() {
    }


    @Override
    public String formatNotification(Message message) {
        return "以上是打招呼的内容";
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        return payload;
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
    }

    protected FriendGreetingMessageContent(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<FriendGreetingMessageContent> CREATOR = new Parcelable.Creator<FriendGreetingMessageContent>() {
        @Override
        public FriendGreetingMessageContent createFromParcel(Parcel source) {
            return new FriendGreetingMessageContent(source);
        }

        @Override
        public FriendGreetingMessageContent[] newArray(int size) {
            return new FriendGreetingMessageContent[size];
        }
    };
}
