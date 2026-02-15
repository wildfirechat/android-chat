/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 投票人详情
 */
public class PollVoterDetail {
    private long optionId;
    private String optionText;
    private String userId;
    private String userName;
    private long createdAt;

    public long getOptionId() {
        return optionId;
    }

    public void setOptionId(long optionId) {
        this.optionId = optionId;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public static PollVoterDetail fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        PollVoterDetail detail = new PollVoterDetail();
        detail.optionId = jsonObject.optLong("optionId");
        detail.optionText = jsonObject.optString("optionText", "");
        detail.userId = jsonObject.optString("userId", "");
        detail.userName = jsonObject.optString("userName", "");
        detail.createdAt = jsonObject.optLong("createdAt", 0);
        return detail;
    }
}
