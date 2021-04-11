/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.voip.conference.message;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Conference_Kickoff_Member;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Conference_Kickoff_Member, flag = PersistFlag.Transparent)
public class ConferenceKickoffMemberContent extends MessageContent {
    private String callId;


    public ConferenceKickoffMemberContent() {
    }

    public ConferenceKickoffMemberContent(String callId) {
        this.callId = callId;
    }

    public String getCallId() {
        return callId;
    }

    public void setCallId(String callId) {
        this.callId = callId;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = callId;
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        callId = payload.content;
    }

    @Override
    public String digest(Message message) {
        return "[网络电话]";
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.callId);
    }

    protected ConferenceKickoffMemberContent(Parcel in) {
        super(in);
        this.callId = in.readString();
    }

    public static final Creator<ConferenceKickoffMemberContent> CREATOR = new Creator<ConferenceKickoffMemberContent>() {
        @Override
        public ConferenceKickoffMemberContent createFromParcel(Parcel source) {
            return new ConferenceKickoffMemberContent(source);
        }

        @Override
        public ConferenceKickoffMemberContent[] newArray(int size) {
            return new ConferenceKickoffMemberContent[size];
        }
    };
}
