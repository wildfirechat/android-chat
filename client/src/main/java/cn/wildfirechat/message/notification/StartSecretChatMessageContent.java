/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Start_Secret_Chat;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

@ContentTag(type = ContentType_Start_Secret_Chat, flag = PersistFlag.Persist)
public class StartSecretChatMessageContent extends NotificationMessageContent {
    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
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

    public static final Creator<StartSecretChatMessageContent> CREATOR = new Creator<StartSecretChatMessageContent>() {
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
