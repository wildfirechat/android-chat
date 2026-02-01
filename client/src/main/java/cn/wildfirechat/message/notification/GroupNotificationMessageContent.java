/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;

/**
 * 群组通知消息内容基类
 * <p>
 * 所有群组相关通知的基类，包含群组ID字段。
 * 继承自NotificationMessageContent，提供群组通知的通用功能。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public abstract class GroupNotificationMessageContent extends NotificationMessageContent {
    /**
     * 群组ID
     */
    public String groupId;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.groupId);
    }

    public GroupNotificationMessageContent() {
    }

    protected GroupNotificationMessageContent(Parcel in) {
        super(in);
        this.groupId = in.readString();
    }

}

