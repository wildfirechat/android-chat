/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Mark_Unread_Sync;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Mark_Unread_Sync, flag = PersistFlag.No_Persist)
public class MarkUnreadMessageContent extends MessageContent {
    private long messageUid;
    private long timestamp;

    public MarkUnreadMessageContent() {
    }

    public MarkUnreadMessageContent(long messageUid, long timestamp) {
        this.messageUid = messageUid;
        this.timestamp = timestamp;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("u", messageUid);
            objWrite.put("t", timestamp);
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
                messageUid = jsonObject.optLong("u");
                timestamp = jsonObject.optLong("t");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(this.messageUid);
        dest.writeLong(this.timestamp);
    }

    protected MarkUnreadMessageContent(Parcel in) {
        super(in);
        this.messageUid = in.readLong();
        this.timestamp = in.readLong();
    }

    public static final Creator<MarkUnreadMessageContent> CREATOR = new Creator<MarkUnreadMessageContent>() {
        @Override
        public MarkUnreadMessageContent createFromParcel(Parcel source) {
            return new MarkUnreadMessageContent(source);
        }

        @Override
        public MarkUnreadMessageContent[] newArray(int size) {
            return new MarkUnreadMessageContent[size];
        }
    };
}
