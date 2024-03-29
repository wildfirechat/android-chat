/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message.notification;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import cn.chatme.message.Message;
import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;
import cn.chatme.remote.ChatManager;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = MessageContentType.ContentType_QUIT_GROUP_VISIABLE, flag = PersistFlag.Persist)
public class QuitGroupVisibleNotificationContent extends GroupNotificationMessageContent {
    public String operator;

    public QuitGroupVisibleNotificationContent() {
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

    protected QuitGroupVisibleNotificationContent(Parcel in) {
        super(in);
        this.operator = in.readString();
    }

    public static final Parcelable.Creator<QuitGroupVisibleNotificationContent> CREATOR = new Parcelable.Creator<QuitGroupVisibleNotificationContent>() {
        @Override
        public QuitGroupVisibleNotificationContent createFromParcel(Parcel source) {
            return new QuitGroupVisibleNotificationContent(source);
        }

        @Override
        public QuitGroupVisibleNotificationContent[] newArray(int size) {
            return new QuitGroupVisibleNotificationContent[size];
        }
    };
}
