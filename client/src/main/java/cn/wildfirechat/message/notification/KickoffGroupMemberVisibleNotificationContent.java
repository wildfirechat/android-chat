/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_KICKOF_GROUP_MEMBER_VISIBLE;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_KICKOF_GROUP_MEMBER_VISIBLE, flag = PersistFlag.Persist)
public class KickoffGroupMemberVisibleNotificationContent extends GroupNotificationMessageContent {
    public String operator;
    public List<String> kickedMembers;

    public KickoffGroupMemberVisibleNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("您把");
        } else {
            sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, operator));
            sb.append("把");
        }

        if (kickedMembers != null) {
            for (int i = 0; i < kickedMembers.size() && i < 4; i++) {
                sb.append(" ");
                sb.append(ChatManager.Instance().getGroupMemberDisplayName(groupId, kickedMembers.get(i)));
            }
            if (kickedMembers.size() > 4) {
                sb.append(" 等");
            }
        }

        sb.append(" 移出了群组");
        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operator);
            JSONArray objArray = new JSONArray();
            for (int i = 0; i < kickedMembers.size(); i++) {
                objArray.put(i, kickedMembers.get(i));
            }
            objWrite.put("ms", objArray);

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
                JSONArray jsonArray = jsonObject.optJSONArray("ms");
                kickedMembers = new ArrayList<>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        kickedMembers.add(jsonArray.getString(i));
                    }
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
        dest.writeString(this.operator);
        dest.writeStringList(this.kickedMembers);
    }

    protected KickoffGroupMemberVisibleNotificationContent(Parcel in) {
        super(in);
        this.operator = in.readString();
        this.kickedMembers = in.createStringArrayList();
    }

    public static final Creator<KickoffGroupMemberVisibleNotificationContent> CREATOR = new Creator<KickoffGroupMemberVisibleNotificationContent>() {
        @Override
        public KickoffGroupMemberVisibleNotificationContent createFromParcel(Parcel source) {
            return new KickoffGroupMemberVisibleNotificationContent(source);
        }

        @Override
        public KickoffGroupMemberVisibleNotificationContent[] newArray(int size) {
            return new KickoffGroupMemberVisibleNotificationContent[size];
        }
    };
}
