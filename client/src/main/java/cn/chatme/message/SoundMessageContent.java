/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Voice, flag = PersistFlag.Persist_And_Count)
public class SoundMessageContent extends MediaMessageContent {
    protected int duration;

    public SoundMessageContent() {
        this.mediaType = MessageContentMediaType.VOICE;
    }

    public SoundMessageContent(String audioPath) {
        this.localPath = audioPath;
        this.mediaType = MessageContentMediaType.VOICE;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "[语音]";

        try {
            JSONObject objWrite = new JSONObject();
            objWrite.put("duration", duration);
            payload.content = objWrite.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            if (payload.content != null) {
                JSONObject jsonObject = new JSONObject(payload.content);
                duration = jsonObject.optInt("duration");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[语音]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(this.duration);
    }

    protected SoundMessageContent(Parcel in) {
        super(in);
        this.duration = in.readInt();
    }

    public static final Creator<SoundMessageContent> CREATOR = new Creator<SoundMessageContent>() {
        @Override
        public SoundMessageContent createFromParcel(Parcel source) {
            return new SoundMessageContent(source);
        }

        @Override
        public SoundMessageContent[] newArray(int size) {
            return new SoundMessageContent[size];
        }
    };
}
