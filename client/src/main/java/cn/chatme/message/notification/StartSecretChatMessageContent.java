/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message.notification;

import android.os.Parcel;
import android.os.Parcelable;

import cn.chatme.message.Message;
import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;
import cn.chatme.remote.ChatManager;

@ContentTag(type = MessageContentType.ContentType_Start_Secret_Chat, flag = PersistFlag.Persist)
public class StartSecretChatMessageContent extends NotificationMessageContent {
    @Override
    public void decode(MessagePayload payload) {

    }

    @Override
    public String formatNotification(Message message) {
        ChatManager.SecretChatState state = ChatManager.Instance().getSecretChatInfo(message.conversation.target).getState();
        if (state == ChatManager.SecretChatState.Starting) {
            return "等待对方响应";
        } else if (state == ChatManager.SecretChatState.Accepting) {
            return "密聊会话建立中";
        } else if (state == ChatManager.SecretChatState.Established) {
            return "密聊会话已建立";
        } else if (state == ChatManager.SecretChatState.Canceled) {
            return "密聊会话已取消";
        } else {
            return "密聊会话不可用";
        }
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

    public StartSecretChatMessageContent() {
    }

    protected StartSecretChatMessageContent(Parcel in) {
        super(in);
    }

    public static final Parcelable.Creator<StartSecretChatMessageContent> CREATOR = new Parcelable.Creator<StartSecretChatMessageContent>() {
        @Override
        public StartSecretChatMessageContent createFromParcel(Parcel source) {
            return new StartSecretChatMessageContent(source);
        }

        @Override
        public StartSecretChatMessageContent[] newArray(int size) {
            return new StartSecretChatMessageContent[size];
        }
    };
}
