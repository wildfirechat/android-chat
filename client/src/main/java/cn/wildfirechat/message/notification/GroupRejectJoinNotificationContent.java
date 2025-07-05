/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.CONTENT_TYPE_ALLOW_MEMBER;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_REJECT_JOIN_GROUP;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

@ContentTag(type = ContentType_REJECT_JOIN_GROUP, flag = PersistFlag.Persist)
public class GroupRejectJoinNotificationContent extends NotificationMessageContent {
    public String groupId;
    public String operator;
    //int是拒绝的原因，1是黑名单，2是陌生人
    public Map<String, Integer> rejectUser;

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        for (String s : rejectUser.keySet()) {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, s));
            sb.append(" ");
        }
        sb.append("拒绝加入群组");

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operator);
            objWrite.put("mi", rejectUser);
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
                rejectUser = new HashMap<>();
                JSONObject mi = jsonObject.getJSONObject("mi");
                for (Iterator<String> it = mi.keys(); it.hasNext(); ) {
                    String user = it.next();
                    rejectUser.put(user, mi.getInt(user));
                }
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
        dest.writeString(this.groupId);
        dest.writeString(this.operator);
        dest.writeMap(this.rejectUser);
    }

    public GroupRejectJoinNotificationContent() {
    }

    protected GroupRejectJoinNotificationContent(Parcel in) {
        super(in);
        this.groupId = in.readString();
        this.operator = in.readString();
        this.rejectUser = in.readHashMap(this.getClass().getClassLoader());
    }

    public static final Creator<GroupRejectJoinNotificationContent> CREATOR = new Creator<GroupRejectJoinNotificationContent>() {
        @Override
        public GroupRejectJoinNotificationContent createFromParcel(Parcel source) {
            return new GroupRejectJoinNotificationContent(source);
        }

        @Override
        public GroupRejectJoinNotificationContent[] newArray(int size) {
            return new GroupRejectJoinNotificationContent[size];
        }
    };
}
