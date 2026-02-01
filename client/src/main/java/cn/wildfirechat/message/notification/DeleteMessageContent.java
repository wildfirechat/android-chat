/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message.notification;

import android.os.Parcel;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Delete;
import static cn.wildfirechat.message.core.MessageContentType.ContentType_Recall;

/**
 * 删除消息内容
 * <p>
 * 当消息被删除时发送的通知消息。
 * 包含删除操作者和被删除消息的UID信息。
 * 此消息不会被持久化。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Delete, flag = PersistFlag.No_Persist)
public class DeleteMessageContent extends MessageContent {
    /**
     * 删除消息的操作者ID
     */
    private String operatorId;

    /**
     * 被删除消息的UID
     */
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
        super.decode(payload);
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
