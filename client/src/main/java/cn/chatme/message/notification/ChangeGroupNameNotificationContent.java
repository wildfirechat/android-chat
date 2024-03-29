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

@ContentTag(type = MessageContentType.ContentType_CHANGE_GROUP_NAME, flag = PersistFlag.Persist)
public class ChangeGroupNameNotificationContent extends GroupNotificationMessageContent {
    public String operateUser;
    public String name;

    public ChangeGroupNameNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operateUser));
        }
        sb.append("修改群名为");
        sb.append(name);

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operateUser);
            objWrite.put("n", name);

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
                operateUser = jsonObject.optString("o");
                name = jsonObject.optString("n");
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
        dest.writeString(this.operateUser);
        dest.writeString(this.name);
    }

    protected ChangeGroupNameNotificationContent(Parcel in) {
        super(in);
        this.operateUser = in.readString();
        this.name = in.readString();
    }

    public static final Parcelable.Creator<ChangeGroupNameNotificationContent> CREATOR = new Parcelable.Creator<ChangeGroupNameNotificationContent>() {
        @Override
        public ChangeGroupNameNotificationContent createFromParcel(Parcel source) {
            return new ChangeGroupNameNotificationContent(source);
        }

        @Override
        public ChangeGroupNameNotificationContent[] newArray(int size) {
            return new ChangeGroupNameNotificationContent[size];
        }
    };
}
