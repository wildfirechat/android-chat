/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Ptt_Invite;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Ptt_Invite, flag = PersistFlag.Persist_And_Count)
public class PttInviteMessageContent extends MessageContent {
    private String callId;
    private String host;
    private String title;
    private String desc;
    private String pin;


    public PttInviteMessageContent() {
    }

    public PttInviteMessageContent(String callId, String host, String title, String desc, String pin) {
        this.callId = callId;
        this.host = host;
        this.title = title;
        this.desc = desc;
        this.pin = pin;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = callId;

        try {
            JSONObject objWrite = new JSONObject();

            if (host != null) {
                objWrite.put("h", host);
            }

            if (title != null) {
                objWrite.put("t", title);
            }

            if (desc != null) {
                objWrite.put("d", desc);
            }

            objWrite.put("p", pin);

            payload.binaryContent = objWrite.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        callId = payload.content;

        try {
            if (payload.binaryContent != null) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                host = jsonObject.optString("h");
                title = jsonObject.optString("t");
                desc = jsonObject.optString("d");
                pin = jsonObject.optString("p");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[对讲]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(callId);
        dest.writeString(host != null ? host : "");
        dest.writeString(title != null ? title : "");
        dest.writeString(desc != null ? desc : "");
        dest.writeString(pin != null ? pin : "");
    }

    protected PttInviteMessageContent(Parcel in) {
        super(in);
        callId = in.readString();
        host = in.readString();
        title = in.readString();
        desc = in.readString();

        pin = in.readString();
    }

    public static final Creator<PttInviteMessageContent> CREATOR = new Creator<PttInviteMessageContent>() {
        @Override
        public PttInviteMessageContent createFromParcel(Parcel source) {
            return new PttInviteMessageContent(source);
        }

        @Override
        public PttInviteMessageContent[] newArray(int size) {
            return new PttInviteMessageContent[size];
        }
    };
}
