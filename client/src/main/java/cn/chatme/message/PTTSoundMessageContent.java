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

@ContentTag(type = MessageContentType.ContentType_Ptt_Voice, flag = PersistFlag.Persist_And_Count)
public class PTTSoundMessageContent extends SoundMessageContent {

    public PTTSoundMessageContent() {
        super();
    }

    public PTTSoundMessageContent(String audioPath) {
        super(audioPath);
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "[对讲语音]";

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
    public String digest(Message message) {
        return "[对讲语音]";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
    }

    public void readFromParcel(Parcel source) {
    }

    protected PTTSoundMessageContent(Parcel in) {
        super(in);
    }

    public static final Creator<PTTSoundMessageContent> CREATOR = new Creator<PTTSoundMessageContent>() {
        @Override
        public PTTSoundMessageContent createFromParcel(Parcel source) {
            return new PTTSoundMessageContent(source);
        }

        @Override
        public PTTSoundMessageContent[] newArray(int size) {
            return new PTTSoundMessageContent[size];
        }
    };
}
