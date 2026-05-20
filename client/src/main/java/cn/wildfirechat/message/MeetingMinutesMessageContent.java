/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Meeting_Minutes;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 会议纪要消息内容类
 */
@ContentTag(type = ContentType_Meeting_Minutes, flag = PersistFlag.Persist_And_Count)
public class MeetingMinutesMessageContent extends MessageContent {
    private String text;
    private String title;
    private String meetingId;

    public MeetingMinutesMessageContent() {
    }

    public MeetingMinutesMessageContent(String text, String title, String meetingId) {
        this.text = text;
        this.title = title;
        this.meetingId = meetingId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = text;
        payload.content = title;
        if (meetingId != null && !meetingId.isEmpty()) {
            JSONObject object = new JSONObject();
            try {
                object.put("meetingId", meetingId);
                payload.binaryContent = object.toString().getBytes();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        text = payload.searchableContent;
        title = payload.content;
        if (payload.binaryContent != null && payload.binaryContent.length > 0) {
            try {
                JSONObject object = new JSONObject(new String(payload.binaryContent));
                if (object.has("meetingId")) {
                    meetingId = object.optString("meetingId");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String digest(Message message) {
        if (title != null && !title.isEmpty()) {
            return title;
        }
        return text;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.text);
        dest.writeString(this.title);
        dest.writeString(this.meetingId);
    }

    protected MeetingMinutesMessageContent(Parcel in) {
        super(in);
        this.text = in.readString();
        this.title = in.readString();
        this.meetingId = in.readString();
    }

    public static final Creator<MeetingMinutesMessageContent> CREATOR = new Creator<MeetingMinutesMessageContent>() {
        @Override
        public MeetingMinutesMessageContent createFromParcel(Parcel source) {
            return new MeetingMinutesMessageContent(source);
        }

        @Override
        public MeetingMinutesMessageContent[] newArray(int size) {
            return new MeetingMinutesMessageContent[size];
        }
    };
}
