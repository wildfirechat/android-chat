/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 投票选项
 */
public class PollOption {
    private long optionId;
    private String optionText;
    private int sortOrder;
    private int voteCount;
    private int votePercent;

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

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public int getVotePercent() {
        return votePercent;
    }

    public void setVotePercent(int votePercent) {
        this.votePercent = votePercent;
    }

    public static PollOption fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        PollOption option = new PollOption();
        option.optionId = jsonObject.optLong("id");
        option.optionText = jsonObject.optString("optionText", "");
        option.sortOrder = jsonObject.optInt("sortOrder", 0);
        option.voteCount = jsonObject.optInt("voteCount", 0);
        option.votePercent = jsonObject.optInt("votePercent", 0);
        return option;
    }
}
