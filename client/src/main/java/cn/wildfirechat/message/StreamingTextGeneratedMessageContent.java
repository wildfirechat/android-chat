/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Streaming_Text_Generated;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = ContentType_Streaming_Text_Generated, flag = PersistFlag.Persist_And_Count)
public class StreamingTextGeneratedMessageContent extends MessageContent {
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
        super.decode(payload);
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

    public StreamingTextGeneratedMessageContent() {
    }

    protected StreamingTextGeneratedMessageContent(Parcel in) {
        super(in);
        this.text = in.readString();
        this.streamId = in.readString();
    }

    public static final Creator<StreamingTextGeneratedMessageContent> CREATOR = new Creator<StreamingTextGeneratedMessageContent>() {
        @Override
        public StreamingTextGeneratedMessageContent createFromParcel(Parcel source) {
            return new StreamingTextGeneratedMessageContent(source);
        }

        @Override
        public StreamingTextGeneratedMessageContent[] newArray(int size) {
            return new StreamingTextGeneratedMessageContent[size];
        }
    };
}
