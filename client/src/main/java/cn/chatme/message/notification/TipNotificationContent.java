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

@ContentTag(type = MessageContentType.ContentType_Tip_Notification, flag = PersistFlag.Persist)
public class TipNotificationContent extends NotificationMessageContent {
    public String tip;

    public TipNotificationContent() {
    }


    @Override
    public String formatNotification(Message message) {
        return tip;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = tip;

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        tip = payload.content;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.tip);
    }

    protected TipNotificationContent(Parcel in) {
        super(in);
        this.tip = in.readString();
    }

    public static final Parcelable.Creator<TipNotificationContent> CREATOR = new Parcelable.Creator<TipNotificationContent>() {
        @Override
        public TipNotificationContent createFromParcel(Parcel source) {
            return new TipNotificationContent(source);
        }

        @Override
        public TipNotificationContent[] newArray(int size) {
            return new TipNotificationContent[size];
        }
    };
}
