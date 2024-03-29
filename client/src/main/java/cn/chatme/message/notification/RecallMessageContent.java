/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.chatme.message.notification;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import cn.chatme.message.Message;
import cn.chatme.message.core.ContentTag;
import cn.chatme.message.core.MessageContentType;
import cn.chatme.message.core.MessagePayload;
import cn.chatme.message.core.PersistFlag;
import cn.chatme.model.Conversation;
import cn.chatme.remote.ChatManager;

/**
 * Created by heavyrain lee on 2017/12/6.
 */

@ContentTag(type = MessageContentType.ContentType_Recall, flag = PersistFlag.Persist)
public class RecallMessageContent extends NotificationMessageContent {
    private String operatorId;
    private long messageUid;

    private String originalSender;
    private int originalContentType;
    private String originalSearchableContent;
    private String originalContent;
    private String originalExtra;
    private long originalMessageTimestamp;

    public RecallMessageContent() {
    }

    public RecallMessageContent(String operatorId, long messageUid) {
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

        if (!TextUtils.isEmpty(payload.extra)) {
            try {
                JSONObject obj = new JSONObject(payload.extra);
                this.originalSender = obj.optString("s");
                this.originalContentType = obj.optInt("t");
                this.originalSearchableContent = obj.optString("sc");
                this.originalContent = obj.optString("c");
                this.originalExtra = obj.optString("e");
                this.originalMessageTimestamp = obj.optLong("ts");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOriginalSender() {
        return originalSender;
    }

    public int getOriginalContentType() {
        return originalContentType;
    }

    public String getOriginalSearchableContent() {
        return originalSearchableContent;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public String getOriginalExtra() {
        return originalExtra;
    }

    public long getOriginalMessageTimestamp() {
        return originalMessageTimestamp;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    public void setOriginalSender(String originalSender) {
        this.originalSender = originalSender;
    }

    public void setOriginalContentType(int originalContentType) {
        this.originalContentType = originalContentType;
    }

    public void setOriginalSearchableContent(String originalSearchableContent) {
        this.originalSearchableContent = originalSearchableContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public void setOriginalExtra(String originalExtra) {
        this.originalExtra = originalExtra;
    }

    public void setOriginalMessageTimestamp(long originalMessageTimestamp) {
        this.originalMessageTimestamp = originalMessageTimestamp;
    }

    @Override
    public String formatNotification(Message message) {
        String notification = "%s撤回了一条消息";
        if (fromSelf) {
            notification = String.format(notification, "您");
        } else {
            String displayName;
            if (message.conversation.type == Conversation.ConversationType.Group) {
                displayName = ChatManager.Instance().getGroupMemberDisplayName(message.conversation.target, operatorId);
            } else {
                displayName = ChatManager.Instance().getUserDisplayName(operatorId);
            }
            notification = String.format(notification, displayName);
        }
        return notification;
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
        dest.writeString(this.originalSender);
        dest.writeInt(this.originalContentType);
        dest.writeString(this.originalSearchableContent);
        dest.writeString(this.originalContent);
        dest.writeString(this.originalExtra);
        dest.writeLong(this.originalMessageTimestamp);
    }

    protected RecallMessageContent(Parcel in) {
        super(in);
        this.operatorId = in.readString();
        this.messageUid = in.readLong();
        this.originalSender = in.readString();
        this.originalContentType = in.readInt();
        this.originalSearchableContent = in.readString();
        this.originalContent = in.readString();
        this.originalExtra = in.readString();
        this.originalMessageTimestamp = in.readLong();
    }

    public static final Parcelable.Creator<RecallMessageContent> CREATOR = new Parcelable.Creator<RecallMessageContent>() {
        @Override
        public RecallMessageContent createFromParcel(Parcel source) {
            return new RecallMessageContent(source);
        }

        @Override
        public RecallMessageContent[] newArray(int size) {
            return new RecallMessageContent[size];
        }
    };
}
