/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_MODIFY_GROUP_ALIAS;

/**
 * 修改群昵称通知内容
 * <p>
 * 当群成员的群昵称被修改时发送的通知消息。
 * 包含操作者、目标成员和新的群昵称信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_MODIFY_GROUP_ALIAS, flag = PersistFlag.Persist)
public class ModifyGroupAliasNotificationContent extends GroupNotificationMessageContent {
    /**
     * 修改群昵称的操作者ID
     */
    public String operateUser;

    /**
     * 新的群昵称
     */
    public String alias;

    /**
     * 被修改群昵称的成员ID
     */
    public String memberId;

    public ModifyGroupAliasNotificationContent() {
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
        sb.append("群昵称为");
        sb.append(alias);

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operateUser);
            objWrite.put("n", alias);
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
        super.decode(payload);
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                groupId = jsonObject.optString("g");
                operateUser = jsonObject.optString("o");
                alias = jsonObject.optString("n");
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
        dest.writeString(this.alias);
        dest.writeString(this.memberId != null ? this.memberId : "");
    }

    protected ModifyGroupAliasNotificationContent(Parcel in) {
        super(in);
        this.operateUser = in.readString();
        this.alias = in.readString();
        this.memberId = in.readString();
    }

    public static final Creator<ModifyGroupAliasNotificationContent> CREATOR = new Creator<ModifyGroupAliasNotificationContent>() {
        @Override
        public ModifyGroupAliasNotificationContent createFromParcel(Parcel source) {
            return new ModifyGroupAliasNotificationContent(source);
        }

        @Override
        public ModifyGroupAliasNotificationContent[] newArray(int size) {
            return new ModifyGroupAliasNotificationContent[size];
        }
    };
}
