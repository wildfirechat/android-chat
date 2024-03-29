/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message;

import android.os.Parcel;

import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Unknown, flag = PersistFlag.No_Persist)
public class RawMessageContent extends MessageContent {
    public MessagePayload payload;


    public RawMessageContent() {
    }

    @Override
    public MessagePayload encode() {
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        this.payload = payload;
    }

    @Override
    public String digest(Message message) {
        return "";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        payload.writeToParcel(dest, flags);
    }

    protected RawMessageContent(Parcel in) {
        payload = new MessagePayload(in);
    }

    public static final Creator<RawMessageContent> CREATOR = new Creator<RawMessageContent>() {
        @Override
        public RawMessageContent createFromParcel(Parcel source) {
            return new RawMessageContent(source);
        }

        @Override
        public RawMessageContent[] newArray(int size) {
            return new RawMessageContent[size];
        }
    };
}
