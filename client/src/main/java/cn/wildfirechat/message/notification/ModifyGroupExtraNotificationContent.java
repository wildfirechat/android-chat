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

import static cn.wildfirechat.message.core.MessageContentType.ContentType_MODIFY_GROUP_EXTRA;

/**
 * Created by heavyrainlee on 20/12/2017.
 */

@ContentTag(type = ContentType_MODIFY_GROUP_EXTRA, flag = PersistFlag.No_Persist)
public class ModifyGroupExtraNotificationContent extends GroupNotificationMessageContent {
    public String operateUser;
    public String groupExtra;

    public ModifyGroupExtraNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        StringBuilder sb = new StringBuilder();
        if (fromSelf) {
            sb.append("你");
        } else {
            UserInfo userInfo = ChatManager.Instance().getUserInfo(operateUser, groupId, false);
            if (!TextUtils.isEmpty(userInfo.groupAlias)) {
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
        sb.append("群附加信息为");
        sb.append(groupExtra);

        return sb.toString();
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("g", groupId);
            objWrite.put("o", operateUser);
            objWrite.put("n", groupExtra);

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
                groupExtra = jsonObject.optString("n");
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
        dest.writeString(this.groupExtra);
    }

    protected ModifyGroupExtraNotificationContent(Parcel in) {
        super(in);
        this.operateUser = in.readString();
        this.groupExtra = in.readString();
    }

    public static final Creator<ModifyGroupExtraNotificationContent> CREATOR = new Creator<ModifyGroupExtraNotificationContent>() {
        @Override
        public ModifyGroupExtraNotificationContent createFromParcel(Parcel source) {
            return new ModifyGroupExtraNotificationContent(source);
        }

        @Override
        public ModifyGroupExtraNotificationContent[] newArray(int size) {
            return new ModifyGroupExtraNotificationContent[size];
        }
    };
}
