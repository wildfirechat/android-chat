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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_QUIT_GROUP;

/**
 * 退出群组通知内容
 * <p>
 * 当群成员主动退出群组时发送的通知消息。
 * 包含退出成员的信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_QUIT_GROUP, flag = PersistFlag.Persist)
public class QuitGroupNotificationContent extends GroupNotificationMessageContent {
    /**
     * 退出群组的成员ID
     */
    public String operator;

    public QuitGroupNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您退出了群组 ");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
            sb.append("退出了群组 ");
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
            if (payload.content != null) {
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

    protected QuitGroupNotificationContent(Parcel in) {
        super(in);
        this.operator = in.readString();
    }

    public static final Creator<QuitGroupNotificationContent> CREATOR = new Creator<QuitGroupNotificationContent>() {
        @Override
        public QuitGroupNotificationContent createFromParcel(Parcel source) {
            return new QuitGroupNotificationContent(source);
        }

        @Override
        public QuitGroupNotificationContent[] newArray(int size) {
            return new QuitGroupNotificationContent[size];
        }
    };
}
