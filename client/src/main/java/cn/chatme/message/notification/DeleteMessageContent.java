/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message.notification;

import android.os.Parcel;

import cn.chatme.message.Message;
import cn.chatme.message.MessageContent;
import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Delete, flag = PersistFlag.No_Persist)
public class DeleteMessageContent extends MessageContent {
    private String operatorId;
    private long messageUid;

    public DeleteMessageContent() {
    }

    public DeleteMessageContent(String operatorId, long messageUid) {
        this.operatorId = operatorId;
        this.messageUid = messageUid;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.content = operatorId;
        payload.binaryContent = new StringBuffer().append(messageUid).toString().getBytes();
        return payload;
    }


    @Override
    public void decode(MessagePayload payload) {
        operatorId = payload.content;
        messageUid = Long.parseLong(new String(payload.binaryContent));
    }

    @Override
    public String digest(Message message) {
        return null;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.operatorId);
        dest.writeLong(this.messageUid);
    }

    protected DeleteMessageContent(Parcel in) {
        super(in);
        this.operatorId = in.readString();
        this.messageUid = in.readLong();
    }

    public static final Creator<DeleteMessageContent> CREATOR = new Creator<DeleteMessageContent>() {
        @Override
        public DeleteMessageContent createFromParcel(Parcel source) {
            return new DeleteMessageContent(source);
        }

        @Override
        public DeleteMessageContent[] newArray(int size) {
            return new DeleteMessageContent[size];
        }
    };
}
