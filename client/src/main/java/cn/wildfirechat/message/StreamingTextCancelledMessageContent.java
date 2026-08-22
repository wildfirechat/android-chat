/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Streaming_Text_Cancelled;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 流式文本取消消息内容（20）
 * <p>
 * 当AI流式文本生成无产出或失败时由机器人发送，携带 streamId。
 * 客户端收到后按 streamId 找到对应的正在生成(14)/已生成(15)消息并从界面删除，
 * 取消消息自身不显示任何内容、不落库（Transparent）。
 * 消息结构：type=20，text 在 searchableContent，streamId 在 content。
 * </p>
 *
 * @author WildFireChat
 * @since 2023
 */
@ContentTag(type = ContentType_Streaming_Text_Cancelled, flag = PersistFlag.Transparent)
public class StreamingTextCancelledMessageContent extends MessageContent {
    /**
     * 取消提示文本
     */
    private String text;

    /**
     * 被取消的流式响应的唯一标识
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

    public StreamingTextCancelledMessageContent() {
    }

    protected StreamingTextCancelledMessageContent(Parcel in) {
        super(in);
        this.text = in.readString();
        this.streamId = in.readString();
    }

    public static final Creator<StreamingTextCancelledMessageContent> CREATOR = new Creator<StreamingTextCancelledMessageContent>() {
        @Override
        public StreamingTextCancelledMessageContent createFromParcel(Parcel source) {
            return new StreamingTextCancelledMessageContent(source);
        }

        @Override
        public StreamingTextCancelledMessageContent[] newArray(int size) {
            return new StreamingTextCancelledMessageContent[size];
        }
    };
}
