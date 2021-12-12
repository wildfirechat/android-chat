/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_MODIFY_GROUP_MEMBER_EXTRA;

import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_MODIFY_GROUP_MEMBER_EXTRA, flag = PersistFlag.No_Persist)
public class ModifyGroupMemberExtraNotificationContent extends GroupNotificationMessageContent {
    public String operateUser;
    public String groupMemberExtra;
    public String memberId;

    public ModifyGroupMemberExtraNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("你");
        } else {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(operateUser, groupId, false);
            if (!TextUtils.isEmpty(memberId) && !TextUtils.isEmpty(userInfo.groupAlias)) {
                sb.append(userInfo.groupAlias);
            } else if (!TextUtils.isEmpty(userInfo.friendAlias)) {
                sb.append(userInfo.friendAlias);
            } else if (!TextUtils.isEmpty(userInfo.displayName)) {
                sb.append(userInfo.displayName);
            } else {
                sb.append(operateUser);
            }
        }
        sb.append("修改");
        if (!TextUtils.isEmpty(memberId)) {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(memberId, false);
            if (!TextUtils.isEmpty(userInfo.friendAlias)) {
                sb.append(userInfo.friendAlias);
            } else if (!TextUtils.isEmpty(userInfo.displayName)) {
                sb.append(userInfo.displayName);
            } else {
                sb.append(memberId);
            }
            sb.append("的");
        }
        sb.append("群成员附加信息为");
        sb.append(groupMemberExtra);

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operateUser);
            objWrite.put("n", groupMemberExtra);
            if (!TextUtils.isEmpty(memberId)) {
                objWrite.put("m", memberId);
            }

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
                groupMemberExtra = jsonObject.optString("n");
                memberId = jsonObject.optString("m");
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
        dest.writeString(this.groupMemberExtra);
        dest.writeString(this.memberId != null ? this.memberId : "");
    }

    protected ModifyGroupMemberExtraNotificationContent(Parcel in) {
        super(in);
        this.operateUser = in.readString();
        this.groupMemberExtra = in.readString();
        this.memberId = in.readString();
    }

    public static final Creator<ModifyGroupMemberExtraNotificationContent> CREATOR = new Creator<ModifyGroupMemberExtraNotificationContent>() {
        @Override
        public ModifyGroupMemberExtraNotificationContent createFromParcel(Parcel source) {
            return new ModifyGroupMemberExtraNotificationContent(source);
        }

        @Override
        public ModifyGroupMemberExtraNotificationContent[] newArray(int size) {
            return new ModifyGroupMemberExtraNotificationContent[size];
        }
    };
}
