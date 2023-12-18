/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_MODIFY_GROUP_EXTRA;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_MODIFY_GROUP_SETTINGS;

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

@ContentTag(type = ContentType_MODIFY_GROUP_SETTINGS, flag = PersistFlag.No_Persist)
public class ModifyGroupSettingsNotificationContent extends GroupNotificationMessageContent {
    public String operateUser;
    //修改设置类型。7为修改是否查看历史消息；8为修改群最大成员数，9为修改是否为超级群
    public int type;
    //修改后的值
    public int value;

    public ModifyGroupSettingsNotificationContent() {
    }

    @Override
    public String formatNotification(Message message) {
        return "";
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                groupId = jsonObject.optString("g");
                operateUser = jsonObject.optString("o");
                type = Integer.parseInt(jsonObject.optString("n"));
                value = Integer.parseInt(jsonObject.getString("m"));
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
        dest.writeInt(this.type);
        dest.writeInt(this.value);
    }

    protected ModifyGroupSettingsNotificationContent(Parcel in) {
        super(in);
        this.operateUser = in.readString();
        this.type = in.readInt();
        this.value = in.readInt();
    }

    public static final Creator<ModifyGroupSettingsNotificationContent> CREATOR = new Creator<ModifyGroupSettingsNotificationContent>() {
        @Override
        public ModifyGroupSettingsNotificationContent createFromParcel(Parcel source) {
            return new ModifyGroupSettingsNotificationContent(source);
        }

        @Override
        public ModifyGroupSettingsNotificationContent[] newArray(int size) {
            return new ModifyGroupSettingsNotificationContent[size];
        }
    };
}
