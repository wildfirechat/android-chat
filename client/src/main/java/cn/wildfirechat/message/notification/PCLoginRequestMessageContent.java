/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * PC端登录请求消息内容
 * <p>
 * 当PC端请求登录时发送的消息。
 * 包含平台类型和会话ID，用于多端登录验证。
 * 此消息不会被持久化。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = MessageContentType.ContentType_PC_LOGIN_REQUSET, flag = PersistFlag.No_Persist)
public class PCLoginRequestMessageContent extends MessageContent {
    /**
     * 平台类型（3-Windows，4-macOS，5-Web）
     */
    private int platform;

    /**
     * 会话ID
     */
    private String sessionId;

    public int getPlatform() {
        return platform;
    }

    public void setPlatform(int platform) {
        this.platform = platform;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public MessagePayload encode() {
        // never
        return null;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            JSONObject obj = new JSONObject(new String(payload.binaryContent));
            platform = obj.optInt("p");
            sessionId = obj.optString("t");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return null;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.platform);
        dest.writeString(this.sessionId);
    }

    public PCLoginRequestMessageContent() {
    }

    protected PCLoginRequestMessageContent(Parcel in) {
        super(in);
        this.platform = in.readInt();
        this.sessionId = in.readString();
    }

    public static final Creator<PCLoginRequestMessageContent> CREATOR = new Creator<PCLoginRequestMessageContent>() {
        @Override
        public PCLoginRequestMessageContent createFromParcel(Parcel source) {
            return new PCLoginRequestMessageContent(source);
        }

        @Override
        public PCLoginRequestMessageContent[] newArray(int size) {
            return new PCLoginRequestMessageContent[size];
        }
    };
}
