/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Transcription;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 转换消息内容类（透传消息）
 */
@ContentTag(type = ContentType_Transcription, flag = PersistFlag.Transparent)
public class TranscriptionMessageContent extends MessageContent {
    private long transcriptionId;
    private String meetingId;
    private String userId;
    private long timestamp;
    private long duration;
    private String content;

    public TranscriptionMessageContent() {
    }

    public TranscriptionMessageContent(long transcriptionId, String meetingId, String userId, long timestamp, long duration, String content) {
        this.transcriptionId = transcriptionId;
        this.meetingId = meetingId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.duration = duration;
        this.content = content;
    }

    public long getTranscriptionId() {
        return transcriptionId;
    }

    public void setTranscriptionId(long transcriptionId) {
        this.transcriptionId = transcriptionId;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        JSONObject object = new JSONObject();
        try {
            if (transcriptionId > 0) {
                object.put("id", transcriptionId);
            }
            if (meetingId != null && !meetingId.isEmpty()) {
                object.put("meetingId", meetingId);
            }
            if (userId != null && !userId.isEmpty()) {
                object.put("userId", userId);
            }
            if (timestamp != 0) {
                object.put("timestamp", timestamp);
            }
            if (duration != 0) {
                object.put("duration", duration);
            }
            if (content != null && !content.isEmpty()) {
                object.put("content", content);
            }
            payload.content = object.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        if (payload.content != null && !payload.content.isEmpty()) {
            try {
                JSONObject object = new JSONObject(payload.content);
                transcriptionId = object.optLong("id");
                meetingId = object.optString("meetingId");
                userId = object.optString("userId");
                timestamp = object.optLong("timestamp");
                duration = object.optLong("duration");
                content = object.optString("content");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String digest(Message message) {
        return content;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeLong(this.transcriptionId);
        dest.writeString(this.meetingId);
        dest.writeString(this.userId);
        dest.writeLong(this.timestamp);
        dest.writeLong(this.duration);
        dest.writeString(this.content);
    }

    protected TranscriptionMessageContent(Parcel in) {
        super(in);
        this.transcriptionId = in.readLong();
        this.meetingId = in.readString();
        this.userId = in.readString();
        this.timestamp = in.readLong();
        this.duration = in.readLong();
        this.content = in.readString();
    }

    public static final Creator<TranscriptionMessageContent> CREATOR = new Creator<TranscriptionMessageContent>() {
        @Override
        public TranscriptionMessageContent createFromParcel(Parcel source) {
            return new TranscriptionMessageContent(source);
        }

        @Override
        public TranscriptionMessageContent[] newArray(int size) {
            return new TranscriptionMessageContent[size];
        }
    };
}
