/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_Enter_Channel_Chat, flag = PersistFlag.Transparent)
public class EnterChannelChatMessageContent extends MessageContent {
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

    public EnterChannelChatMessageContent() {
    }

    protected EnterChannelChatMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<EnterChannelChatMessageContent> CREATOR = new Creator<EnterChannelChatMessageContent>() {
        @Override
        public EnterChannelChatMessageContent createFromParcel(Parcel source) {
            return new EnterChannelChatMessageContent(source);
        }

        @Override
        public EnterChannelChatMessageContent[] newArray(int size) {
            return new EnterChannelChatMessageContent[size];
        }
    };
}
