/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Streaming_Text_Generating;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 流式文本生成中消息内容
 * <p>
 * 用于AI流式文本生成过程中，实时发送正在生成的文本片段。
 * 此消息不会被持久化，仅用于实时显示AI正在生成的内容。
 * </p>
 *
 * @author WildFireChat
 * @since 2023
 */
@ContentTag(type = ContentType_Streaming_Text_Generating, flag = PersistFlag.Transparent)
public class StreamingTextGeneratingMessageContent extends MessageContent {

    /**
     * 生成的文本内容
     */
    private String text;

    /**
     * 流式响应的唯一标识
     */
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
