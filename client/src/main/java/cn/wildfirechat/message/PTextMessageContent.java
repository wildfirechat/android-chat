/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_P_Text;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Text;

/**
 * 加密文本消息内容
 * <p>
 * 用于端到端加密的文本消息，继承自TextMessageContent。
 * 在秘密聊天或需要额外加密的场景中使用。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_P_Text, flag = PersistFlag.Persist)
public class PTextMessageContent extends TextMessageContent {
    /**
     * 消息文本内容
     */
    private String content;

    public PTextMessageContent() {
    }

    public PTextMessageContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = content;
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        content = payload.searchableContent;
    }

    @Override
    public String digest(Message message) {
        return content;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.content);
    }

    protected PTextMessageContent(Parcel in) {
        super(in);
        this.content = in.readString();
    }

    public static final Creator<PTextMessageContent> CREATOR = new Creator<PTextMessageContent>() {
        @Override
        public PTextMessageContent createFromParcel(Parcel source) {
            return new PTextMessageContent(source);
        }

        @Override
        public PTextMessageContent[] newArray(int size) {
            return new PTextMessageContent[size];
        }
    };
}
