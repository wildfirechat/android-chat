/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_ADD_GROUP_MEMBER;

import android.content.Context;
import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.client.GroupMemberSource;
import cn.wildfirechat.client.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.remote.ChatManager;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_ADD_GROUP_MEMBER, flag = PersistFlag.Persist)
public class AddGroupMemberNotificationContent extends GroupNotificationMessageContent {
    public String invitor;
    public List<String> invitees;

    public AddGroupMemberNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        Context context = ChatManager.Instance().getApplicationContext();
        GroupMemberSource source = GroupMemberSource.getGroupMemberSource(this.extra);
        if (source.type == GroupMemberSource.Type_QRCode && this.invitees.size() == 1) {
            // 为了支持国际化，故strings.xml
            return context.getString(R.string.join_group_by_qrcode, getGroupMemberDisplayName(this.invitees.get(0)), getGroupMemberDisplayName(source.targetId));
        } else if (source.type == GroupMemberSource.Type_Card && this.invitees.size() == 1) {
            return context.getString(R.string.join_group_by_card, getGroupMemberDisplayName(this.invitees.get(0)), getGroupMemberDisplayName(source.targetId));
        }

        if (invitees.size() == 1 && invitees.get(0).equals(invitor)) {
            sb.append(getGroupMemberDisplayName(invitor));
            sb.append(" 加入了群聊");
            return sb.toString();
        }
        sb.append(getGroupMemberDisplayName(invitor));
        sb.append(" 邀请");

        if (invitees != null) {
            for (int i = 0; i < invitees.size() && i < 4; i++) {
                sb.append(" ");
                sb.append(getGroupMemberDisplayName(invitees.get(i)));
            }
            if (invitees.size() > 4) {
                sb.append(" 等");
            }
        }

        sb.append(" 加入了群聊");
        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", invitor);
            JSONArray objArray = new JSONArray();
            for (int i = 0; i < invitees.size(); i++) {
                objArray.put(i, invitees.get(i));
            }
            objWrite.put("ms", objArray);

            payload.binaryContent = objWrite.toString().getBytes();
            return payload;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                invitor = jsonObject.optString("o");
                groupId = jsonObject.optString("g");
                JSONArray jsonArray = jsonObject.optJSONArray("ms");
                invitees = new ArrayList<>();
                if (jsonArray != null) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        invitees.add(jsonArray.getString(i));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getGroupMemberDisplayName(String userId) {
        if (TextUtils.equals(ChatManager.Instance().getUserId(), userId)) {
            return "你";
        }
        return ChatManager.Instance().getGroupMemberDisplayName(this.groupId, userId);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.invitor);
        dest.writeStringList(this.invitees);
    }

    protected AddGroupMemberNotificationContent(Parcel in) {
        super(in);
        this.invitor = in.readString();
        this.invitees = in.createStringArrayList();
    }

    public static final Creator<AddGroupMemberNotificationContent> CREATOR = new Creator<AddGroupMemberNotificationContent>() {
        @Override
        public AddGroupMemberNotificationContent createFromParcel(Parcel source) {
            return new AddGroupMemberNotificationContent(source);
        }

        @Override
        public AddGroupMemberNotificationContent[] newArray(int size) {
            return new AddGroupMemberNotificationContent[size];
        }
    };
}
