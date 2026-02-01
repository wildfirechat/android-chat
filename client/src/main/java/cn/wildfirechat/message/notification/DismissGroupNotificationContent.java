/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_DISMISS_GROUP;

/**
 * 解散群组通知内容
 * <p>
 * 当群组被解散时发送的通知消息。
 * 包含操作者信息，通知所有群成员群组已被解散。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_DISMISS_GROUP, flag = PersistFlag.Persist)
public class DismissGroupNotificationContent extends GroupNotificationMessageContent {
    /**
     * 解散群组的操作者ID
     */
    public String operator;

    public DismissGroupNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您解散了群组 ");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
            sb.append("解散了群组 ");
        }

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operator);
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                groupId = jsonObject.optString("g");
                operator = jsonObject.optString("o");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.operator);
    }

    protected DismissGroupNotificationContent(Parcel in) {
        super(in);
        this.operator = in.readString();
    }

    public static final Creator<DismissGroupNotificationContent> CREATOR = new Creator<DismissGroupNotificationContent>() {
        @Override
        public DismissGroupNotificationContent createFromParcel(Parcel source) {
            return new DismissGroupNotificationContent(source);
        }

        @Override
        public DismissGroupNotificationContent[] newArray(int size) {
            return new DismissGroupNotificationContent[size];
        }
    };
}
