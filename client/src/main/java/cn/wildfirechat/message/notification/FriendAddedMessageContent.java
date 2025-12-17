/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Friend_Added;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_Friend_Added, flag = PersistFlag.Persist)
public class FriendAddedMessageContent extends NotificationMessageContent {
    public FriendAddedMessageContent() {
    }


    @Override
    public String formatNotification(Message message) {
        String name = ChatManager.Instance().getUserDisplayName(message.sender);
        if (message.direction == MessageDirection.Receive) {
            return "我通过了你的朋友验证请求，现在我们可以开始聊天了";
        } else {
            return "你已添加了" + name + "，现在可以开始聊天了。";
        }
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    protected FriendAddedMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<FriendAddedMessageContent> CREATOR = new Creator<FriendAddedMessageContent>() {
        @Override
        public FriendAddedMessageContent createFromParcel(Parcel source) {
            return new FriendAddedMessageContent(source);
        }

        @Override
        public FriendAddedMessageContent[] newArray(int size) {
            return new FriendAddedMessageContent[size];
        }
    };
}
