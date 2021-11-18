/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.QuoteInfo;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Mark_Unread_Sync;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Text;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Mark_Unread_Sync, flag = PersistFlag.No_Persist)
public class MarkUnreadMessageContent extends MessageContent {
    private long messageUid;

    public MarkUnreadMessageContent() {
    }

    public MarkUnreadMessageContent(long messageUid) {
        this.messageUid = messageUid;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("u", messageUid);
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
    }

    protected MarkUnreadMessageContent(Parcel in) {
        super(in);
        this.messageUid = in.readLong();
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
