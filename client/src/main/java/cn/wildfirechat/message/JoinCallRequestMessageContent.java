/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

@ContentTag(type = MessageContentType.ContentType_Call_Join_Call_Request, flag = PersistFlag.Transparent)
public class JoinCallRequestMessageContent extends MessageContent {
    private String callId;
    private String clientId;

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public JoinCallRequestMessageContent() {
    }

    public JoinCallRequestMessageContent(String callId, String clientId) {
        this.callId = callId;
        this.clientId = clientId;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = this.callId;
        JSONObject object = new JSONObject();
        try {
            object.put("clientId", clientId);
            payload.binaryContent = object.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        this.callId = payload.content;
        if (payload.binaryContent != null) {
            try {
                JSONObject object = new JSONObject(new String(payload.binaryContent));
                this.clientId = object.optString("clientId");
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
        dest.writeString(this.callId);
        dest.writeString(this.clientId);
    }

    public void readFromParcel(Parcel source) {
        this.callId = source.readString();
        this.clientId = source.readString();
    }

    protected JoinCallRequestMessageContent(Parcel in) {
        super(in);
        this.callId = in.readString();
        this.clientId = in.readString();
    }

    public static final Creator<JoinCallRequestMessageContent> CREATOR = new Creator<JoinCallRequestMessageContent>() {
        @Override
        public JoinCallRequestMessageContent createFromParcel(Parcel source) {
            return new JoinCallRequestMessageContent(source);
        }

        @Override
        public JoinCallRequestMessageContent[] newArray(int size) {
            return new JoinCallRequestMessageContent[size];
        }
    };
}
