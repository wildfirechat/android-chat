/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_Call_Join_Call_Request, flag = PersistFlag.Transparent)
public class JoinCallRequestMessageContent extends MessageContent {
    private String callId;

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public JoinCallRequestMessageContent() {
    }

    public JoinCallRequestMessageContent(String callId) {
        this.callId = callId;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = this.callId;
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        this.callId = payload.content;
    }

    @Override
    public String digest(Message message) {
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.callId);
    }

    public void readFromParcel(Parcel source) {
        this.callId = source.readString();
    }

    protected JoinCallRequestMessageContent(Parcel in) {
        super(in);
        this.callId = in.readString();
    }

    public static final Creator<JoinCallRequestMessageContent> CREATOR = new Creator<JoinCallRequestMessageContent>() {
        @Override
        public JoinCallRequestMessageContent createFromParcel(Parcel source) {
            return new JoinCallRequestMessageContent(source);
        }

        @Override
        public JoinCallRequestMessageContent[] newArray(int size) {
            return new JoinCallRequestMessageContent[size];
        }
    };
}
