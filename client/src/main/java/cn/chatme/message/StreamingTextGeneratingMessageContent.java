/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message;

import android.os.Parcel;

import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_Streaming_Text_Generating, flag = PersistFlag.Transparent)
public class StreamingTextGeneratingMessageContent extends MessageContent {

    private String text;
    private String streamId;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = this.text;
        payload.content = this.streamId;
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        this.extra = payload.extra;
        this.text = payload.searchableContent;
        this.streamId = payload.content;
    }

    @Override
    public String digest(Message message) {
        return this.text;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.text);
        dest.writeString(this.streamId);
    }

    public void readFromParcel(Parcel source) {
        this.text = source.readString();
        this.streamId = source.readString();
    }

    public StreamingTextGeneratingMessageContent() {
    }

    protected StreamingTextGeneratingMessageContent(Parcel in) {
        super(in);
        this.text = in.readString();
        this.streamId = in.readString();
    }

    public static final Creator<StreamingTextGeneratingMessageContent> CREATOR = new Creator<StreamingTextGeneratingMessageContent>() {
        @Override
        public StreamingTextGeneratingMessageContent createFromParcel(Parcel source) {
            return new StreamingTextGeneratingMessageContent(source);
        }

        @Override
        public StreamingTextGeneratingMessageContent[] newArray(int size) {
            return new StreamingTextGeneratingMessageContent[size];
        }
    };
}
