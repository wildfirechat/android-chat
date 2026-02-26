/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Poll_Result;

/**
 * 投票结果消息内容类型
 * <p>
 * 用于表示投票结束时的结果通知消息。
 * 消息类型: 19
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Poll_Result, flag = PersistFlag.Persist_And_Count)
public class PollResultMessageContent extends MessageContent {
    private String pollId;
    private String groupId;
    private String creatorId;
    private String title;
    private int totalVotes;
    private int voterCount;
    private List<String> winningOptionIds;
    private List<String> winningOptionTexts;
    private long endedAt;

    public PollResultMessageContent() {
    }

    public String getPollId() {
        return pollId;
    }

    public void setPollId(String pollId) {
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

    public List<String> getWinningOptionIds() {
        return winningOptionIds;
    }

    public void setWinningOptionIds(List<String> winningOptionIds) {
        this.winningOptionIds = winningOptionIds;
    }

    public List<String> getWinningOptionTexts() {
        return winningOptionTexts;
    }

    public void setWinningOptionTexts(List<String> winningOptionTexts) {
        this.winningOptionTexts = winningOptionTexts;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = "投票结果: " + title;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("pollId", pollId != null ? pollId : "");
            jsonObject.put("groupId", groupId != null ? groupId : "");
            jsonObject.put("creatorId", creatorId != null ? creatorId : "");
            jsonObject.put("title", title != null ? title : "");
            jsonObject.put("totalVotes", totalVotes);
            jsonObject.put("voterCount", voterCount);
            
            if (winningOptionIds != null) {
                JSONArray idsArray = new JSONArray();
                for (String id : winningOptionIds) {
                    idsArray.put(id);
                }
                jsonObject.put("winningOptionIds", idsArray);
            }
            
            if (winningOptionTexts != null) {
                JSONArray textsArray = new JSONArray();
                for (String text : winningOptionTexts) {
                    textsArray.put(text);
                }
                jsonObject.put("winningOptionTexts", textsArray);
            }
            
            jsonObject.put("endedAt", endedAt);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        payload.binaryContent = jsonObject.toString().getBytes();
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            if (payload.binaryContent != null && payload.binaryContent.length > 0) {
                JSONObject jsonObject = new JSONObject(new String(payload.binaryContent));
                pollId = jsonObject.optString("pollId");
                groupId = jsonObject.optString("groupId");
                creatorId = jsonObject.optString("creatorId");
                title = jsonObject.optString("title");
                totalVotes = jsonObject.optInt("totalVotes", 0);
                voterCount = jsonObject.optInt("voterCount", 0);
                endedAt = jsonObject.optLong("endedAt", 0);
                
                JSONArray idsArray = jsonObject.optJSONArray("winningOptionIds");
                if (idsArray != null) {
                    winningOptionIds = new ArrayList<>();
                    for (int i = 0; i < idsArray.length(); i++) {
                        winningOptionIds.add(idsArray.getString(i));
                    }
                }
                
                JSONArray textsArray = jsonObject.optJSONArray("winningOptionTexts");
                if (textsArray != null) {
                    winningOptionTexts = new ArrayList<>();
                    for (int i = 0; i < textsArray.length(); i++) {
                        winningOptionTexts.add(textsArray.getString(i));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[投票结果] " + (title != null ? title : "");
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.pollId);
        dest.writeString(this.groupId);
        dest.writeString(this.creatorId);
        dest.writeString(this.title);
        dest.writeInt(this.totalVotes);
        dest.writeInt(this.voterCount);
        dest.writeStringList(this.winningOptionIds);
        dest.writeStringList(this.winningOptionTexts);
        dest.writeLong(this.endedAt);
    }

    protected PollResultMessageContent(Parcel in) {
        super(in);
        this.pollId = in.readString();
        this.groupId = in.readString();
        this.creatorId = in.readString();
        this.title = in.readString();
        this.totalVotes = in.readInt();
        this.voterCount = in.readInt();
        this.winningOptionIds = in.createStringArrayList();
        this.winningOptionTexts = in.createStringArrayList();
        this.endedAt = in.readLong();
    }

    public static final Creator<PollResultMessageContent> CREATOR = new Creator<PollResultMessageContent>() {
        @Override
        public PollResultMessageContent createFromParcel(Parcel source) {
            return new PollResultMessageContent(source);
        }

        @Override
        public PollResultMessageContent[] newArray(int size) {
            return new PollResultMessageContent[size];
        }
    };
}
