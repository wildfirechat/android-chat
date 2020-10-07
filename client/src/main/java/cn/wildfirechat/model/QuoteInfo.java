/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.remote.ChatManager;

public class QuoteInfo {
    private long messageUid;
    private String userId;
    private String userDisplayName;
    private String messageDigest;

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
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

    public static QuoteInfo init(long messageUid) {
        QuoteInfo info = new QuoteInfo();
        Message message = ChatManager.Instance().getMessageByUid(messageUid);
        if (message != null) {
            info.messageUid = messageUid;
            info.userId = message.sender;
            UserInfo userInfo = ChatManager.Instance().getUserInfo(message.sender, false);
            info.userDisplayName = userInfo.displayName;
            info.messageDigest = message.content.digest(message);
            if (info.messageDigest.length() > 48) {
                info.messageDigest = info.messageDigest.substring(0, 48);
            }
        }
        return info;
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
}
