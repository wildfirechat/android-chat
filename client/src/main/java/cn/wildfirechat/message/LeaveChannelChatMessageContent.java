/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 离开频道聊天消息内容
 * <p>
 * 当用户离开频道聊天时发送的透明消息。
 * 此消息不会被持久化，仅用于实时状态同步。
 * </p>
 *
 * @author WildFireChat
 * @since 2022
 */
@ContentTag(type = MessageContentType.ContentType_Leave_Channel_Chat, flag = PersistFlag.Transparent)
public class LeaveChannelChatMessageContent extends MessageContent {
    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
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
    }

    public void readFromParcel(Parcel source) {
    }

    public LeaveChannelChatMessageContent() {
    }

    protected LeaveChannelChatMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<LeaveChannelChatMessageContent> CREATOR = new Creator<LeaveChannelChatMessageContent>() {
        @Override
        public LeaveChannelChatMessageContent createFromParcel(Parcel source) {
            return new LeaveChannelChatMessageContent(source);
        }

        @Override
        public LeaveChannelChatMessageContent[] newArray(int size) {
            return new LeaveChannelChatMessageContent[size];
        }
    };
}
