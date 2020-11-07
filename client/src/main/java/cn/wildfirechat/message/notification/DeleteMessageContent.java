/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Delete;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = ContentType_Delete, flag = PersistFlag.No_Persist)
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
}
