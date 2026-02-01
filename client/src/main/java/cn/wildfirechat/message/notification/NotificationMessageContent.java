/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;

/**
 * 通知消息内容基类
 * <p>
 * 所有通知类型消息的基类，用于处理系统通知消息。
 * 通知消息包括：群组创建、成员变更、消息撤回等系统通知。
 * 提供格式化通知文本的方法，子类需要实现具体的格式化逻辑。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public abstract class NotificationMessageContent extends MessageContent {
    /**
     * 是否是自己发送的
     * <p>
     * 用户可以不用设置这个值，client会自动置上
     * </p>
     */
    public boolean fromSelf;

    public abstract String formatNotification(Message message);

    @Override
    public String digest(Message message) {
        return formatNotification(message);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte(this.fromSelf ? (byte) 1 : (byte) 0);
    }

    public NotificationMessageContent() {
    }

    protected NotificationMessageContent(Parcel in) {
        super(in);
        this.fromSelf = in.readByte() != 0;
    }
}
