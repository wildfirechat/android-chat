/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Live_Streaming_End;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = ContentType_Live_Streaming_End, flag = PersistFlag.Persist_And_Count)
public class LiveStreamingEndMessageContent extends MessageContent {
    /**
     * 直播唯一标识
     */
    private String callId;

    public LiveStreamingEndMessageContent() {
    }

    public LiveStreamingEndMessageContent(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = callId;

        payload.pushContent = "直播 结束";
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        callId = payload.content;
        pushContent = payload.pushContent;

    }

    @Override
    public String digest(Message message) {
        return "[直播]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(callId);
    }

    protected LiveStreamingEndMessageContent(Parcel in) {
        super(in);
        callId = in.readString();
    }

    public static final Creator<LiveStreamingEndMessageContent> CREATOR = new Creator<LiveStreamingEndMessageContent>() {
        @Override
        public LiveStreamingEndMessageContent createFromParcel(Parcel source) {
            return new LiveStreamingEndMessageContent(source);
        }

        @Override
        public LiveStreamingEndMessageContent[] newArray(int size) {
            return new LiveStreamingEndMessageContent[size];
        }
    };
}
