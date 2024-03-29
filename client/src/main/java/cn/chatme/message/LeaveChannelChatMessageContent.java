/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message;

import android.os.Parcel;

import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_Leave_Channel_Chat, flag = PersistFlag.Transparent)
public class LeaveChannelChatMessageContent extends MessageContent {
    @Override
    public void decode(MessagePayload payload) {

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
