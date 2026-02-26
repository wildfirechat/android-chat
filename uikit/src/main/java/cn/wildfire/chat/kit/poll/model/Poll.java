/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.remote.ChatManager;

/**
 * 投票数据模型
 */
public class Poll {
    private long pollId;
    private String groupId;
    private String creatorId;
    private String title;
    private String desc;
    private int visibility;      // 1=仅群内, 2=公开
    private int type;            // 1=单选, 2=多选
    private int maxSelect;       // 多选时最多选几项
    private int anonymous;       // 0=实名, 1=匿名
    private int status;          // 0=进行中, 1=已结束
    private long endTime;
    private int showResult;      // 0=投票前隐藏, 1=始终显示
    private long createdAt;
    private long updatedAt;

    // 当前用户相关
    private boolean hasVoted;
    private boolean isCreator;
    private List<Integer> myOptionIds;

    // 状态标记
    private boolean deleted;

    // 统计
    private int totalVotes;     // 总票数（多选时可能大于人数）
    private int voterCount;     // 投票人数（去重后的用户数）

    // 选项列表
    private List<PollOption> options;

    // 投票人详情（仅实名投票且是发起者）
    private List<PollVoterDetail> voterDetails;

    public long getPollId() {
        return pollId;
    }

    public void setPollId(long pollId) {
        this.pollId = pollId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getMaxSelect() {
        return maxSelect;
    }

    public void setMaxSelect(int maxSelect) {
        this.maxSelect = maxSelect;
    }

    public int getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(int anonymous) {
        this.anonymous = anonymous;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getShowResult() {
        return showResult;
    }

    public void setShowResult(int showResult) {
        this.showResult = showResult;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isHasVoted() {
        return hasVoted;
    }

    public void setHasVoted(boolean hasVoted) {
        this.hasVoted = hasVoted;
    }

    public boolean isCreator() {
        return isCreator;
    }

    public void setCreator(boolean creator) {
        isCreator = creator;
    }

    public List<Integer> getMyOptionIds() {
        return myOptionIds;
    }

    public void setMyOptionIds(List<Integer> myOptionIds) {
        this.myOptionIds = myOptionIds;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public int getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    public int getVoterCount() {
        return voterCount;
    }

    public void setVoterCount(int voterCount) {
        this.voterCount = voterCount;
    }

    public List<PollOption> getOptions() {
        return options;
    }

    public void setOptions(List<PollOption> options) {
        this.options = options;
    }

    public List<PollVoterDetail> getVoterDetails() {
        return voterDetails;
    }

    public void setVoterDetails(List<PollVoterDetail> voterDetails) {
        this.voterDetails = voterDetails;
    }

    /**
     * 是否显示结果
     * 与iOS端保持一致：投票已结束或已投票者可见
     */
    public boolean shouldShowResult() {
        // 投票已结束，所有人可见
        if (status == 1) return true;
        // 已投票者可见
        if (hasVoted) return true;
        return false;
    }

    /**
     * 是否已过期
     */
    public boolean isExpired() {
        if (endTime > 0) {
            return endTime < System.currentTimeMillis();
        }
        return false;
    }

    /**
     * 获取剩余时间文本
     */
    public String getRemainingTimeText() {
        // 投票已结束（手动关闭）
        if (status == 1) {
            return "已结束";
        }

        if (endTime <= 0) return null;

        long now = System.currentTimeMillis();
        long remaining = endTime - now;

        if (remaining <= 0) {
            return "已过期";
        }

        long minutes = remaining / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("还剩%d天", days);
        } else if (hours > 0) {
            return String.format("还剩%d小时", hours);
        } else {
            return String.format("还剩%d分钟", minutes);
        }
    }

    public static Poll fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        Poll poll = new Poll();
        poll.pollId = jsonObject.optLong("id");
        poll.groupId = jsonObject.optString("groupId", "");
        poll.creatorId = jsonObject.optString("creatorId", "");
        poll.title = jsonObject.optString("title", "");
        poll.desc = jsonObject.isNull("description") ? null : jsonObject.optString("description");
        poll.visibility = jsonObject.optInt("visibility", 1);
        poll.type = jsonObject.optInt("type", 1);
        poll.maxSelect = jsonObject.optInt("maxSelect", 1);
        poll.anonymous = jsonObject.optInt("anonymous", 0);
        poll.status = jsonObject.optInt("status", 0);
        poll.endTime = jsonObject.optLong("endTime", 0);
        poll.showResult = jsonObject.optInt("showResult", 0);
        poll.createdAt = jsonObject.optLong("createdAt", 0);
        poll.updatedAt = jsonObject.optLong("updatedAt", 0);
        poll.hasVoted = jsonObject.optBoolean("hasVoted", false);
        poll.isCreator = jsonObject.optBoolean("isCreator", false);
        poll.totalVotes = jsonObject.optInt("totalVotes", 0);
        poll.voterCount = jsonObject.optInt("voterCount", 0);
        poll.deleted = jsonObject.optBoolean("deleted", false);

        // 解析选项
        JSONArray optionsArray = jsonObject.optJSONArray("options");
        if (optionsArray != null) {
            List<PollOption> options = new ArrayList<>();
            for (int i = 0; i < optionsArray.length(); i++) {
                JSONObject optionJson = optionsArray.optJSONObject(i);
                PollOption option = PollOption.fromJson(optionJson);
                if (option != null) {
                    options.add(option);
                }
            }
            poll.options = options;
        }

        // 解析我选的选项
        JSONArray myOptionIdsArray = jsonObject.optJSONArray("myOptionIds");
        if (myOptionIdsArray != null) {
            List<Integer> myOptionIds = new ArrayList<>();
            for (int i = 0; i < myOptionIdsArray.length(); i++) {
                myOptionIds.add(myOptionIdsArray.optInt(i));
            }
            poll.myOptionIds = myOptionIds;
        }

        // 解析投票人详情
        JSONArray voterDetailsArray = jsonObject.optJSONArray("voterDetails");
        if (voterDetailsArray != null) {
            List<PollVoterDetail> voterDetails = new ArrayList<>();
            for (int i = 0; i < voterDetailsArray.length(); i++) {
                JSONObject detailJson = voterDetailsArray.optJSONObject(i);
                PollVoterDetail detail = PollVoterDetail.fromJson(detailJson);
                if (detail != null) {
                    voterDetails.add(detail);
                }
            }
            poll.voterDetails = voterDetails;
        }

        return poll;
    }
}
