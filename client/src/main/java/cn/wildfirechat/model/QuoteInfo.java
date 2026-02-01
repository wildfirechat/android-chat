/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.remote.ChatManager;

/**
 * 引用消息信息类
 * <p>
 * 用于消息回复功能，存储被引用消息的基本信息。
 * 包含消息UID、发送者、显示名称和消息摘要等信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class QuoteInfo implements Parcelable {
    /**
     * 被引用消息的UID
     */
    private long messageUid;

    /**
     * 被引用的消息对象（本地使用）
     */
    private Message message;

    /**
     * 消息发送者ID
     */
    private String userId;

    /**
     * 消息发送者显示名称
     */
    private String userDisplayName;

    /**
     * 消息摘要
     */
    private String messageDigest;

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Message getMessage() {
        return this.message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public String getMessageDigest() {
        return messageDigest;
    }

    public void setMessageDigest(String messageDigest) {
        this.messageDigest = messageDigest;
    }

    public static QuoteInfo initWithMessage(Message message) {
        QuoteInfo info = new QuoteInfo();
        info.message = message;
        if (message != null) {
            info.messageUid = message.messageUid;
            info.userId = message.sender;
            UserInfo userInfo;
            if (message.conversation.type == Conversation.ConversationType.Group) {
                userInfo = ChatManager.Instance().getUserInfo(message.sender, message.conversation.target, false);
                info.userDisplayName = ChatManager.Instance().getGroupMemberDisplayName(userInfo);
            } else {
                userInfo = ChatManager.Instance().getUserInfo(message.sender, false);
                info.userDisplayName = userInfo.displayName;
            }
            info.messageDigest = message.content.digest(message);
            if (info.messageDigest.length() > 48) {
                info.messageDigest = info.messageDigest.substring(0, 48);
            }
        }
        return info;

    }

    public static QuoteInfo initWithMessage(long messageUid) {
        Message message = ChatManager.Instance().getMessageByUid(messageUid);
        return initWithMessage(message);
    }

    public JSONObject encode() {
        JSONObject object = new JSONObject();
        try {
            object.put("u", messageUid);
            object.put("i", userId);
            object.put("n", userDisplayName);
            object.put("d", messageDigest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    public void decode(JSONObject object) {
        messageUid = object.optLong("u");
        userId = object.optString("i");
        userDisplayName = object.optString("n");
        messageDigest = object.optString("d");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.messageUid);
        dest.writeString(this.userId);
        dest.writeString(this.userDisplayName);
        dest.writeString(this.messageDigest);
    }

    public QuoteInfo() {
    }

    protected QuoteInfo(Parcel in) {
        this.messageUid = in.readLong();
        this.userId = in.readString();
        this.userDisplayName = in.readString();
        this.messageDigest = in.readString();
    }

    public static final Parcelable.Creator<QuoteInfo> CREATOR = new Parcelable.Creator<QuoteInfo>() {
        @Override
        public QuoteInfo createFromParcel(Parcel source) {
            return new QuoteInfo(source);
        }

        @Override
        public QuoteInfo[] newArray(int size) {
            return new QuoteInfo[size];
        }
    };
}
