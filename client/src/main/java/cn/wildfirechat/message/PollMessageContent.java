/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

import static cn.wildfirechat.message.core.MessageContentType.ContentType_Poll;

/**
 * 投票消息内容类型
 * <p>
 * 用于表示群聊中的投票创建消息。
 * 消息类型: 18
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
@ContentTag(type = ContentType_Poll, flag = PersistFlag.Persist_And_Count)
public class PollMessageContent extends MessageContent {
    private String pollId;
    private String groupId;
    private String creatorId;
    private String title;
    private String desc;
    private int visibility;      // 1=仅群内, 2=公开
    private int type;            // 1=单选, 2=多选
    private int anonymous;       // 0=实名, 1=匿名
    private int status;          // 0=进行中, 1=已结束
    private long endTime;
    private int totalVotes;

    public PollMessageContent() {
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

    public int getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(int totalVotes) {
        this.totalVotes = totalVotes;
    }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = title;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("pollId", pollId != null ? pollId : "");
            jsonObject.put("groupId", groupId != null ? groupId : "");
            jsonObject.put("creatorId", creatorId != null ? creatorId : "");
            jsonObject.put("title", title != null ? title : "");
            if (desc != null) {
                jsonObject.put("desc", desc);
            }
            jsonObject.put("visibility", visibility);
            jsonObject.put("type", type);
            jsonObject.put("anonymous", anonymous);
            jsonObject.put("status", status);
            jsonObject.put("endTime", endTime);
            jsonObject.put("totalVotes", totalVotes);
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
                desc = jsonObject.optString("desc");
                visibility = jsonObject.optInt("visibility", 1);
                type = jsonObject.optInt("type", 1);
                anonymous = jsonObject.optInt("anonymous", 0);
                status = jsonObject.optInt("status", 0);
                endTime = jsonObject.optLong("endTime", 0);
                totalVotes = jsonObject.optInt("totalVotes", 0);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return "[投票] " + (title != null ? title : "");
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
        dest.writeString(this.desc);
        dest.writeInt(this.visibility);
        dest.writeInt(this.type);
        dest.writeInt(this.anonymous);
        dest.writeInt(this.status);
        dest.writeLong(this.endTime);
        dest.writeInt(this.totalVotes);
    }

    protected PollMessageContent(Parcel in) {
        super(in);
        this.pollId = in.readString();
        this.groupId = in.readString();
        this.creatorId = in.readString();
        this.title = in.readString();
        this.desc = in.readString();
        this.visibility = in.readInt();
        this.type = in.readInt();
        this.anonymous = in.readInt();
        this.status = in.readInt();
        this.endTime = in.readLong();
        this.totalVotes = in.readInt();
    }

    public static final Creator<PollMessageContent> CREATOR = new Creator<PollMessageContent>() {
        @Override
        public PollMessageContent createFromParcel(Parcel source) {
            return new PollMessageContent(source);
        }

        @Override
        public PollMessageContent[] newArray(int size) {
            return new PollMessageContent[size];
        }
    };
}
