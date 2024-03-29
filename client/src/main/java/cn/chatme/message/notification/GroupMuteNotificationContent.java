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

@ContentTag(type = MessageContentType.CONTENT_TYPE_CHANGE_MUTE, flag = PersistFlag.Persist)
public class GroupMuteNotificationContent extends GroupNotificationMessageContent {
    public String operator;

    //0 正常；1 全局禁言
    public int type;

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
        }
        if (type == 0) {
            sb.append("关闭了全员禁言");
        } else {
            sb.append("开启了全员禁言");
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
            objWrite.put("n", type + "");
            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                groupId = jsonObject.optString("g");
                operator = jsonObject.optString("o");
                type = Integer.parseInt(jsonObject.optString("n", "0"));
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
        dest.writeInt(this.type);
    }

    public GroupMuteNotificationContent() {
    }

    protected GroupMuteNotificationContent(Parcel in) {
        super(in);
        this.operator = in.readString();
        this.type = in.readInt();
    }

    public static final Parcelable.Creator<GroupMuteNotificationContent> CREATOR = new Parcelable.Creator<GroupMuteNotificationContent>() {
        @Override
        public GroupMuteNotificationContent createFromParcel(Parcel source) {
            return new GroupMuteNotificationContent(source);
        }

        @Override
        public GroupMuteNotificationContent[] newArray(int size) {
            return new GroupMuteNotificationContent[size];
        }
    };
}
