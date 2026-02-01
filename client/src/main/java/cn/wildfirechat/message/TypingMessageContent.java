/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Typing;

/**
 * 正在输入消息内容
 * <p>
 * 用于实时显示对方正在输入的状态消息。
 * 支持文本、语音、视频、位置、文件等多种输入类型的提示。
 * 此消息不会被持久化存储，仅在会话期间有效。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Typing, flag = PersistFlag.Transparent)
public class TypingMessageContent extends MessageContent {
    /**
     * 文本输入类型
     */
    public static final int TYPING_TEXT = 0;

    /**
     * 语音输入类型
     */
    public static final int TYPING_VOICE = 1;

    /**
     * 视频/相机输入类型
     */
    public static final int TYPING_CAMERA = 2;

    /**
     * 位置输入类型
     */
    public static final int TYPING_LOCATION = 3;

    /**
     * 文件输入类型
     */
    public static final int TYPING_FILE = 4;

    /**
     * 输入类型
     */
    private int typingType;

    public TypingMessageContent() {
    }

    public TypingMessageContent(int typingType) {
        this.typingType = typingType;
    }

    public int getTypingType() {
        return typingType;
    }

    public void setTypingType(int typingType) {
        this.typingType = typingType;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = typingType + "";
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        typingType = Integer.parseInt(payload.content);
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
        super.writeToParcel(dest, flags);
        dest.writeInt(this.typingType);
    }

    protected TypingMessageContent(Parcel in) {
        super(in);
        this.typingType = in.readInt();
    }

    public static final Creator<TypingMessageContent> CREATOR = new Creator<TypingMessageContent>() {
        @Override
        public TypingMessageContent createFromParcel(Parcel source) {
            return new TypingMessageContent(source);
        }

        @Override
        public TypingMessageContent[] newArray(int size) {
            return new TypingMessageContent[size];
        }
    };
}
